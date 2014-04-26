package mitll.langtest.server;

import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.trie.EmitValue;
import mitll.langtest.server.trie.TextEntityValue;
import mitll.langtest.server.trie.Trie;
import mitll.langtest.shared.CommonExercise;
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
public class ExerciseTrie extends Trie<CommonExercise> {
  private static final Logger logger = Logger.getLogger(ExerciseTrie.class);
  private static final int MB = (1024 * 1024);

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   * @param exercisesForState
   * @param language
   */
  public ExerciseTrie(Collection<CommonExercise> exercisesForState, String language,SmallVocabDecoder smallVocabDecoder) {
    boolean includeForeign = !language.equals("English");
    startMakingNodes();
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory()/ MB ;
   // logger.debug("ExerciseTrie : searching over " + exercisesForState.size());

    long then = System.currentTimeMillis();
    boolean isMandarin = language.equalsIgnoreCase("Mandarin");

    for (CommonExercise e : exercisesForState) {
      if (e.getEnglish() != null && !e.getEnglish().isEmpty()) {
        addEntryToTrie(new ExerciseWrapper(e, true));
        Collection<String> tokens = smallVocabDecoder.getTokens(e.getEnglish());
        if (tokens.size() > 1) {
          for (String token : tokens) {
            addEntryToTrie(new ExerciseWrapper(token, e));
          }
        }
      }
      if (includeForeign) {
        if (e.getForeignLanguage() != null && !e.getForeignLanguage().isEmpty()) {
          addEntryToTrie(new ExerciseWrapper(e, false));

          Collection<String> tokens = isMandarin ? getFLTokens(smallVocabDecoder, e) : smallVocabDecoder.getTokens(e.getForeignLanguage());
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

  protected Collection<String> getFLTokens(SmallVocabDecoder smallVocabDecoder, CommonExercise e) {
    return smallVocabDecoder.getTokens(smallVocabDecoder.segmentation(e.getForeignLanguage()));
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseListWrapperForPrefix(int, String, java.util.Collection, long, String)
   * @param prefix
   * @return
   */
  public List<CommonExercise> getExercises(String prefix) {
    List<EmitValue<CommonExercise>> emits = getEmits(prefix);
    Set<CommonExercise> unique = new HashSet<CommonExercise>();
    List<CommonExercise> ids = new ArrayList<CommonExercise>();
    for (EmitValue<CommonExercise> ev : emits) {
      CommonExercise exercise = ev.getValue();
      if (!unique.contains(exercise)) {
        ids.add(exercise);
        unique.add(exercise);
      }
    }
    logger.debug("getExercises : for '" +prefix + "' got " + ids.size() + " matches");
    return ids;
  }

  private static class ExerciseWrapper implements TextEntityValue<CommonExercise> {
    private final String value;
    private final CommonExercise e;

    /**
     * @see ExerciseTrie#ExerciseTrie
     * @param e
     * @param useEnglish
     */
    public ExerciseWrapper(CommonExercise e, boolean useEnglish) {
      this((useEnglish ? e.getEnglish() : e.getForeignLanguage()), e);
    }

    public ExerciseWrapper(String value, CommonExercise e) {
      this.value = value;
      this.e = e;
    }

    @Override
    public CommonExercise getValue() { return e; }

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
