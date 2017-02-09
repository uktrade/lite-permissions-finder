package models.controlcode;

import components.services.controlcode.Ancestor;
import components.services.controlcode.ControlCodeData;
import components.services.controlcode.FrontendControlCode;
import models.GoodsType;

import java.util.List;

public class ControlCodeSummaryDisplay {
  public final String title;
  public final String friendlyDescription;
  public final String controlCodeAlias;
  public final Ancestor greatestAncestor;
  public final List<Ancestor> otherAncestors;
  public final boolean showGreatestAncestor;
  public final String couldDescribeItemsLabel;
  public final ControlCodeSubJourney controlCodeSubJourney;
  public final boolean showTechNotesQuestion;
  public final boolean showAdditionalSpecsPanel;

  public ControlCodeSummaryDisplay(ControlCodeSubJourney controlCodeSubJourney, FrontendControlCode frontendControlCode) {
    ControlCodeData controlCodeData = frontendControlCode.controlCodeData;
    this.title = controlCodeData.title;
    this.friendlyDescription = controlCodeData.friendlyDescription;
    this.controlCodeAlias = controlCodeData.alias;
    this.controlCodeSubJourney = controlCodeSubJourney;
    this.showTechNotesQuestion = controlCodeData.canShowTechnicalNotes() && !controlCodeData.canShowAdditionalSpecifications();
    this.showAdditionalSpecsPanel = controlCodeData.canShowAdditionalSpecifications();
    if (frontendControlCode.greatestAncestor.isPresent()) {
      this.greatestAncestor = frontendControlCode.greatestAncestor.get();
      this.showGreatestAncestor = true;
    }
    else {
      this.greatestAncestor = null;
      this.showGreatestAncestor = false;
    }
    this.otherAncestors = frontendControlCode.otherAncestors;
    if (controlCodeSubJourney.isPhysicalGoodsSearchVariant()) {
      GoodsType goodsType = controlCodeSubJourney.getGoodsType();
      if (goodsType == GoodsType.PHYSICAL) {
        this.couldDescribeItemsLabel = "Could this describe your items?";
      }
      else if (goodsType == GoodsType.SOFTWARE){
        this.couldDescribeItemsLabel = "Could this describe the item your software is used with?";
      }
      else {
        // GoodsType.TECHNOLOGY
        this.couldDescribeItemsLabel = "Could this describe the item your technology is used with?";
      }
    }
    else if (controlCodeSubJourney.isSoftTechControlsVariant() ||
        controlCodeSubJourney.isSoftTechControlsRelatedToPhysicalGoodVariant() ||
        controlCodeSubJourney.isSoftTechCatchallControlsVariant() ||
        controlCodeSubJourney.isNonExemptControlsVariant()) {
      this.couldDescribeItemsLabel = "Could this describe your items?";
    }
    else {
      throw new RuntimeException(String.format("Unexpected member of ControlCodeSubJourney enum: \"%s\""
          , controlCodeSubJourney.toString()));
    }
  }

}
