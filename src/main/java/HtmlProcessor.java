import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class HtmlProcessor {
  public static List<String> retrieveEmbeddedObjectLocations(String htmlPageContents) {
    List<String> result = new ArrayList<>();

    Document document = Jsoup.parse(htmlPageContents);
    Elements images = document.getElementsByTag("img");
    Elements metaElements = document.getElementsByTag("meta");

    for (Element imgElem : images) {
      result.add(imgElem.absUrl("src"));
    }
    for (Element metaElem : metaElements) {
      String itemProp = metaElem.attr("itemprop");
      if (Objects.equals(itemProp, "image")) {
        String objName = metaElem.attr("name");
        String objLoc = metaElem.attr("content");
        result.add(objLoc);
      }
    }
    return result;
  }

  public String retrieveHtml(String text){
    //TODO
    return text;
  }
}