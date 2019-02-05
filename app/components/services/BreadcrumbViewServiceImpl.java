package components.services;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import components.cms.dao.JourneyDao;
import components.cms.dao.SessionDao;
import components.comparator.AlphanumComparator;
import controllers.routes;
import models.cms.Journey;
import models.cms.enums.QuestionType;
import models.view.BreadcrumbItemView;
import models.view.BreadcrumbView;
import models.view.NoteView;
import models.view.RelatedEntryView;
import models.view.SubAnswerView;
import triage.config.ControlEntryConfig;
import triage.config.JourneyConfigService;
import triage.config.StageConfig;
import triage.session.TriageSession;
import triage.text.HtmlRenderOption;
import triage.text.HtmlRenderService;
import utils.ListNameToFriendlyNameUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BreadcrumbViewServiceImpl implements BreadcrumbViewService {

  private final JourneyConfigService journeyConfigService;
  private final RenderService renderService;
  private final HtmlRenderService htmlRenderService;
  private final AnswerViewService answerViewService;
  private final SessionDao sessionDao;
  private final JourneyDao journeyDao;

  @Inject
  public BreadcrumbViewServiceImpl(JourneyConfigService journeyConfigService, RenderService renderService,
                                   HtmlRenderService htmlRenderService, AnswerViewService answerViewService,
                                   SessionDao sessionDao, JourneyDao journeyDao) {
    this.journeyConfigService = journeyConfigService;
    this.renderService = renderService;
    this.htmlRenderService = htmlRenderService;
    this.answerViewService = answerViewService;
    this.sessionDao = sessionDao;
    this.journeyDao = journeyDao;
  }

  @Override
  public BreadcrumbView createBreadcrumbViewFromControlEntry(String sessionId, ControlEntryConfig controlEntryConfig) {
    List<BreadcrumbItemView> breadcrumbItemViews = createBreadcrumbItemViews(sessionId, controlEntryConfig, true);
    return new BreadcrumbView(breadcrumbItemViews, new ArrayList<>());
  }

  @Override
  public BreadcrumbView createBreadcrumbView(StageConfig stageConfig, String sessionId, boolean includeChangeLinks,
                                             HtmlRenderOption... htmlRenderOptions) {
    ControlEntryConfig controlEntryConfig = getControlEntryConfig(stageConfig);

    List<BreadcrumbItemView> breadcrumbItemViews = createBreadcrumbItemViews(sessionId, controlEntryConfig, includeChangeLinks,
        htmlRenderOptions);
    List<NoteView> noteViews = createNoteViews(stageConfig.getStageId());
    return new BreadcrumbView(breadcrumbItemViews, noteViews);
  }

  @Override
  public ControlEntryConfig getControlEntryConfig(StageConfig stageConfig) {
    Optional<ControlEntryConfig> controlEntryConfigOptional = stageConfig.getRelatedControlEntry();
    if (controlEntryConfigOptional.isPresent()) {
      return controlEntryConfigOptional.get();
    } else {
      StageConfig parentStageConfig = journeyConfigService.getStageConfigForPreviousStage(stageConfig.getStageId());
      if (parentStageConfig != null) {
        return getControlEntryConfig(parentStageConfig);
      } else {
        return null;
      }
    }
  }

  @Override
  public List<BreadcrumbItemView> createBreadcrumbItemViews(String sessionId, ControlEntryConfig controlEntryConfig,
                                                            boolean includeChangeLinks,
                                                            HtmlRenderOption... htmlRenderOptions) {
    TriageSession triageSession = sessionDao.getSessionById(sessionId);
    Journey journey = journeyDao.getJourney(triageSession.getJourneyId());
    List<BreadcrumbItemView> breadcrumbItemViews = new ArrayList<>();
    if (controlEntryConfig != null) {
      breadcrumbItemViews.addAll(createControlCodeBreadcrumbItemViews(sessionId, controlEntryConfig, includeChangeLinks, htmlRenderOptions));
    }
    breadcrumbItemViews.add(new BreadcrumbItemView(null, ListNameToFriendlyNameUtil.GetFriendlyNameFromListName(journey.getJourneyName()), null, new ArrayList<>()));
    return Lists.reverse(breadcrumbItemViews);
  }

  private List<BreadcrumbItemView> createControlCodeBreadcrumbItemViews(String sessionId,
                                                                        ControlEntryConfig controlEntryConfig,
                                                                        boolean includeChangeLinks,
                                                                        HtmlRenderOption... htmlRenderOptions) {
    String controlCode = controlEntryConfig.getControlCode();
    List<String> stageIds = journeyConfigService.getStageIdsForControlEntry(controlEntryConfig);
    List<NoteView> noteViews = createNoteViews(stageIds, htmlRenderOptions);
    String description = renderService.getSummaryDescription(controlEntryConfig, htmlRenderOptions);
    String url = null;
    if (includeChangeLinks) {
      url = createChangeUrl(sessionId, stageIds);
    }
    BreadcrumbItemView breadcrumbItemView = new BreadcrumbItemView(controlCode, description, url, noteViews);
    List<BreadcrumbItemView> breadcrumbItemViews = new ArrayList<>();
    breadcrumbItemViews.add(breadcrumbItemView);
    Optional<ControlEntryConfig> parentControlEntry = controlEntryConfig.getParentControlEntry();
    parentControlEntry.ifPresent(parent -> breadcrumbItemViews.addAll(createControlCodeBreadcrumbItemViews(sessionId, parent, includeChangeLinks, htmlRenderOptions)));
    return breadcrumbItemViews;
  }

  private String createChangeUrl(String sessionId, List<String> stageIds) {
    Optional<StageConfig> stageConfigOptional = stageIds.stream()
        .map(journeyConfigService::getStageConfigById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(stageConfigIterate -> stageConfigIterate.getQuestionType() == QuestionType.STANDARD ||
            stageConfigIterate.getQuestionType() == QuestionType.ITEM)
        .findAny()
        .map(stageConfigIterate -> journeyConfigService.getStageConfigForPreviousStage(stageConfigIterate.getStageId()))
        .map(this::getNonDecontrolStageConfig);
    if (stageConfigOptional.isPresent()) {
      StageConfig stageConfig = stageConfigOptional.get();
      return routes.StageController.render(stageConfig.getStageId(), sessionId).toString();
    } else {
      return null;
    }
  }

  private StageConfig getNonDecontrolStageConfig(StageConfig stageConfig) {
    if (stageConfig.getQuestionType() == QuestionType.DECONTROL) {
      StageConfig parentStageConfig = journeyConfigService.getStageConfigForPreviousStage(stageConfig.getStageId());
      if (parentStageConfig != null) {
        return getNonDecontrolStageConfig(parentStageConfig);
      } else {
        return null;
      }
    } else {
      return stageConfig;
    }
  }

  @Override
  public String createDecontrolUrl(String sessionId, ControlEntryConfig controlEntryConfig) {
    List<String> stageIds = journeyConfigService.getStageIdsForControlEntry(controlEntryConfig);
    StageConfig stageConfig = stageIds.stream()
        .map(journeyConfigService::getStageConfigById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(stageConfigIterate -> stageConfigIterate.getQuestionType() == QuestionType.DECONTROL)
        .findAny()
        .orElse(null);
    if (stageConfig != null) {
      return routes.StageController.render(stageConfig.getStageId(), sessionId).toString();
    } else {
      return null;
    }
  }

  @Override
  public List<RelatedEntryView> createRelatedEntryViews(String sessionId, ControlEntryConfig controlEntryConfig,
                                                       boolean includeChangeLinks,
                                                       HtmlRenderOption... htmlRenderOptions) {
    List<ControlEntryConfig> controlEntryHierarchy = new ArrayList<>();
    controlEntryHierarchy.add(controlEntryConfig);
    ControlEntryConfig nextControlEntryConfig = controlEntryConfig;
    while (nextControlEntryConfig.getParentControlEntry().isPresent()) {
        nextControlEntryConfig = nextControlEntryConfig.getParentControlEntry().get();
        controlEntryHierarchy.add(nextControlEntryConfig);
    }

    return controlEntryHierarchy.stream()
        .flatMap(entry -> journeyConfigService.getRelatedControlEntries(entry).stream())
        .collect(Collectors.toMap(ControlEntryConfig::getControlCode, e -> e, (a, b) -> a)) //Distinct by control code
        .values()
        .stream()
        .map(entry -> createRelatedEntryView(sessionId, includeChangeLinks, entry, htmlRenderOptions))
        .sorted(Comparator.comparing(RelatedEntryView::getControlCode, new AlphanumComparator()))
        .collect(Collectors.toList());
  }

  private RelatedEntryView createRelatedEntryView(String sessionId, boolean includeChangeLinks,
                                                  ControlEntryConfig controlEntryConfig,
                                                  HtmlRenderOption... htmlRenderOptions) {
    String fullDescription = renderService.getFullDescription(controlEntryConfig, htmlRenderOptions);
    String changeUrl;
    if (includeChangeLinks) {
      changeUrl = createChangeUrl(sessionId, journeyConfigService.getStageIdsForControlEntry(controlEntryConfig));
    } else {
      changeUrl = null;
    }
    List<SubAnswerView> subAnswerViews = answerViewService.createSubAnswerViews(controlEntryConfig, true,
        htmlRenderOptions);

    return new RelatedEntryView(controlEntryConfig.getControlCode(), fullDescription, changeUrl, subAnswerViews);
  }

  private List<NoteView> createNoteViews(List<String> stageIds, HtmlRenderOption... htmlRenderOptions) {
    return stageIds.stream()
        .map(stageId -> createNoteViews(stageId, htmlRenderOptions))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<NoteView> createNoteViews(String stageId, HtmlRenderOption... htmlRenderOptions) {
    return journeyConfigService.getNoteConfigsByStageId(stageId).stream()
        .map(noteConfig -> new NoteView(htmlRenderService.convertRichTextToHtml(noteConfig.getNoteText(), htmlRenderOptions)))
        .collect(Collectors.toList());
  }

}
