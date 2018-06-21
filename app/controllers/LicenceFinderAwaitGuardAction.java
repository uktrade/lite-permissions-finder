package controllers;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.inject.Inject;
import components.persistence.LicenceFinderDao;
import models.persistence.RegisterLicence;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Checks for a Registration reference associated with session and redirects to RegisterAwaitController controller if found
 */
public class LicenceFinderAwaitGuardAction extends Action.Simple {

  private final LicenceFinderDao licenceFinderDao;
  private final views.html.licencefinder.errorPage errorPage;

  @Inject
  public LicenceFinderAwaitGuardAction(LicenceFinderDao licenceFinderDao,
                                       views.html.licencefinder.errorPage errorPage) {
    this.licenceFinderDao = licenceFinderDao;
    this.errorPage = errorPage;
  }

  @Override
  public CompletionStage<Result> call(Http.Context ctx) {

    // Ensure we have a sessionId in request
    String sessionId = ctx.request().getQueryString("sessionId");
    if (StringUtils.isBlank(sessionId)) {
      return completedFuture(badRequest(errorPage.render("No session found")));
    }

    // Redirect to registerWait
    Optional<RegisterLicence> optRegisterLicence = licenceFinderDao.getRegisterLicence(sessionId);
    if (optRegisterLicence.isPresent()) {
      return completedFuture(redirect(controllers.licencefinder.routes.RegisterAwaitController.renderAwaitResult(sessionId)));
    }

    // No action required
    return delegate.call(ctx);
  }
}
