package components.cms.dao;

import com.google.inject.Inject;
import components.cms.jdbi.SessionOutcomeJDBIDao;
import org.skife.jdbi.v2.DBI;
import triage.session.SessionOutcome;

public class SessionOutcomeDaoImpl implements SessionOutcomeDao {

  private final SessionOutcomeJDBIDao sessionOutcomeJDBIDao;

  @Inject
  public SessionOutcomeDaoImpl(DBI dbi) {
    this.sessionOutcomeJDBIDao = dbi.onDemand(SessionOutcomeJDBIDao.class);
  }

  @Override
  public void insert(SessionOutcome sessionOutcome) {
    sessionOutcomeJDBIDao.insert(sessionOutcome.getSessionId(),
        sessionOutcome.getUserId(),
        sessionOutcome.getCustomerId(),
        sessionOutcome.getSiteId(),
        sessionOutcome.getOutcomeType().toString(),
        sessionOutcome.getOutcomeHtml());
  }

  @Override
  public SessionOutcome getSessionOutcomeBySessionId(String sessionId) {
    return sessionOutcomeJDBIDao.getSessionOutcomeBySessionId(sessionId);
  }

}
