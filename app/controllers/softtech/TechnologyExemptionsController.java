package controllers.softtech;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.common.journey.StandardEvents;
import components.persistence.PermissionsFinderDao;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.mvc.Result;
import views.html.softtech.technologyExemptions;

import java.util.concurrent.CompletionStage;

public class TechnologyExemptionsController {
  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final PermissionsFinderDao permissionsFinderDao;

  @Inject
  public TechnologyExemptionsController(JourneyManager journeyManager,
                                        FormFactory formFactory,
                                        PermissionsFinderDao permissionsFinderDao) {
    this.journeyManager = journeyManager;
    this.formFactory = formFactory;
    this.permissionsFinderDao = permissionsFinderDao;
  }

  public Result renderForm() {
    TechnologyExemptionsForm templateForm = new TechnologyExemptionsForm();
    templateForm.doExemptionsApply = permissionsFinderDao.getTechnologyExemptionsApply().orElse(null);
    return ok(technologyExemptions.render(formFactory.form(TechnologyExemptionsForm.class).fill(templateForm)));
  }

  public CompletionStage<Result> handleSubmit() {
    Form<TechnologyExemptionsForm> form = formFactory.form(TechnologyExemptionsForm.class).bindFromRequest();
    if (form.hasErrors()) {
      completedFuture(ok(technologyExemptions.render(form)));
    }
    Boolean doExemptionsApply = form.get().doExemptionsApply;
    permissionsFinderDao.saveTechnologyExemptionsApply(doExemptionsApply);
    if (doExemptionsApply) {
      return journeyManager.performTransition(StandardEvents.YES);
    }
    else {
      return journeyManager.performTransition(StandardEvents.NO);
    }
  }

  public static class TechnologyExemptionsForm {

    @Required(message = "Select whether you're exporting any of the types of technical information listed")
    public Boolean doExemptionsApply;

  }

}