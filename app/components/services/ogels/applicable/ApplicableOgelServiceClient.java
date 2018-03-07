package components.services.ogels.applicable;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
import exceptions.ServiceException;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import uk.gov.bis.lite.ogel.api.view.ApplicableOgelView;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApplicableOgelServiceClient {

  private final HttpExecutionContext httpExecutionContext;
  private final WSClient wsClient;
  private final int webServiceTimeout;
  private final String webServiceUrl;
  private final String credentials;

  @Inject
  public ApplicableOgelServiceClient(HttpExecutionContext httpExecutionContext,
                                     WSClient wsClient,
                                     @Named("ogelServiceAddress") String webServiceAddress,
                                     @Named("ogelServiceTimeout") int webServiceTimeout,
                                     @Named("ogelServiceCredentials") String credentials) {
    this.httpExecutionContext = httpExecutionContext;
    this.wsClient = wsClient;
    this.webServiceTimeout = webServiceTimeout;
    this.webServiceUrl = webServiceAddress + "/applicable-ogels";
    this.credentials = credentials;
  }

  public CompletionStage<List<ApplicableOgelView>> get(String controlCode, String sourceCountry,
                                                       List<String> destinationCountries, List<String> activityTypes) {

    WSRequest req = wsClient.url(webServiceUrl)
        .setAuth(credentials)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("OGEL", "GET", httpExecutionContext))
        .setRequestTimeout(Duration.ofMillis(webServiceTimeout))
        .addQueryParameter("controlCode", controlCode)
        .addQueryParameter("sourceCountry", sourceCountry);

    destinationCountries.forEach(country -> req.setQueryParameter("destinationCountry", country));

    activityTypes.forEach(activityType -> req.setQueryParameter("activityType", activityType));

    return req.get().handleAsync((response, error) -> {
      if (error != null) {
        throw new ServiceException("OGEL service request failed", error);
      } else if (response.getStatus() != 200) {
        throw new ServiceException(String.format("Unexpected HTTP status code from OGEL service /applicable-ogels: %s",
            response.getStatus()));
      } else {
        ApplicableOgelView[] applicableOgelView = Json.fromJson(response.asJson(), ApplicableOgelView[].class);
        return Stream.of(applicableOgelView).collect(Collectors.toList());
      }
    }, httpExecutionContext.current());
  }

}
