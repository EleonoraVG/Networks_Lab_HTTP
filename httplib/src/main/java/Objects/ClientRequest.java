package Objects;

import com.google.common.base.Preconditions;

public class ClientRequest {

  private RequestHeader requestHeader;
  private byte[] content;

  //Use builder (because of optional content).

  private ClientRequest() {
  }

  public static ClientRequest.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    RequestHeader requestHeader;
    byte[] content = null;

    private Builder() {
    }

    public Builder setRequestHeader(RequestHeader requestHeader) {
      Preconditions.checkNotNull(requestHeader);
      this.requestHeader = requestHeader;
      return this;
    }

    public Builder setContent(byte[] content) {
      this.content = content;
      return this;
    }

    public ClientRequest build() {
      Preconditions.checkNotNull(requestHeader);
      ClientRequest request = new ClientRequest();
      request.requestHeader = requestHeader;
      request.content = content;
      return request;
    }
  }
}
