package components.cms.parser.model.definition;

public class Definition {
  private final String rowNum;
  private final String name;
  private final String list;
  private final String newContent;

  public Definition(String rowNum, String name, String list, String newContent) {
    this.rowNum = rowNum;
    this.name = name;
    this.list = list;
    this.newContent = newContent;
  }

  public String getRowNum() {
    return rowNum;
  }

  public String getName() {
    return name;
  }

  public String getList() {
    return list;
  }

  public String getNewContent() {
    return newContent;
  }
}