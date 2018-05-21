package components.cms.parser.model;

import components.cms.parser.model.column.Breadcrumbs;
import components.cms.parser.model.column.Buttons;
import components.cms.parser.model.column.ControlListEntries;
import components.cms.parser.model.column.Decontrols;
import components.cms.parser.model.column.Definitions;
import components.cms.parser.model.column.Loops;
import components.cms.parser.model.column.NavigationExtras;
import components.cms.parser.model.column.Nesting;
import components.cms.parser.model.column.Notes;
import components.cms.parser.model.column.OnPageContent;

import java.util.ArrayList;
import java.util.Collection;

public class NavigationLevel {
  private final String cellAddress;
  private final String content;
  private final int level;
  private final ArrayList<NavigationLevel> subNavigationLevels;
  private final NavigationExtras navigationExtras;
  private final OnPageContent onPageContent;
  private final ControlListEntries controlListEntries;
  private final Buttons buttons;
  private final Nesting nesting;
  private final Loops loops;
  private final Breadcrumbs breadcrumbs;
  private final Decontrols decontrols;
  private final Definitions definitions;
  private final Notes notes;
  private final LoadingMetadata loadingMetadata;

  public NavigationLevel(String cellAddress, String content, int level) {
    this(cellAddress, content, level, null, null, null, null, null, null, null, null, null, null);
  }

  public NavigationLevel(
      String cellAddress,
      String content,
      int level,
      NavigationExtras navigationExtras,
      OnPageContent onPageContent,
      ControlListEntries controlListEntries,
      Buttons buttons,
      Nesting nesting,
      Loops loops,
      Breadcrumbs breadcrumbs,
      Decontrols decontrols,
      Definitions definitions,
      Notes notes
  ) {
    this.cellAddress = cellAddress;
    this.content = content;
    this.level = level;
    this.navigationExtras = navigationExtras;
    this.onPageContent = onPageContent;
    this.controlListEntries = controlListEntries;
    this.buttons = buttons;
    this.nesting = nesting;
    this.loops = loops;
    this.breadcrumbs = breadcrumbs;
    this.decontrols = decontrols;
    this.definitions = definitions;
    this.notes = notes;
    loadingMetadata = new LoadingMetadata();
    subNavigationLevels = new ArrayList<>();
  }

  @Override
  public String toString() {
    return "NavigationLevel{" +
        "cellAddress='" + cellAddress + '\'' +
        ", content='" + content + '\'' +
        ", level=" + level +
        '}';
  }

  public String getCellAddress() {
    return cellAddress;
  }

  public String getContent() {
    return content;
  }

  public int getLevel() {
    return level;
  }

  public ArrayList<NavigationLevel> getSubNavigationLevels() {
    return subNavigationLevels;
  }

  public NavigationExtras getNavigationExtras() {
    return navigationExtras;
  }

  public OnPageContent getOnPageContent() {
    return onPageContent;
  }

  public ControlListEntries getControlListEntries() {
    return controlListEntries;
  }

  public Buttons getButtons() {
    return buttons;
  }

  public Nesting getNesting() {
    return nesting;
  }

  public Loops getLoops() {
    return loops;
  }

  public Breadcrumbs getBreadcrumbs() {
    return breadcrumbs;
  }

  public Decontrols getDecontrols() {
    return decontrols;
  }

  public Definitions getDefinitions() {
    return definitions;
  }

  public Notes getNotes() {
    return notes;
  }

  public void addSubNavigationLevel(NavigationLevel navigationLevel) {
    subNavigationLevels.add(navigationLevel);
  }

  public void addAllSubNavigationlevels(Collection<NavigationLevel> navigationLevels) {
    subNavigationLevels.addAll(navigationLevels);
  }

  public LoadingMetadata getLoadingMetadata() {
    return loadingMetadata;
  }
}