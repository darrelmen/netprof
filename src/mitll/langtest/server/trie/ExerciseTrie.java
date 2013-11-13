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

  public ExerciseTrie(Collection<Exercise> exercisesForState) {
    //Trie trie = new Trie();
    startMakingNodes();
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory()/ MB ;

    //logMemory();

    long then = System.currentTimeMillis();

    for (Exercise e : exercisesForState) {
      addEntryToTrie(new ExerciseWrapper(e, true));
      addEntryToTrie(new ExerciseWrapper(e, false));
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

  public List<Exercise> getExercises(String prefix) {
    List<EmitValue> emits = getEmits(prefix);
    List<Exercise> ids = new ArrayList<Exercise>();
    for (EmitValue ev : emits) {
      ids.add(ev.getExercise());
    }
    logger.debug("for '" +prefix + "' got " + ids.size() + " matches");
    return ids;
  }

  private static class ExerciseWrapper implements TextEntityValue {
    String value; Exercise e;

    public ExerciseWrapper(Exercise e, boolean useEnglish) {
      value = (useEnglish ? e.getEnglishSentence() : e.getRefSentence());
      this.e = e;
    }

    @Override
    public Exercise getExercise() {
      return e;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getNormalizedValue() {
      return value;
    }
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
