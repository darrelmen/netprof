package mitll.langtest.server.database.instrumentation;

import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.instrumentation.SlimEvent;
import mitll.npdata.dao.EventDAOExample;
import mitll.npdata.dao.SlickEvent;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

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
    return new SlickEvent(-1,
        event.getCreatorID(),
        event.getExerciseID(),
        event.getContext() == null ? "":event.getContext(),
        event.getWidgetID(),
        event.getWidgetType(),
        event.getDevice() == null ? "" :event.getDevice(),
        event.getTimestamp());
  }

  @Override
  public List<Event> getAll() {
    List<SlickEvent> all = eventDAOExample.getAll();

    logger.info("getting " + all.size() + " events to ");
    List<Event> copy = new ArrayList<>();
    for (SlickEvent event : all) {
      copy.add(new Event(
          event.widgetid(),
          event.widgettype(),
          event.exerciseid(),
          event.context(),
          event.creatorid(),
          event.modified(),
          event.device())
      );
    }
    return copy;
  }

  @Override
  public List<SlimEvent> getAllSlim() {
    return null;
  }

  @Override
  public List<SlimEvent> getAllDevicesSlim() {
    return null;
  }

  @Override
  public SlimEvent getFirstSlim() {
    return null;
  }

  @Override
  public void addPlayedMarkings(long userID, CommonExercise firstExercise) {

  }

  @Override
  public Number getNumRows() {
    return eventDAOExample.getNumRows();
  }
}
