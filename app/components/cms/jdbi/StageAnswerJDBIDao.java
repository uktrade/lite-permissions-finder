package components.cms.jdbi;

import components.cms.mapper.StageAnswerRSMapper;
import models.cms.StageAnswer;
import models.cms.enums.OutcomeType;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;

public interface StageAnswerJDBIDao {

  @Mapper(StageAnswerRSMapper.class)
  @SqlQuery("SELECT * FROM stage_answer WHERE id = :id")
  StageAnswer get(@Bind("id") long id);

  @SqlQuery(
      "INSERT INTO stage_answer (parent_stage_id, go_to_stage_id, go_to_outcome_type, control_entry_id, answer_text, display_order, answer_precedence, divider_above, nested_content, more_info_content) "
          + "VALUES(:parentStageId, :goToStageId, :goToOutcomeType::outcome_type, :controlEntryId, :answerText, :displayOrder, :answerPrecedence, :dividerAbove, :nestedContent, :moreInfoContent) "
          + "RETURNING id")
  Long insert(
      @Bind("parentStageId") Long parentStageId,
      @Bind("goToStageId") Long goToStageId,
      @Bind("goToOutcomeType") OutcomeType goToOutcomeType,
      @Bind("controlEntryId") Long controlEntryId,
      @Bind("answerText") String answerText,
      @Bind("displayOrder") Integer displayOrder,
      @Bind("answerPrecedence") Integer answerPrecedence,
      @Bind("dividerAbove") Boolean dividerAbove,
      @Bind("nestedContent") String nestedContent,
      @Bind("moreInfoContent") String moreInfoContent);

  @SqlUpdate("DELETE FROM stage_answer WHERE id = :id")
  void delete(@Bind("id") long id);

  @SqlUpdate("DELETE FROM stage_answer")
  void truncate();

}
