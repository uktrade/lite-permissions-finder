package triage.session;

public class TriageSession {

  private final String id;
  private final long journeyId;
  private final String resumeCode;
  private final String outcomeType;
  private final String outcomeHtml;
  private final Long lastStageId;

  public TriageSession(String id, long journeyId, String resumeCode, String outcomeType, String outcomeHtml,
                       Long lastStageId) {
    this.id = id;
    this.journeyId = journeyId;
    this.resumeCode = resumeCode;
    this.outcomeType = outcomeType;
    this.outcomeHtml = outcomeHtml;
    this.lastStageId = lastStageId;
  }

  public String getId() {
    return id;
  }

  public long getJourneyId() {
    return journeyId;
  }

  public String getResumeCode() {
    return resumeCode;
  }

  public String getOutcomeType() {
    return outcomeType;
  }

  public String getOutcomeHtml() {
    return outcomeHtml;
  }

  public Long getLastStageId() {
    return lastStageId;
  }

}
