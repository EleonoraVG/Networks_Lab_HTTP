import Objects.ClientRequest;

import java.net.Socket;

public class ClientRequestWithSocket {

  private final ClientRequest clientRequest;
  private final Socket clientSocket;

  public ClientRequestWithSocket(ClientRequest request, Socket socket) {
    clientRequest = request;
    clientSocket = socket;
  }

  public ClientRequest getClientRequest() {
    return clientRequest;
  }

  public Socket getClientSocket() {
    return clientSocket;
  }
}
