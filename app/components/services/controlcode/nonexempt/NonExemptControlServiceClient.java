package components.services.controlcode.nonexempt;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.logging.CorrelationId;
import components.common.logging.ServiceClientLogger;
import exceptions.ServiceException;
import models.GoodsType;
import models.softtech.SoftTechCategory;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;

import java.util.concurrent.CompletionStage;

public class NonExemptControlServiceClient {
  private final HttpExecutionContext httpExecutionContext;
  private final WSClient wsClient;
  private final int webServiceTimeout;
  private final String webServiceUrl;
  private final String credentials;

  @Inject
  public NonExemptControlServiceClient(HttpExecutionContext httpExecutionContext,
                                       WSClient wsClient,
                                       @Named("controlCodeServiceAddress") String webServiceAddress,
                                       @Named("controlCodeServiceTimeout") int webServiceTimeout,
                                       @Named("controlCodeServiceCredentials") String credentials) {
    this.httpExecutionContext = httpExecutionContext;
    this.wsClient = wsClient;
    this.webServiceTimeout = webServiceTimeout;
    this.webServiceUrl = webServiceAddress + "/non-exempt";
    this.credentials = credentials;
  }

  public CompletionStage<NonExemptControlsServiceResult> get(GoodsType goodsType, SoftTechCategory softTechCategory) {
    if (goodsType != GoodsType.SOFTWARE && goodsType != GoodsType.TECHNOLOGY) {
      throw new RuntimeException(String.format("Unexpected member of GoodsType enum: \"%s\"", goodsType.toString()));
    }
    String url;
    if (softTechCategory.isDualUseSoftTechCategory()) {
      url = webServiceUrl + "/" + goodsType.urlString() + "/dual-use/" + softTechCategory.toUrlString();
    } else {
      // Military
      url = webServiceUrl + "/" + goodsType.urlString() + "/" + softTechCategory.toUrlString();
    }
    return wsClient.url(url)
        .setAuth(credentials)
        .withRequestFilter(CorrelationId.requestFilter)
        .withRequestFilter(ServiceClientLogger.requestFilter("Control Code", "GET", httpExecutionContext))
        .setRequestTimeout(webServiceTimeout)
        .get()
        .handleAsync((response, error) -> {
          if (error != null) {
            throw new ServiceException("Control Code service request failed", error);
          } else if (response.getStatus() != 200) {
            String errorMessage = response.asJson() != null ? response.asJson().get("message").asText() : "";
            throw new ServiceException(String.format("Unexpected HTTP status code from control code service /non-exempt: %s %s"
                , response.getStatus(), errorMessage));
          } else {
            return new NonExemptControlsServiceResult(response.asJson());
          }
        }, httpExecutionContext.current());
  }

}
