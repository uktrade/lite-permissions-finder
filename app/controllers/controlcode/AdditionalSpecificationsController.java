package controllers.controlcode;

import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.common.journey.StandardEvents;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.FrontendServiceClient;
import journey.Events;
import journey.helpers.ControlCodeSubJourneyHelper;
import models.controlcode.AdditionalSpecificationsDisplay;
import models.controlcode.ControlCodeSubJourney;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.controlcode.additionalSpecifications;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class AdditionalSpecificationsController {

  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext httpExecutionContext;
  private final FrontendServiceClient frontendServiceClient;


  @Inject
  public AdditionalSpecificationsController(JourneyManager journeyManager,
                                            FormFactory formFactory,
                                            PermissionsFinderDao permissionsFinderDao,
                                            HttpExecutionContext httpExecutionContext,
                                            FrontendServiceClient frontendServiceClient) {
    this.journeyManager = journeyManager;
    this.formFactory = formFactory;
    this.permissionsFinderDao = permissionsFinderDao;
    this.httpExecutionContext = httpExecutionContext;
    this.frontendServiceClient = frontendServiceClient;
  }

  private CompletionStage<Result> renderWithForm(String controlCode, Form<AdditionalSpecificationsForm> form) {
    return frontendServiceClient.get(controlCode)
        .thenApplyAsync(result ->
            ok(additionalSpecifications.render(form, new AdditionalSpecificationsDisplay(result)))
            , httpExecutionContext.current());
  }

  public CompletionStage<Result> renderForm(String controlCodeVariantText, String goodsTypeText) {
    ControlCodeSubJourney controlCodeSubJourney = ControlCodeSubJourneyHelper.resolveUrlToSubJourneyAndUpdateContext(controlCodeVariantText, goodsTypeText);
    String controlCode = permissionsFinderDao.getSelectedControlCode(controlCodeSubJourney);
    return renderFormInternal(controlCodeSubJourney, controlCode);
  }

  private CompletionStage<Result> renderFormInternal(ControlCodeSubJourney controlCodeSubJourney, String controlCode) {
    Optional<Boolean> additionalSpecificationsApply = permissionsFinderDao.getControlCodeAdditionalSpecificationsApply(controlCodeSubJourney);
    Optional<Boolean> showTechNotes = permissionsFinderDao.getShowTechNotes(controlCodeSubJourney, controlCode);
    AdditionalSpecificationsForm templateForm = new AdditionalSpecificationsForm();
    templateForm.stillDescribesItems = additionalSpecificationsApply.orElse(null);
    templateForm.showTechNotes = showTechNotes.orElse(null);
    return renderWithForm(controlCode, formFactory.form(AdditionalSpecificationsForm.class).fill(templateForm));
  }

  public CompletionStage<Result> handleSubmit() {
    ControlCodeSubJourney controlCodeSubJourney = ControlCodeSubJourneyHelper.resolveContextToSubJourney();
    return handleSubmitInternal(controlCodeSubJourney);
  }

  private CompletionStage<Result> handleSubmitInternal(ControlCodeSubJourney controlCodeSubJourney) {
    Form<AdditionalSpecificationsForm> form = formFactory.form(AdditionalSpecificationsForm.class).bindFromRequest();
    String controlCode = permissionsFinderDao.getSelectedControlCode(controlCodeSubJourney);

    if (form.hasErrors()) {
      return renderWithForm(controlCode, form);
    }
    else {
      Boolean stillDescribesItems = form.get().stillDescribesItems;

      Boolean showTechNotes = form.get().showTechNotes;

      if (showTechNotes != null) {
        permissionsFinderDao.saveShowTechNotes(controlCodeSubJourney, controlCode, showTechNotes);
      }

      if(stillDescribesItems) {
        permissionsFinderDao.saveControlCodeAdditionalSpecificationsApply(controlCodeSubJourney, true);
        return journeyManager.performTransition(StandardEvents.NEXT);
      }
      else {
        permissionsFinderDao.saveControlCodeAdditionalSpecificationsApply(controlCodeSubJourney, false);
        return journeyManager.performTransition(Events.CONTROL_CODE_NOT_APPLICABLE);
      }
    }
  }

  public static class AdditionalSpecificationsForm {

    @Required(message = "You must answer this question")
    public Boolean stillDescribesItems;

    public Boolean showTechNotes;

  }
}
