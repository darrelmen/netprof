package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickEvent;
import mitll.npdata.dao.SlickSlimEvent;
import mitll.npdata.dao.event.EventDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by go22670 on 5/13/16.
 */
public class SlickEventImpl implements IEventDAO/*, ISchema<Event, SlickEvent>*/ {
  private static final Logger logger = LogManager.getLogger(SlickEventImpl.class);
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
   * @see mitll.langtest.server.database.CopyToPostgres#copyOneConfig(DatabaseImpl, String, String)
   */
  public void copyTableOnlyOnce(IEventDAO other,
                                int projid,
                                Map<Integer, Integer> oldToNewUserID,
                                Map<String, Integer> exToInt) {
    if (getNumRows(projid).intValue() == 0) {  // Needed?
      List<SlickEvent> copy = new ArrayList<>();
      List<Event> all = other.getAll(projid);
      logger.info("copyTableOnlyOnce " + all.size() + " events, ex->int size " + exToInt.size() + " old->new user " + oldToNewUserID.size());

      Set<String> missingEx = new TreeSet<>();
      Set<String> ex = new TreeSet<>();
      Set<Integer> userids = new TreeSet<>();

      int missing = 0;
      for (Event event : all) {
        Integer newUserID = oldToNewUserID.get(event.getUserID());

        SlickEvent slickEvent = null;
        String exerciseID = "";
        boolean foundUser = newUserID != null;
        if (foundUser) {
          slickEvent = getSlickEvent(event, projid, newUserID, exToInt);
          exerciseID = event.getExerciseID();
        }
        if (slickEvent != null) {
          ex.add(exerciseID);
          copy.add(slickEvent);
        } else {
          missing++;
          boolean add = missingEx.add(exerciseID);
          if (add && missing < 100 && !exerciseID.isEmpty()) {
            logger.warn("missing '" + exerciseID + "'");
          }

          if (!foundUser) {
            boolean missingUser = userids.add(event.getUserID());
            if (missingUser) {
              logger.info("missing user " + event.getUserID() + " : " + newUserID);
            }
          }
        }
      }

      logMemory();
      if (missing > 0) {
        logger.warn("skipped " + missing + " out of " + all.size() + " : " + missingEx.size());// + " missing " + missingEx);
        logger.warn("missing users " + userids);
        logger.warn("found   " + ex.size() + " exercises");// + " events ex " + ex);
      }

      logger.info("adding " + copy.size() + " events");
      eventDAOWrapper.addBulk(copy);
      logger.info("added  " + copy.size() + " events");

    }
  }


  private void logMemory() {
    int MB = (1024 * 1024);
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    logger.debug(" current thread group " + threadGroup.getName() + " = " + threadGroup.activeCount() +
        " : # cores = " + Runtime.getRuntime().availableProcessors() + " heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
  }

  /**
   * @param event
   * @param projid
   * @return true always - deal with possiblity it might fail???
   */
  @Override
  public boolean add(Event event, int projid) {
    eventDAOWrapper.add(getSlickEvent(event, projid, event.getExid(), event.getUserID()));
    return true;
  }

  private Set<String> missing = new TreeSet<>();


  public SlickEvent toSlick(Event event, int projid, Map<String, Integer> exToInt, int userid) {
    String trim = event.getExerciseID().trim();
    Integer exid = exToInt.get(trim);
    if (exid == null) {
      if (missing.add(trim) && missing.size() < 100) {
        logger.warn("toSlick missing ex " + trim + " : " + event);
      }
      return null;
    } else {
      return getSlickEvent(event, projid, exid, userid);
    }
  }

  private SlickEvent getSlickEvent(Event event, int projid, Integer exid, int userID) {
    long timestamp = event.getTimestamp();
    if (timestamp < 1) timestamp = System.currentTimeMillis();
    //Timestamp modified = new Timestamp(timestamp);
    //int userID = event.getUserID();
    return new SlickEvent(-1,
        userID,
        exid,
        timestamp,
        event.getContext() == null ? "" : event.getContext(),
        event.getWidgetID(),
        event.getWidgetType(),
        event.getDevice() == null ? "" : event.getDevice(),
        projid);
  }

  // @Override
  private Event fromSlick(SlickEvent event) {
    return new Event(
        event.widgetid(),
        event.widgettype(),
        "" + event.exid(),
        event.context(),
        event.userid(),
        event.modified(),//.getTime(),
        event.device(), event.exid());
  }

  /**
   * @param event
   * @param projid
   * @param oldToNewUser
   * @param exToID
   * @return
   * @see #copyTableOnlyOnce(IEventDAO, int, Map, Map)
   */
  private SlickEvent getSlickEvent(Event event,
                                   int projid,
                                   Integer newUserID,
                                   Map<String, Integer> exToID) {
 //   Integer newUserID = oldToNewUser.get(event.getUserID());
    return /*newUserID == null ? null :*/ toSlick(event, projid, exToID, newUserID);
  }

  @Override
  public List<Event> getAll() {
    return getEvents(eventDAOWrapper.all());
  }

  @Override
  public List<Event> getAll(Integer projid) {
    logger.info("get events for " + projid);
    List<SlickEvent> all = eventDAOWrapper.getAll(projid);
    logger.info("got events for " + projid + " num = " + all.size());

    return getEvents(all);
  }

  @Override
  public List<Event> getAllMax(int projid) {
    List<SlickEvent> all = eventDAOWrapper.getAllMax(projid, MAX_EVENTS_TO_SHOW);
    return getEvents(all);
  }

  private List<Event> getEvents(Collection<SlickEvent> all) {
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
  public List<SlickSlimEvent> getAllSlim() {
    return eventDAOWrapper.allSlim();
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
