package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.PathHelper;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.EventDAOExample;
import mitll.npdata.dao.SlickEvent;
import mitll.npdata.dao.SlickSlimEvent;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 5/13/16.
 */
public class SlickEventImpl implements IEventDAO {
  private static final Logger logger = Logger.getLogger(SlickEventImpl.class);
  private EventDAOExample eventDAOExample;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(PathHelper)
   */
  public SlickEventImpl(DBConnection dbConnection) {
    eventDAOExample = new EventDAOExample(dbConnection);
  }

  public void copyTableOnlyOnce(IEventDAO other, String language) {
    if (getNumRows(language).intValue() == 0) {
      List<SlickEvent> copy = new ArrayList<>();
      List<Event> all = other.getAll(language);
      logger.info("copyTableOnlyOnce " + all.size() + " events ");

      for (Event event : all) copy.add(getSlickEvent(event, language));
      eventDAOExample.addBulk(copy);
    }
  }

  @Override
  public boolean add(Event event, String language) {
    eventDAOExample.add(getSlickEvent(event, language.toLowerCase()));
    return true;
  }

  private SlickEvent getSlickEvent(Event event, String language) {
    long timestamp = event.getTimestamp();
    if (timestamp < 1) timestamp = System.currentTimeMillis();
    Timestamp modified = new Timestamp(timestamp);

    SlickEvent slickEvent = new SlickEvent(-1,
        event.getUserID(),
        event.getExerciseID(),
        event.getContext() == null ? "" : event.getContext(),
        event.getWidgetID(),
        event.getWidgetType(),
        event.getDevice() == null ? "" : event.getDevice(),
        modified,
        language.toLowerCase());

//    logger.info("insert " +event);
 //   logger.info("insert " +slickEvent);

    return slickEvent;
  }

  @Override
  public List<Event> getAll(String language) {
    List<SlickEvent> all = eventDAOExample.getAll(language.toLowerCase());

//    logger.info("getting " + all.size() + " events to ");
    List<Event> copy = new ArrayList<>();
    for (SlickEvent event : all) {
      copy.add(new Event(
          event.widgetid(),
          event.widgettype(),
          event.exerciseid(),
          event.context(),
          event.userid(),
          event.modified().getTime(),
          event.device())
      );
    }
    return copy;
  }

  @Override
  public List<SlickSlimEvent> getAllSlim(String language) {
    return eventDAOExample.getAllSlim(language.toLowerCase());
  }

  @Override
  public List<SlickSlimEvent> getAllDevicesSlim(String language) {
    return eventDAOExample.getAllSlim(language.toLowerCase());
  }

  @Override
  public SlickSlimEvent getFirstSlim(String language) {
    return eventDAOExample.getFirstSlim(language.toLowerCase());
  }

  @Override
  public void addPlayedMarkings(int userID, CommonExercise firstExercise) {
    List<String> forUserAndExercise = eventDAOExample.getForUserAndExercise(userID, firstExercise.getID());
    Map<String, AudioAttribute> audioToAttr = firstExercise.getAudioRefToAttr();
    for (String eventContext : forUserAndExercise) {
      AudioAttribute audioAttribute = audioToAttr.get(eventContext);
      if (audioAttribute == null) {
        //logger.warn("addPlayedMarkings huh? can't find " + event.getContext() + " in " + audioToAttr.keySet());
      } else {
        audioAttribute.setHasBeenPlayed(true);
      }
    }
  }

  @Override
  public Number getNumRows(String language) {
    return eventDAOExample.getNumRows(language.toLowerCase());
  }
}
