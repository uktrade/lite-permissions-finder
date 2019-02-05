package components.cms.mapper;

import components.cms.mapper.util.ResultSetWrapper;
import models.cms.GlobalDefinition;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GlobalDefinitionRSMapper implements ResultSetMapper<GlobalDefinition> {
  @Override
  public GlobalDefinition map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    ResultSetWrapper rsw = new ResultSetWrapper(r);
    Long id = rsw.getLong("id");
    Long journeyId = rsw.getLong("journey_id");
    String term = r.getString("term");
    String definitionText = r.getString("definition_text");
    return new GlobalDefinition(id, journeyId, term, definitionText);
  }
}
