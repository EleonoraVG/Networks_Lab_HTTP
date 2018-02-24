import com.google.common.base.Preconditions;

public class Program {

  public static void main(String[] args) throws Exception {
    Preconditions.checkArgument(args.length == 3); // The client needs 3 arguments.

    // Process command line arguments
    HttpCommand httpCommand = HttpCommand.stringToCommand(args[0]);
    String url = args[1];
    int port = Integer.parseInt(args[2]);

    //Build and run tcpClient
    TCPClient tcpClient = TCPClient.newBuilder().setIpAddress(url).setPort(port).build();
    tcpClient.run(httpCommand);
  }
}
