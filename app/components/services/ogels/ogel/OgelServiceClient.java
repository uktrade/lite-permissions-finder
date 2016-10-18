package components.services.ogels.ogel;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import exceptions.ServiceException;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;

import java.util.concurrent.CompletionStage;

public class OgelServiceClient {

  private final HttpExecutionContext httpExecutionContext;
  private final WSClient wsClient;
  private final int webServiceTimeout;
  private final String webServiceUrl;

  @Inject
  public OgelServiceClient(HttpExecutionContext httpExecutionContext,
                           WSClient wsClient,
                           @Named("ogelServiceHost") String webServiceHost,
                           @Named("ogelServicePort") int webServicePort,
                           @Named("ogelServiceTimeout") int webServiceTimeout) {
    this.httpExecutionContext = httpExecutionContext;
    this.wsClient = wsClient;
    this.webServiceTimeout = webServiceTimeout;
    this.webServiceUrl= "http://" + webServiceHost + ":" + webServicePort + "/ogels/";
  }

  public CompletionStage<OgelServiceResult> get(String ogelId){
    return wsClient.url(webServiceUrl + ogelId)
        .withRequestFilter(CorrelationId.requestFilter)
        .setRequestTimeout(webServiceTimeout)
        .get().handleAsync((response, error) -> {
          if (response.getStatus() != 200) {
            throw new ServiceException(String.format("Unexpected HTTP status code from OGEL service /ogels/%s: %s",
                ogelId, response.getStatus()));
          }
          else {
            return OgelServiceResult.build(response.asJson());
          }
        }, httpExecutionContext.current());
  }
}
