import Constants.HTTPConstants;
import Objects.ClientRequest;
import Objects.HTTPVersion;
import Objects.ServerResponse;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 *
 */
public class RequestResponder implements Runnable {

  ServerResponse serverResponse;
  Socket clientSocket;
  ClientRequest clientRequest;
  ExecutorService threadPool;

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

      System.out.println("error while writing to output socket, client closed the socket");
      System.out.println(e.getMessage());
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
        //TODO: Don't close connection for HTTP/1.1
        //  clientSocket.close();
        threadPool.execute(new RequestHandler(clientSocket, threadPool));
      }
    }
  }
}
