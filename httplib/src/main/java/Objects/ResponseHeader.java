
package Objects;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static Constants.HTTPConstants.ENDOFLINE;
import static Constants.HTTPConstants.SPACE;

public class ResponseHeader {

  private StatusCode statusCode;
  private String connectionField;
  private String contentEncoding;
  private Charset charSet;
  private Integer contentLength;
  private String transferEncoding;
  private String headerText;
  private ContentType contentType;
  private HTTPVersion version;

  public ResponseHeader(List<String> headerTextList) {
    headerText = createHeaderText(headerTextList);
  }

  private String createHeaderText(List<String> headerTextList) {
    StringBuilder headerTextBuilder = new StringBuilder();

    // Extract information from the header text.
    for (String line : headerTextList) {

      // Build the header text
      headerTextBuilder.append(line);
      headerTextBuilder.append(ENDOFLINE);

      // extract information from the line.
      if (Pattern.matches("HTTP/.*", line.toUpperCase())) {
        String[] elements = line.split(SPACE);
        statusCode = StatusCode.getStatusCodeForInt(Integer.parseInt(elements[1]));

        String httpVersionLine = elements[0].trim();
        if (httpVersionLine.equals(HTTPVersion.HTTP_1_0.toString())) {
          version = HTTPVersion.HTTP_1_0;
        } else {
          version = HTTPVersion.HTTP_1_1;
        }
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
    headerTextBuilder.append(ENDOFLINE);
    return headerTextBuilder.toString();
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
      return StandardCharsets.ISO_8859_1;
    }
    return charSet;
  }

  public StatusCode getStatusCode() {
    return statusCode;
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


