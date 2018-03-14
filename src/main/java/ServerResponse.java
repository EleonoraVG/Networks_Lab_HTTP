import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

public class ServerResponse {

  private ResponseHeader responseHeader;
  private String contentText;

  public ServerResponse(ResponseHeader responseHeader, String content) {
    this.responseHeader = responseHeader;
    //TODO: Encode content in the given character encoding if specified.
    //TODO: Receive the content in bytes
    contentText = content;
  }

  public ResponseHeader getResponseHeader() {
    return responseHeader;
  }

  public String getContentText() {
    return contentText;
  }

  public static class ResponseHeader {
    //Only keeps the header elements that are important for use
    //TODO: expand to all standard headers.
    private String connectionField = null;
    private String contentEncoding = null;
    private Charset charSet = null;
    private Integer contentLength = null;
    private String contentType = null;
    private String transferEncoding = null;
    private String headerText = null;

    public ResponseHeader(List<String> headerTextList) {
      StringBuilder headerTextBuilder = new StringBuilder();

      // Extract information from the header text.
      for (String line : headerTextList) {
        // Build the header text
        headerTextBuilder.append(line);

        // extract information from the line.
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
          contentType = contentTypeSplit[0].trim();
          if (contentTypeSplit.length >= 2) {
            if (Pattern.matches("charset.*", contentTypeSplit[2].trim())) {
              charSet = findCharSet(contentTypeSplit[1].trim().split("=")[1]);
            }
          }
        } else if (Pattern.matches("Connection: .*", line)) {
          connectionField = line.split(":")[1].trim();
        }
      }
      headerText = headerTextBuilder.toString();
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

    public String getContentType() {
      return contentType;
    }

    public String getTransferEncoding() {
      return transferEncoding;
    }

    public String getHeaderText() {
      return headerText;
    }

    public Charset getCharSet() {
      return charSet;
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
