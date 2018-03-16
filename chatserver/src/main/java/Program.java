import Helpers.HTTPHelper;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Program {

  public static void main(String[] args) throws Exception{
    int port = 80;
    InetAddress hostAddress = Inet4Address.getLocalHost();
    Socket socket = HTTPHelper.startConnectionSocket(hostAddress,80);
  }
}
