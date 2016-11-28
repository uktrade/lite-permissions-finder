package journey.helpers;

import com.google.inject.Inject;
import components.common.journey.JourneyManager;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.controls.ControlCode;
import components.services.controlcode.controls.catchall.CatchallControlsServiceClient;
import components.services.controlcode.controls.category.CategoryControlsServiceClient;
import components.services.controlcode.controls.related.RelatedControlsServiceClient;
import components.services.controlcode.controls.relationship.SoftwareAndTechnologyRelationshipServiceClient;
import journey.Events;
import models.GoodsType;
import models.controlcode.ControlCodeJourney;
import models.software.ApplicableSoftwareControls;
import models.software.CatchallSoftwareControlsFlow;
import models.software.Relationship;
import models.software.SoftwareCatchallControlsNotApplicableFlow;
import models.software.SoftwareCategory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class SoftwareJourneyHelper {

  private final CategoryControlsServiceClient categoryControlsServiceClient;
  private final RelatedControlsServiceClient relatedControlsServiceClient;
  private final CatchallControlsServiceClient catchallControlsServiceClient;
  private final SoftwareAndTechnologyRelationshipServiceClient softwareAndTechnologyRelationshipServiceClient;
  private final PermissionsFinderDao permissionsFinderDao;
  private final HttpExecutionContext httpExecutionContext;
  private final JourneyManager journeyManager;

  @Inject
  public SoftwareJourneyHelper(CategoryControlsServiceClient categoryControlsServiceClient,
                               RelatedControlsServiceClient relatedControlsServiceClient,
                               CatchallControlsServiceClient catchallControlsServiceClient,
                               SoftwareAndTechnologyRelationshipServiceClient softwareAndTechnologyRelationshipServiceClient,
                               PermissionsFinderDao permissionsFinderDao,
                               HttpExecutionContext httpExecutionContext,
                               JourneyManager journeyManager) {
    this.categoryControlsServiceClient = categoryControlsServiceClient;
    this.relatedControlsServiceClient = relatedControlsServiceClient;
    this.catchallControlsServiceClient = catchallControlsServiceClient;
    this.softwareAndTechnologyRelationshipServiceClient = softwareAndTechnologyRelationshipServiceClient;
    this.permissionsFinderDao = permissionsFinderDao;
    this.httpExecutionContext = httpExecutionContext;
    this.journeyManager = journeyManager;
  }

  /**
   * Check for software controls of the given software category
   * Note: Writes to DAO if ONE would be returns and saveToDao is true. This is a small shortcut, preventing a
   * separate call to CategoryControlsServiceClient request the single control code to save.
   * @param softwareCategory The software category to check the controls of
   * @param saveToDao Whether to save the single control code to the DAO, if a single control code were to be returned
   *                  by the CategoryControlsServiceClient request
   * @return The applicable software controls
   */
  public CompletionStage<ApplicableSoftwareControls> checkSoftwareControls(SoftwareCategory softwareCategory, boolean saveToDao) {
    return categoryControlsServiceClient.get(GoodsType.SOFTWARE, softwareCategory) //TODO TECHNOLOGY
        .thenApplyAsync(result -> {
          int size = result.controlCodes.size();
          if (size == 0) {
            return ApplicableSoftwareControls.ZERO;
          }
          else if (size == 1) {
            // Saving to the DAO here prevents a separate call to the CategoryControlsServiceClient, if not a little hacky
            if (saveToDao) {
              ControlCode controlCode = result.controlCodes.get(0);
              permissionsFinderDao.clearAndUpdateControlCodeJourneyDaoFieldsIfChanged(
                  ControlCodeJourney.SOFTWARE_CONTROLS, controlCode.controlCode);
            }
            return ApplicableSoftwareControls.ONE;
          }
          else if (size > 1) {
            return ApplicableSoftwareControls.GREATER_THAN_ONE;
          }
          else {
            throw new RuntimeException(String.format("Invalid value for size: \"%d\"", size));
          }
        }, httpExecutionContext.current());
  }

  /**
   * Check for software controls of the given software category
   * @param softwareCategory The software category to check the controls of
   * @return The applicable software controls
   */
  public CompletionStage<ApplicableSoftwareControls> checkSoftwareControls(SoftwareCategory softwareCategory) {
    return checkSoftwareControls(softwareCategory, false);
  }


  public CompletionStage<ApplicableSoftwareControls> checkRelatedSoftwareControls(String controlCode, boolean saveToDao) {

    return relatedControlsServiceClient.get(GoodsType.SOFTWARE, controlCode) // TODO TECHNOLOGY
        .thenApplyAsync(result -> {
          int size = result.controlCodes.size();
          if (size == 0) {
            return ApplicableSoftwareControls.ZERO;
          }
          else if (size == 1) {
            // Saving to the DAO here prevents a separate call to the CategoryControlsServiceClient, if not a little hacky
            if (saveToDao) {
              ControlCode mappedControlCode = result.controlCodes.get(0);
              permissionsFinderDao.clearAndUpdateControlCodeJourneyDaoFieldsIfChanged(
                  ControlCodeJourney.SOFTWARE_CONTROLS_RELATED_TO_A_PHYSICAL_GOOD, mappedControlCode.controlCode);
            }
            return ApplicableSoftwareControls.ONE;
          }
          else if (size > 1) {
            return ApplicableSoftwareControls.GREATER_THAN_ONE;
          }
          else {
            throw new RuntimeException(String.format("Invalid value for size: \"%d\"", size));
          }
        }, httpExecutionContext.current());
  }

  public CompletionStage<ApplicableSoftwareControls> checkCatchtallSoftwareControls(SoftwareCategory softwareCategory, boolean saveToDao) {
    return catchallControlsServiceClient.get(GoodsType.SOFTWARE, softwareCategory) // TODO TECHNOLOGY
        .thenApplyAsync(result -> {
          int size = result.controlCodes.size();
          if (size == 0) {
            return ApplicableSoftwareControls.ZERO;
          }
          else if (size == 1) {
            // Saving to the DAO here prevents a separate call to the CatchallControlsServiceClient, if not a little hacky
            if (saveToDao) {
              ControlCode catchallControlCode = result.controlCodes.get(0);
              permissionsFinderDao.clearAndUpdateControlCodeJourneyDaoFieldsIfChanged(
                  ControlCodeJourney.SOFTWARE_CATCHALL_CONTROLS, catchallControlCode.controlCode);
            }
            return ApplicableSoftwareControls.ONE;
          }
          else if (size > 1) {
            return ApplicableSoftwareControls.GREATER_THAN_ONE;
          }
          else {
            throw new RuntimeException(String.format("Invalid value for size: \"%d\"", size));
          }
        }, httpExecutionContext.current());
  }

  public CompletionStage<Relationship> checkRelationshipExists(SoftwareCategory softwareCategory) {
    boolean relationshipExists = softwareCategory == SoftwareCategory.MILITARY;
    return softwareAndTechnologyRelationshipServiceClient.get(softwareCategory, relationshipExists)
        .thenApplyAsync(result -> {
          if (result.relationshipExists) {
            return Relationship.RELATIONSHIP_EXISTS;
          }
          else {
            return Relationship.RELATIONSHIP_DOES_NOT_EXIST;
          }
        }, httpExecutionContext.current());
  }

  public CompletionStage<Result> performCatchallSoftwareControlsTransition() {
    SoftwareCategory softwareCategory = permissionsFinderDao.getSoftwareCategory().get();

    return checkCatchtallSoftwareControls(softwareCategory, true)
        .thenComposeAsync(controls -> {
          if (controls == ApplicableSoftwareControls.ZERO) {
            return checkRelationshipExists(softwareCategory)
                .thenComposeAsync(relationship -> {
                  if (relationship == Relationship.RELATIONSHIP_EXISTS) {
                    return journeyManager.performTransition(Events.CATCHALL_SOFTWARE_CONTROLS_FLOW,
                        CatchallSoftwareControlsFlow.RELATIONSHIP_EXISTS);
                  }
                  else if (relationship == Relationship.RELATIONSHIP_DOES_NOT_EXIST) {
                    return journeyManager.performTransition(Events.CATCHALL_SOFTWARE_CONTROLS_FLOW,
                        CatchallSoftwareControlsFlow.RELATIONSHIP_DOES_NOT_EXIST);
                  }
                  else {
                    throw new RuntimeException(String.format("Unexpected member of Relationship enum: \"%s\""
                        , relationship.toString()));
                  }
                }, httpExecutionContext.current());
          }
          else if (controls == ApplicableSoftwareControls.ONE) {
            return journeyManager.performTransition(Events.CATCHALL_SOFTWARE_CONTROLS_FLOW,
                CatchallSoftwareControlsFlow.CATCHALL_ONE);
          }
          else if (controls == ApplicableSoftwareControls.GREATER_THAN_ONE) {
            return journeyManager.performTransition(Events.CATCHALL_SOFTWARE_CONTROLS_FLOW,
                CatchallSoftwareControlsFlow.CATCHALL_GREATER_THAN_ONE);
          }
          else {
            throw new RuntimeException(String.format("Unexpected member of ApplicableSoftwareControls enum: \"%s\""
                , controls.toString()));
          }
        }, httpExecutionContext.current());
  }

  public CompletionStage<Result> performCatchallSoftwareControlNotApplicableTransition() {
    SoftwareCategory softwareCategory = permissionsFinderDao.getSoftwareCategory().get();
    return checkCatchtallSoftwareControls(softwareCategory, true)
        .thenComposeAsync(controls -> {
         if (controls == ApplicableSoftwareControls.ONE) {
           return checkRelationshipExists(softwareCategory)
               .thenComposeAsync(relationship -> {
                 if (relationship == Relationship.RELATIONSHIP_EXISTS) {
                   return journeyManager.performTransition(Events.CONTROL_CODE_SOFTWARE_CATCHALL_CONTROLS_NOT_APPLICABLE_FLOW,
                       SoftwareCatchallControlsNotApplicableFlow.RELATIONSHIP_EXISTS);
                 }
                 else if (relationship == Relationship.RELATIONSHIP_DOES_NOT_EXIST) {
                   return journeyManager.performTransition(Events.CONTROL_CODE_SOFTWARE_CATCHALL_CONTROLS_NOT_APPLICABLE_FLOW,
                       SoftwareCatchallControlsNotApplicableFlow.RELATIONSHIP_NOT_EXISTS);
                 }
                 else {
                   throw new RuntimeException(String.format("Unexpected member of Relationship enum: \"%s\""
                       , relationship.toString()));
                 }
               }, httpExecutionContext.current());
          }
          else if (controls == ApplicableSoftwareControls.GREATER_THAN_ONE) {
            return journeyManager.performTransition(Events.CONTROL_CODE_SOFTWARE_CATCHALL_CONTROLS_NOT_APPLICABLE_FLOW,
                SoftwareCatchallControlsNotApplicableFlow.RETURN_TO_SOFTWARE_CATCHALL_CONTROLS);
          }
          else {
            throw new RuntimeException(String.format("Unexpected member of ApplicableSoftwareControls enum: \"%s\""
                , controls.toString()));
          }
        }, httpExecutionContext.current());
  }

  public CompletionStage<Result> performCatchallSoftwareControlRelationshipTransition() {
    SoftwareCategory softwareCategory = permissionsFinderDao.getSoftwareCategory().get();
    return checkRelationshipExists(softwareCategory)
        .thenComposeAsync(relationship ->
                journeyManager.performTransition(Events.CONTROL_CODE_SOFTWARE_CATCHALL_RELATIONSHIP, relationship)
            , httpExecutionContext.current());
  }
}
