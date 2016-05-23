package mitll.langtest.server.database.instrumentation;

import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.instrumentation.SlimEvent;
import mitll.npdata.dao.SlickSlimEvent;

import java.util.List;

/**
 * Created by go22670 on 3/28/16.
 */
public interface IEventDAO {
  boolean add(Event event);

  List<Event> getAll();

  List<SlickSlimEvent> getAllSlim();

  List<SlickSlimEvent> getAllDevicesSlim();

  SlickSlimEvent getFirstSlim();

  void addPlayedMarkings(long userID, CommonExercise firstExercise);

  Number getNumRows();
}
