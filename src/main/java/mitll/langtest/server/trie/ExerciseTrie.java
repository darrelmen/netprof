/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.trie;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @see Project#fullTrie
 * @since 11/13/13
 * Time: 4:42 PM
 */
public class ExerciseTrie<T extends CommonExercise> extends Trie<T> {
  private static final Logger logger = LogManager.getLogger(ExerciseTrie.class);

  private static final int TOOLONG_TO_WAIT = 150;
  private static final String MANDARIN = "Mandarin";
  private static final String ENGLISH = "English";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";
  private SmallVocabDecoder smallVocabDecoder;

  /**
   * Tokens are normalized to lower case.
   * <p>
   * Also allow lookup by transliteration.
   * <p>
   * For mandarin, add each individual character.
   *
   * @param exercisesForState
   * @param language
   * @see Project#buildExerciseTrie
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseIds
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseListWrapperForPrefix
   */
  public ExerciseTrie(Collection<T> exercisesForState,
                      String language,
                      SmallVocabDecoder smallVocabDecoder) {
    boolean includeForeign = !language.equals(ENGLISH);
    this.smallVocabDecoder = smallVocabDecoder;
    startMakingNodes();

    long then = System.currentTimeMillis();
    boolean isMandarin = language.equalsIgnoreCase(MANDARIN);
    boolean isKorean = language.equalsIgnoreCase(KOREAN);
    boolean isJapanese = language.equalsIgnoreCase(JAPANESE);
    boolean hasClickableCharacters = isMandarin || isKorean || isJapanese;

    //logger.debug("lang " + language + " looking at " + exercisesForState.size());
    for (T exercise : exercisesForState) {
      addEntryForExercise(includeForeign, isMandarin, hasClickableCharacters, exercise);
    }
    endMakingNodes();
    long now = System.currentTimeMillis();

    if (now - then > TOOLONG_TO_WAIT) {
      logger.debug("getExercisesForSelectionState : took " + (now - then) + " millis to build ");
    }
  }

  private void addEntryForExercise(
      boolean includeForeign,
      boolean isMandarin,
      boolean hasClickableCharacters,
      T exercise) {
    addEnglish(exercise);
    if (includeForeign) {
      addForeign(isMandarin, hasClickableCharacters, exercise);
    } else {
      String meaning = exercise.getMeaning();
      String trimmed = getTrimmed(meaning);

      for (String t : smallVocabDecoder.getTokens(trimmed)) {
        addEntry(exercise, t);
      }
    }

    for (CommonExercise ex : exercise.getDirectlyRelated()) {
      addEnglish(exercise, ex.getEnglish());
      if (includeForeign) {
        addForeign(isMandarin, hasClickableCharacters, exercise, ex.getForeignLanguage(), ex.getTransliteration());
      }
    }
  }

  /**
   * Mandarin has a special tokenizer.
   *
   * @param isMandarin
   * @param hasClickableCharacters
   * @param exercise
   * @paramz smallVocabDecoder
   */
  private void addForeign(boolean isMandarin, boolean hasClickableCharacters,
                          T exercise) {
    String fl = exercise.getForeignLanguage();
    String transliteration = exercise.getTransliteration();

    addForeign(isMandarin, hasClickableCharacters, exercise, fl, transliteration);
  }

  private void addForeign(boolean isMandarin, boolean hasClickableCharacters, T exercise, String fl, String transliteration) {
    if (fl != null && !fl.isEmpty()) {
      addFL(isMandarin, hasClickableCharacters, exercise, fl);
      addTransliteration(transliteration, exercise);
    }
  }

  private void addTransliteration(String transliteration, T exercise) {
    for (String token : smallVocabDecoder.getTokens(transliteration)) {
      addEntry(exercise, token);
      String noAccents = StringUtils.stripAccents(token);

      if (!token.equals(noAccents) && !noAccents.isEmpty()) {
        addEntry(exercise, noAccents);
      }
      //addEntry(exercise, removeDiacritics(token));
    }
  }

  private void addFL(boolean isMandarin, boolean hasClickableCharacters, T exercise, String fl) {
    fl = getTrimmed(fl);
    addEntryToTrie(new ExerciseWrapper<>(fl, exercise));
    addSubstrings(exercise, fl);

    Collection<String> tokens = isMandarin ? getMandarinTokens(fl) : smallVocabDecoder.getTokens(fl);
    for (String token : tokens) {
      addEntry(exercise, token);
      // String noAccents = removeDiacritics(token);
      String noAccents = StringUtils.stripAccents(token);

      if (!token.equals(noAccents) && !noAccents.isEmpty()) {
        addEntry(exercise, noAccents);
      }
    }

    if (hasClickableCharacters) {
      addClickableCharacters(exercise, fl);
    }
  }

  private void addEnglish(T exercise) {
    String english = exercise.getEnglish();
    addEnglish(exercise, english);
  }

  private void addEnglish(T exercise, String english) {
    if (english != null && !english.isEmpty()) {
      String trimmed = getTrimmed(english);

      addEntryToTrie(new ExerciseWrapper<>(trimmed, exercise));
      Collection<String> tokens = smallVocabDecoder.getTokens(trimmed);
      if (tokens.size() > 1) {
        for (String token : tokens) {
          addEntry(exercise, token);
        }
      }
    }
  }

  private String getTrimmed(String english) {
    return smallVocabDecoder.getTrimmed(english.toLowerCase());
  }

  private void addClickableCharacters(T exercise, String fl) {
    final CharacterIterator it = new StringCharacterIterator(fl);

    for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
      Character character = c;
      if (!Character.isSpaceChar(c)) {
        addEntry(exercise, character.toString());
      }
    }
  }

  private void addSubstrings(T exercise, String fl) {
    List<String> tokens = smallVocabDecoder.getTokens(fl);

    List<String> collect = tokens.stream().map(String::toLowerCase).collect(Collectors.toList());
    for (String token : new HashSet<>(collect)) {
      for (int i = 0; i < token.length(); i++) {
        String substring = token.substring(i);
        char c = substring.charAt(0);
        addEntry(exercise, substring);
//        logger.info("adding " + substring);

      }
    }
  }

  private boolean addEntry(T exercise, String token) {
    //logger.info("add token '" + token + "' for  " + exercise.getID());
    return addEntryToTrie(new ExerciseWrapper<>(token.toLowerCase(), exercise));
  }

//  private Collection<String> getMandarinTokens(T e) {
//    String foreignLanguage = e.getForeignLanguage();
//    return getMandarinTokens(foreignLanguage);
//  }

  private Collection<String> getMandarinTokens(String foreignLanguage) {
    return smallVocabDecoder.getMandarinTokens(foreignLanguage);
  }

  /**
   * @param prefix
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseIds
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseListWrapperForPrefix
   */
  public List<T> getExercises(String prefix) {
    String trimmed = getTrimmed(prefix);
    //  logger.info("trim '" + prefix.toLowerCase() + "' = '" + trimmed + "'");
    return getMatches(trimmed);
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
