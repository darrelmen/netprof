package mitll.langtest.server;

import mitll.langtest.server.trie.EmitValue;
import mitll.langtest.server.trie.TextEntityValue;
import mitll.langtest.server.trie.Trie;
import mitll.langtest.shared.Exercise;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/13/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseTrie extends Trie<Exercise> {
  private static final Logger logger = Logger.getLogger(ExerciseTrie.class);
  private static final int MB = (1024 * 1024);

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds(int, long, String)
   * @param exercisesForState
   * @param includeForeign
   */
  public ExerciseTrie(Collection<Exercise> exercisesForState, boolean includeForeign) {
    startMakingNodes();
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory()/ MB ;
    logger.debug("ExerciseTrie : searching over " + exercisesForState.size());

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
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds(int, long, String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesForSelectionState(int, java.util.Map, long, String)
   * @param prefix
   * @return
   */
  public List<Exercise> getExercises(String prefix) {
    List<EmitValue<Exercise>> emits = getEmits(prefix);
    Set<Exercise> unique = new HashSet<Exercise>();
    List<Exercise> ids = new ArrayList<Exercise>();
    for (EmitValue<Exercise> ev : emits) {
      Exercise exercise = ev.getValue();
      if (!unique.contains(exercise)) {
        ids.add(exercise);
        unique.add(exercise);
      }
    }
    logger.debug("getExercises : for '" +prefix + "' got " + ids.size() + " matches");
    return ids;
  }

  private static class ExerciseWrapper implements TextEntityValue<Exercise> {
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
    public Exercise getValue() { return e; }

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
