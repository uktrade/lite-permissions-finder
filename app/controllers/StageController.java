package controllers;

import com.google.inject.Inject;
import components.services.AnswerConfigService;
import components.services.AnswerViewService;
import components.services.BreadcrumbViewService;
import components.services.ProgressViewService;
import components.services.RenderService;
import exceptions.BusinessRuleException;
import models.enums.Action;
import models.enums.PageType;
import models.view.AnswerView;
import models.view.BreadcrumbView;
import models.view.ProgressView;
import models.view.form.AnswerForm;
import models.view.form.MultiAnswerForm;
import org.apache.commons.collections4.ListUtils;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import triage.config.AnswerConfig;
import triage.config.ControlEntryConfig;
import triage.config.JourneyConfigService;
import triage.config.OutcomeType;
import triage.config.StageConfig;
import triage.session.SessionService;
import utils.EnumUtil;
import utils.PageTypeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@With(SessionGuardAction.class)
public class StageController extends Controller {

  private final BreadcrumbViewService breadcrumbViewService;
  private final AnswerConfigService answerConfigService;
  private final AnswerViewService answerViewService;
  private final SessionService sessionService;
  private final FormFactory formFactory;
  private final JourneyConfigService journeyConfigService;
  private final RenderService renderService;
  private final ProgressViewService progressViewService;
  private final views.html.triage.decontrol decontrol;
  private final views.html.triage.selectOne selectOne;
  private final views.html.triage.selectMany selectMany;

  @Inject
  public StageController(BreadcrumbViewService breadcrumbViewService, AnswerConfigService answerConfigService,
                         AnswerViewService answerViewService, SessionService sessionService, FormFactory formFactory,
                         JourneyConfigService journeyConfigService, RenderService renderService,
                         ProgressViewService progressViewService,
                         views.html.triage.selectOne selectOne,
                         views.html.triage.selectMany selectMany,
                         views.html.triage.decontrol decontrol) {
    this.breadcrumbViewService = breadcrumbViewService;
    this.answerConfigService = answerConfigService;
    this.answerViewService = answerViewService;
    this.sessionService = sessionService;
    this.formFactory = formFactory;
    this.journeyConfigService = journeyConfigService;
    this.renderService = renderService;
    this.progressViewService = progressViewService;
    this.selectOne = selectOne;
    this.selectMany = selectMany;
    this.decontrol = decontrol;
  }

  public Result index(String sessionId) {
    return redirectToIndex(sessionId);
  }

  public Result render(String stageId, String sessionId) {
    StageConfig stageConfig = journeyConfigService.getStageConfigById(stageId);
    if (stageConfig == null) {
      return redirectToIndex(sessionId);
    } else {
      PageType pageType = PageTypeUtil.getPageType(stageConfig);
      switch (pageType) {
        case SELECT_ONE:
          AnswerForm answerForm = new AnswerForm();
          answerForm.answer = sessionService.getAnswersForStageId(sessionId, stageId).stream().findFirst().orElse(null);
          Form<AnswerForm> filledAnswerForm = formFactory.form(AnswerForm.class).fill(answerForm);
          return renderSelectOne(filledAnswerForm, stageId, sessionId);
        case SELECT_MANY:
          MultiAnswerForm multiAnswerForm = new MultiAnswerForm();
          multiAnswerForm.answers = new ArrayList<>(sessionService.getAnswersForStageId(sessionId, stageId));
          Form<MultiAnswerForm> filledMultiAnswerFormForm = formFactory.form(MultiAnswerForm.class).fill(multiAnswerForm);
          return renderSelectMany(filledMultiAnswerFormForm, stageId, sessionId);
        case DECONTROL:
          MultiAnswerForm form = new MultiAnswerForm();
          form.answers = new ArrayList<>(sessionService.getAnswersForStageId(sessionId, stageId));
          Form<MultiAnswerForm> filledForm = formFactory.form(MultiAnswerForm.class).fill(form);
          return renderDecontrol(filledForm, stageId, sessionId);
        case UNKNOWN:
        default:
          Logger.error("Unknown stageId " + stageId);
          return redirectToIndex(sessionId);
      }
    }
  }

  public Result handleSubmit(String stageId, String sessionId) {
    StageConfig stageConfig = journeyConfigService.getStageConfigById(stageId);
    if (stageConfig == null) {
      Logger.error("Unknown stageId " + stageId);
      return redirectToIndex(sessionId);
    } else {
      PageType pageType = PageTypeUtil.getPageType(stageConfig);
      switch (pageType) {
        case SELECT_ONE:
          return handleSelectOneSubmit(stageId, sessionId, stageConfig);
        case SELECT_MANY:
          return handleSelectManySubmit(stageId, sessionId, stageConfig);
        case DECONTROL:
          return handleDecontrolSubmit(stageId, sessionId, stageConfig);
        case UNKNOWN:
        default:
          Logger.error("Unknown stageId " + stageId);
          return redirectToIndex(sessionId);
      }
    }
  }

