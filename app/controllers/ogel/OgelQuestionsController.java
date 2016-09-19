package controllers.ogel;

import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import journey.Events;
import model.OgelActivityType;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import views.html.ogel.ogelQuestions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class OgelQuestionsController {

  private final FormFactory formFactory;
  private final PermissionsFinderDao dao;
  private final HttpExecutionContext ec;
  private final JourneyManager jm;

  @Inject
  public OgelQuestionsController(JourneyManager jm,
                                 FormFactory formFactory,
                                 PermissionsFinderDao dao,
                                 HttpExecutionContext ec) {
    this.jm = jm;
    this.formFactory = formFactory;
    this.dao = dao;
    this.ec = ec;
  }

  public Result renderForm() {
    Optional<OgelQuestionsForm> templateFormOptional = dao.getOgelQuestionsForm();
    OgelQuestionsForm templateForm = templateFormOptional.isPresent() ? templateFormOptional.get() : new OgelQuestionsForm();
    return ok(ogelQuestions.render(formFactory.form(OgelQuestionsForm.class).fill(templateForm)));
  }

  public CompletionStage<Result> handleSubmit() {
    return CompletableFuture.supplyAsync(() -> {
      Form<OgelQuestionsForm> form = formFactory.form(OgelQuestionsForm.class).bindFromRequest();
      if (form.hasErrors()) {
        return CompletableFuture.completedFuture(ok(ogelQuestions.render(form)));
      }
      else {
        OgelQuestionsForm ogelQuestionsForm = form.get();
        dao.saveOgelQuestionsForm(ogelQuestionsForm);
        return jm.performTransition(Events.OGEL_QUESTIONS_ANSWERED);
      }
    }, ec.current()).thenCompose(Function.identity());
  }

  public static class OgelQuestionsForm {

    @Required(message = "You must answer this question")
    public String toGovernment;

    @Required(message = "You must answer this question")
    public String forRepairReplacement;

    @Required(message = "You must answer this question")
    public String forExhibitionDemonstration;

    @Required(message = "You must answer this question")
    public String before1897upto35k;

    public static List<String> formToActivityTypes(Optional<OgelQuestionsForm> ogelQuestionsFormOptional) {
      // TODO before1897upto35k currently unused
      // TODO account for TECH
      List<String> activityTypes = new ArrayList<>();

      if (ogelQuestionsFormOptional.isPresent()) {
        OgelQuestionsForm ogelQuestionsForm = ogelQuestionsFormOptional.get();
        if ("true".equals(ogelQuestionsForm.toGovernment)) {
          activityTypes.add(OgelActivityType.MIL_GOV.value());
        }
        if ("true".equals(ogelQuestionsForm.forRepairReplacement)) {
          activityTypes.add(OgelActivityType.REPAIR.value());
        }
        if ("true".equals(ogelQuestionsForm.forExhibitionDemonstration)) {
          activityTypes.add(OgelActivityType.EXHIBITION.value());
        }
        // Always add these types
        activityTypes.add(OgelActivityType.MIL_ANY.value());
        activityTypes.add(OgelActivityType.DU_ANY.value());
      }

      return activityTypes;
    }

  }

}