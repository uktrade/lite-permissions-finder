package triage.config;

import models.cms.enums.OutcomeType;
import triage.text.RichText;

import java.util.Optional;

public class AnswerConfig {

  private final String answerId;

  private final String nextStageId;
  private final OutcomeType outcomeType;

  private final RichText labelText;
  private final RichText nestedContent;
  private final RichText moreInfoContent;

  private final ControlEntryConfig associatedControlEntryConfig;

  private final int displayOrder;
  private final int answerPrecedence;

  public AnswerConfig(String answerId, String nextStageId, OutcomeType outcomeType, RichText labelText,
                      RichText nestedContent, RichText moreInfoContent,
                      ControlEntryConfig associatedControlEntryConfig, int displayOrder, int answerPrecedence) {
    this.answerId = answerId;
    this.nextStageId = nextStageId;
    this.outcomeType = outcomeType;
    this.labelText = labelText;
    this.nestedContent = nestedContent;
    this.moreInfoContent = moreInfoContent;
    this.associatedControlEntryConfig = associatedControlEntryConfig;
    this.displayOrder = displayOrder;
    this.answerPrecedence = answerPrecedence;
  }

  public String getAnswerId() {
    return answerId;
  }

  //Mutually exclusive with getOutcomeType
  public Optional<String> getNextStageId() {
    return Optional.ofNullable(nextStageId);
  }

  //Mutually exclusive with getNextStageId
  public Optional<OutcomeType> getOutcomeType() {
    return Optional.ofNullable(outcomeType);
  }

  //If empty, use ControlEntryConfig fullDescription
  public Optional<RichText> getLabelText() {
    return Optional.ofNullable(labelText);
  }

  public Optional<RichText> getNestedContent() {
    return Optional.ofNullable(nestedContent);
  }

  public Optional<RichText> getMoreInfoContent() {
    return Optional.ofNullable(moreInfoContent);
  }

  public Optional<ControlEntryConfig> getAssociatedControlEntryConfig() {
    return Optional.ofNullable(associatedControlEntryConfig);
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public int getAnswerPrecedence() {
    return answerPrecedence;
  }
}
