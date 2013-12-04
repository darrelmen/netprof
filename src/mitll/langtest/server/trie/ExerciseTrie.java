package mitll.langtest.server.trie;

import mitll.langtest.shared.Exercise;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/13/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseTrie extends Trie {
  private static final Logger logger = Logger.getLogger(ExerciseTrie.class);
  private static final int MB = (1024 * 1024);

  public ExerciseTrie(Collection<Exercise> exercisesForState, boolean includeForeign) {
    startMakingNodes();
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory()/ MB ;
   // logger.debug("getExercisesForSelectionState : before " + free);

    long then = System.currentTimeMillis();

    for (Exercise e : exercisesForState) {
      if (e.getEnglishSentence() != null) {
        addEntryToTrie(new ExerciseWrapper(e, true));
      }
      if (includeForeign) {
        addEntryToTrie(new ExerciseWrapper(e, false));
      }
    }
    endMakingNodes();
    long now = System.currentTimeMillis();

    if (now - then > 50) {
      logger.debug("getExercisesForSelectionState : took " + (now - then) + " millis to build ");
    }
    long freeAfter = rt.freeMemory()/ MB ;

    if (freeAfter-free > 10) {
      logMemory();
    }
    else {
     // logger.debug("getExercisesForSelectionState : after " + freeAfter);
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds(int, long, String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesForSelectionState(int, java.util.Map, long, String)
   * @param prefix
   * @return
   */
  public List<Exercise> getExercises(String prefix) {
    List<EmitValue> emits = getEmits(prefix);
    List<Exercise> ids = new ArrayList<Exercise>();
    for (EmitValue ev : emits) {
      ids.add(ev.getExercise());
    }
    logger.debug("getExercises : for '" +prefix + "' got " + ids.size() + " matches");
    return ids;
  }

  private static class ExerciseWrapper implements TextEntityValue {
    private String value;
    private Exercise e;

    /**
     * @see ExerciseTrie#ExerciseTrie(java.util.Collection, boolean)
     * @param e
     * @param useEnglish
     */
    public ExerciseWrapper(Exercise e, boolean useEnglish) {
      value = (useEnglish ? e.getEnglishSentence() : e.getRefSentence());
      this.e = e;
    }

    @Override
    public Exercise getExercise() { return e; }

    @Override
    public String getNormalizedValue() { return value; }
  }

  private long logMemory() {
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();
    logger.debug("heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
    return free;
  }
}
