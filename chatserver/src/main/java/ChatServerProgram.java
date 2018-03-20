public class ChatServerProgram {

  public static void main(String[] args) throws Exception{

    //TODO: use a builder for the chatserver
    //TODO: allow options for a different port & host
    //TODO: allow a different directory to be given where it searches for the website to host.

    // Start a chat server
      ChatServer chatServer = new ChatServer();
      chatServer.run();
  }

  // THESE ARE TODOS FOR THE ASSIGNMENT

  //TODO: Print response.
  //TODO: Support if-modified-since header!
  //TODO: Check for the HTTP/1.1 host field
  //TODO: Support the status codes.
  //TODO: Send the following to the client
    // date
    // content-type
    // content-length
}
