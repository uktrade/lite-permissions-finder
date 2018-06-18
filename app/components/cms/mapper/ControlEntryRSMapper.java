package components.cms.mapper;

import components.cms.mapper.util.ResultSetWrapper;
import models.cms.ControlEntry;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ControlEntryRSMapper implements ResultSetMapper<ControlEntry> {
  @Override
  public ControlEntry map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    ResultSetWrapper rsw = new ResultSetWrapper(r);
    Long id = rsw.getLong("id");
    Long parentControlEntryId = rsw.getLong("parent_control_entry_id");
    String controlCode = r.getString("control_code");
    String fullDescription = r.getString("full_description");
    String summaryDescription = r.getString("summary_description");
    Boolean nested = r.getBoolean("nested");
    Boolean selectable = r.getBoolean("selectable");
    String regime = r.getString("regime");
    Integer displayOrder = rsw.getInt("display_order");
    return new ControlEntry()
        .setId(id)
        .setParentControlEntryId(parentControlEntryId)
        .setControlCode(controlCode)
        .setFullDescription(fullDescription)
        .setSummaryDescription(summaryDescription)
        .setNested(nested)
        .setSelectable(selectable)
        .setRegime(regime)
        .setDisplayOrder(displayOrder);
  }
}
