package Objects;

import java.util.List;
import java.util.regex.Pattern;

public class HTTPRequest {
  HTTPVersion version;
  HTTPCommand command;
  String path;
  int contentLength;
  String contentType;
  String host;

  public HTTPRequest(List<String> requestText) {
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

  public int getContentLength() {
    return contentLength;
  }

  public String getContentType() {
    return contentType;
  }

  public String getHost() {
    return host;
  }
}
