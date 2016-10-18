package controllers.controlcode;

import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.FrontendServiceClient;
import exceptions.FormStateException;
import journey.Events;
import models.ControlCodeFlowStage;
import models.ExportCategory;
import models.controlcode.DecontrolledItemDisplay;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.controlcode.decontrolledItem;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class DecontrolledItemController {

  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext httpExecutionContext;
  private final FrontendServiceClient frontendServiceClient;

  @Inject
  public DecontrolledItemController(JourneyManager journeyManager,
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
    Optional<ExportCategory> exportCategoryOptional = permissionsFinderDao.getExportCategory();
    boolean isFirearmsOrMilitary = exportCategoryOptional.isPresent() && exportCategoryOptional.get() == ExportCategory.MILITARY;
    return frontendServiceClient.get(permissionsFinderDao.getPhysicalGoodControlCode())
        .thenApplyAsync(result -> ok(decontrolledItem.render(new DecontrolledItemDisplay(result, isFirearmsOrMilitary)))
            , httpExecutionContext.current());
  }

  public CompletionStage<Result> handleSubmit() {
    Form<DecontrolledItemForm> form = formFactory.form(DecontrolledItemForm.class).bindFromRequest();
    if (!form.hasErrors()) {
      String action = form.get().action;
      if ("backToSearch".equals(action)) {
        return journeyManager.performTransition(Events.CONTROL_CODE_FLOW_NEXT, ControlCodeFlowStage.BACK_TO_SEARCH);
      }
      if ("backToSearchResults".equals(action)) {
        return journeyManager.performTransition(Events.CONTROL_CODE_FLOW_NEXT, ControlCodeFlowStage.BACK_TO_SEARCH_RESULTS);
      }
    }
    throw new FormStateException("Unhandled form state");
  }

  public static class DecontrolledItemForm {

    public String action;

  }

}
