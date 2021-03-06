import Helpers.HTTPReader;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Create the thread pool and start the listener thread.
 * <p>
 * This is what initiates the chat server program.
 */
public class ChatServer {
  private int defaultThreadPoolSize = 30;
  private int port = 80;

  public void run() throws IOException {
    InetAddress hostAddress = Inet4Address.getLocalHost();
    ServerSocket serverSocket = startServerSocket(hostAddress, port);

    // Create a thread pool.
    // which will receive tasks asynchronously from other threads.
    ExecutorService threadPool = Executors.newFixedThreadPool(defaultThreadPoolSize);

    // Start a connection listener
    // which will schedule tasks until the socket closes.
    Thread connectionListener = new Thread(new ConnectionListener(serverSocket, threadPool));
    connectionListener.start();

  }

  private ServerSocket startServerSocket(InetAddress ipAddress, int port) throws IOException {
    int backlog = 50;  // The maximum queue length for incoming connection indications.
    return new ServerSocket(port, backlog, ipAddress);
  }
}
