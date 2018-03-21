package Objects;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ContentType {

  private final String contentTypeText;
  private final String imageType;
  private final String textType;

  public ContentType(String text) {
    contentTypeText = text;
    if (Pattern.matches("text/.*", contentTypeText.toLowerCase())) {
      textType = contentTypeText.split("/")[1].trim();
      imageType = null;
    } else if (Pattern.matches("image/.*", contentTypeText.toLowerCase())) {
      imageType = contentTypeText.split("/")[1].trim();
      textType = null;
    } else {
      imageType = null;
      textType = null;
    }
  }

  public boolean isImage() {
    return imageType != null;
  }

  public boolean isText() {
    return textType != null;
  }

  public String getContentTypeText() {
    return contentTypeText;
  }

  public String getImageType() {
    return imageType;
  }

  public String getTextType() {
    return textType;
  }
}

