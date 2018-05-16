package models.cms;

public class ControlEntry {
  private Long id;
  private Long parentControlEntryId;
  private String controlCode;
  private String fullDescription;
  private String summaryDescription;
  private Boolean nested;
  private Boolean selectable;
  private String regime;

  public Long getId() {
    return id;
  }

  public ControlEntry setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getParentControlEntryId() {
    return parentControlEntryId;
  }

  public ControlEntry setParentControlEntryId(Long parentControlEntryId) {
    this.parentControlEntryId = parentControlEntryId;
    return this;
  }

  public String getControlCode() {
    return controlCode;
  }

  public ControlEntry setControlCode(String controlCode) {
    this.controlCode = controlCode;
    return this;
  }

  public String getFullDescription() {
    return fullDescription;
  }

  public ControlEntry setFullDescription(String fullDescription) {
    this.fullDescription = fullDescription;
    return this;
  }

  public String getSummaryDescription() {
    return summaryDescription;
  }

  public ControlEntry setSummaryDescription(String summaryDescription) {
    this.summaryDescription = summaryDescription;
    return this;
  }

  public Boolean isNested() {
    return nested;
  }

  public ControlEntry setNested(Boolean nested) {
    this.nested = nested;
    return this;
  }

  public Boolean isSelectable() {
    return selectable;
  }

  public ControlEntry setSelectable(Boolean selectable) {
    this.selectable = selectable;
    return this;
  }

  public String getRegime() {
    return regime;
  }

  public ControlEntry setRegime(String regime) {
    this.regime = regime;
    return this;
  }
}
