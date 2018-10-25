package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.CommonExercise;

import java.util.Collections;
import java.util.List;

/**
 * Three buckets - match by id, matches on vocab item, then match on context sentences
 *
 * @param <T>
 */
public class TripleExercises<T extends CommonExercise> {
  private List<T> byID = Collections.emptyList();
  private List<T> byExercise = Collections.emptyList();
  private List<T> byContext = Collections.emptyList();

  public TripleExercises() {
  }

  TripleExercises(List<T> byID, List<T> byExercise, List<T> byContext) {
    this.byID = byID;
    this.byExercise = byExercise;
    this.byContext = byContext;
  }

  public List<T> getByID() {
    return byID;
  }

  public List<T> getByExercise() {
    return byExercise;
  }

  public TripleExercises<T> setByExercise(List<T> byExercise) {
    this.byExercise = byExercise;
    return this;
  }

  /**
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getSortedContext
   */
  public List<T> getByContext() {
    return byContext;
  }

  public void setByID(List<T> byID) {
    this.byID = byID;
  }

  public String toString() {
    return
            "\n\tby id      " + byID.size() +
            "\n\tby ex      " + byExercise.size() +
            "\n\tby context " + byContext.size();
  }
}
