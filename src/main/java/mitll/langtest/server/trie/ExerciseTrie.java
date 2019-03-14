/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.trie;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collection;
import java.util.List;

public class ExerciseTrie<T extends CommonExercise> extends Trie<T> {
  private static final Logger logger = LogManager.getLogger(ExerciseTrie.class);

  private static final int TOOLONG_TO_WAIT = 150;

  private static final String MANDARIN = "Mandarin";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";
  private final boolean isMandarin;
  private final boolean removeAllPunct;
  private final SmallVocabDecoder smallVocabDecoder;

  // private static boolean DEBUG = false;
  private boolean debug = false;

  /**
   * Tokens are normalized to lower case.
   * <p>
   * Also allow lookup by transliteration.
   * <p>
   * For mandarin, add each individual character.
   *
   * @param exercisesForState
   * @param language
   * @param doExercise
   * @param debug
   * @see Project#buildExerciseTrie
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseIds
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseListWrapperForPrefix
   */
  public ExerciseTrie(Collection<T> exercisesForState,
                      String language,
                      SmallVocabDecoder smallVocabDecoder,
                      boolean doExercise, boolean debug) {
    this.smallVocabDecoder = smallVocabDecoder;
    this.debug = debug;
    startMakingNodes();

    long then = System.currentTimeMillis();
    isMandarin = language.equalsIgnoreCase(MANDARIN);
    boolean isKorean = language.equalsIgnoreCase(KOREAN);
    boolean isJapanese = language.equalsIgnoreCase(JAPANESE);
    boolean isAsianLanguage = isMandarin || isKorean || isJapanese;
    boolean hasClickableCharacters = isAsianLanguage;
    removeAllPunct = !language.equalsIgnoreCase("french");
    //logger.debug("lang " + language + " looking at " + exercisesForState.size());

    exercisesForState.forEach(exercise -> {
      //  for (T exercise : exercisesForState) {
      if (doExercise) {
        addEntriesForExercise(isAsianLanguage, hasClickableCharacters, exercise);
      } else {
        addContextSentences(isAsianLanguage, hasClickableCharacters, exercise);
      }
    });

    endMakingNodes();
    long now = System.currentTimeMillis();

    if (now - then > TOOLONG_TO_WAIT) {
      logger.debug("ExerciseTrie : took " + (now - then) +
          " millis to build trie for " + language + " over " + exercisesForState.size() + " exercises.");
    }
  }

  public String getNormalized(String fl) {
    StringBuilder builder = new StringBuilder();
    Collection<String> tokens = smallVocabDecoder.getTokensAllLanguages(isMandarin, fl, removeAllPunct);
    tokens.forEach(t -> builder.append(t).append(" "));
    return builder.toString().trim();
  }

  /**
   * @param isMandarin
   * @param hasClickableCharacters
   * @param exercise
   * @see #ExerciseTrie(Collection, String, SmallVocabDecoder, boolean, boolean)
   */
  private void addEntriesForExercise(boolean isMandarin, boolean hasClickableCharacters, T exercise) {
    addEnglish(exercise);
    addForeign(isMandarin, hasClickableCharacters, exercise);

    {
      String meaning = exercise.getMeaning().trim();
      if (!meaning.isEmpty()) {
        smallVocabDecoder
            .getTokens(getTrimmed(meaning), false, debug)
            .forEach(token -> addEntry(exercise, token));
      }
    }
  }

  private void addContextSentences(boolean isMandarin, boolean hasClickableCharacters, T exercise) {
    for (ClientExercise ex : exercise.getDirectlyRelated()) {
      addEnglish(exercise, ex.getEnglish());
      includeForeign(isMandarin, hasClickableCharacters, exercise, ex);
    }
  }

  private void includeForeign(boolean isMandarin, boolean hasClickableCharacters, T exercise, ClientExercise ex) {
    addForeign(isMandarin, hasClickableCharacters, exercise, ex.getForeignLanguage(), ex.getTransliteration());
    if (!exercise.getAltFL().isEmpty()) {
      addForeign(isMandarin, hasClickableCharacters, exercise, ex.getAltFL(), ex.getTransliteration());
    }
  }

  /**
   * Mandarin has a special tokenizer.
   *
   * @param isMandarin
   * @param hasClickableCharacters
   * @param exercise
   * @see #addEntriesForExercise
   */
  private void addForeign(boolean isMandarin, boolean hasClickableCharacters,
                          T exercise) {
    String fl = exercise.getForeignLanguage();
    String transliteration = exercise.getTransliteration();

    addForeign(isMandarin, hasClickableCharacters, exercise, fl, transliteration);
    if (!exercise.getAltFL().isEmpty()) {
      addForeign(isMandarin, hasClickableCharacters, exercise, exercise.getAltFL(), transliteration);
    }
  }

  private void addForeign(boolean isMandarin,
                          boolean hasClickableCharacters, T exercise, String fl, String transliteration) {
    if (fl != null && !fl.isEmpty()) {
      addFL(isMandarin, hasClickableCharacters, exercise, fl);
      if (!transliteration.isEmpty()) {
        addTransliteration(transliteration, exercise);
      }
    }
  }

  private void addTransliteration(String transliteration, T exercise) {
    for (String token : smallVocabDecoder.getTokens(transliteration, false, debug)) {
      addEntry(exercise, token);
      String noAccents = StringUtils.stripAccents(token);

      if (!token.equals(noAccents) && !noAccents.isEmpty()) {
        addEntry(exercise, noAccents);
      }
      //addEntry(exercise, removeDiacritics(token));
    }
  }

