package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.ISchema;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.npdata.dao.SlickEvent;
import mitll.npdata.dao.SlickSlimEvent;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.event.EventDAOWrapper;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 5/13/16.
 */
public class SlickEventImpl implements IEventDAO, ISchema<Event, SlickEvent> {
  private static final Logger logger = Logger.getLogger(SlickEventImpl.class);
  public static final int MAX_EVENTS_TO_SHOW = 20000;
  private EventDAOWrapper eventDAOWrapper;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(PathHelper)
   */
  public SlickEventImpl(DBConnection dbConnection) {
    eventDAOWrapper = new EventDAOWrapper(dbConnection);
  }

  public void createTable() {
    eventDAOWrapper.createTable();
  }

  public void dropTable() {
    eventDAOWrapper.drop();
  }


  /**
   * @param other
   * @param language
   * @see mitll.langtest.server.database.DatabaseImpl#oneTimeDataCopy
   */
  public void copyTableOnlyOnce(IEventDAO other, String language, Map<Integer, Integer> oldToNew) {
    if (getNumRows(language).intValue() == 0) {
      List<SlickEvent> copy = new ArrayList<>();
      List<Event> all = other.getAll(language);
      logger.info("copyTableOnlyOnce " + all.size() + " events ");

      for (Event event : all) {
        SlickEvent slickEvent = getSlickEvent(event, language, oldToNew);
        if (slickEvent != null) {
          copy.add(slickEvent);
        }
      }
      eventDAOWrapper.addBulk(copy);
    }
  }

  @Override
  public boolean add(Event event, String language) {
    eventDAOWrapper.add(toSlick(event, language.toLowerCase()));
    return true;
  }
/*
  public SlickEvent getSlickEvent(Event event, String language) {
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
  }*/


  @Override
  public SlickEvent toSlick(Event event, String language) {
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

    return slickEvent;
  }

  @Override
  public Event fromSlick(SlickEvent event) {
    return new Event(
        event.widgetid(),
        event.widgettype(),
        event.exerciseid(),
        event.context(),
        event.userid(),
        event.modified().getTime(),
        event.device());
  }


  public SlickEvent getSlickEvent(Event event, String language, Map<Integer, Integer> oldToNew) {
    long timestamp = event.getTimestamp();
    if (timestamp < 1) timestamp = System.currentTimeMillis();
    Timestamp modified = new Timestamp(timestamp);

    Integer userid = oldToNew.get(event.getUserID());
    if (userid == null) return null;
    else {
      SlickEvent slickEvent = /*new SlickEvent(-1,
          userid,
          event.getExerciseID(),
          event.getContext() == null ? "" : event.getContext(),
          event.getWidgetID(),
          event.getWidgetType(),
          event.getDevice() == null ? "" : event.getDevice(),
          modified,
          language.toLowerCase());*/
          toSlick(event, language);

//    logger.info("insert " +event);
      //   logger.info("insert " +slickEvent);

      return slickEvent;
    }
  }

  @Override
  public List<Event> getAll(String language) {
    List<SlickEvent> all = eventDAOWrapper.getAll(language.toLowerCase());
    return getEvents(all);
  }

  @Override
  public List<Event> getAllMax(String language) {
    List<SlickEvent> all = eventDAOWrapper.getAllMax(language.toLowerCase(), MAX_EVENTS_TO_SHOW);
    return getEvents(all);
  }

  List<Event> getEvents(List<SlickEvent> all) {
    List<Event> copy = new ArrayList<>();
    for (SlickEvent event : all) {
      copy.add(fromSlick(event));
    }
    return copy;
  }

  @Override
  public List<SlickSlimEvent> getAllSlim(String language) {
    return eventDAOWrapper.getAllSlim(language.toLowerCase());
  }

  @Override
  public List<SlickSlimEvent> getAllDevicesSlim(String language) {
    return eventDAOWrapper.getAllSlim(language.toLowerCase());
  }

  @Override
  public SlickSlimEvent getFirstSlim(String language) {
    return eventDAOWrapper.getFirstSlim(language.toLowerCase());
  }

  @Override
  public void addPlayedMarkings(int userID, CommonExercise firstExercise) {
    List<String> forUserAndExercise = eventDAOWrapper.getForUserAndExercise(userID, firstExercise.getID());
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
    return eventDAOWrapper.getNumRows(language.toLowerCase());
  }

  public int getNumRows() {
    return eventDAOWrapper.getNumRows();
  }
}
