package components.cms.dao;

import models.cms.StageAnswer;

import java.util.List;

public interface StageAnswerDao {

  StageAnswer getStageAnswer(long id);

  List<StageAnswer> getStageAnswersForStageId(long stageId);

  Long insertStageAnswer(StageAnswer stageAnswer);

  void deleteStageAnswer(long id);

  void deleteAllStageAnswers();

}
