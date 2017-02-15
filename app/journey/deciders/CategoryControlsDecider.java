package journey.deciders;

import com.google.inject.Inject;
import components.common.journey.Decider;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.controls.category.CategoryControlsServiceClient;
import journey.SubJourneyContextParamProvider;
import models.GoodsType;
import models.controlcode.ControlCodeSubJourney;
import models.softtech.ApplicableSoftTechControls;
import models.softtech.SoftTechCategory;
import play.libs.concurrent.HttpExecutionContext;

import java.util.concurrent.CompletionStage;

public class CategoryControlsDecider implements Decider<ApplicableSoftTechControls> {

  private final PermissionsFinderDao dao;
  private final SubJourneyContextParamProvider subJourneyContextParamProvider;
  private final CategoryControlsServiceClient categoryControlsServiceClient;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public CategoryControlsDecider(PermissionsFinderDao dao,
                                 SubJourneyContextParamProvider subJourneyContextParamProvider,
                                 CategoryControlsServiceClient categoryControlsServiceClient,
                                 HttpExecutionContext httpExecutionContext) {
    this.dao = dao;
    this.subJourneyContextParamProvider = subJourneyContextParamProvider;
    this.categoryControlsServiceClient = categoryControlsServiceClient;
    this.httpExecutionContext = httpExecutionContext;
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public CompletionStage<ApplicableSoftTechControls> decide() {

    GoodsType goodsType = dao.getGoodsType().get();

    SoftTechCategory softTechCategory = dao.getSoftTechCategory(goodsType).get();

    return categoryControlsServiceClient.get(goodsType, softTechCategory)
        .thenApplyAsync(result -> {

          ApplicableSoftTechControls applicableSoftTechControls = ApplicableSoftTechControls.fromInt(result.controlCodes.size());

          // TODO Setting DAO state here is very hacky and needs rethinking
          if (applicableSoftTechControls == ApplicableSoftTechControls.ONE) {

            ControlCodeSubJourney subJourney = ControlCodeSubJourney.getSoftTechControlsVariant(goodsType);

            // TODO This is a massive hack, relies on knowing where the stage transition for ApplicableSoftTechControls.ONE will go
            subJourneyContextParamProvider.updateSubJourneyValueOnContext(subJourney);

            dao.saveSelectedControlCode(subJourney, result.controlCodes.get(0).getControlCode());
          }

          return applicableSoftTechControls;
        }, httpExecutionContext.current());
  }
}
