/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.trie;

import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.trie.TextEntityValue;
import mitll.langtest.server.trie.Trie;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.log4j.Logger;

import java.text.CharacterIterator;
import java.text.Normalizer;
import java.text.StringCharacterIterator;
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

  private static final int TOOLONG_TO_WAIT = 150;
  private static final String MANDARIN = "Mandarin";
  private static final String ENGLISH = "English";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";

  /**
   * Tokens are normalized to lower case.
   *
   * Also allow lookup by transliteration.
   *
   * For mandarin, add each individual character.
   *
   * @param exercisesForState
   * @param language
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseListWrapperForPrefix
   */
  public ExerciseTrie(Collection<CommonExercise> exercisesForState, String language, SmallVocabDecoder smallVocabDecoder) {
    boolean includeForeign = !language.equals(ENGLISH);
    startMakingNodes();

    long then = System.currentTimeMillis();
    boolean isMandarin = language.equalsIgnoreCase(MANDARIN);
    boolean isKorean   = language.equalsIgnoreCase(KOREAN);
    boolean isJapanese = language.equalsIgnoreCase(JAPANESE);
    boolean hasClickableCharacters = isMandarin || isKorean || isJapanese;

    //logger.debug("lang " + language + " looking at " + exercisesForState.size());

    for (CommonExercise exercise : exercisesForState) {
      addEnglish(smallVocabDecoder, exercise);
      if (includeForeign) {
        addForeign(smallVocabDecoder, isMandarin, hasClickableCharacters, exercise);
      }
      else {
        for (String t : smallVocabDecoder.getTokens(exercise.getMeaning())) {
          addEntry(exercise, t);
        }
      }
    }
    endMakingNodes();
    long now = System.currentTimeMillis();

    if (now - then > TOOLONG_TO_WAIT) {
      logger.debug("getExercisesForSelectionState : took " + (now - then) + " millis to build ");
    }
  }

  /**
   * Mandarin has a special tokenizer.
   *
   * @param smallVocabDecoder
   * @param isMandarin
   * @param hasClickableCharacters
   * @param exercise
   */
  private void addForeign(SmallVocabDecoder smallVocabDecoder, boolean isMandarin, boolean hasClickableCharacters,
                          CommonExercise exercise) {
    String fl = exercise.getForeignLanguage();
    if (fl != null && !fl.isEmpty()) {
      addEntryToTrie(new ExerciseWrapper(exercise, false));

      Collection<String> tokens = isMandarin ?
          getMandarinTokens(smallVocabDecoder, exercise) : smallVocabDecoder.getTokens(fl);
      for (String token : tokens) {
        addEntry(exercise, token);
        addEntry(exercise, removeDiacritics(token));
      }

      if (hasClickableCharacters) {
        addClickableCharacters(exercise, fl);
      }

      String transliteration = exercise.getTransliteration();

      for (String t : smallVocabDecoder.getTokens(transliteration)) {
        addEntry(exercise, t);
        addEntry(exercise, removeDiacritics(t));
      }
    }
  }

  private void addEnglish(SmallVocabDecoder smallVocabDecoder, CommonExercise exercise) {
    String english = exercise.getEnglish();
    if (english != null && !english.isEmpty()) {
      addEntryToTrie(new ExerciseWrapper(exercise, true));
      Collection<String> tokens = smallVocabDecoder.getTokens(english.toLowerCase());
      if (tokens.size() > 1) {
        for (String token : tokens) {
          addEntry(exercise, token);
        }
      }
    }
  }

  private void addClickableCharacters(CommonExercise exercise, String fl) {
    final CharacterIterator it = new StringCharacterIterator(fl);

    for(char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
      Character character = c;
      if (!Character.isSpaceChar(c)) {
        addEntry(exercise, character.toString());
      }
    }
  }

  private boolean addEntry(CommonExercise exercise, String token) {
    return addEntryToTrie(new ExerciseWrapper(token.toLowerCase(), exercise));
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
      this((useEnglish ? e.getEnglish().toLowerCase() : e.getForeignLanguage().toLowerCase()), e);
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
   *
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
}
