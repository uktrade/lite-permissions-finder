package journey.helpers;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import journey.Events;
import models.ControlCodeFlowStage;
import models.controlcode.ControlCodeJourney;
import models.software.SoftwareCategory;
import org.apache.commons.lang3.StringUtils;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class ControlCodeJourneyHelper {

  private final JourneyManager journeyManager;
  private final SoftwareJourneyHelper softwareJourneyHelper;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ControlCodeJourneyHelper(JourneyManager journeyManager,
                                  SoftwareJourneyHelper softwareJourneyHelper,
                                  PermissionsFinderDao permissionsFinderDao,
                                  HttpExecutionContext httpExecutionContext) {
    this.journeyManager = journeyManager;
    this.softwareJourneyHelper = softwareJourneyHelper;
    this.permissionsFinderDao = permissionsFinderDao;
    this.httpExecutionContext = httpExecutionContext;
  }

  public CompletionStage<Result> notApplicableJourneyTransition(ControlCodeJourney controlCodeJourney) {
    if (controlCodeJourney == ControlCodeJourney.PHYSICAL_GOODS_SEARCH) {
      return journeyManager.performTransition(Events.CONTROL_CODE_FLOW_NEXT, ControlCodeFlowStage.NOT_APPLICABLE);
    }
    else if (controlCodeJourney == ControlCodeJourney.PHYSICAL_GOODS_SEARCH_RELATED_TO_SOFTWARE) {
      return journeyManager.performTransition(Events.CONTROL_CODE_FLOW_NEXT, ControlCodeFlowStage.NOT_APPLICABLE);
    }
    else if (controlCodeJourney == ControlCodeJourney.SOFTWARE_CONTROLS) {
      SoftwareCategory softwareCategory = permissionsFinderDao.getSoftwareCategory().get();
      return softwareJourneyHelper.checkSoftwareControls(softwareCategory)
          .thenComposeAsync(asc ->
                  journeyManager.performTransition(Events.CONTROL_CODE_SOFTWARE_CONTROLS_NOT_APPLICABLE, asc)
              , httpExecutionContext.current());
    }
    else {
      throw new RuntimeException(String.format("Unexpected member of ControlCodeJourney enum: \"%s\""
          , controlCodeJourney.toString()));
    }
  }

  public void clearControlCodeJourneyDaoFields(ControlCodeJourney controlCodeJourney) {
    permissionsFinderDao.clearControlCodeApplies(controlCodeJourney);
    permissionsFinderDao.clearControlCodeDecontrolsApply(controlCodeJourney);
    permissionsFinderDao.clearControlCodeAdditionalSpecificationsApply(controlCodeJourney);
    permissionsFinderDao.clearControlCodeTechnicalNotesApply(controlCodeJourney);
  }

  public void clearControlCodeJourneyDaoFieldsIfChanged(ControlCodeJourney controlCodeJourney, String newSelectedControlCode) {
    String oldSelectedControlCode = permissionsFinderDao.getSelectedControlCode(controlCodeJourney);
    if (!StringUtils.equals(newSelectedControlCode, oldSelectedControlCode)) {
      clearControlCodeJourneyDaoFields(controlCodeJourney);
    }
  }
}
