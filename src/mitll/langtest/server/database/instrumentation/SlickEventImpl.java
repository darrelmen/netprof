package mitll.langtest.server.database.instrumentation;

import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.instrumentation.SlimEvent;
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
  EventDAOExample eventDAOExample;

  public SlickEventImpl(String language) {

    eventDAOExample = new EventDAOExample("localhost", 5432, language);
  }

  public void copyTableOnlyOnce(IEventDAO other) {
    if (getNumRows().intValue() == 0) {
      List<SlickEvent> copy = new ArrayList<>();
      List<Event> all = other.getAll();

      logger.info("copyTableOnlyOnce " + all.size() + " events ");

      for (Event event : all) copy.add(getSlickEvent(event));
      eventDAOExample.addBulk(copy);
    }
  }

  @Override
  public boolean add(Event event) {
    eventDAOExample.add(getSlickEvent(event));
    return true;
  }

  private SlickEvent getSlickEvent(Event event) {
    long timestamp = event.getTimestamp();
    if (timestamp == 0) timestamp = System.currentTimeMillis();
    return new SlickEvent(-1,
        event.getUserID(),
        event.getExerciseID(),
        event.getContext() == null ? "":event.getContext(),
        event.getWidgetID(),
        event.getWidgetType(),
        event.getDevice() == null ? "" :event.getDevice(),
        new Timestamp(timestamp));
  }

  @Override
  public List<Event> getAll() {
    List<SlickEvent> all = eventDAOExample.getAll();

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
  public List<SlickSlimEvent> getAllSlim() {
    return eventDAOExample.getAllSlim();
  }

  @Override
  public List<SlickSlimEvent> getAllDevicesSlim() {
    return eventDAOExample.getAllSlim();
  }

  @Override
  public SlickSlimEvent getFirstSlim() {
    return eventDAOExample.getFirstSlim();
  }

  @Override
  public void addPlayedMarkings(long userID, CommonExercise firstExercise) {
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
  public Number getNumRows() {
    return eventDAOExample.getNumRows();
  }
}
