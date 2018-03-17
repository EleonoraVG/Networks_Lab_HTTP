import Helpers.HTTPReader;
import Objects.ClientRequest;
import Objects.RequestHeader;
import com.google.common.base.Preconditions;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

import static Helpers.HTTPReader.readHeader;

/**
 * A callable for handling incoming requests to the server.
 * Callable is used because it supports a return value.
 */
public class RequestHandler implements Callable<ClientRequest> {

  private DataInputStream inFromClient;

  public ClientRequest call() throws Exception {
    // TODO: Separate threads for handling requests and responding?
    Preconditions.checkNotNull(inFromClient);
    ClientRequest.Builder requestBuilder = ClientRequest.newBuilder();
    RequestHeader header = readRequestHeader(inFromClient);
    requestBuilder.setRequestHeader(header);
    if (header.RequestHasMessageBody()) {
      requestBuilder.setContent(retrieveContent(header));
    }
    return requestBuilder.build();
  }

  private byte[] retrieveContent(RequestHeader header) throws IOException {
    byte[] content = null;
    if (header.getContentLength() != null) {
      content = HTTPReader.processWithContentLength(inFromClient, header.getContentLength());
    } else if (header.getTransferEncoding() != null) {
      //Use transfer transcoding
      content = HTTPReader.readChunkFromServer(inFromClient, Charset.defaultCharset());
    }
    return content;
  }

  public static RequestHeader readRequestHeader(DataInputStream inFromClient) throws IOException {
    return new RequestHeader(readHeader(inFromClient));
  }

  public DataInputStream getInputStream() {
    return inFromClient;
  }

  public void setInputStream(DataInputStream inFromClient) {
    this.inFromClient = inFromClient;
  }
}
