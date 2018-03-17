import Objects.HTTPVersion;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class RequestResponder implements Runnable {

  BlockingQueue<ClientRequestWithSocket> inputQueue;

  public void run() {
    try {

      // Retrieve an element from the inputQueue.
      ClientRequestWithSocket requestWithSocket = inputQueue.poll();

      // Create a new outputStream to the client socket
      DataOutputStream outputStream = new DataOutputStream(requestWithSocket.getClientSocket().getOutputStream());

      //TODO: Write the headerResponse

      //Close the connection if HTTP/1.0
      if (requestWithSocket.getClientRequest().getRequestHeader().getVersion().equals(HTTPVersion.HTTP_1_0))
      requestWithSocket.getClientSocket().close();
      else {
        // Add the socket back to the sockets queue.
      }

    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}
