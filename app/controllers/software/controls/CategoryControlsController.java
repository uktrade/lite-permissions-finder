package controllers.software.controls;

import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.category.controls.CategoryControlsServiceClient;
import exceptions.FormStateException;
import journey.Events;
import models.software.SoftwareCategory;
import models.software.controls.ControlsBaseDisplay;
import org.apache.commons.lang3.StringUtils;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.search.controls.categoryControls;

import java.util.concurrent.CompletionStage;

public class CategoryControlsController {
  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final PermissionsFinderDao permissionsFinderDao;
  private final CategoryControlsServiceClient categoryControlsServiceClient;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public CategoryControlsController(JourneyManager journeyManager,
                                    FormFactory formFactory,
                                    PermissionsFinderDao permissionsFinderDao,
                                    CategoryControlsServiceClient categoryControlsServiceClient,
                                    HttpExecutionContext httpExecutionContext) {
    this.journeyManager = journeyManager;
    this.formFactory = formFactory;
    this.permissionsFinderDao = permissionsFinderDao;
    this.categoryControlsServiceClient = categoryControlsServiceClient;
    this.httpExecutionContext = httpExecutionContext;
  }

  public CompletionStage<Result> renderForm() {
    return renderWithForm(formFactory.form(ControlsBaseForm.class));
  }

  public CompletionStage<Result> handleSubmit() {
    Form<ControlsBaseForm> form = formFactory.form(ControlsBaseForm.class).bindFromRequest();
    if (form.hasErrors()) {
      renderWithForm(form);
    }
    String action = form.get().action;
    String controlCode = form.get().controlCode;
    if (StringUtils.isNotEmpty(action)) {
      if ("noMatchedControlCode".equals(action)) {
        return journeyManager.performTransition(Events.NONE_MATCHED);
      }
      else {
        throw new FormStateException(String.format("Unknown value for action: \"%s\"", action));
      }
    }
    else if (StringUtils.isNotEmpty(controlCode)) {
      return journeyManager.performTransition(Events.CONTROL_CODE_SELECTED);
    }
    else {
      throw new FormStateException("Unhandled form state");
    }
  }

  public CompletionStage<Result> renderWithForm(Form<ControlsBaseForm> form) {
    // Software category is a requirement for this stage in the journey
    SoftwareCategory softwareCategory = permissionsFinderDao.getDualUseSoftwareCategory().get();
    int count =
        softwareCategory == SoftwareCategory.MILITARY ? 0
            : softwareCategory == SoftwareCategory.DUMMY ? 1
            : softwareCategory == SoftwareCategory.RADIOACTIVE ? 2
            : 0;

    return categoryControlsServiceClient.get(softwareCategory, count)
        .thenApplyAsync(result -> {
          ControlsBaseDisplay display = new ControlsBaseDisplay(result.controlCodes);
          return ok(categoryControls.render(form, display));
        }, httpExecutionContext.current());
  }

}