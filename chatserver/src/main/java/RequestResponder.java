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
    try {

      // Create a new outputStream to the client socket
      System.out.println(clientSocket.getOutputStream());
      System.out.println(serverResponse.getResponseHeader().getHeaderText());
      DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());


      outputStream.writeBytes(serverResponse.getResponseHeader().getHeaderText());
      outputStream.write(serverResponse.getContent());
      outputStream.writeByte('\r');
      outputStream.writeByte('\n');
      //TODO: Write the headerResponse

      //Close the connection if HTTP/1.0
      if (clientRequest.getRequestHeader().getVersion().equals(HTTPVersion.HTTP_1_0) || clientRequest.getRequestHeader().isConnectionClose())
        clientSocket.close();
      else {
        // Wait for more requests from the socket.
       //TODO: Don't close connection for HTTP/1.1
      //  clientSocket.close();
        threadPool.execute(new RequestHandler(clientSocket, threadPool));
      }

    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}
