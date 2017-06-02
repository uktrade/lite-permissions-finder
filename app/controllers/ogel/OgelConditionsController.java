package controllers.ogel;

import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import components.services.ogels.conditions.OgelConditionsServiceClient;
import components.services.ogels.ogel.OgelServiceClient;
import components.services.ogels.virtualeu.VirtualEUOgelClient;
import exceptions.BusinessRuleException;
import exceptions.FormStateException;
import journey.Events;
import models.VirtualEUOgelStage;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import utils.CountryUtils;
import views.html.ogel.ogelConditions;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class OgelConditionsController {

  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext httpExecutionContext;
  private final OgelServiceClient ogelServiceClient;
  private final OgelConditionsServiceClient ogelConditionsServiceClient;
  private final VirtualEUOgelClient virtualEUOgelClient;

  @Inject
  public OgelConditionsController(JourneyManager journeyManager,
                                  FormFactory formFactory,
                                  PermissionsFinderDao permissionsFinderDao,
                                  HttpExecutionContext httpExecutionContext,
                                  OgelServiceClient ogelServiceClient,
                                  OgelConditionsServiceClient ogelConditionsServiceClient,
                                  VirtualEUOgelClient virtualEUOgelClient) {
    this.journeyManager = journeyManager;
    this.formFactory = formFactory;
    this.permissionsFinderDao = permissionsFinderDao;
    this.httpExecutionContext = httpExecutionContext;
    this.ogelServiceClient = ogelServiceClient;
    this.ogelConditionsServiceClient = ogelConditionsServiceClient;
    this.virtualEUOgelClient = virtualEUOgelClient;
  }

  public CompletionStage<Result> renderForm() {
    OgelConditionsForm templateForm = new OgelConditionsForm();
    Optional<Boolean> doesRestrictionApplyOptional = permissionsFinderDao.getOgelConditionsApply();
    templateForm.doConditionsApply = doesRestrictionApplyOptional.orElse(null);

    return renderWithForm(formFactory.form(OgelConditionsForm.class).fill(templateForm));
  }

  public CompletionStage<Result> handleSubmit() {
    Form<OgelConditionsForm> form = formFactory.form(OgelConditionsForm.class).bindFromRequest();
    if (form.hasErrors()) {
      return renderWithForm(form);
    }

    boolean doConditionsApply = form.get().doConditionsApply;
    permissionsFinderDao.saveOgelConditionsApply(doConditionsApply);
    String controlCode = permissionsFinderDao.getControlCodeForRegistration();

    // Check this ogel required conditions
    return ogelConditionsServiceClient.get(permissionsFinderDao.getOgelId(),
        permissionsFinderDao.getControlCodeForRegistration())
        .thenComposeAsync(conditionsResult -> {
          if (conditionsResult.isEmpty) {
            throw new BusinessRuleException("Should not be able to progress without conditions");
          }
          else if (conditionsResult.isMissingControlCodes) {
            throw new BusinessRuleException("Should not be able to progress with missing control codes");
          }
          else {
            String sourceCountry = permissionsFinderDao.getSourceCountry();
            List<String> destinationCountries = CountryUtils.getDestinationCountries(
                permissionsFinderDao.getFinalDestinationCountry(), permissionsFinderDao.getThroughDestinationCountries());

            // branch on this ogel being a virtual eu or not
            return virtualEUOgelClient.sendServiceRequest(controlCode, sourceCountry, destinationCountries)
                .thenComposeAsync(virtualEUResult -> {
                  if(virtualEUResult.isVirtualEu()) {
                    // Optional.get() should be fine, doConditionApply checks the state of this optional
                    if (OgelConditionsServiceClient.isItemAllowed(conditionsResult, doConditionsApply)) {
                      return journeyManager.performTransition(Events.VIRTUAL_EU_OGEL_STAGE,
                          VirtualEUOgelStage.VIRTUAL_EU_CONDITIONS_DO_APPLY);
                    }
                    else {
                      return journeyManager.performTransition(Events.VIRTUAL_EU_OGEL_STAGE,
                          VirtualEUOgelStage.VIRTUAL_EU_CONDITIONS_DO_NOT_APPLY);
                    }
                  }
                  else {
                    if (OgelConditionsServiceClient.isItemAllowed(conditionsResult, doConditionsApply)) {
                      return journeyManager.performTransition(Events.OGEL_CONDITIONS_DO_APPLY);
                    }
                    else {
                      return journeyManager.performTransition(Events.OGEL_CONDITIONS_DO_NOT_APPLY);
                    }
                  }
                }, httpExecutionContext.current());
          }
        }, httpExecutionContext.current());
  }

  private CompletionStage<Result> renderWithForm(Form<OgelConditionsForm> form) {
    String ogelId = permissionsFinderDao.getOgelId();
    String controlCode = permissionsFinderDao.getControlCodeForRegistration();
    return ogelConditionsServiceClient.get(ogelId, controlCode)
        .thenApplyAsync(conditionsResult -> {
          if (conditionsResult.isMissingControlCodes) {
            Logger.error("OGEL conditions service returned a response with missing control codes. " +
                "OGEL ID: {}, control code: {}.", ogelId, controlCode);
            form.reject("We have encountered a problem with this particular licence. Unfortunately you will not be " +
                "able to progress through the service any further with the requested licence. This issue has been logged, " +
                "we thank your for your patience.");
          }
          return ogelServiceClient.get(permissionsFinderDao.getOgelId())
              .thenApplyAsync(ogelResult -> {
                String sourceCountry = permissionsFinderDao.getSourceCountry();
                List<String> destinationCountries = CountryUtils.getDestinationCountries(
                    permissionsFinderDao.getFinalDestinationCountry(), permissionsFinderDao.getThroughDestinationCountries());

                return virtualEUOgelClient.sendServiceRequest(controlCode, sourceCountry, destinationCountries)
                    .thenApplyAsync(result -> ok(ogelConditions.render(form, ogelResult,
                        conditionsResult, conditionsResult.isMissingControlCodes, result.isVirtualEu())),
                        httpExecutionContext.current());

              }, httpExecutionContext.current())
              .thenCompose(Function.identity());

        }, httpExecutionContext.current())
        .thenCompose(Function.identity());
  }

  public static class OgelConditionsForm {

    @Required(message = "Answer this question")
    public Boolean doConditionsApply;

  }

}
