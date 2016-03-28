package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.hibernate.SessionManagement;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 3/28/16.
 */
public class HEventDAO implements IEventDAO {
  private static final Logger logger = Logger.getLogger(HEventDAO.class);

  // private final Database database;
  SessionManagement sessionManagement;
  long defectDetector = -1;

  public HEventDAO(Database database, long defectDetector) {
   // this.database = database;
    sessionManagement = database.getSessionManagement();
    this.defectDetector = defectDetector;
  }

  public void addAll(Collection<Event> allEvents) {
    if (isEmpty()) {
      logger.info("Adding initial events from H2 " + allEvents.size());
      sessionManagement.doInTranscation((session) -> {
        for (Event event : allEvents) {
          if (event.getContext().length() > 255) {
            event.setContext(event.getContext().substring(0,255));
          }
          session.save(event);
        }
      });
    }
  }

  @Override
  public boolean add(Event event) {
    if (event.getCreatorID() == -1) {
      event.setTimestamp(System.currentTimeMillis());
      event.setCreatorID(defectDetector);
    }
    sessionManagement.doInTranscation((session) -> {
      session.save(event);
    });

    return true;
  }

  public boolean isEmpty() {
    Boolean ret = sessionManagement.getFromTransaction(new SessionManagement.SessionSupplier<Boolean>() {
      @Override
      public Boolean get() {
        return session.createQuery("select 1 from Event ").setMaxResults(1).list().isEmpty();
      }
    });
    return ret;
  }


  @Override
  public List<Event> getAll() {
    List<Event> ret = sessionManagement.getFromTransaction(new SessionManagement.SessionSupplier<List<Event>>() {
      @Override
      public List<Event> get() {
        return session.createQuery("from Event ").list();
      }
    });
    return ret;
  }

  @Override
  public List<Event> getAllDevices() {
    List<Event> ret = sessionManagement.getFromTransaction(new SessionManagement.SessionSupplier<List<Event>>() {
      @Override
      public List<Event> get() {
        return session.createQuery("from Event  where length(device)=36").list();
      }
    });
    return ret;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addPlayedMarkings(long, CommonExercise)
   * @param userID
   * @param firstExercise
   */
  @Override
  public void addPlayedMarkings(long userID, CommonExercise firstExercise) {
    List<Event> allForUserAndExercise = getAllForUserAndExercise(userID, firstExercise);

   // logger.info("addPlayedMarkings got " + allForUserAndExercise + " for " + userID + " and " +firstExercise.getID());

    Map<String, AudioAttribute> audioToAttr = firstExercise.getAudioRefToAttr();
    for (Event event : allForUserAndExercise) {
      AudioAttribute audioAttribute = audioToAttr.get(event.getContext());
      if (audioAttribute == null) {
        //logger.warn("addPlayedMarkings huh? can't find " + event.getContext() + " in " + audioToAttr.keySet());
      } else {
        audioAttribute.setHasBeenPlayed(true);
      }
    }
  }

  private List<Event> getAllForUserAndExercise(final long userID, final CommonExercise firstExercise) {
    return sessionManagement.getFromTransaction(new SessionManagement.SessionSupplier<List<Event>>() {
      @Override
      public List<Event> get() {
        return session.createQuery(
            "from Event  " +
                "where widgetType='qcPlayAudio' AND creatorID=" + userID + " AND exerciseID='" +
                firstExercise.getID() +
                "'").list();
      }
    });
  }
}
