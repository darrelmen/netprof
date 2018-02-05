package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.database.IDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.npdata.dao.SlickSlimEvent;

import java.util.List;

/**
 * Created by go22670 on 3/28/16.
 */
public interface IEventDAO extends IDAO {
  boolean addToProject(Event event, int projid);

  List<Event> getAll();
  List<Event> getAll(Integer projid);
  List<Event> getAllMax(int projid);
  List<SlickSlimEvent> getAllSlim(int projid);
  List<SlickSlimEvent> getAllSlim();

  List<SlickSlimEvent> getAllDevicesSlim(int projid);

  SlickSlimEvent getFirstSlim(int projid);
  Number getNumRows(int projid);

  void addPlayedMarkings(int userID, CommonExercise firstExercise);

  void updateUser(int old, int newUser);
}
