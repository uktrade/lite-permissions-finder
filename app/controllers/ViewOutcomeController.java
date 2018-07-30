package controllers;

import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import com.google.inject.Inject;
import components.cms.dao.SessionOutcomeDao;
import components.common.auth.SamlAuthorizer;
import components.common.auth.SpireAuthManager;
import components.common.auth.SpireSAML2Client;
import components.services.AccountService;
import components.services.SessionOutcomeService;
import components.services.UserPrivilegeService;
import controllers.guard.SessionGuardAction;
import exceptions.UnknownParameterException;
import models.AccountData;
import models.enums.SessionOutcomeType;
import models.view.form.ItemDescriptionForm;
import org.pac4j.play.java.Secure;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Result;
import play.mvc.With;
import play.twirl.api.Html;
import triage.config.ControlEntryConfig;
import triage.config.ControllerConfigService;
import triage.config.StageConfig;
import triage.session.SessionOutcome;
import triage.session.SessionService;
import triage.session.TriageSession;
import utils.HtmlUtil;

import java.util.Optional;

@Secure(clients = SpireSAML2Client.CLIENT_NAME, authorizers = SamlAuthorizer.AUTHORIZER_NAME)
public class ViewOutcomeController {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ViewOutcomeController.class);

  private final AccountService accountService;
  private final SessionService sessionService;
  private final SessionOutcomeService sessionOutcomeService;
  private final SessionOutcomeDao sessionOutcomeDao;
  private final UserPrivilegeService userPrivilegeService;
  private final SpireAuthManager spireAuthManager;
  private final ControllerConfigService controllerConfigService;
  private final FormFactory formFactory;
  private final views.html.nlr.nlrRegisterSuccess nlrRegisterSuccess;
  private final views.html.nlr.nlrOutcome nlrOutcome;
  private final views.html.triage.listedOutcomeSaved listedOutcomeSaved;
  private final views.html.nlr.nlrItemDescription nlrItemDescription;

  @Inject
  public ViewOutcomeController(AccountService accountService, SessionService sessionService,
                               SessionOutcomeService sessionOutcomeService,
                               SessionOutcomeDao sessionOutcomeDao, UserPrivilegeService userPrivilegeService,
                               SpireAuthManager spireAuthManager,
                               ControllerConfigService controllerConfigService,
                               FormFactory formFactory, views.html.nlr.nlrRegisterSuccess nlrRegisterSuccess,
                               views.html.nlr.nlrOutcome nlrOutcome,
                               views.html.triage.listedOutcomeSaved listedOutcomeSaved,
                               views.html.nlr.nlrItemDescription nlrItemDescription) {
    this.accountService = accountService;
    this.sessionService = sessionService;
    this.sessionOutcomeService = sessionOutcomeService;
    this.sessionOutcomeDao = sessionOutcomeDao;
    this.userPrivilegeService = userPrivilegeService;
    this.spireAuthManager = spireAuthManager;
    this.controllerConfigService = controllerConfigService;
    this.formFactory = formFactory;
    this.nlrRegisterSuccess = nlrRegisterSuccess;
    this.nlrOutcome = nlrOutcome;
    this.listedOutcomeSaved = listedOutcomeSaved;
    this.nlrItemDescription = nlrItemDescription;
  }

  public Result renderOutcome(String outcomeId) {
    SessionOutcome sessionOutcome = sessionOutcomeDao.getSessionOutcomeById(outcomeId);
    if (sessionOutcome == null) {
      LOGGER.warn("Unknown outcomeId {}", outcomeId);
      throw UnknownParameterException.unknownOutcomeId(outcomeId);
    } else {
      String userId = spireAuthManager.getAuthInfoFromContext().getId();
      if (userPrivilegeService.canViewOutcome(userId, sessionOutcome)) {
        if (sessionOutcome.getOutcomeType() == SessionOutcomeType.CONTROL_ENTRY_FOUND) {
          String resumeCode = sessionService.getSessionById(sessionOutcome.getSessionId()).getResumeCode();
          return ok(listedOutcomeSaved.render(resumeCode, new Html(sessionOutcome.getOutcomeHtml())));
        } else {
          return ok(nlrOutcome.render(new Html(sessionOutcome.getOutcomeHtml())));
        }
      } else {
        LOGGER.error("User with userId {} doesn't have privilege to view outcome with outcomeId {} ",
            userId, sessionOutcome.getId());
        throw UnknownParameterException.unknownOutcomeId(outcomeId);
      }
    }
  }

  @With(SessionGuardAction.class)
  public Result registerSuccess(String sessionId) {
    SessionOutcome sessionOutcome = sessionOutcomeDao.getSessionOutcomeBySessionId(sessionId);
    if (sessionOutcome != null) {
      String resumeCode = sessionService.getSessionById(sessionId).getResumeCode();
      return ok(nlrRegisterSuccess.render(sessionOutcome.getId(), resumeCode));
    } else {
      LOGGER.warn("Unknown sessionId or no outcome for sessionId {}", sessionId);
      throw UnknownParameterException.unknownOutcomeForSessionId(sessionId);
    }
  }

  @With(SessionGuardAction.class)
  public Result saveListedOutcome(String sessionId, String controlEntryId) {
    ControlEntryConfig controlEntryConfig = controllerConfigService.getControlEntryConfig(controlEntryId);

    SessionOutcome sessionOutcome = sessionOutcomeDao.getSessionOutcomeBySessionId(sessionId);
    if (sessionOutcome == null) {
      String userId = spireAuthManager.getAuthInfoFromContext().getId();
      Optional<AccountData> accountDataOptional = accountService.getAccountData(userId);
      if (accountDataOptional.isPresent()) {
        sessionOutcomeService.generateItemListedOutcome(sessionId, userId, accountDataOptional.get(), controlEntryConfig);
      } else {
        return redirect(routes.StaticContentController.renderInvalidUserAccount());
      }
    }
    TriageSession triageSession = sessionService.getSessionById(sessionId);
    return redirect(controllers.licencefinder.routes.EntryController.entry(controlEntryConfig.getControlCode(), triageSession.getResumeCode()));
  }

  @With(SessionGuardAction.class)
  public Result registerNotFoundNlr(String sessionId, String controlEntryId) {
    ControlEntryConfig controlEntryConfig = controllerConfigService.getControlEntryConfig(controlEntryId);

    SessionOutcome sessionOutcome = sessionOutcomeDao.getSessionOutcomeBySessionId(sessionId);
    String submitUrl = controllers.routes.ViewOutcomeController.handleRegisterNotFoundNlrSubmit(sessionId, controlEntryConfig.getId()).toString();
    String resumeCode = sessionService.getSessionById(sessionId).getResumeCode();
    if (sessionOutcome == null) {
      Form<ItemDescriptionForm> itemDescriptionForm = formFactory.form(ItemDescriptionForm.class);
      return ok(nlrItemDescription.render(itemDescriptionForm, resumeCode, submitUrl));
    } else {
      return redirect(routes.ViewOutcomeController.registerSuccess(sessionId));
    }
  }

  @With(SessionGuardAction.class)
  public Result handleRegisterNotFoundNlrSubmit(String sessionId, String controlEntryId) {
    ControlEntryConfig controlEntryConfig = controllerConfigService.getControlEntryConfig(controlEntryId);

    SessionOutcome sessionOutcome = sessionOutcomeDao.getSessionOutcomeBySessionId(sessionId);
    String submitUrl = controllers.routes.ViewOutcomeController.handleRegisterNotFoundNlrSubmit(sessionId, controlEntryConfig.getId()).toString();
    String resumeCode = sessionService.getSessionById(sessionId).getResumeCode();
    if (sessionOutcome == null) {
      Form<ItemDescriptionForm> form = formFactory.form(ItemDescriptionForm.class).bindFromRequest();
      if (form.hasErrors()) {
        return ok(nlrItemDescription.render(form, resumeCode, submitUrl));
      } else {
        String description = form.get().description.trim();
        if (description.length() < 2) {
          return ok(nlrItemDescription.render(form.withError("description", "Item description is required."),
              resumeCode, submitUrl));
        } else {
          String userId = spireAuthManager.getAuthInfoFromContext().getId();
          Optional<AccountData> accountDataOptional = accountService.getAccountData(userId);
          if (accountDataOptional.isPresent()) {
            Html htmlDescription = HtmlUtil.newlinesToParagraphs(description);
            sessionOutcomeService.generateNotFoundNlrLetter(sessionId, userId, accountDataOptional.get(), controlEntryConfig, resumeCode, htmlDescription);
            return redirect(routes.ViewOutcomeController.registerSuccess(sessionId));
          } else {
            return redirect(routes.StaticContentController.renderInvalidUserAccount());
          }
        }
      }
    } else {
      return redirect(routes.ViewOutcomeController.registerSuccess(sessionId));
    }
  }

  @With(SessionGuardAction.class)
  public Result registerDecontrolNlr(String sessionId, String stageId) {
    StageConfig stageConfig = controllerConfigService.getStageConfig(stageId);

    SessionOutcome sessionOutcome = sessionOutcomeDao.getSessionOutcomeBySessionId(sessionId);
    String submitUrl = controllers.routes.ViewOutcomeController.handleRegisterDecontrolNlrSubmit(sessionId, stageConfig.getStageId()).toString();
    String resumeCode = sessionService.getSessionById(sessionId).getResumeCode();
    if (sessionOutcome == null) {
      Form<ItemDescriptionForm> itemDescriptionForm = formFactory.form(ItemDescriptionForm.class);
      return ok(nlrItemDescription.render(itemDescriptionForm, resumeCode, submitUrl));
    } else {
      return redirect(routes.ViewOutcomeController.registerSuccess(sessionId));
    }
  }

  @With(SessionGuardAction.class)
  public Result handleRegisterDecontrolNlrSubmit(String sessionId, String stageId) {
    StageConfig stageConfig = controllerConfigService.getStageConfig(stageId);

    SessionOutcome sessionOutcome = sessionOutcomeDao.getSessionOutcomeBySessionId(sessionId);
    String submitUrl = controllers.routes.ViewOutcomeController.handleRegisterDecontrolNlrSubmit(sessionId, stageConfig.getStageId()).toString();
    String resumeCode = sessionService.getSessionById(sessionId).getResumeCode();
    if (sessionOutcome == null) {
      Form<ItemDescriptionForm> form = formFactory.form(ItemDescriptionForm.class).bindFromRequest();
      if (form.hasErrors()) {
        return ok(nlrItemDescription.render(form, resumeCode, submitUrl));
      } else {
        String description = form.get().description.trim();
        if (description.length() < 2) {
          return ok(nlrItemDescription.render(form.withError("description", "Item description is required."),
              resumeCode, submitUrl));
        } else {
          String userId = spireAuthManager.getAuthInfoFromContext().getId();
          Optional<AccountData> accountDataOptional = accountService.getAccountData(userId);
          if (accountDataOptional.isPresent()) {
            Html htmlDescription = HtmlUtil.newlinesToParagraphs(description);
            sessionOutcomeService.generateDecontrolNlrLetter(sessionId, userId, accountDataOptional.get(), stageConfig,
                resumeCode, htmlDescription);
            return redirect(routes.ViewOutcomeController.registerSuccess(sessionId));
          } else {
            return redirect(routes.StaticContentController.renderInvalidUserAccount());
          }
        }
      }
    } else {
      return redirect(routes.ViewOutcomeController.registerSuccess(sessionId));
    }
  }

}
