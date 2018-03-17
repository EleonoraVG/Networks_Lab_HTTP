import Helpers.HTTPReader;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

//Use start on threads! not Run!
//Create a new inputstream for every requestHandler!!!
//RequestHandlers give tasks to requestListeners???

public class ChatServer {
  private int defaultQueueSize = 50;
  private int port = 80;
  private InetAddress hostAddress;
  private ServerSocket serverSocket;

  private ChatServer() throws IOException{
    //TODO: remove logic from constructor!!!
    hostAddress = Inet4Address.getLocalHost();
    serverSocket = HTTPReader.startServerSocket(hostAddress, 80);

    // Collect the connections from the connectionListener
    BlockingQueue<Socket> clientSockets = new ArrayBlockingQueue<Socket>(50); //TODO: choose implementation

    // Start a connection listener
    Thread connectionListener = new Thread(new ConnectionListener(serverSocket,clientSockets));
    connectionListener.start();

    //TODO: Datastructure to collect requestHeaders with their connection.

    // Get client socket from blocking queue in a first come first serve manner
    // and retrieve the requestHeader.

    //TODO don't forget to close the datastreams in all the threads.

  }

}
