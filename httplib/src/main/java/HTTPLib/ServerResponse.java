package HTTPLib;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

//TODO: Comments!!!!
public class ServerResponse {

  private ResponseHeader responseHeader;
  private byte[] content;

  public ServerResponse(ResponseHeader responseHeader, byte[] content) {
    this.responseHeader = responseHeader;
    this.content = content;
  }

  public ResponseHeader getResponseHeader() {
    return responseHeader;
  }

  public byte[] getContent() {
    return content;
  }

  public boolean isText() {
    return responseHeader.getContentType().isText();
  }

  public boolean isImage() {
    if (responseHeader.getContentType() == null){
      return false;
    }
    return responseHeader.getContentType().isImage();
  }

  public String getImageType() {
    return responseHeader.getContentType().getImageType();
  }

  public String getTextType() {
    return responseHeader.getContentType().getTextType();
  }

  public static class ResponseHeader {
    //Only keeps the header elements that are important for use
    //TODO: expand to all standard headers.
    private int returnCode;
    private String connectionField = null;
    private String contentEncoding = null;
    private Charset charSet = null;
    private Integer contentLength = null;
    private ContentType contentType = null;
    private String transferEncoding = null;
    private String headerText = null;


    public ResponseHeader(List<String> headerTextList) {
      StringBuilder headerTextBuilder = new StringBuilder();

      // Extract information from the header text.
      for (String line : headerTextList) {
        // Build the header text
        headerTextBuilder.append(line + '\n');

        // extract information from the line.
        if (Pattern.matches("HTTP/.*",line)){
          returnCode = Integer.parseInt(line.split(" ")[1]);
        }
        if (Pattern.matches("Transfer-Encoding:.*", line)) {
          transferEncoding = line.split(":")[1].trim();
        } else if (Pattern.matches("Content-Length:.*", line)) {
          // Replace all non-digits in the content line with an empty string.
          // Retrieve integer from this result.
          contentLength = Integer.parseInt(line.replaceAll("[\\D]", ""));
        } else if (Pattern.matches("Content-Encoding:.*", line)) {
          contentEncoding = line.split(":")[1].trim();
        } else if (Pattern.matches("Content-Type:.*", line)) {
          String[] contentTypeSplit = line.split(":")[1].trim().split(";");
          contentType = new ContentType(contentTypeSplit[0].trim());
          if (contentTypeSplit.length >= 2) {
            if (Pattern.matches("charset.*", contentTypeSplit[1].trim())) {
              charSet = findCharSet(contentTypeSplit[1].trim().split("=")[1]);
            }
          }
        } else if (Pattern.matches("Connection: .*", line)) {
          connectionField = line.split(":")[1].trim();
        }
      }
      headerText = headerTextBuilder.toString();
    }

    private class ContentType {

      private final String contentTypeText;
      private final String imageType;
      private final String textType;

      private ContentType(String text) {
        contentTypeText = text;
        if (Pattern.matches("text/.*", contentTypeText)) {
          textType = contentTypeText.split("/")[1].trim();
          imageType = null;
        } else if (Pattern.matches("image/.*", contentTypeText)) {
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

    public String getConnectionField() {
      return connectionField;
    }

    public String getContentEncoding() {
      return contentEncoding;
    }

    public Integer getContentLength() {
      return contentLength;
    }

    public ContentType getContentType() {
      return contentType;
    }

    public String getTransferEncoding() {
      return transferEncoding;
    }

    public String getHeaderText() {
      return headerText;
    }

    public Charset getCharSet() {
      if (charSet == null) {
        return StandardCharsets.UTF_8;
      }
      return charSet;
    }

    public int getReturnCode() {
      return returnCode;
    }

    private Charset findCharSet(String string) {
      switch (string.trim().toUpperCase()) {
        case "ISO-8859-1":
          return StandardCharsets.ISO_8859_1;
        case "UTF-8":
          return StandardCharsets.UTF_8;
        case "UTF-16":
          return StandardCharsets.UTF_16;
        case "UTF-16BE":
          return StandardCharsets.UTF_16BE;
        case "UTF-16LE":
          return StandardCharsets.UTF_16LE;
        case "US-ASCII":
          return StandardCharsets.US_ASCII;
        default:
          return StandardCharsets.UTF_8;
      }
    }
  }
}
