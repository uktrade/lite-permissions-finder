package controllers.controlcode;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.frontend.FrontendServiceClient;
import journey.Events;
import model.ControlCodeFlowStage;
import model.ExportCategory;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.controlcode.decontrolledItem;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class DecontrolledItemController {

  private final JourneyManager jm;
  private final FormFactory formFactory;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext ec;
  private final FrontendServiceClient frontendServiceClient;

  @Inject
  public DecontrolledItemController(JourneyManager jm,
                                    FormFactory formFactory,
                                    PermissionsFinderDao permissionsFinderDao,
                                    HttpExecutionContext ec,
                                    FrontendServiceClient frontendServiceClient) {
    this.jm = jm;
    this.formFactory = formFactory;
    this.permissionsFinderDao = permissionsFinderDao;
    this.ec = ec;
    this.frontendServiceClient = frontendServiceClient;
  }

  public CompletionStage<Result> renderForm() {
    Optional<ExportCategory> exportCategoryOptional = permissionsFinderDao.getExportCategory();
    boolean showFirearmsOrMilitary = exportCategoryOptional.isPresent() && exportCategoryOptional.get() == ExportCategory.MILITARY;
    return frontendServiceClient.get(permissionsFinderDao.getPhysicalGoodControlCode())
        .thenApplyAsync(response -> {
          if (response.isOk()) {
            return ok(decontrolledItem.render(response.getFrontendServiceResult(), showFirearmsOrMilitary));
          }
          return badRequest("An issue occurred while processing your request, please try again later.");
        }, ec.current());
  }

  public CompletionStage<Result> handleSubmit() {
    Form<DecontrolledItemForm> form = formFactory.form(DecontrolledItemForm.class).bindFromRequest();
    if (!form.hasErrors()) {
      String action = form.get().action;
      if ("backToSearch".equals(action)) {
        return jm.performTransition(Events.CONTROL_CODE_FLOW_NEXT, ControlCodeFlowStage.BACK_TO_SEARCH);
      }
      if ("backToSearchResults".equals(action)) {
        return jm.performTransition(Events.CONTROL_CODE_FLOW_NEXT, ControlCodeFlowStage.BACK_TO_SEARCH_RESULTS);
      }
    }
    return completedFuture(badRequest("Invalid form state"));
  }

  public static class DecontrolledItemForm {

    public String action;

  }

}