  private Result renderSelectOne(Form<AnswerForm> answerFormForm, String stageId, String sessionId) {
    StageConfig stageConfig = journeyConfigService.getStageConfigById(stageId);
    String title = stageConfig.getQuestionTitle().orElse("Select one");
    String explanatoryText = renderService.getExplanatoryText(stageConfig);
    List<AnswerView> answerViews = answerViewService.createAnswerViews(stageConfig);
    BreadcrumbView breadcrumbView = breadcrumbViewService.createBreadcrumbView(stageId);
    ProgressView progressView = progressViewService.createProgressView(stageConfig);
    return ok(selectOne.render(answerFormForm, stageId, sessionId, progressView, title, explanatoryText, answerViews, breadcrumbView));
  }

  private Result renderDecontrol(Form<MultiAnswerForm> multiAnswerForm, String stageId, String sessionId) {
    StageConfig stageConfig = journeyConfigService.getStageConfigById(stageId);
    String title = stageConfig.getQuestionTitle().orElse("Does your item have any of these characteristics?");
    String explanatoryText = renderService.getExplanatoryText(stageConfig);
    List<AnswerView> answerViews = answerViewService.createAnswerViews(stageConfig);
    ControlEntryConfig controlEntryConfig = stageConfig.getRelatedControlEntry()
        .orElseThrow(() -> new BusinessRuleException("Missing relatedControlEntry for decontrol stage " + stageId));
    String controlCode = controlEntryConfig.getControlCode();
    BreadcrumbView breadcrumbView = breadcrumbViewService.createBreadcrumbView(stageId);
    return ok(decontrol.render(multiAnswerForm, stageId, sessionId, controlCode, title, explanatoryText, answerViews, breadcrumbView));
  }

  private Result renderSelectMany(Form<MultiAnswerForm> multiAnswerFormForm, String stageId, String sessionId) {
    StageConfig stageConfig = journeyConfigService.getStageConfigById(stageId);
    String title = stageConfig.getQuestionTitle().orElse("Select at least one");
    String explanatoryText = renderService.getExplanatoryText(stageConfig);
    List<AnswerView> answerViews = answerViewService.createAnswerViews(stageConfig);
    BreadcrumbView breadcrumbView = breadcrumbViewService.createBreadcrumbView(stageId);
    ProgressView progressView = progressViewService.createProgressView(stageConfig);
    return ok(selectMany.render(multiAnswerFormForm, stageId, sessionId, progressView, title, explanatoryText, answerViews, breadcrumbView));
  }

  private Result handleDecontrolSubmit(String stageId, String sessionId, StageConfig stageConfig) {
    Form<MultiAnswerForm> multiAnswerFormForm = formFactory.form(MultiAnswerForm.class).bindFromRequest();
    String actionParam = multiAnswerFormForm.rawData().get("action");
    Action action = EnumUtil.parse(actionParam, Action.class);
    if (multiAnswerFormForm.hasErrors()) {
      Logger.error("MultiAnswerForm has unexpected errors");
      return redirectToStage(stageId, sessionId);
    } else {
      if (action == Action.CONTINUE) {
        List<String> actualAnswers = ListUtils.emptyIfNull(multiAnswerFormForm.get().answers);
        List<AnswerConfig> matchingAnswers = answerConfigService.getMatchingAnswerConfigs(actualAnswers, stageConfig);
        if (matchingAnswers.isEmpty()) {
          return renderDecontrol(multiAnswerFormForm.withError("answers", "Please select at least one answer"),
              stageId, sessionId);
        } else {
          sessionService.saveAnswersForStageId(sessionId, stageId, getAnswerIds(matchingAnswers));
          return redirect(routes.OutcomeController.outcomeDecontrol(stageId, sessionId));
        }
      } else if (action == Action.NONE) {
        Optional<String> nextStageId = stageConfig.getNextStageId();
        if (nextStageId.isPresent()) {
          return redirectToStage(nextStageId.get(), sessionId);
        } else if (stageConfig.getOutcomeType().map(e -> e == OutcomeType.CONTROL_ENTRY_FOUND).orElse(false)) {
          String controlEntryId = stageConfig.getRelatedControlEntry()
              .map(ControlEntryConfig::getId)
              .orElseThrow(() -> new BusinessRuleException(String.format(
                  "Decontrol stage %s must have an associated control entry if it has a CONTROL_ENTRY_FOUND outcome type",
                  stageId)));

          return redirect(routes.OutcomeController.outcomeListed(controlEntryId, sessionId));
        } else {
          Logger.error("Decontrol stageConfig doesn't have nextStageId or applicable outcomeType");
          return redirectToStage(stageId, sessionId);
        }
      } else {
        Logger.error("Unknown action " + actionParam);
        return redirectToStage(stageId, sessionId);
      }
    }
  }

