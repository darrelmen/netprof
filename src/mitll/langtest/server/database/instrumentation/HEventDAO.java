package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.hibernate.HDAO;
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
public class HEventDAO extends HDAO<Event> implements IEventDAO {
  private static final Logger logger = Logger.getLogger(HEventDAO.class);
  private long defectDetector = -1;

  public HEventDAO(Database database, long defectDetector) {
    super(database);
    this.defectDetector = defectDetector;
  }

  public void addAll(Collection<Event> allEvents) {
    if (isEmpty()) {
      logger.info("Adding initial events from H2 " + allEvents.size());
      sessionManagement.doInTranscation((session) -> {
        for (Event event : allEvents) {
          event.truncate();
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
    return isEmpty("Event");
  }

  @Override
  public List<Event> getAll() {
    return getAll("Event");
  }

  @Override
  public List<Event> getAllDevices() {
    return getAll("Event where length(device)=36");
  }

  /**
   * @param userID
   * @param firstExercise
   * @see mitll.langtest.server.LangTestDatabaseImpl#addPlayedMarkings(long, CommonExercise)
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

  @Override
  public Number getNumRows() {
    return getNumRowsByClass(Event.class);
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
