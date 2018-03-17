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

//Use start on threads! not Run!
//Create a new inputstream for every requestHandler!!!
//RequestHandlers give tasks to requestListeners???

public class ChatServer {
  private int defaultQueueSize = 50;
  private int defaultThreadPoolSize = 10;
  private int port = 80;
  private InetAddress hostAddress;
  private ServerSocket serverSocket;

  private ChatServer() throws IOException {
    //TODO: remove logic from constructor!!!
    hostAddress = Inet4Address.getLocalHost();
    serverSocket = HTTPReader.startServerSocket(hostAddress, 80);

    // Collect the connections from the connectionListener
    BlockingQueue<Socket> clientSockets = new ArrayBlockingQueue<Socket>(defaultQueueSize); //TODO: choose implementation

    // Start a connection listener
    Thread connectionListener = new Thread(new ConnectionListener(serverSocket, clientSockets));
    connectionListener.start();

    //TODO: Datastructure to collect requestHeaders with their connection.
    BlockingQueue<ClientRequestWithSocket> requestWithSockets = new ArrayBlockingQueue<ClientRequestWithSocket>(defaultQueueSize);

    //Create a thread pool for requestHandlers
    ExecutorService requestHandlersThreadPool = Executors.newFixedThreadPool(defaultThreadPoolSize);
    requestHandlersThreadPool.execute(new RequestHandler(clientSockets,requestWithSockets));

    // Create a thread pool for requestResponders
    ExecutorService requestRespondersThreadPool = Executors.newFixedThreadPool(defaultThreadPoolSize);
    requestRespondersThreadPool.execute(new RequestResponder());
    //TODO don't forget to close the datastreams in all the threads.

  }

}
