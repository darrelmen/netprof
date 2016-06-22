package mitll.langtest.server.database.instrumentation;

import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.npdata.dao.SlickSlimEvent;

import java.util.List;

/**
 * Created by go22670 on 3/28/16.
 */
public interface IEventDAO {
  boolean add(Event event, String language);

  List<Event> getAll(String language);
  List<Event> getAllMax(String language);
  List<SlickSlimEvent> getAllSlim(String language);

  List<SlickSlimEvent> getAllDevicesSlim(String language);

  SlickSlimEvent getFirstSlim(String language);

  void addPlayedMarkings(int userID, CommonExercise firstExercise);

  Number getNumRows(String language);
}
