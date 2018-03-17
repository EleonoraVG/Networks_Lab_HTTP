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
  private int defaultThreadPoolSize = 20;
  private int port = 80;
  private InetAddress hostAddress;
  private ServerSocket serverSocket;

  private ChatServer() throws IOException {
    //TODO: remove logic from constructor!!!
    hostAddress = Inet4Address.getLocalHost();
    serverSocket = HTTPReader.startServerSocket(hostAddress, 80);

    // Create a thread pool
    ExecutorService threadPool = Executors.newFixedThreadPool(defaultThreadPoolSize);

    // Collect the connections from the connectionListener
    BlockingQueue<Socket> clientSockets = new ArrayBlockingQueue<Socket>(defaultQueueSize); //TODO: choose implementation

    // Start a connection listener
    Thread connectionListener = new Thread(new ConnectionListener(serverSocket, threadPool));
    connectionListener.start();

    //collect requestHeaders with their corresponding socket.
    BlockingQueue<ClientRequestWithSocket> requestWithSockets = new ArrayBlockingQueue<ClientRequestWithSocket>(defaultQueueSize);

    //TODO: Create ServiceResponseCreators. (Maybe dont create new threads for this?)
    // And alternatively process the request in the requestHandler. ??

    requestRespondersThreadPool.execute(new RequestResponder());

    //TODO don't forget to close the datastreams in all the threads.

    // Shut down executor Services
    requestHandlersThreadPool.shutdown();
    requestRespondersThreadPool.shutdown();
  }

}
