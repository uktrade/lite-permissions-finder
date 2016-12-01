package controllers.ogel;

import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import components.services.ogels.ogel.OgelServiceClient;
import exceptions.FormStateException;
import journey.Events;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.ogel.ogelNotApplicable;

import java.util.concurrent.CompletionStage;

public class OgelNotApplicableController {

  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext httpExecutionContext;
  private final OgelServiceClient ogelServiceClient;

  @Inject
  public OgelNotApplicableController(JourneyManager journeyManager,
                                    FormFactory formFactory,
                                    PermissionsFinderDao permissionsFinderDao,
                                    HttpExecutionContext httpExecutionContext,
                                    OgelServiceClient ogelServiceClient) {
    this.journeyManager = journeyManager;
    this.formFactory = formFactory;
    this.permissionsFinderDao = permissionsFinderDao;
    this.httpExecutionContext = httpExecutionContext;
    this.ogelServiceClient = ogelServiceClient;
  }

  public CompletionStage<Result> renderForm() {
    return renderWithForm(formFactory.form(OgelNotApplicableForm.class));
  }

  public CompletionStage<Result> handleSubmit() {
    Form<OgelNotApplicableForm> form = formFactory.form(OgelNotApplicableForm.class).bindFromRequest();
    if (form.hasErrors()) {
      return renderWithForm(form);
    }
    String action = form.get().action;
    if ("continueToLicence".equals(action)) {
      return journeyManager.performTransition(Events.OGEL_CONTINUE_TO_NON_APPLICABLE_LICENCE);
    }
    else if("chooseAnotherLicence".equals(action)) {
      return journeyManager.performTransition(Events.OGEL_CHOOSE_AGAIN);
    }
    else {
      throw new FormStateException("Unknown value for action: \"" + action + "\"");
    }
  }

  public CompletionStage<Result> renderWithForm(Form<OgelNotApplicableForm> form) {
    return ogelServiceClient.get(permissionsFinderDao.getOgelId())
        .thenApplyAsync(ogelResult -> ok(ogelNotApplicable.render(form, ogelResult,
              permissionsFinderDao.getConfirmedControlCode())), httpExecutionContext.current());
  }

  public static class OgelNotApplicableForm {

    public String action;

  }

}
