package controllers;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.common.transaction.TransactionManager;
import components.persistence.ApplicationCodeDao;
import components.persistence.PermissionsFinderDao;
import components.services.PermissionsFinderNotificationClient;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.data.validation.ValidationError;
import play.mvc.Result;
import views.html.startApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class StartApplicationController {

  private static final List<Character> CODE_DIGITS = Collections.unmodifiableList(Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'));

  private final TransactionManager transactionManager;
  private final JourneyManager journeyManager;
  private final FormFactory formFactory;
  private final ApplicationCodeDao applicationCodeDao;
  private final PermissionsFinderDao permissionsFinderDao;
  private final PermissionsFinderNotificationClient notificationClient;

  @Inject
  public StartApplicationController(TransactionManager transactionManager,
                                    JourneyManager journeyManager,
                                    FormFactory formFactory,
                                    PermissionsFinderDao permissionsFinderDao,
                                    ApplicationCodeDao applicationCodeDao,
                                    PermissionsFinderNotificationClient notificationClient) {
	this.transactionManager = transactionManager;
    this.journeyManager = journeyManager;
    this.formFactory = formFactory;
    this.permissionsFinderDao = permissionsFinderDao;
    this.applicationCodeDao = applicationCodeDao;
    this.notificationClient = notificationClient;
  }

  public Result renderForm() {
    // Hack to test if a transaction Id has already been set.
    // TODO JourneyManager getTransactionId() should return a named exception
    try {
      transactionManager.getTransactionId();
    }
    catch (RuntimeException e) {
      transactionManager.createTransaction();
    }

    String applicationCode = permissionsFinderDao.getApplicationCode();
    if (applicationCode == null || applicationCode.isEmpty()) {
      applicationCode = generateApplicationCode();
      // TODO where should this be saved?
      applicationCodeDao.writeTransactionId(applicationCode);
      permissionsFinderDao.saveApplicationCode(applicationCode);
    }

    return ok(startApplication.render(formFactory.form(StartApplicationForm.class), applicationCode));
  }

  public CompletionStage<Result> handleSubmit() {
    Form<StartApplicationForm> form = formFactory.form(StartApplicationForm.class).bindFromRequest();
    if (form.hasErrors()) {
      return completedFuture(ok(startApplication.render(form, permissionsFinderDao.getApplicationCode())));
    }
    String emailAddress = form.get().emailAddress;
    String memorableWord = form.get().memorableWord;
    if (emailAddress != null && !emailAddress.isEmpty() && memorableWord != null && !memorableWord.isEmpty()) {
      permissionsFinderDao.saveEmailAddress(emailAddress);
      permissionsFinderDao.saveMemorableWord(memorableWord);
	  notificationClient.sendApplicationReferenceEmail(emailAddress, permissionsFinderDao.getApplicationCode());
      return journeyManager.startJourney("start");
    }
    return completedFuture(badRequest("Unhandled form state"));
  }

  /**
   * Builds a random application code satisfying the regular expression \[0-9A-Z]{4}[\-][0-9A-Z]{4}\ and Crockford encoding compliant
   * e.g. XME1-BM7S
   * @return The application code
   */
  public String generateApplicationCode() {
    StringBuilder sb = new StringBuilder();
    IntStream.range(0,8).forEach(i -> sb.append(CODE_DIGITS.get(ThreadLocalRandom.current().nextInt(0, CODE_DIGITS.size()))));
    return sb.insert(4,"-").toString();
  }

  public static class StartApplicationForm {

    @Email()
    public String emailAddress;

    @Required(message = "You must enter a memorable word")
    public String memorableWord;

    public List<ValidationError> validate() {
      List<ValidationError> errors = new ArrayList<>();
      if (memorableWord != null && memorableWord.trim().length() < 3) {
        errors.add(new ValidationError("memorableWord", "Please make your word at least three letters in length"));
      }
      return errors.isEmpty() ? null : errors;
    }

  }
}
