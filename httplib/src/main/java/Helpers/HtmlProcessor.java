package Helpers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A processor for html text.
 */
public class HtmlProcessor {

  String htmlText;
  Document document;

  HtmlProcessor(String htmlPageContents){
    htmlText = htmlPageContents;
    document = Jsoup.parse(htmlPageContents);
  }

  /**
   * Retrieve the location of images in the html page.
   * @return
   */
  public List<String> retrieveImageLocations() {
    List<String> result = new ArrayList<>();

    // Retrieve elements that indicate images
    Elements images = document.getElementsByTag("img");
    Elements metaElements = document.getElementsByTag("meta");

    // Add all links to an image to the result list
    images.forEach(elem -> result.add(elem.attr("src")));
    metaElements.stream()
            // Filter out the non image elements
            .filter(element -> element.attr("itemprop").trim().equals("image"))
            .forEach(element -> result.add(element.attr("content")));

    return result;
  }

  /**
   * Retrieve the characterset specified in the htmlPage.
   * @return
   */
  public Charset retrieveCharSet() {
    return document.charset();
  }

  /**
   * Change all links to images in the Html to a relative path.
   *
   * @return The HTML text with all image paths made relative.
   */
  public String MakeAllImgPathsRelativeInHtml(){
    Elements images = document.getElementsByTag("img");
    Elements metaElements = document.getElementsByTag("meta");

    // Change all paths to a relative path
    for (Element imgElem : images) {
      String path = imgElem.attr("src");
      if (Pattern.matches("/.*",path)){
        imgElem.attr("src","." + path);
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
}
