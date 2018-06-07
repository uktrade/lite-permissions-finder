package controllers.licencefinder;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.inject.Inject;
import components.auth.SamlAuthorizer;
import components.common.auth.SpireAuthManager;
import components.common.auth.SpireSAML2Client;
import components.common.state.ContextParamManager;
import components.persistence.LicenceFinderDao;
import components.services.LicenceFinderService;
import components.services.OgelService;
import models.view.RegisterResultView;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

@Secure(clients = SpireSAML2Client.CLIENT_NAME, authorizers = SamlAuthorizer.AUTHORIZER_NAME)
public class ResultsController extends Controller {

  private final FormFactory formFactory;
  private final LicenceFinderDao licenceFinderDao;
  private final HttpExecutionContext httpContext;
  private final ContextParamManager contextParam;
  private final views.html.licencefinder.results results;
  private final SpireAuthManager authManager;
  private final LicenceFinderService licenceFinderService;
  private final views.html.licencefinder.registerResult registerResult;
  private final OgelService ogelService;
  private final String dashboardUrl;

  public static final String NONE_ABOVE_KEY = "NONE_ABOVE_KEY";
  public static final String IS_ALREADY_REGISTERED_KEY = "IS_ALREADY_REGISTERED_KEY";

  @Inject
  public ResultsController(FormFactory formFactory,
                           HttpExecutionContext httpContext,
                           LicenceFinderDao licenceFinderDao, ContextParamManager contextParam,
                           views.html.licencefinder.results results,
                           SpireAuthManager authManager, LicenceFinderService licenceFinderService, views.html.licencefinder.registerResult registerResult, OgelService ogelService,
                           @com.google.inject.name.Named("dashboardUrl") String dashboardUrl) {
    this.formFactory = formFactory;
    this.httpContext = httpContext;
    this.licenceFinderDao = licenceFinderDao;
    this.contextParam = contextParam;
    this.results = results;
    this.authManager = authManager;
    this.licenceFinderService = licenceFinderService;
    this.registerResult = registerResult;
    this.ogelService = ogelService;
    this.dashboardUrl = dashboardUrl;
  }

  /************************************************************************************************
   * 'Results' page
   *******************************************************************************************/
  public CompletionStage<Result> renderResultsForm() {
    return renderWithForm(formFactory.form(ResultsForm.class));
  }

  public CompletionStage<Result> handleResultsSubmit() {
    Form<ResultsForm> form = formFactory.form(ResultsForm.class).bindFromRequest();
    if (form.hasErrors()) {
      return renderWithForm(form);
    }
    String chosenOgelId = form.get().chosenOgel;
    licenceFinderDao.saveOgelId(chosenOgelId);

    // Return No licences available when 'None of the above' chosen
    if (chosenOgelId.equals(NONE_ABOVE_KEY)) {
      return completedFuture(ok(results.render(form, licenceFinderService.getNoResultsView())));
    }

    // Check if we have a Ogel that the is already registered - return registerResult view
    if(licenceFinderService.isOgelIdAlreadyRegistered(chosenOgelId)) {
      return ogelService.get(licenceFinderDao.getOgelId()).thenApplyAsync(ogelFullView -> {
        return ok(registerResult.render(new RegisterResultView("You are already registered to use Open general export licence (" + ogelFullView.getName() + ")"), ogelFullView, dashboardUrl));
      }, httpContext.current());
    }

    return contextParam.addParamsAndRedirect(routes.RegisterToUseController.renderRegisterToUseForm());
  }

  private CompletionStage<Result> renderWithForm(Form<ResultsForm> form) {
    return completedFuture(ok(results.render(form, licenceFinderService.getResultsView())));
  }

  public static class ResultsForm {
    @Required(message = "You must choose from the list of results below")
    public String chosenOgel;
  }

}

