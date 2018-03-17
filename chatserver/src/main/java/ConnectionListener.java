import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ConnectionListener implements Runnable {

  // Reference to the blockingQueue to write to
  BlockingQueue<Socket> clientSockets;
  ServerSocket serverSocket;

  public ConnectionListener(ServerSocket socket, BlockingQueue<Socket> clientSockets) {
    this.serverSocket = socket;
    this.clientSockets = clientSockets;
  }

  public void run() {
    Preconditions.checkNotNull(serverSocket);
    try {
      while (!serverSocket.isClosed()) {
        //Wait until the client requests a connection then accept returns a new Socket that is bound to the client
        // The server can communicate over the new clientSocket to the client.
        clientSockets.add(serverSocket.accept());
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}
