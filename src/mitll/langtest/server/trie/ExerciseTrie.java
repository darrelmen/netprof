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

import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.exercise.CommonShell;
import org.apache.log4j.Logger;

import java.text.CharacterIterator;
import java.text.Normalizer;
import java.text.StringCharacterIterator;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/13/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseTrie<T extends CommonShell> extends Trie<T> {
  private static final Logger logger = Logger.getLogger(ExerciseTrie.class);

  private static final int TOOLONG_TO_WAIT = 150;
  private static final String MANDARIN = "Mandarin";
  private static final String ENGLISH = "English";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";

  /**
   * Tokens are normalized to lower case.
   * <p>
   * Also allow lookup by transliteration.
   * <p>
   * For mandarin, add each individual character.
   *
   * @param exercisesForState
   * @param language
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseListWrapperForPrefix
   */
  public ExerciseTrie(Collection<T> exercisesForState, String language,
                      SmallVocabDecoder smallVocabDecoder) {
    boolean includeForeign = !language.equals(ENGLISH);
    startMakingNodes();

    long then = System.currentTimeMillis();
    boolean isMandarin = language.equalsIgnoreCase(MANDARIN);
    boolean isKorean = language.equalsIgnoreCase(KOREAN);
    boolean isJapanese = language.equalsIgnoreCase(JAPANESE);
    boolean hasClickableCharacters = isMandarin || isKorean || isJapanese;

    //logger.debug("lang " + language + " looking at " + exercisesForState.size());

    for (T exercise : exercisesForState) {
      addEnglish(smallVocabDecoder, exercise);
      if (includeForeign) {
        addForeign(smallVocabDecoder, isMandarin, hasClickableCharacters, exercise);
      } else {
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
                          T exercise) {
    String fl = exercise.getForeignLanguage();
    if (fl != null && !fl.isEmpty()) {
      ExerciseWrapper<T> textEntityDescription = new ExerciseWrapper<>(exercise, false);
      addEntryToTrie(textEntityDescription);

      Collection<String> tokens = isMandarin ?
          getMandarinTokens(smallVocabDecoder, exercise) : smallVocabDecoder.getTokens(fl);

      for (String token : tokens) {
        addEntry(exercise, token);
        String token1 = removeDiacritics(token);

        if (!token1.equals(token) && !token1.isEmpty()) {
          addEntry(exercise, token1);
        }
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

  private void addEnglish(SmallVocabDecoder smallVocabDecoder, T exercise) {
    String english = exercise.getEnglish();
    if (english != null && !english.isEmpty()) {
      ExerciseWrapper<T> textEntityDescription = new ExerciseWrapper<>(exercise, true);

      //if (textEntityDescription.isEmpty()) logger.error("huh? for " +exercise.getID() + " entry is empty?");
      addEntryToTrie(textEntityDescription);
      Collection<String> tokens = smallVocabDecoder.getTokens(english.toLowerCase());
      if (tokens.size() > 1) {
        for (String token : tokens) {
          addEntry(exercise, token);
        }
      }
    }
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

  private boolean addEntry(T exercise, String token) {
    //boolean debug = exercise.getID().equals("14");
    //if (debug) logger.warn("adding '" + token + "' : '" + token.toLowerCase() + "'");

    ExerciseWrapper<T> textEntityDescription = new ExerciseWrapper<>(token.toLowerCase(), exercise);
    //if (textEntityDescription.isEmpty()) logger.error("addEntry - huh? "+textEntityDescription.getNormalizedValue() + " is empty " + exercise.getID());
    return addEntryToTrie(textEntityDescription);
  }

  private Collection<String> getMandarinTokens(SmallVocabDecoder smallVocabDecoder, T e) {
    return smallVocabDecoder.getMandarinTokens(e.getForeignLanguage());
  }

  /**
   * @param prefix
   * @param smallVocabDecoder
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseListWrapperForPrefix
   */
  public Collection<T> getExercises(String prefix, SmallVocabDecoder smallVocabDecoder) {
    return getMatches(smallVocabDecoder.getTrimmed(prefix.toLowerCase()));
  }

  private static class ExerciseWrapper<T extends CommonShell> implements TextEntityValue<T> {
    private final String value;
    private final T e;

    /**
     * @param e
     * @param useEnglish
     * @see ExerciseTrie#ExerciseTrie
     */
    public ExerciseWrapper(T e, boolean useEnglish) {
      this((useEnglish ? e.getEnglish().toLowerCase() : e.getForeignLanguage().toLowerCase()), e);
    }

    public ExerciseWrapper(String value, T e) {
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

    public boolean isEmpty() { return value.isEmpty(); }

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
