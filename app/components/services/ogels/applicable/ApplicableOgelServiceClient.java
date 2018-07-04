package components.services.ogels.applicable;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import uk.gov.bis.lite.ogel.api.view.ApplicableOgelView;
import utils.MyLogger;
import utils.MyLoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApplicableOgelServiceClient {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ApplicableOgelServiceClient.class);
  private static final MyLogger MY_LOGGER = MyLoggerFactory.getLogger(ApplicableOgelServiceClient.class);

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
                                                       List<String> destinationCountries,
                                                       List<String> activityTypes, boolean showHistoricOgel) {
    return getApplicableOgelViews(controlCode, sourceCountry, destinationCountries, activityTypes, showHistoricOgel);
  }


  public CompletionStage<List<ApplicableOgelView>> get(String controlCode, String sourceCountry,
                                                       List<String> destinationCountries,
                                                       List<String> activityTypes) {
    return getApplicableOgelViews(controlCode, sourceCountry, destinationCountries, activityTypes, true);
  }

  private CompletionStage<List<ApplicableOgelView>> getApplicableOgelViews(String controlCode, String sourceCountry,
                                                                           List<String> destinationCountries,
                                                                           List<String> activityTypes,
                                                                           boolean showHistoricOgel) {

    WSRequest req = wsClient.url(webServiceUrl)
        .setAuth(credentials)
        .setRequestFilter(CorrelationId.requestFilter)
        .setRequestFilter(ServiceClientLogger.requestFilter("OGEL", "GET", httpExecutionContext))
        .setRequestTimeout(Duration.ofMillis(webServiceTimeout))
        .addQueryParameter("controlCode", controlCode)
        .addQueryParameter("sourceCountry", sourceCountry);

    return req.get().handleAsync((response, error) -> {
      MY_LOGGER.error("handleAsync");
      return new ArrayList<>();
    }, httpExecutionContext.current());

//    destinationCountries.forEach(country -> req.addQueryParameter("destinationCountry", country));
//
//    activityTypes.forEach(activityType -> req.addQueryParameter("activityType", activityType));
//
//    return req.get().handleAsync((response, error) -> {
//      if (error != null) {
//        throw new ServiceException("OGEL service request failed", error);
//      } else if (response.getStatus() != 200) {
//        throw new ServiceException(String.format("Unexpected HTTP status code from OGEL service /applicable-ogels: %s",
//            response.getStatus()));
//      } else {
//        return filterHistoric(Json.fromJson(response.asJson(), ApplicableOgelView[].class), showHistoricOgel);
//      }
//    }, httpExecutionContext.current());
  }

  /**
   * Removes historic ogels from results if required
   */
  private List<ApplicableOgelView> filterHistoric(ApplicableOgelView[] views, boolean showHistoricOgel) {
    List<ApplicableOgelView> filteredViews = Stream.of(views)
        .filter(view -> showHistoricOgel || !StringUtils.containsIgnoreCase(view.getName(), "historic military goods"))
        .collect(Collectors.toList());

    LOGGER.info("Filter Historic Ogels (" + showHistoricOgel + ") [" + views.length + "/" + filteredViews.size() + "]");
    return filteredViews;
  }

}
