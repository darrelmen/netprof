/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.instrumentation;

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
public class SlickEventImpl implements IEventDAO {
  private static final Logger logger = LogManager.getLogger(SlickEventImpl.class);
  private static final int MAX_EVENTS_TO_SHOW = 20000;
  private final EventDAOWrapper eventDAOWrapper;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
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

  @Override
  public void deleteForProject(int projID) {
    eventDAOWrapper.deleteForProject(projID);
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return false;
  }

  /**
   * @param other
   * @param projid
   * @param exToInt
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyOneConfig
   */
 /* private void copyTableOnlyOnce(IEventDAO other,
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

      ProjectManagement.logMemory();
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
*/
/*
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
*/

  /**
   * @param event
   * @param projid
   * @return true always - deal with possiblity it might fail???
   */
  @Override
  public boolean addToProject(Event event, int projid) {
    eventDAOWrapper.add(getSlickEvent(event, projid, event.getExid(), event.getUserID()));
    return true;
  }

  private final Set<String> missing = new TreeSet<>();


  private SlickEvent toSlick(Event event, int projid, Map<String, Integer> exToInt, int userid) {
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
   * @param exToID
   * @return
   * @paramx oldToNewUser
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
  public List<Event> getAll(int projid) {
    logger.info("get events for " + projid);
    List<SlickEvent> all = eventDAOWrapper.getAll(projid);
    logger.info("got events for " + projid + " num = " + all.size());

    return getEvents(all);
  }

  /**
   *
   * @param projid
   * @param limit
   * @return
   */
  @Override
  public List<Event> getAllWithLimit(int projid, int limit) {
    return getEvents(eventDAOWrapper.getAllMax(projid, limit));
  }

  private List<Event> getEvents(Collection<SlickEvent> all) {
    List<Event> copy = new ArrayList<>();
    all.forEach(slickEvent -> copy.add(fromSlick(slickEvent)));
    Collections.sort(copy);
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

  /**
   * @see mitll.langtest.server.services.ExerciseServiceImpl#addPlayedMarkings(int, CommonExercise)
   * @param userID
   * @param firstExercise
   */
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

  @Override
  public void updateUser(int old, int newUser) {
    eventDAOWrapper.updateUser(old, newUser);
  }
}
