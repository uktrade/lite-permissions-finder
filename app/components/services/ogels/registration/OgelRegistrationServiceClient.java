package components.services.ogels.registration;

import static play.mvc.Results.redirect;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import components.common.client.CountryServiceClient;
import components.common.state.ContextParamManager;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.frontend.FrontendServiceClient;
import components.services.ogels.ogel.OgelServiceClient;
import models.summary.Summary;
import org.apache.commons.lang3.StringUtils;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class OgelRegistrationServiceClient {

  public final static String STATUS_CODE_OK = "ok";

  private final WSClient wsClient;
  private final int webServiceTimeout;
  private final String webServiceSharedSecret;
  private final String createTransactionUrl;
  private final String updateTransactionUrl;
  private final String ogelRegistrationRootUrl;
  private final ContextParamManager contextParamManager;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext httpExecutionContext;
  private final FrontendServiceClient frontendServiceClient;
  private final CountryServiceClient countryServiceClient;
  private final OgelServiceClient ogelServiceClient;

  @Inject
  public OgelRegistrationServiceClient(WSClient wsClient,
                                       @Named("ogelRegistrationServiceHost") String webServiceHost,
                                       @Named("ogelRegistrationServicePort") int webServicePort,
                                       @Named("ogelRegistrationServiceTimeout") int webServiceTimeout,
                                       @Named("ogelRegistrationServiceSharedSecret") String webServiceSharedSecret,
                                       ContextParamManager contextParamManager,
                                       PermissionsFinderDao permissionsFinderDao,
                                       HttpExecutionContext httpExecutionContext,
                                       FrontendServiceClient frontendServiceClient,
                                       CountryServiceClient countryServiceClient,
                                       OgelServiceClient ogelServiceClient) {
    this.wsClient = wsClient;
    this.webServiceTimeout = webServiceTimeout;
    this.webServiceSharedSecret = webServiceSharedSecret;
    this.ogelRegistrationRootUrl = "http://" + webServiceHost + ":" + webServicePort;
    this.createTransactionUrl = ogelRegistrationRootUrl + "/create-transaction";
    this.updateTransactionUrl = ogelRegistrationRootUrl + "/update-transaction";
    this.contextParamManager = contextParamManager;
    this.permissionsFinderDao = permissionsFinderDao;
    this.httpExecutionContext = httpExecutionContext;
    this.frontendServiceClient = frontendServiceClient;
    this.countryServiceClient = countryServiceClient;
    this.ogelServiceClient = ogelServiceClient;
  }

  public CompletionStage<Result> createTransaction(String transactionId) {
    return sendWebServiceRequest(transactionId, createTransactionUrl);
  }

  public CompletionStage<Result> updateTransaction(String transactionId) {
    return sendWebServiceRequest(transactionId, updateTransactionUrl);
  }

  private CompletionStage<Result> sendWebServiceRequest(String transactionId, String webServiceUrl) {
    WSRequest wsRequest = wsClient.url(webServiceUrl)
        .setRequestTimeout(webServiceTimeout)
        .setQueryParameter("securityToken", webServiceSharedSecret);

    CompletionStage<Summary> summaryStage = Summary.composeSummary(contextParamManager, permissionsFinderDao,
        httpExecutionContext, frontendServiceClient, countryServiceClient, ogelServiceClient);

    CompletionStage<OgelRegistrationServiceRequest> requestStage =
        summaryStage.thenApply(summary -> new OgelRegistrationServiceRequest(transactionId, summary));

    return requestStage.thenApply(request -> wsRequest.post(Json.toJson(request)))
        .thenCompose(Function.identity())
        .thenApplyAsync(wsResponse -> {
          if (wsResponse.getStatus() != 200) {
            throw new OgelRegistrationServiceException(String.format("Unexpected HTTP status code: %s", wsResponse.getStatus()));
          }
          else {
            OgelRegistrationServiceResult result = OgelRegistrationServiceResult.buildFromJson(wsResponse.asJson());
            if (!StringUtils.isNotBlank(result.redirectUrl)) {
              throw new OgelRegistrationServiceException("Empty redirect URL supplied");
            }
            else if (!StringUtils.equals(result.status, STATUS_CODE_OK)) {
              throw new OgelRegistrationServiceException(String.format("Bad status code returned: %s", result.status));
            }
            else {
              permissionsFinderDao.saveOgelRegistrationServiceTransactionExists(true);
              return redirect(ogelRegistrationRootUrl + result.redirectUrl);
            }
          }
        }, httpExecutionContext.current());
  }

}
