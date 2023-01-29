package kz.sabyrzhan.main;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class MainController {
    @GetMapping("/")
    public String index(@RequestParam(required = false, defaultValue = "false") boolean reload, @RequestParam(required = false, defaultValue = "") String order, @RequestParam(required = false) boolean asc, Model model) throws Exception {
        if (reload) {
            saveData();
        }

        var itemsContainer = readData();

        if ("views".equals(order)) {
            if (asc) {
                Collections.sort(itemsContainer, (o1, o2) -> o1.views - o2.views);
            } else {
                Collections.sort(itemsContainer, (o1, o2) -> o2.views - o1.views);
            }
        } else if ("seeds".equals(order)) {
            if (asc) {
                Collections.sort(itemsContainer, (o1, o2) -> o1.seeds - o2.seeds);
            } else {
                Collections.sort(itemsContainer, (o1, o2) -> o2.seeds - o1.seeds);
            }
        } else if ("leeches".equals(order)) {
            if (asc) {
                Collections.sort(itemsContainer, (o1, o2) -> o1.leeches - o2.leeches);
            } else {
                Collections.sort(itemsContainer, (o1, o2) -> o2.leeches - o1.leeches);
            }
        } else if ("az".equals(order)) {
            if (asc) {
                Collections.sort(itemsContainer, Comparator.comparing(o -> o.name));
            } else {
                Collections.sort(itemsContainer, (o1, o2) -> o2.name.compareTo(o1.name));
            }
        }

        model.addAttribute("items", itemsContainer);

        return "main";
    }

    private static String getSavePath() {
        var savePath = System.getProperty("save_path").trim();
        if (savePath.isBlank()) {
            throw new RuntimeException("Save path not specified");
        }

        return savePath;
    }

    private static List<Item> readData() throws Exception {
        var savePath = getSavePath();
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(savePath));
        List<Item> items = (List<Item>) inputStream.readObject();
        return items;
    }

    private static List<String> readUrls() {
        var result = new ArrayList<String>();
        String path = System.getProperty("url_list_file").trim();
        if (path.isBlank()) {
            throw new RuntimeException("URL list path not specified");
        }

        try (var fis = new FileInputStream(path);
             var scanner = new Scanner(fis)) {
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                result.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static void saveData() throws Exception {
        var savePath = getSavePath();
        var urls = readUrls();

        var itemsContainer = new ArrayList<Item>();

        for(var url : urls) {
            String content = IOUtils.toString(new URI(url), StandardCharsets.UTF_8);
            var doc = Jsoup.parse(content);
            var items = doc.selectXpath("//div[contains(@class, 'tgxtablerow') and contains(@class, 'txlight')]");


            for (var item : items) {
                var itemData = new Item();
                itemData.name = item.child(3).text();
                String text = item.child(3).toString();
                Pattern pattern = Pattern.compile("href=\"(.*?)\"");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    var href = matcher.group();
                    var link = href.substring(href.indexOf("\"") + 1, href.lastIndexOf("\""));
                    itemData.link = link;
                }

                itemData.uploader = item.child(6).text();
                itemData.size = item.child(7).text();
                itemData.views = Integer.parseInt(item.child(9).text().replace(",", ""));
                var seedLeech = item.child(10).text().replace("[", "").replace("]", "").split("/");
                itemData.seeds = Integer.parseInt(seedLeech[0]);
                itemData.leeches = Integer.parseInt(seedLeech[1]);
                itemData.date = item.child(11).text();
                itemsContainer.add(itemData);
            }

            Thread.sleep(1_000);
            System.out.println("Finished: " + url);
        }

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(savePath));

        objectOutputStream.writeObject(itemsContainer);
    }
}
