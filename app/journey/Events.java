package journey;

import components.common.journey.JourneyEvent;
import components.common.journey.ParameterisedJourneyEvent;
import model.ControlCodeFlowStage;
import model.ExportCategory;
import model.GoodsType;
import model.LifeType;
import model.TradeType;

public class Events {

  public static final JourneyEvent START_APPLICATION = new JourneyEvent("START_APPLICATION");

  public static final JourneyEvent CONTINUE_APPLICATION = new JourneyEvent("CONTINUE_APPLICATION");

  public static final JourneyEvent APPLICATION_FOUND = new JourneyEvent("APPLICATION_FOUND");

  public static final JourneyEvent APPLICATION_NOT_FOUND = new JourneyEvent("APPLICATION_NOT_FOUND");

  public static final ParameterisedJourneyEvent<TradeType> TRADE_TYPE_SELECTED =
      new ParameterisedJourneyEvent<>("TRADE_TYPE_SELECTED", TradeType.class);

  public static final ParameterisedJourneyEvent<ExportCategory> EXPORT_CATEGORY_SELECTED =
      new ParameterisedJourneyEvent<>("EXPORT_CATEGORY_SELECTED", ExportCategory.class);

  public static final JourneyEvent EXPORT_CATEGORY_COULD_BE_DUAL_USE = new JourneyEvent("EXPORT_CATEGORY_COULD_BE_DUAL_USE");

  public static final ParameterisedJourneyEvent<Boolean> IS_CONTROLLED_HISTORIC_GOOD
      = new ParameterisedJourneyEvent<>("IS_CONTROLLED_HISTORIC_GOOD", Boolean.class);

  public static final ParameterisedJourneyEvent<Boolean> IS_DUAL_USE
      = new ParameterisedJourneyEvent<>("IS_DUAL_USE", Boolean.class);

  public static final ParameterisedJourneyEvent<Boolean> IS_USED_FOR_EXECUTION_TORTURE =
      new ParameterisedJourneyEvent<>("IS_USED_FOR_EXECUTION_TORTURE", Boolean.class);

  public static final ParameterisedJourneyEvent<LifeType> LIFE_TYPE_SELECTED =
      new ParameterisedJourneyEvent<>("LIFE_TYPE_SELECTED", LifeType.class);

  public static final ParameterisedJourneyEvent<GoodsType> GOODS_TYPE_SELECTED =
      new ParameterisedJourneyEvent<>("GOODS_TYPE_SELECTED", GoodsType.class);

  public static final JourneyEvent SEARCH_PHYSICAL_GOODS = new JourneyEvent("SEARCH_PHYSICAL_GOODS");

  public static final JourneyEvent NONE_MATCHED = new JourneyEvent("NONE_MATCHED");

  public static final JourneyEvent CONTROL_CODE_SELECTED = new JourneyEvent("CONTROL_CODE_SELECTED");

  public static final ParameterisedJourneyEvent<ControlCodeFlowStage> CONTROL_CODE_FLOW_NEXT
      = new ParameterisedJourneyEvent<>("CONTROL_CODE_FLOW_NEXT", ControlCodeFlowStage.class);

  public static final JourneyEvent DESTINATION_COUNTRIES_SELECTED = new JourneyEvent("DESTINATION_COUNTRIES_SELECTED");

}
