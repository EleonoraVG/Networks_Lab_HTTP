package Objects;

/**
 * Represents the response of a server.
 */
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

  public String getTextType() {
    if (responseHeader.getContentType() == null) {
      return "html";
    } else return responseHeader.getContentType().getTextType();
  }

  public boolean isText() {
    return responseHeader.getContentType() == null || responseHeader.getContentType().isText();
  }

  public boolean isImage() {
    if (responseHeader.getContentType() == null) {
      return false;
    }
    return responseHeader.getContentType() != null && responseHeader.getContentType().isImage();
  }
}
