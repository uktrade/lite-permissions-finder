package components.cms.jdbi;

import components.cms.mapper.NoteRSMapper;
import models.cms.Note;
import models.cms.enums.NoteType;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;

public interface NoteJDBIDao {

  @Mapper(NoteRSMapper.class)
  @SqlQuery("SELECT * FROM note WHERE id = :id")
  Note get(@Bind("id") long id);

  @SqlQuery(
      "INSERT INTO note (stage_id, note_text, note_type) " +
          "VALUES (:stageId, :noteText, :noteType::note_type) " +
          "RETURNING id")
  Long insert(@Bind("stageId") Long stageId, @Bind("noteText") String noteText, @Bind("noteType") NoteType noteType);

  @SqlUpdate("DELETE FROM note WHERE id = :id")
  void delete(@Bind("id") long id);

  @SqlUpdate("DELETE FROM note")
  void truncate();

}
