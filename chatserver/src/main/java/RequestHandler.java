import Helpers.HTTPReader;
import Objects.ClientRequest;
import Objects.RequestHeader;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;

import static Helpers.HTTPReader.readHeader;

/**
 * A callable for handling incoming requests to the server.
 * Callable is used because it supports a return value.
 */
//TODO: make runnable (no return value)
public class RequestHandler implements Runnable {

  //The queue to take input from
  private BlockingQueue<Socket> clientSockets;
  private BlockingQueue<ClientRequestWithSocket> outputQueue;

  public RequestHandler(BlockingQueue<Socket> clientSockets, BlockingQueue<ClientRequestWithSocket> outputQueue) {
    this.clientSockets = clientSockets;
    this.outputQueue = outputQueue;
  }

  public void run() {
    try {
      // Process the request heder and add to the resultQueue.

      Socket clientSocket = clientSockets.poll();
      DataInputStream inFromClient = new DataInputStream(clientSocket.getInputStream());
      ClientRequest.Builder requestBuilder = ClientRequest.newBuilder();
      RequestHeader header = readRequestHeader(inFromClient);
      requestBuilder.setRequestHeader(header);
      if (header.RequestHasMessageBody()) {
        requestBuilder.setContent(retrieveContent(header, inFromClient));
      }
      ClientRequestWithSocket result = new ClientRequestWithSocket(requestBuilder.build(), clientSocket);
      outputQueue.add(result);
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private byte[] retrieveContent(RequestHeader header, DataInputStream inFromClient) throws IOException {
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
}
