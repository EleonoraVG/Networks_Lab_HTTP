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

public class ChatServer {
  private int defaultQueueSize = 50;
  private int defaultThreadPoolSize = 20;
  private int port = 80;
  private InetAddress hostAddress;
  private ServerSocket serverSocket;

  public void run() throws IOException{
    hostAddress = Inet4Address.getLocalHost();
    serverSocket = HTTPReader.startServerSocket(hostAddress, 80);
    // Create a thread pool
    // The thread pool will receive tasks asynchronously from other threads.
    ExecutorService threadPool = Executors.newFixedThreadPool(defaultThreadPoolSize);

    // Start a connection listener
    // This connection listener will schedule tasks until the socket closes.
    Thread connectionListener = new Thread(new ConnectionListener(serverSocket, threadPool));
    connectionListener.start();


    // Shut down the thread pool
    //threadPool.shutdown();
  }
}
