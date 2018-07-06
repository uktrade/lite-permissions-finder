package controllers.licencefinder;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.inject.Inject;
import components.common.auth.SamlAuthorizer;
import components.common.auth.SpireSAML2Client;
import components.persistence.LicenceFinderDao;
import components.services.LicenceFinderService;
import components.services.OgelService;
import controllers.guard.LicenceFinderUserGuardAction;
import exceptions.UnknownParameterException;
import models.view.RegisterResultView;
import org.pac4j.play.java.Secure;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Secure(clients = SpireSAML2Client.CLIENT_NAME, authorizers = SamlAuthorizer.AUTHORIZER_NAME)
@With({LicenceFinderUserGuardAction.class})
public class RegisterAwaitController extends Controller {

  private final LicenceFinderDao licenceFinderDao;
  private final HttpExecutionContext httpContext;
  private final String dashboardUrl;
  private final OgelService ogelService;
  private final LicenceFinderService licenceFinderService;
  private final views.html.licencefinder.registerResult registerResult;
  private final views.html.licencefinder.registerWait registerWait;

  @Inject
  public RegisterAwaitController(HttpExecutionContext httpContext,
                                 LicenceFinderDao licenceFinderDao,
                                 @com.google.inject.name.Named("dashboardUrl") String dashboardUrl,
                                 OgelService ogelService, LicenceFinderService licenceFinderService,
                                 views.html.licencefinder.registerResult registerResult,
                                 views.html.licencefinder.registerWait registerWait) {
    this.httpContext = httpContext;
    this.licenceFinderDao = licenceFinderDao;
    this.dashboardUrl = dashboardUrl;
    this.ogelService = ogelService;
    this.licenceFinderService = licenceFinderService;
    this.registerResult = registerResult;
    this.registerWait = registerWait;
  }

  /**
   * renderAwaitResult
   */
  public CompletionStage<Result> renderAwaitResult(String sessionId) {
    Optional<String> reference = licenceFinderService.getRegistrationReference(sessionId);
    if (reference.isPresent()) {
      return registrationSuccess(sessionId, reference.get());
    } else {
      return completedFuture(ok(registerWait.render(sessionId)));
    }
  }

  /**
   * registrationSuccess
   */
  public CompletionStage<Result> registrationSuccess(String sessionId, String registrationRef) {
    Optional<String> reference = licenceFinderService.getRegistrationReference(sessionId);
    if (reference.isPresent()) {
      String ogelId = licenceFinderDao.getOgelId(sessionId).orElseThrow(UnknownParameterException::unknownOgelRegistrationOrder);
      return ogelService.getById(ogelId)
          .thenApplyAsync(ogelFullView -> {
            String title = "You have successfully registered to use Open general export licence " + ogelFullView.getName();
            RegisterResultView view = new RegisterResultView(title, registrationRef);
            return ok(registerResult.render(view, ogelFullView, dashboardUrl));
          }, httpContext.current());
    } else {
      throw UnknownParameterException.unknownOgelReference(registrationRef);
    }

  }

  /**
   * Handles the RegistrationInterval form submission
   */
  public CompletionStage<Result> handleRegistrationProcessed(String sessionId) {
    return completedFuture(redirect(routes.RegisterAwaitController.renderAwaitResult(sessionId)));
  }
}
