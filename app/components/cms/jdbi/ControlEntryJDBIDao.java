package components.cms.jdbi;

import components.cms.mapper.ControlEntryRSMapper;
import models.cms.ControlEntry;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;

import java.util.List;

public interface ControlEntryJDBIDao {

  @Mapper(ControlEntryRSMapper.class)
  @SqlQuery("SELECT * FROM control_entry WHERE id = :id")
  ControlEntry get(@Bind("id") long id);

  @Mapper(ControlEntryRSMapper.class)
  @SqlQuery("SELECT * FROM control_entry WHERE parent_control_entry_id = :parentId ORDER BY display_order ASC")
  List<ControlEntry> getChildren(@Bind("parentId") long parentId);

  @Mapper(ControlEntryRSMapper.class)
  @SqlQuery("SELECT * FROM control_entry")
  List<ControlEntry> getAll();

  @Mapper(ControlEntryRSMapper.class)
  @SqlQuery("SELECT * FROM control_entry WHERE control_code = :controlCode")
  ControlEntry getByControlCode(@Bind("controlCode") String controlCode);

  @Mapper(ControlEntryRSMapper.class)
  @SqlQuery("SELECT stage.id, control_entry.control_code, control_entry.description, control_entry.parent_control_entry_id," +
    " control_entry.nested, control_entry.display_order, control_entry.journey_id," +
    " control_entry.jump_to_control_codes, control_entry.decontrolled" +
    " FROM control_entry" +
    " INNER JOIN stage" +
    " ON control_entry.id = stage.control_entry_id" +
    " WHERE control_entry.decontrolled = false" +
    " AND UPPER(control_entry.control_code)" +
    " LIKE '%' || UPPER(:value) || '%'" +
    " ORDER BY control_entry.control_code" +
    " LIMIT 6")
  List<ControlEntry> findControlEntriesByControlCode(@Bind("value") String value);

  @Mapper(ControlEntryRSMapper.class)
  @SqlQuery("SELECT ce.* FROM related_control_entry rce JOIN control_entry ce ON rce.related_control_entry_id = ce.id" +
      "      WHERE rce.control_entry_id = :controlEntryId")
  List<ControlEntry> getRelatedControlCodeEntries(@Bind("controlEntryId") long controlEntryId);

  @SqlQuery(
      "INSERT INTO control_entry (parent_control_entry_id, control_code, description, nested, display_order, journey_id, decontrolled, jump_to_control_codes) "
          + "VALUES (:parentControlEntryId, :controlCode, :description, :nested, :displayOrder, :journeyId, :decontrolled, :jumpToControlCodes) "
          + "RETURNING id")
  Long insert(
      @Bind("parentControlEntryId") Long parentControlEntryId,
      @Bind("controlCode") String controlCode,
      @Bind("description") String description,
      @Bind("nested") Boolean nested,
      @Bind("displayOrder") Integer displayOrder,
      @Bind("journeyId") Long journeyId,
      @Bind("decontrolled") Boolean decontrolled,
      @Bind("jumpToControlCodes") String jumpToControlCodes
  );

  @SqlUpdate("DELETE FROM control_entry")
  void truncate();

}
