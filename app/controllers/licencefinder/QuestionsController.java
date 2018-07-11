package controllers.licencefinder;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.inject.Inject;
import components.common.auth.SamlAuthorizer;
import components.common.auth.SpireSAML2Client;
import components.persistence.LicenceFinderDao;
import controllers.guard.LicenceFinderAwaitGuardAction;
import controllers.guard.LicenceFinderUserGuardAction;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Secure(clients = SpireSAML2Client.CLIENT_NAME, authorizers = SamlAuthorizer.AUTHORIZER_NAME)
@With({LicenceFinderUserGuardAction.class, LicenceFinderAwaitGuardAction.class})
public class QuestionsController extends Controller {

  private final FormFactory formFactory;
  private final LicenceFinderDao licenceFinderDao;
  private final views.html.licencefinder.questions questions;

  @Inject
  public QuestionsController(FormFactory formFactory, LicenceFinderDao licenceFinderDao,
                             views.html.licencefinder.questions questions) {
    this.formFactory = formFactory;
    this.licenceFinderDao = licenceFinderDao;
    this.questions = questions;
  }

  public CompletionStage<Result> renderQuestionsForm(String sessionId) {
    QuestionsForm questionsForm = licenceFinderDao.getQuestionsForm(sessionId).orElseGet(QuestionsForm::new);
    return completedFuture(ok(questions.render(formFactory.form(QuestionsForm.class).fill(questionsForm), sessionId)));
  }


  public CompletionStage<Result> handleQuestionsSubmit(String sessionId) {
    Form<QuestionsForm> form = formFactory.form(QuestionsForm.class).bindFromRequest();
    if (form.hasErrors()) {
      return completedFuture(ok(questions.render(form, sessionId)));
    } else {
      licenceFinderDao.saveQuestionsForm(sessionId, form.get());
      return CompletableFuture.completedFuture(redirect(routes.ChooseOgelController.renderResultsForm(sessionId)));
    }
  }

  public static class QuestionsForm {

    @Required(message = "Select whether you are exporting goods for or after repair or replacement")
    public Boolean forRepair;

    @Required(message = "Select whether you are exporting goods for or after exhibition or demonstration")
    public Boolean forExhibition;

    @Required(message = "Select whether your goods were manufactured before 1897 and are worth less than £35,000")
    public Boolean beforeOrLess;

  }
}

