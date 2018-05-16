package components.cms.jdbi;

import components.cms.mapper.StageRSMapper;
import models.cms.Stage;
import models.cms.enums.AnswerType;
import models.cms.enums.QuestionType;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;

public interface StageJDBIDao {

  @Mapper(StageRSMapper.class)
  @SqlQuery("SELECT * FROM stage WHERE id = :id")
  Stage get(@Bind("id") long id);

  @SqlQuery(
      "INSERT INTO stage (journey_id, control_entry_id, title, explanatory_notes, question_type, answer_type, next_stage_id) "
          + "VALUES (:journeyId, :controlEntryId, :title, :explanatoryNotes, :questionType::question_type, :answerType::answer_type, :nextStageId) "
          + "RETURNING id")
  Long insert(
      @Bind("journeyId") Long journeyId,
      @Bind("controlEntryId") Long controlEntryId,
      @Bind("title") String title,
      @Bind("explanatoryNotes") String explanatoryNotes,
      @Bind("questionType") QuestionType questionType,
      @Bind("answerType") AnswerType answerType,
      @Bind("nextStageId") Long nextStageId);

  @SqlUpdate("DELETE FROM stage WHERE id = :id")
  void delete(@Bind("id") long id);

  @SqlUpdate("DELETE FROM stage")
  void truncate();

}
