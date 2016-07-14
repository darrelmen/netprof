package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.ISchema;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickEvent;
import mitll.npdata.dao.SlickSlimEvent;
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
  private static final int MAX_EVENTS_TO_SHOW = 20000;
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

  @Override
  public String getName() {
    return eventDAOWrapper.dao().name();
  }

  /**
   * @param other
   * @param projid
   * @param exToInt
   * @see mitll.langtest.server.database.CopyToPostgres#copyToPostgres(DatabaseImpl)
   */
  public void copyTableOnlyOnce(IEventDAO other, int projid, Map<Integer, Integer> oldToNew, Map<String, Integer> exToInt) {
    if (getNumRows(projid).intValue() == 0) {
      List<SlickEvent> copy = new ArrayList<>();
      List<Event> all = other.getAll(projid);
      logger.info("copyTableOnlyOnce " + all.size() + " events ");

      for (Event event : all) {
        SlickEvent slickEvent = getSlickEvent(event, projid, oldToNew, exToInt);
        if (slickEvent != null) {
          copy.add(slickEvent);
        }
      }
      eventDAOWrapper.addBulk(copy);
    }
  }

  /**
   * @param event
   * @param projid
   * @return true always - deal with possiblity it might fail???
   */
  @Override
  public boolean add(Event event, int projid) {
    eventDAOWrapper.add(getSlickEvent(event, projid, event.getExid()));
    return true;
  }

  @Override
  public SlickEvent toSlick(Event event, int projid, Map<String, Integer> exToInt) {
    Integer exid = exToInt.get(event.getExerciseID());
    SlickEvent slickEvent = getSlickEvent(event, projid, exid);

    return slickEvent;
  }

  SlickEvent getSlickEvent(Event event, int projid, Integer exid) {
    long timestamp = event.getTimestamp();
    if (timestamp < 1) timestamp = System.currentTimeMillis();
    Timestamp modified = new Timestamp(timestamp);
    return new SlickEvent(-1,
        event.getUserID(),
        exid,
        modified,
        event.getContext() == null ? "" : event.getContext(),
        event.getWidgetID(),
        event.getWidgetType(),
        event.getDevice() == null ? "" : event.getDevice(),
        projid);
  }

  @Override
  public Event fromSlick(SlickEvent event) {
    return new Event(
        event.widgetid(),
        event.widgettype(),
        "" + event.exid(),
        event.context(),
        event.userid(),
        event.modified().getTime(),
        event.device(), event.exid());
  }

  /**
   * @param event
   * @param projid
   * @param oldToNew
   * @param exToID
   * @return
   * @see #copyTableOnlyOnce(IEventDAO, int, Map, Map)
   */
  private SlickEvent getSlickEvent(Event event, int projid, Map<Integer, Integer> oldToNew, Map<String, Integer> exToID) {
//    long timestamp = event.getTimestamp();
//    if (timestamp < 1) timestamp = System.currentTimeMillis();
//    Timestamp modified = new Timestamp(timestamp);
    return (oldToNew.containsKey(event.getUserID())) ? null : toSlick(event, projid, exToID);
  }

  @Override
  public List<Event> getAll(Integer projid) {
    List<SlickEvent> all = eventDAOWrapper.getAll(projid);
    return getEvents(all);
  }

  @Override
  public List<Event> getAllMax(int projid) {
    List<SlickEvent> all = eventDAOWrapper.getAllMax(projid, MAX_EVENTS_TO_SHOW);
    return getEvents(all);
  }

  private List<Event> getEvents(List<SlickEvent> all) {
    List<Event> copy = new ArrayList<>();
    for (SlickEvent event : all) {
      copy.add(fromSlick(event));
    }
    return copy;
  }

  @Override
  public List<SlickSlimEvent> getAllSlim(int projid) {
    return eventDAOWrapper.getAllSlim(projid);
  }

  @Override
  public List<SlickSlimEvent> getAllDevicesSlim(int projid) {
    return eventDAOWrapper.getAllSlim(projid);
  }

  @Override
  public SlickSlimEvent getFirstSlim(int projid) {
    return eventDAOWrapper.getFirstSlim(projid);
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
  public Number getNumRows(int projid) {
    return eventDAOWrapper.getNumRows(projid);
  }

  public int getNumRows() {
    return eventDAOWrapper.getNumRows();
  }
}
