import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Listen for a connection on the server socket.
 * If a connection has been made add a new task to the thread pool
 * and continue listening to the server socket.
 */
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

      // Wait until a connection is made to the serverSocket.
      while (!serverSocket.isClosed()) {

        // accept returns a new Socket that is bound to the client when a connection is made.
        Socket clientSocket = serverSocket.accept();

        threadPool.execute(new RequestHandler(clientSocket, threadPool));
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}
