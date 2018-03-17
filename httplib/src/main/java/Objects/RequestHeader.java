package Objects;

import java.util.List;
import java.util.regex.Pattern;

public class RequestHeader {
  HTTPVersion version;
  HTTPCommand command;
  String path;
  Integer contentLength;
  String contentType;
  private String transferEncoding = null;
  boolean shouldConnectionClose = false;
  String host;


  public boolean RequestHasMessageBody() {
    // The presence of a message-body in a request is signaled by the
    // inclusion of a Content-Length or Transfer-Encoding header field in
    // the request's message-headers.
    return (contentLength != null || transferEncoding != null);
  }

  public RequestHeader(List<String> requestText) {
    // Parse the lines
    for (String line : requestText) {
      if (Pattern.matches(".*HTTP/.*", line)) {
        String[] elements = line.split(" ");
        command = HTTPCommand.fromString(elements[0].trim());
        path = elements[1].trim();
        String httpVersionLine = elements[2].trim();
        if (httpVersionLine.equals(HTTPVersion.HTTP_1_0.toString())) {
          version = HTTPVersion.HTTP_1_0;
        } else {
          version = HTTPVersion.HTTP_1_1;
        }
      } else if (Pattern.matches("host:.*", line.toLowerCase())) {
        host = line.split(":")[1].trim();
      } else if (Pattern.matches("content-length", line.toLowerCase())) {
        contentLength = Integer.parseInt(line.split(":")[1].trim());
      } else if (Pattern.matches("content-type", line.toLowerCase())) {
        contentType = line.split(":")[1].trim();
      } else if (Pattern.matches("transfer-encoding:.*", line.toLowerCase())) {
        transferEncoding = line.split(":")[1].trim();
      } else if (Pattern.matches("connection:.*", line.toLowerCase())) {
        if (line.split(":")[1].trim().toLowerCase().equals("close")) {
          shouldConnectionClose = true;
        }
      }
    }
  }

  public HTTPVersion getVersion() {
    return version;
  }

  public HTTPCommand getCommand() {
    return command;
  }

  public String getPath() {
    return path;
  }

  public Integer getContentLength() {
    return contentLength;
  }

  public String getTransferEncoding() {
    return transferEncoding;
  }

  public String getContentType() {
    return contentType;
  }

  public String getHost() {
    return host;
  }

  public boolean isShouldConnectionClose() {
    return shouldConnectionClose;
  }
}
