import Helpers.HTTPHelper;
import Objects.ClientRequest;

import java.util.concurrent.Callable;

/**
 * A callable for handling incoming requests to the server.
 * Callable is used because it supports a return value.
 */
public class RequestHandler implements Callable<ClientRequest> {


  public ClientRequest call() throws Exception {
    return HTTPHelper.readHeader(in)
  }
}
