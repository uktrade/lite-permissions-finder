package components.cms.dao.impl;

import com.google.inject.Inject;
import components.cms.dao.JourneyDao;
import components.cms.jdbi.JourneyJDBIDao;
import java.util.List;
import models.cms.Journey;
import org.skife.jdbi.v2.DBI;

public class JourneyDaoImpl implements JourneyDao {

  private final JourneyJDBIDao journeyJDBIDao;

  @Inject
  public JourneyDaoImpl(DBI dbi) {
    this.journeyJDBIDao = dbi.onDemand(JourneyJDBIDao.class);
  }

  @Override
  public Journey getJourney(long id) {
    return journeyJDBIDao.get(id);
  }

  @Override
  public List<Journey> getAllJourneys() {
    return journeyJDBIDao.getAll();
  }

  @Override
  public Journey getByJourneyName(String journeyName) {
    return journeyJDBIDao.getByJourneyName(journeyName);
  }

  @Override
  public Long insertJourney(Journey journey) {
    return journeyJDBIDao.insert(journey.getJourneyName(), journey.getFriendlyJourneyName(), journey.getInitialStageId());
  }

  @Override
  public void updateJourney(long id, Journey journey) {
    journeyJDBIDao.update(id, journey.getJourneyName(), journey.getFriendlyJourneyName(), journey.getInitialStageId());
  }

  @Override
  public void deleteAllJournies() {
    journeyJDBIDao.truncate();
  }
}
