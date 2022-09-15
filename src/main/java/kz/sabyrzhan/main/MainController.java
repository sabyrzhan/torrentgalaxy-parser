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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class MainController {
    private static final String savePath = "/Users/sabyrzhan/projects/torrentgalaxy-parser/saved.obj";

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
        }

        model.addAttribute("items", itemsContainer);

        return "main";
    }

    private static List<Item> readData() throws Exception {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(new File(savePath)));
        List<Item> items = (List<Item>) inputStream.readObject();
        return items;
    }

    private static void saveData() throws Exception {
        var urls = new ArrayList<String>();
        urls.add("https://torrentgalaxy.to/torrents.php?c33=1&page=0");
        urls.add("https://torrentgalaxy.to/torrents.php?c33=1&page=1");
        urls.add("https://torrentgalaxy.to/torrents.php?c33=1&page=2");
        urls.add("https://torrentgalaxy.to/torrents.php?c33=1&page=3");
        urls.add("https://torrentgalaxy.to/torrents.php?c33=1&page=4");
        urls.add("https://torrentgalaxy.to/torrents.php?c33=1&page=5");

        var itemsContainer = new ArrayList<Item>();

        for(var url : urls) {
            String content = IOUtils.toString(new URI(url), StandardCharsets.UTF_8);
            var doc = Jsoup.parse(content);
            var items = doc.selectXpath("//div[contains(@class, 'tgxtablerow') and contains(@class, 'txlight')]");


            for (var item : items) {
                var itemData = new Item();
                itemData.name = item.child(3).text();
                itemData.uploader = item.child(6).text();
                itemData.size = item.child(7).text();
                itemData.views = Integer.parseInt(item.child(9).text());
                var seedLeech = item.child(10).text().replace("[", "").replace("]", "").split("/");
                itemData.seeds = Integer.parseInt(seedLeech[0]);
                itemData.leeches = Integer.parseInt(seedLeech[1]);
                itemData.date = item.child(11).text();
                itemsContainer.add(itemData);
            }

            Thread.sleep(1_000);
            System.out.println("Finished: " + url);
        }

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(new File(savePath)));

        objectOutputStream.writeObject(itemsContainer);
    }
}
