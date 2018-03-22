import Constants.HTTPConstants;
import Objects.ClientRequest;
import Objects.HTTPVersion;
import Objects.ServerResponse;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * A runnable for writing a response to the client socket.
 */
public class RequestResponder implements Runnable {

  private ServerResponse serverResponse;
  private Socket clientSocket;
  private ClientRequest clientRequest;
  private ExecutorService threadPool;

  public RequestResponder(ServerResponse serverResponse, ClientRequest clientRequest, Socket socket, ExecutorService threadPool) {
    this.serverResponse = serverResponse;
    clientSocket = socket;
    this.clientRequest = clientRequest;
    this.threadPool = threadPool;
  }

  public void run() {

    System.out.println(serverResponse.getResponseHeader().getHeaderText());

    try {
      DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());

      // Write to the clientSocket.
      outputStream.writeBytes(serverResponse.getResponseHeader().getHeaderText());
      outputStream.write(serverResponse.getContent());
      outputStream.writeByte(HTTPConstants.CR);
      outputStream.writeByte(HTTPConstants.LF);
    } catch (IOException e) {

      // Happens if the socket has been closed.
      // Either by the client or by the timeout.
      return;
    }
    System.out.println(serverResponse.getResponseHeader());

    if (clientRequest != null) {
      //Close the connection if HTTP/1.0
      if (clientRequest.getRequestHeader().getVersion().equals(HTTPVersion.HTTP_1_0) || clientRequest.getRequestHeader().isConnectionClose()) {
        try {
          clientSocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        // Wait for more requests from the socket.
        threadPool.execute(new RequestHandler(clientSocket, threadPool));
      }
    }
  }
}
