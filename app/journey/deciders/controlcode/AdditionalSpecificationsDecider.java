package journey.deciders.controlcode;

import com.google.inject.Inject;
import components.common.journey.Decider;
import components.persistence.PermissionsFinderDao;
import components.services.controlcode.FrontendServiceClient;
import journey.SubJourneyContextParamProvider;
import models.controlcode.ControlCodeSubJourney;

import java.util.concurrent.CompletionStage;

public class AdditionalSpecificationsDecider implements Decider<Boolean> {

  private final PermissionsFinderDao dao;
  private final FrontendServiceClient client;
  private final SubJourneyContextParamProvider subJourneyContextParamProvider;

  @Inject
  public AdditionalSpecificationsDecider(PermissionsFinderDao dao, FrontendServiceClient client) {
    this.dao = dao;
    this.client = client;
    this.subJourneyContextParamProvider = new SubJourneyContextParamProvider();
  }

  @Override
  public CompletionStage<Boolean> decide() {

    ControlCodeSubJourney controlCodeSubJourney = subJourneyContextParamProvider.getSubJourneyValueFromContext();

    String controlCode = dao.getSelectedControlCode(controlCodeSubJourney);

    return client.get(controlCode).thenApply(e -> e.getControlCodeData().canShowAdditionalSpecifications());
  }
}
