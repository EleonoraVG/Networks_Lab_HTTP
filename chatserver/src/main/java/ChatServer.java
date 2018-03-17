import Helpers.HTTPHelper;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

//Use start on threads! not Run!
//Create a new inputstream for every requestHandler!!!
//RequestHandlers give tasks to requestListeners???

public class ChatServer {

  private int port = 80;
  private InetAddress hostAddress;
  private ServerSocket serverSocket;

  private ChatServer() throws IOException{
    hostAddress = Inet4Address.getLocalHost();
    serverSocket = HTTPHelper.startServerSocket(hostAddress, 80);

    //Wait until the client requests a connection and accept returns a new Socket that is bound to the client
    // The server can communicate over the new clientSocket to the client.
    Socket clientSocket = serverSocket.accept();
  }

}