  private Result handleSelectManySubmit(String stageId, String sessionId, StageConfig stageConfig) {
    Form<MultiAnswerForm> multiAnswerFormForm = formFactory.form(MultiAnswerForm.class).bindFromRequest();
    String actionParam = multiAnswerFormForm.rawData().get("action");
    Action action = EnumUtil.parse(actionParam, Action.class);
    if (multiAnswerFormForm.hasErrors()) {
      Logger.error("MultiAnswerForm has unexpected errors");
      return redirectToStage(stageId, sessionId);
    } else {
      if (action == Action.CONTINUE) {
        List<String> actualAnswers = ListUtils.emptyIfNull(multiAnswerFormForm.get().answers);
        List<AnswerConfig> matchingAnswers = answerConfigService.getMatchingAnswerConfigs(actualAnswers, stageConfig);
        if (matchingAnswers.isEmpty()) {
          return renderSelectMany(multiAnswerFormForm.withError("answers", "Please select at least one answer"),
              stageId, sessionId);
        } else {
          AnswerConfig answerConfig = answerConfigService.getAnswerConfigWithLowestPrecedence(matchingAnswers);
          sessionService.saveAnswersForStageId(sessionId, stageId, getAnswerIds(matchingAnswers));
          return resultForStandardStageAnswer(stageId, sessionId, answerConfig);
        }
      } else if (action == Action.NONE) {
        return redirect(routes.OutcomeController.outcomeDropout(sessionId));
      } else {
        Logger.error("Unknown action " + actionParam);
        return redirectToStage(stageId, sessionId);
      }
    }
  }

  private Set<String> getAnswerIds(List<AnswerConfig> answers) {
    return answers.stream()
        .map(AnswerConfig::getAnswerId)
        .collect(Collectors.toSet());
  }

  private Result handleSelectOneSubmit(String stageId, String sessionId, StageConfig stageConfig) {
    Form<AnswerForm> answerForm = formFactory.form(AnswerForm.class).bindFromRequest();
    String actionParam = answerForm.rawData().get("action");
    String answer = answerForm.rawData().get("answer");
    Action action = EnumUtil.parse(actionParam, Action.class);
    if (action == Action.CONTINUE) {
      Optional<AnswerConfig> answerConfigOptional = stageConfig.getAnswerConfigs().stream()
          .filter(answerConfigIterate -> answerConfigIterate.getAnswerId().equals(answer))
          .findAny();
      if (answerConfigOptional.isPresent()) {
        AnswerConfig answerConfig = answerConfigOptional.get();
        sessionService.saveAnswersForStageId(sessionId, stageId, Collections.singleton(answerConfig.getAnswerId()));
        return resultForStandardStageAnswer(stageId, sessionId, answerConfig);
      } else {
        Logger.error("Unknown answer " + answer);
        return renderSelectOne(answerForm, stageId, sessionId);
      }
    } else if (action == Action.NONE) {
      return redirect(routes.OutcomeController.outcomeDropout(sessionId));
    } else {
      Logger.error("Unknown action " + actionParam);
      return redirectToStage(stageId, sessionId);
    }
  }

  private Result resultForStandardStageAnswer(String stageId, String sessionId, AnswerConfig answerConfig) {
    if (answerConfig.getOutcomeType().isPresent()) {
      OutcomeType outcomeType = answerConfig.getOutcomeType().get();
      if (outcomeType == OutcomeType.CONTROL_ENTRY_FOUND) {
        String controlEntryId = answerConfig.getAssociatedControlEntryConfig()
            .map(ControlEntryConfig::getId)
            .orElseThrow(() -> new BusinessRuleException("Expected a control code to be associated with answer " +
                answerConfig.getAnswerId()));
        return redirect(controllers.routes.OutcomeController.outcomeListed(controlEntryId, sessionId));
      } else if (outcomeType == OutcomeType.TOO_COMPLEX) {
        //TODO too complex for code finder outcome
        return ok("Too complex content TODO");
      } else {
        Logger.error("Unexpected outcome type %s on answer %s", outcomeType, answerConfig.getAnswerId());
        return redirectToStage(stageId, sessionId);
      }
    } else {
      Optional<String> nextStageId = answerConfig.getNextStageId();
      if (nextStageId.isPresent()) {
        return redirectToStage(nextStageId.get(), sessionId);
      } else {
        Logger.error("AnswerConfig doesn't have next stageId.");
        return redirectToStage(stageId, sessionId);
      }
    }
  }

  private Result redirectToIndex(String sessionId) {
    String initialStageId = journeyConfigService.getInitialStageId();
    return redirect(routes.StageController.render(initialStageId, sessionId));
  }

  private Result redirectToStage(String stageId, String sessionId) {
    return redirect(routes.StageController.render(stageId, sessionId));
  }

}
