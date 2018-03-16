package Processors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class HtmlProcessor {
  public static List<String> retrieveImageLocations(String htmlPageContents) {
    List<String> result = new ArrayList<>();

    Document document = Jsoup.parse(htmlPageContents);
    Elements images = document.getElementsByTag("img");
    Elements metaElements = document.getElementsByTag("meta");

    for (Element imgElem : images) {
      result.add(imgElem.attr("src"));
    }
    for (Element metaElem : metaElements) {
      String itemProp = metaElem.attr("itemprop");
      if (Objects.equals(itemProp, "image")) {
        String imgLoc = metaElem.attr("content");
        result.add(imgLoc);
      }
    }
    return result;
  }

  public static Charset retrieveCharSet(String htmlPageContents) {
    Document doc = Jsoup.parse(htmlPageContents);
    return doc.charset();
  }

  public static String MakeAllImgPathsRelativeInHtml(String htmlPageContents){
    Document document = Jsoup.parse(htmlPageContents);
    Elements images = document.getElementsByTag("img");
    Elements metaElements = document.getElementsByTag("meta");

    List<String> imgPaths = new ArrayList<>();
    for (Element imgElem : images) {
      String path = imgElem.attr("src");
      if (Pattern.matches("/.*",path)){
        imgElem.attr("src","./" + path);
      }
    }
    for (Element metaElem : metaElements) {
      String itemProp = metaElem.attr("itemprop");
      if (Objects.equals(itemProp, "image")) {
        String imgLoc = metaElem.attr("content");

        if (Pattern.matches("/.*", imgLoc)) {
          metaElem.attr("content", "."+imgLoc);
        }
      }
    }
    return document.html();
  }

  public String retrieveHtml(String text) {
    //TODO
    return text;
  }
}
