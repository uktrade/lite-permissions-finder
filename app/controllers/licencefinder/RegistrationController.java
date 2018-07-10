package controllers.licencefinder;

import static models.callback.RegistrationCallbackResponse.errorResponse;
import static models.callback.RegistrationCallbackResponse.okResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import components.services.LicenceFinderService;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import uk.gov.bis.lite.permissions.api.view.CallbackView;

public class RegistrationController extends Controller {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);

  private final LicenceFinderService licenceFinderService;

  @Inject
  public RegistrationController(LicenceFinderService licenceFinderService) {
    this.licenceFinderService = licenceFinderService;
  }

  /**
   * Endpoint for PermissionsService register callback
   */
  @BodyParser.Of(BodyParser.Json.class)
  public Result handleRegistrationCallback(String sessionId) {

    CallbackView callbackView;
    try {
      JsonNode json = request().body().asJson();
      callbackView = Json.fromJson(json, CallbackView.class);
      LOGGER.info("Registration callback received {transactionId={}, callbackView={}}", sessionId, json.toString());
    } catch (RuntimeException e) {
      String errorMessage = String.format("Registration callback error - Invalid callback registration request for sessionId %s, callbackBody=\"%s\"", sessionId, request().body().asText());
      LOGGER.error(errorMessage, e);
      return badRequest(Json.toJson(errorResponse(errorMessage)));
    }

    try {
      // TODO Possible refactor in separate pull request
      licenceFinderService.handleCallback(sessionId, callbackView);
      return ok(Json.toJson(okResponse()));
    } catch (Exception e) {
      String errorMessage = String.format("Registration callback handling error for sessionId %s", sessionId);
      LOGGER.error(errorMessage, e);
      return badRequest(Json.toJson(errorResponse(errorMessage + " - " + e.getMessage())));
    }
  }

}
