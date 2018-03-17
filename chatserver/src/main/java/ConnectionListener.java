import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;

public class ConnectionListener implements Runnable {

  ServerSocket serverSocket;
  ExecutorService threadPool;

  public ConnectionListener(ServerSocket socket, ExecutorService threadPool) {
    this.serverSocket = socket;
    this.threadPool = threadPool;
  }

  public void run() {
    Preconditions.checkNotNull(serverSocket);
    try {
      while (!serverSocket.isClosed()) {
        //Wait until the client requests a connection then accept returns a new Socket that is bound to the client
        // The server can communicate over the new clientSocket to the client.
        threadPool.execute(new RequestHandler(serverSocket.accept(), threadPool));
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}
