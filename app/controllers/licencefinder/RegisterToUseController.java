package controllers.licencefinder;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.inject.Inject;
import components.auth.SamlAuthorizer;
import components.common.CommonContextAction;
import components.common.auth.SpireSAML2Client;
import components.common.cache.CountryProvider;
import components.persistence.LicenceFinderDao;
import components.services.LicenceFinderService;
import components.services.OgelService;
import models.view.QuestionView;
import models.view.RegisterResultView;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Named;

@Secure(clients = SpireSAML2Client.CLIENT_NAME, authorizers = SamlAuthorizer.AUTHORIZER_NAME)
@With(CommonContextAction.class)
public class RegisterToUseController extends Controller {

  private static final String CONTROL_CODE_QUESTION = "What Control list entry describes your goods?";
  private static final String GOODS_GOING_QUESTION = "Where are your goods going?";
  private static final String FIRST_COUNTRY = "First country or territory that will receive the items";
  private static final String REPAIR_QUESTION = "Are you exporting goods for or after repair or replacement?";
  private static final String EXHIBITION_QUESTION = "Are you exporting goods for or after exhibition or demonstration?";
  private static final String BEFORE_OR_LESS_QUESTION = "Were your goods manufactured before 1897, and worth less than £30,000?";

  private static final String YES = "Yes";
  private static final String NO = "No";

  private final FormFactory formFactory;
  private final LicenceFinderDao licenceFinderDao;
  private final CountryProvider countryProvider;
  private final HttpExecutionContext httpContext;
  private final String dashboardUrl;
  private final OgelService ogelService;
  private final LicenceFinderService licenceFinderService;
  private final views.html.licencefinder.registerResult registerResult;
  private final views.html.licencefinder.registerToUse registerToUse;
  private final views.html.licencefinder.registerWait registerWait;

  @Inject
  public RegisterToUseController(FormFactory formFactory,
                                 HttpExecutionContext httpContext,
                                 LicenceFinderDao licenceFinderDao,
                                 @Named("countryProviderExport") CountryProvider countryProvider,
                                 @com.google.inject.name.Named("dashboardUrl") String dashboardUrl,
                                 OgelService ogelService, LicenceFinderService licenceFinderService,
                                 views.html.licencefinder.registerResult registerResult,
                                 views.html.licencefinder.registerToUse registerToUse,
                                 views.html.licencefinder.registerWait registerWait) {
    this.formFactory = formFactory;
    this.httpContext = httpContext;
    this.licenceFinderDao = licenceFinderDao;
    this.countryProvider = countryProvider;
    this.dashboardUrl = dashboardUrl;
    this.ogelService = ogelService;
    this.licenceFinderService = licenceFinderService;
    this.registerResult = registerResult;
    this.registerToUse = registerToUse;
    this.registerWait = registerWait;
  }

  /**
   * renderRegisterToUseForm
   */
  public CompletionStage<Result> renderRegisterToUseForm(String sessionId) {
    return renderWithRegisterToUseForm(formFactory.form(RegisterToUseForm.class), sessionId);
  }

  /**
   * handleRegisterToUseSubmit
   */
  public CompletionStage<Result> handleRegisterToUseSubmit(String sessionId) {
    Form<RegisterToUseForm> form = formFactory.form(RegisterToUseForm.class).bindFromRequest();
    if (form.hasErrors()) {
      return renderWithRegisterToUseForm(form, sessionId);
    }

    licenceFinderService.registerOgel(sessionId);

    Optional<String> regRef = licenceFinderService.getRegistrationReference(sessionId);
    if (regRef.isPresent()) {
      return ogelService.get(licenceFinderDao.getOgelId(sessionId))
          .thenApplyAsync(ogelFullView -> {
            RegisterResultView view = new RegisterResultView("You have successfully registered to use Open general export licence (" + ogelFullView.getName() + ") ", regRef.get());
            return ok(registerResult.render(view, ogelFullView, dashboardUrl, sessionId));
          }, httpContext.current());
    }

    return CompletableFuture.completedFuture(redirect(routes.RegisterToUseController.renderAwaitResult(sessionId)));
  }

  /**
   * renderAwaitResult
   */
  public CompletionStage<Result> renderAwaitResult(String sessionId) {
    Optional<String> regRef = licenceFinderService.getRegistrationReference(sessionId);
    if (regRef.isPresent()) {
      return ogelService.get(licenceFinderDao.getOgelId(sessionId))
          .thenApplyAsync(ogelFullView -> {
            RegisterResultView view = new RegisterResultView("You have successfully registered to use Open general export licence (" + ogelFullView.getName() + ") ", regRef.get());
            return ok(registerResult.render(view, ogelFullView, dashboardUrl, sessionId));
          }, httpContext.current());
    }
    return completedFuture(ok(registerWait.render(sessionId)));
  }

  /**
   * Handles the RegistrationInterval form submission
   */
  public CompletionStage<Result> handleRegistrationProcessed(String sessionId) {
    return CompletableFuture.completedFuture(redirect(routes.RegisterToUseController.renderAwaitResult(sessionId)));
  }

  /**
   * Private methods
   */
  private CompletionStage<Result> renderWithRegisterToUseForm(Form<RegisterToUseForm> form, String sessionId) {
    return ogelService.get(licenceFinderDao.getOgelId(sessionId))
        .thenApplyAsync(
            ogelFullView -> ok(registerToUse.render(form, ogelFullView, getLicenceFinderAnswers(sessionId), sessionId)), httpContext.current());
  }

  private List<QuestionView> getLicenceFinderAnswers(String sessionId) {
    List<QuestionView> views = new ArrayList<>();

    views.add(new QuestionView(CONTROL_CODE_QUESTION, licenceFinderDao.getControlCode(sessionId)));
    licenceFinderDao.getTradeType(sessionId).ifPresent(tradeType -> views.add(new QuestionView(GOODS_GOING_QUESTION, tradeType.getTitle())));
    views.add(new QuestionView(DestinationController.DESTINATION_QUESTION, countryProvider.getCountry(licenceFinderDao.getDestinationCountry(sessionId)).getCountryName()));
    licenceFinderDao.getMultipleCountries(sessionId).ifPresent(aBoolean -> views.add(new QuestionView(DestinationController.DESTINATION_MULTIPLE_QUESTION, aBoolean ? "Yes" : "No")));

    Optional<Boolean> optMultipleCountries = licenceFinderDao.getMultipleCountries(sessionId);
    if (optMultipleCountries.isPresent()) {
      boolean isMultiple = optMultipleCountries.get();
      if (isMultiple) {
        views.add(new QuestionView(FIRST_COUNTRY, countryProvider.getCountry(licenceFinderDao.getFirstConsigneeCountry(sessionId)).getCountryName()));
      }
    }

    Optional<QuestionsController.QuestionsForm> optForm = licenceFinderDao.getQuestionsForm(sessionId);
    if (optForm.isPresent()) {
      QuestionsController.QuestionsForm form = optForm.get();
      views.add(new QuestionView(REPAIR_QUESTION, form.forRepair ? YES : NO));
      views.add(new QuestionView(EXHIBITION_QUESTION, form.forExhibition ? YES : NO));
      views.add(new QuestionView(BEFORE_OR_LESS_QUESTION, form.beforeOrLess ? YES : NO));
    }
    return views;
  }

  public static class RegisterToUseForm {
    @Constraints.Required(message = "Confirm you have read the OGEL and its criteria in full.")
    public Boolean confirmRead;

    @Constraints.Required(message = "Confirm your export complies with the OGEL criteria stated.")
    public Boolean confirmComplies;
  }
}
