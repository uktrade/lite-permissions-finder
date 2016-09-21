package controllers.ogel;

import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import components.services.ogels.ogel.OgelServiceClient;
import journey.Events;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.ogel.ogelSummary;

import java.util.concurrent.CompletionStage;

public class OgelSummaryController {

  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext httpExecutionContext;
  private final OgelServiceClient ogelServiceClient;

  @Inject
  public OgelSummaryController(JourneyManager journeyManager,
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
    return renderWithForm(formFactory.form(OgelSummaryForm.class));
  }

  public CompletionStage<Result> handleSubmit() {
    Form<OgelSummaryForm> form = formFactory.form(OgelSummaryForm.class).bindFromRequest();
    if (form.hasErrors()) {
      return renderWithForm(form);
    }
    return journeyManager.performTransition(Events.OGEL_REGISTERED);
  }

  public CompletionStage<Result> renderWithForm(Form<OgelSummaryForm> form) {
    return ogelServiceClient.get(permissionsFinderDao.getOgelId())
        .thenApplyAsync(response -> {
          if (!response.isOk()) {
            return badRequest("An issue occurred while processing your request, please try again later.");
          }
          return ok(ogelSummary.render(form, response.getResult()));
        }, httpExecutionContext.current());
  }

  public static class OgelSummaryForm {

    @Required(message = "You must confirm you have read the full licence text before you can register")
    public String fullLicenceRead;

  }

}
