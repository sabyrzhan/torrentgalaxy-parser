package kz.sabyrzhan.main;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    private static final String savePath = "/Users/sabyrzhan/projects/torrentgalaxy-parser/saved.obj";

    public static void main(String[] args) throws Exception {

//        if (true) {
//            saveData();
//            return;
//        }

        var itemsContainer = readData();

        var result = "<html><head><title>title</title></head><body><table border='1'>";
        result += "<tr>";
        result += "<td>Name</td>";
        result += "<td>Uploader</td>";
        result += "<td>Size</td>";
        result += "<td>Views</td>";
        result += "<td>Seeds</td>";
        result += "<td>Leeches</td>";
        result += "<td>Date</td>";
        result += "</tr>";

        Collections.sort(itemsContainer, (o1, o2) -> o2.seeds - o1.seeds);

        for(var itemData : itemsContainer) {
            result += "<tr>";
            result += "<td>" + itemData.name + "</td>"; //Name
            result += "<td>" + itemData.uploader + "</td>"; //Uploader
            result += "<td>" + itemData.size + "</td>"; //Size
            result += "<td>" + itemData.views + "</td>"; //Views
            result += "<td>" + itemData.seeds + "</td>"; //Seeds
            result += "<td>" + itemData.leeches + "</td>"; //Leeches
            result += "<td>" + itemData.date + "</td>"; //Date
            result += "</tr>";
        }

        result += "</table>";
        result += "</html>";

        FileUtils.write(new File("/Users/sabyrzhan/projects/torrentgalaxy-parser/out.html"), result, StandardCharsets.UTF_8);
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

    public static class Item implements Serializable {
        String name;
        String uploader;
        String size;
        int views;
        int seeds;
        int leeches;
        String date;
    }
}
