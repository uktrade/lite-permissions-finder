package controllers.controlcode;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.frontend.FrontendServiceClient;
import journey.Events;
import models.ControlCodeFlowStage;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.controlcode.decontrols;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class DecontrolsController {

  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext httpExecutionContext;
  private final FrontendServiceClient frontendServiceClient;

  @Inject
  public DecontrolsController(JourneyManager journeyManager,
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

  public CompletionStage<Result> renderForm() {
    return frontendServiceClient.get(permissionsFinderDao.getPhysicalGoodControlCode(), httpExecutionContext)
        .thenApplyAsync(response -> {
          if (response.isOk()) {
            return ok(decontrols.render(formFactory.form(DecontrolsForm.class), response.getFrontendServiceResult()));
          }
          return badRequest("An issue occurred while processing your request, please try again later.");
        }, httpExecutionContext.current());
  }

  public CompletionStage<Result> handleSubmit(){
    Form<DecontrolsForm> form = formFactory.form(DecontrolsForm.class).bindFromRequest();
    String code = permissionsFinderDao.getPhysicalGoodControlCode();
    return frontendServiceClient.get(code, httpExecutionContext)
        .thenApplyAsync(response -> {
          if (response.isOk()) {
            if (form.hasErrors()) {
              return completedFuture(ok(decontrols.render(form, response.getFrontendServiceResult())));
            }
            String decontrolsDescribeItem = form.get().decontrolsDescribeItem;
            if("true".equals(decontrolsDescribeItem)) {
              return journeyManager.performTransition(Events.CONTROL_CODE_FLOW_NEXT, ControlCodeFlowStage.DECONTROLLED_ITEM);
            }
            if ("false".equals(decontrolsDescribeItem)) {
              if (response.getFrontendServiceResult().controlCodeData.canShowTechnicalNotes()) {
                return journeyManager.performTransition(Events.CONTROL_CODE_FLOW_NEXT, ControlCodeFlowStage.TECHNICAL_NOTES);
              }
              else {
                return journeyManager.performTransition(Events.CONTROL_CODE_FLOW_NEXT, ControlCodeFlowStage.CONFIRMED);
              }
            }
          }
          return completedFuture(badRequest("An issue occurred while processing your request, please try again later."));
        }, httpExecutionContext.current()).thenCompose(Function.identity());
  }

  public static class DecontrolsForm {

    @Required(message = "You must answer this question")
    public String decontrolsDescribeItem;

  }

}
