import Objects.HTTPCommand;
import Objects.HTTPVersion;
import com.google.common.base.Preconditions;
import org.apache.commons.cli.*;

public class ChatClientProgram {

  public static void main(String[] args) throws Exception {
    // The client needs at least 3 arguments.
    Preconditions.checkArgument(args.length >= 3);

    // Check for the HTTP/1.0 flag option.
    Options options = new Options();

    Option http10 = new Option("10", "http10", false, "Use HTTP/1.0 for the chatClient.");
    http10.setRequired(false);
    options.addOption(http10);

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);
    Boolean useHttp10 = cmd.hasOption(http10.getOpt());

    // Process command line arguments
    HTTPCommand httpCommand = HTTPCommand.fromString(args[0]);
    String url = args[1];
    int port = Integer.parseInt(args[2]);

    //Build and run chatClient with HTTP/1.0
    ChatClient chatClient;
    if (useHttp10) {
      chatClient = ChatClient.newBuilder()
              .setIpAddress(url)
              .setPort(port)
              .setHTTPVersion(HTTPVersion.HTTP_1_0)
              .build();
    } else {
      chatClient = ChatClient.newBuilder()
              .setIpAddress(url)
              .setPort(port)
              .setHTTPVersion(HTTPVersion.HTTP_1_1)
              .build();
    }
    chatClient.runAndSaveResult(httpCommand);
  }
}