  /**
   * @param isMandarin
   * @param hasClickableCharacters
   * @param exToReturnOnMatch
   * @param fl
   * @see #addForeign(boolean, boolean, CommonExercise, String, String)
   */
  private void addFL(boolean isMandarin, boolean hasClickableCharacters, T exToReturnOnMatch, String fl) {
    fl = getTrimmed(fl);

    if (!fl.isEmpty()) {
      if (debug) logger.info("addFL " + fl + " for " + exToReturnOnMatch.getID());
      addEntryToTrie(new ExerciseWrapper<>(fl, exToReturnOnMatch));
      //addSubstrings(exToReturnOnMatch, fl);

      addSuffixes(exToReturnOnMatch, fl);

      Collection<String> tokens = smallVocabDecoder.getTokensAllLanguages(isMandarin, fl, removeAllPunct);
      for (String token : tokens) {
        addEntry(exToReturnOnMatch, token);
        // String noAccents = removeDiacritics(token);
        String noAccents = StringUtils.stripAccents(token);

        if (!token.equals(noAccents) && !noAccents.isEmpty()) {
          addEntry(exToReturnOnMatch, noAccents);
        }
      }

      if (hasClickableCharacters) {
        addClickableCharacters(exToReturnOnMatch, fl);
      }
    }
  }

  private void addEnglish(T exercise) {
    //String english = exercise.getEnglish();
    if (debug) logger.info("addEnglish " + exercise.getEnglish());
    addEnglish(exercise, exercise.getEnglish());
    // addSubstrings(exercise, english);
  }

  private void addEnglish(T exercise, String english) {
    if (english != null && !english.isEmpty()) {
      String trimmed = getTrimmed(english);
      if (!trimmed.isEmpty()) {
        if (debug) logger.info("addEnglish 2 " + trimmed);
        addEntryToTrie(new ExerciseWrapper<>(trimmed, exercise));
        addSuffixes(exercise, trimmed);
      }
    }
  }

  private void addSuffixes(T exercise, String trimmed) {
    Collection<String> tokens = smallVocabDecoder.getTokens(trimmed, false, debug);

    trimmed = trimmed.toLowerCase();

    if (tokens.size() > 1) {
      for (String token : tokens) {
        if (token.length() > trimmed.length()) {
          logger.error("token   " + token);
          logger.error("trimmed " + trimmed);
        } else {
          String substring = trimmed.substring(token.length());

          String trimmed1 = smallVocabDecoder.getTrimmed(substring);
          if (trimmed1.isEmpty()) {
            //logger.error("is empty ");
          } else {
            if (debug) logger.info("addSuffixes '" + trimmed1 + "'");
            addEntryNoLC(exercise, trimmed1);
          }
          trimmed = trimmed1;
        }
      }
    }
  }

  private String getTrimmed(String english) {
    String sentence = english.toLowerCase();
    String trimmed = smallVocabDecoder.getTrimmed(sentence);
//    if (!sentence.equals(trimmed)) {
//      logger.info("before " + sentence);
//      logger.info("after  " + trimmed);
//    }
    return trimmed;
  }

  private void addClickableCharacters(T exercise, String fl) {
    final CharacterIterator it = new StringCharacterIterator(fl);

    for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
      if (!Character.isSpaceChar(c)) {
        addEntry(exercise, Character.toString(c));
      }
    }
  }

  /**
   * @param exercise
   * @paramx fl
   * @see #addEnglish(CommonExercise)
   * @see #addFL(boolean, boolean, CommonExercise, String)
   */
/*  private void addSubstrings(T exercise, String fl) {
    List<String> tokens = smallVocabDecoder.getTokens(fl);

    List<String> collect = tokens.stream().map(String::toLowerCase).collect(Collectors.toList());
    for (String token : new HashSet<>(collect)) {
      for (int i = 0; i < token.length(); i++) {
        String substring = token.substring(i);
        //char c = substring.charAt(0);
        addEntry(exercise, substring);
//        logger.info("adding " + substring);

      }
    }
  }*/
  private void addEntry(T exercise, String token) {
    addEntryToTrie(new ExerciseWrapper<>(token.toLowerCase(), exercise));
  }

  private void addEntryNoLC(T exercise, String token) {
    addEntryToTrie(new ExerciseWrapper<>(token, exercise));
  }

  /**
   * @param prefix
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseIds
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseListWrapperForPrefix
   */
  public List<T> getExercises(String prefix) {
    List<T> matches = getMatches(getTrimmed(prefix));

/*    for (T ex : matches)
      if (ex.isContext()) logger.info("getExercises : returning context exercise " + ex.getID() + " " + ex.getEnglish());*/
    //   logger.info("getExercises trim '" + prefix.toLowerCase() + "' = '" + trimmed + "' => " +matches.size());

    return matches;
  }

  private static class ExerciseWrapper<T extends CommonShell> implements TextEntityValue<T> {
    private final String value;
    private final T e;

    ExerciseWrapper(String value, T e) {
      this.value = value;
      this.e = e;
    }

    @Override
    public T getValue() {
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
/*  private String removeDiacritics(String input) {
    String nrml = Normalizer.normalize(input, Normalizer.Form.NFD);
    StringBuilder stripped = new StringBuilder();
    for (int i = 0; i < nrml.length(); ++i) {
      if (Character.getProperty(nrml.charAt(i)) != Character.NON_SPACING_MARK) {
        stripped.append(nrml.charAt(i));
      }
    }
    return stripped.toString();
  }*/
}
