package mitll.langtest.server;

import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.trie.TextEntityValue;
import mitll.langtest.server.trie.Trie;
import mitll.langtest.shared.CommonExercise;
import org.apache.log4j.Logger;

import java.text.Normalizer;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/13/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseTrie extends Trie<CommonExercise> {
  private static final Logger logger = Logger.getLogger(ExerciseTrie.class);

//  private static final int MB = (1024 * 1024);
  private static final int TOOLONG_TO_WAIT = 150;
  private static final String MANDARIN = "Mandarin";
  private static final String ENGLISH = "English";

  /**
   * Tokens are normalized to lower case.
   *
   * @param exercisesForState
   * @param language
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseListWrapperForPrefix
   */
  public ExerciseTrie(Collection<CommonExercise> exercisesForState, String language, SmallVocabDecoder smallVocabDecoder) {
    boolean includeForeign = !language.equals(ENGLISH);
    startMakingNodes();
  //  Runtime rt = Runtime.getRuntime();
  //  long free = rt.freeMemory() / MB;

    long then = System.currentTimeMillis();
    boolean isMandarin = language.equalsIgnoreCase(MANDARIN);

    for (CommonExercise exercise : exercisesForState) {
      String english = exercise.getEnglish();
      if (english != null && !english.isEmpty()) {
        addEntryToTrie(new ExerciseWrapper(exercise, true));
        Collection<String> tokens = smallVocabDecoder.getTokens(english.toLowerCase());
        if (tokens.size() > 1) {
          for (String token : tokens) {
            addEntryToTrie(new ExerciseWrapper(token, exercise));
          }
        }
      }
      if (includeForeign) {
        if (exercise.getForeignLanguage() != null && !exercise.getForeignLanguage().isEmpty()) {
          addEntryToTrie(new ExerciseWrapper(exercise, false));

          Collection<String> tokens = isMandarin ? getMandarinTokens(smallVocabDecoder, exercise) : smallVocabDecoder.getTokens(exercise.getForeignLanguage());
          for (String token : tokens) {
            addEntryToTrie(new ExerciseWrapper(token, exercise));
            addEntryToTrie(new ExerciseWrapper(removeDiacritics(token), exercise));
          }
        }
      }
    }
    endMakingNodes();
    long now = System.currentTimeMillis();

    if (now - then > TOOLONG_TO_WAIT) {
      logger.debug("getExercisesForSelectionState : took " + (now - then) + " millis to build ");
    }
/*    long freeAfter = rt.freeMemory() / MB;

    if (freeAfter - free > 40) {
      logMemory();
    }*/
  }

  private Collection<String> getMandarinTokens(SmallVocabDecoder smallVocabDecoder, CommonExercise e) {
    return smallVocabDecoder.getMandarinTokens(e.getForeignLanguage());
  }

  /**
   * @param prefix
   * @param smallVocabDecoder
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseListWrapperForPrefix
   */
  public Collection<CommonExercise> getExercises(String prefix, SmallVocabDecoder smallVocabDecoder) {
    return getMatches(smallVocabDecoder.getTrimmed(prefix.toLowerCase()));
  }

  private static class ExerciseWrapper implements TextEntityValue<CommonExercise> {
    private final String value;
    private final CommonExercise e;

    /**
     * @param e
     * @param useEnglish
     * @see ExerciseTrie#ExerciseTrie
     */
    public ExerciseWrapper(CommonExercise e, boolean useEnglish) {
      this((useEnglish ? e.getEnglish().toLowerCase() : e.getForeignLanguage()), e);
    }

    public ExerciseWrapper(String value, CommonExercise e) {
      this.value = value;
      this.e = e;
    }

    @Override
    public CommonExercise getValue() {
      return e;
    }

    @Override
    public String getNormalizedValue() {
      return value;
    }

    public String toString() {
      return "e " + e.getID() + " : " + value;
    }
  }

  /**
   * So we can match when we don't type with accent marks in search box.
   * @param input
   * @return
   */
  private String removeDiacritics(String input) {
    String nrml = Normalizer.normalize(input, Normalizer.Form.NFD);
    StringBuilder stripped = new StringBuilder();
    for (int i = 0; i < nrml.length(); ++i) {
      if (Character.getType(nrml.charAt(i)) != Character.NON_SPACING_MARK) {
        stripped.append(nrml.charAt(i));
      }
    }
    return stripped.toString();
  }

/*  private void logMemory() {
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();
    logger.debug("heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
  }*/
}
