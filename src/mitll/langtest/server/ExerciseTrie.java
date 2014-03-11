package mitll.langtest.server;

import mitll.langtest.server.scoring.SmallVocabDecoder;
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds(int, long, String, long)
   * @param exercisesForState
   * @param language
   */
  public ExerciseTrie(Collection<Exercise> exercisesForState, String language,SmallVocabDecoder smallVocabDecoder) {
    boolean includeForeign = !language.equals("English");
    startMakingNodes();
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory()/ MB ;
    logger.debug("ExerciseTrie : searching over " + exercisesForState.size());

    long then = System.currentTimeMillis();
    boolean isMandarin = language.equalsIgnoreCase("Mandarin");

    for (Exercise e : exercisesForState) {
      if (e.getEnglishSentence() != null && !e.getEnglishSentence().isEmpty()) {
        addEntryToTrie(new ExerciseWrapper(e, true));
        Collection<String> tokens = smallVocabDecoder.getTokens(e.getEnglishSentence());
        if (tokens.size() > 1) {
          for (String token : tokens) {
            addEntryToTrie(new ExerciseWrapper(token, e));
          }
        }
      }
      if (includeForeign) {
        if (e.getRefSentence() != null && !e.getRefSentence().isEmpty()) {
          addEntryToTrie(new ExerciseWrapper(e, false));

          Collection<String> tokens = isMandarin ? getFLTokens(smallVocabDecoder, e) : smallVocabDecoder.getTokens(e.getRefSentence());
          if (tokens.size() > 1) {
            for (String token : tokens) {
              addEntryToTrie(new ExerciseWrapper(token, e));
            }
          }
        }
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

  protected Collection<String> getFLTokens(SmallVocabDecoder smallVocabDecoder, Exercise e) {
    return smallVocabDecoder.getTokens(smallVocabDecoder.segmentation(e.getRefSentence()));
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
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
     * @see ExerciseTrie#ExerciseTrie
     * @param e
     * @param useEnglish
     */
    public ExerciseWrapper(Exercise e, boolean useEnglish) {
      this((useEnglish ? e.getEnglishSentence() : e.getRefSentence()), e);
    }

    public ExerciseWrapper(String value, Exercise e) {
      this.value = value;
      this.e = e;
    }

    @Override
    public Exercise getValue() { return e; }

    @Override
    public String getNormalizedValue() { return value; }

    public String toString() { return "e " +e.getID() + " : " + value; }
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
