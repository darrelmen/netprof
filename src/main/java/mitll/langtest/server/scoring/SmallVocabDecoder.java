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

package mitll.langtest.server.scoring;

import corpus.HTKDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;

/**
 * Creates the LM for a small vocab decoder from foreground and background sentences.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/7/13
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class SmallVocabDecoder {
  private static final Logger logger = LogManager.getLogger(SmallVocabDecoder.class);

  /**
   * @see #getTrimmed(String)
   * @see mitll.langtest.server.audio.SLFFile#cleanToken(String)
   * <p>
   * remove latin capital letter i with dot above - 0130
   */
  public static final String REMOVE_ME = "[\\u0130\\u2022\\u2219\\u2191\\u2193;~/']";
  private static final char FULL_WIDTH_ZERO = '\uFF10';
  private static final char ZERO = '0';
  private static final String P_Z = "\\p{Z}+";
  private HTKDictionary htkDictionary;

  public SmallVocabDecoder() {
  }

  /**
   * @param htkDictionary
   * @see PronunciationLookup#makeDecoder
   */
  public SmallVocabDecoder(HTKDictionary htkDictionary) {
    this.htkDictionary = htkDictionary;
    //logger.info("SmallVocabDecoder dict now " + ((htkDictionary != null) ? htkDictionary.size() : " null dict"));
  }

  /**
   * Get the vocabulary to use when generating a language model. <br></br>
   * Very important to limit the vocabulary (less than 300 words) or else the small vocab dcodr will run out of
   * memory and segfault! <br></br>
   * Remember to add special tokens like silence, pause, and unk
   *
   * @param background sentences
   * @return most frequent vocabulary words
   * @see ASRWebserviceScoring#getUsedTokens
   */
  List<String> getVocab(Collection<String> background, int vocabSizeLimit) {
    return getSimpleVocab(background, vocabSizeLimit);
  }

  /**
   * @param s
   * @return
   * @see #getTokens(String, boolean)
   */
  String toFull(String s) {
    StringBuilder builder = new StringBuilder();

    final CharacterIterator it = new StringCharacterIterator(s);
    for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
      if (c >= ZERO && c <= '9') {
        int offset = c - ZERO;
        int full = FULL_WIDTH_ZERO + offset;
        builder.append(Character.valueOf((char) full).toString());
      } else {
        builder.append(c);
      }
    }
    return builder.toString();
  }

  /**
   * @param sentences
   * @param vocabSizeLimit
   * @return
   * @see PronunciationLookup#getUniqueTokensInLM
   */
  List<String> getSimpleVocab(Collection<String> sentences, int vocabSizeLimit) {
    // childCount the tokens
    final Map<String, Integer> sc = new HashMap<>();
    sentences.forEach(sent -> getTokens(sent, false).forEach(token -> {
      Integer c = sc.get(token);
      sc.put(token, (c == null) ? 1 : c + 1);
    }));

/*    for (String sentence : sentences) {
      for (String token : getTokens(sentence)) {
        //  if (isValid(scoring, token)) {
        Integer c = sc.get(token);
        sc.put(token, (c == null) ? 1 : c + 1);
     *//*   } else {
          logger.warn("getSimpleVocab : skipping '" + token + "' which is not in dictionary.");
        }*//*
      }
    }*/

    // sort by frequency
    List<String> vocab = sortedByFreq(sc);

    // take top n most frequent
    if (vocab.size() > vocabSizeLimit)
      logger.warn("truncating vocab size from " + vocab.size() + " to " + vocabSizeLimit);
    return new ArrayList<>(vocab.subList(0, Math.min(vocab.size(), vocabSizeLimit)));
  }

  @NotNull
  private List<String> sortedByFreq(Map<String, Integer> sc) {
    List<String> vocab = new ArrayList<>(sc.keySet());
    vocab.sort((s, s2) -> -1 * Integer.compare(sc.get(s), sc.get(s2)));
    //return first < second ? +1 : first > second ? -1 : 0;
    // });
    return vocab;
  }

  public String getSegmented(String longPhrase, boolean removeAllAccents) {
    Collection<String> tokens = getTokens(longPhrase, removeAllAccents);
    StringBuilder builder = new StringBuilder();
    tokens.forEach(token -> builder.append(segmentation(token.trim())).append(" "));
//    for (String token : tokens) {
//      builder.append(segmentation(token.trim()));
//      builder.append(" ");
//    }
    return builder.toString();
  }

  public List<String> getTokensAllLanguages(boolean isMandarin, String fl, boolean removeAllAccents) {
    return isMandarin ? getMandarinTokens(fl) : getTokens(fl, removeAllAccents);
  }



  /**
   * @param sentence
   * @param removeAllAccents
   * @return
   * @see IPronunciationLookup#getPronunciationsFromDictOrLTS
   * @see mitll.langtest.server.audio.SLFFile#createSimpleSLFFile
   */
  public List<String> getTokens(String sentence, boolean removeAllAccents) {
    List<String> all = new ArrayList<>();
    if (sentence.isEmpty()) {
      logger.warn("huh? empty ", new Exception());
    }
  //  logger.info("getTokens initial    '" + sentence + "'");
    String trimmedSent = getTrimmedSent(sentence, removeAllAccents);
    //if (removeAllAccents) {
     // logger.info("getTokens after trim '" + trimmedSent + "'");
   // }

    for (String untrimedToken : trimmedSent.split(P_Z)) { // split on spaces
      //String tt = untrimedToken.replaceAll("\\p{P}", ""); // remove all punct
      String token = untrimedToken.trim();  // necessary?
      if (token.length() > 0) {
        all.add(toFull(token));
//        if (!token.equals("UNKNOWNMODEL")) {
//          logger.debug("\ttoken " + token);
//        }
      }
    }

    return all;
  }

  private String getTrimmedSent(String sentence, boolean removeAllAccents) {
    return removeAllAccents ?
        getTrimmed(sentence) :
        getTrimmedLeaveAccents(sentence);
  }

  /**
   * @param foreignLanguage
   * @return
   */
  private List<String> getMandarinTokens(String foreignLanguage) {
    String segmentation = segmentation(foreignLanguage);
//    logger.info("getMandarinTokens '" + foreignLanguage +  "' = '" + segmentation + "'");
    return getTokens(segmentation, false);
  }

  /**
   * Tries to remove junky characters from the sentence so hydec won't choke on them.
   * <p>
   * Also removes the chinese unicode bullet character, like in Bill Gates.
   * <p>
   * TODO : really slow - why not smarter?
   *
   * @param sentence
   * @return
   * @see #getTokens
   * @see mitll.langtest.server.trie.ExerciseTrie#getExercises
   */
  public String getTrimmed(String sentence) {
    String trim = getTrimmedLeaveLastSpace(sentence)
        //.replaceAll("\\s+", " ")
        .trim();
    //logger.warn("getTrimmed before " + sentence + " after "+ trim);
    return trim;
  }

  public String getTrimmedLeaveAccents(String sentence) {
    String trim = sentence.replaceAll("[.?]", "")
        //.replaceAll("\\s+", " ")
        .trim();
    //logger.warn("getTrimmedLeaveAccents before " + sentence + " after "+ trim);
    return trim;
  }

  /**
   * We want to keep accents - french accents especially...
   * They are in the dictionary.
   *
   * @param sentence
   * @return
   * @see #getTrimmed
   * @see mitll.langtest.server.trie.ExerciseTrie#getTrimmed
   */
  public String getTrimmedLeaveLastSpace(String sentence) {
    String s = sentence
        .replaceAll(REMOVE_ME, " ")
        //   .replaceAll("", " ")
        .replaceAll(P_Z, " ")  // normalize all whitespace
        // .replaceAll(";", " ")
        // .replaceAll("~", " ")
        //  .replaceAll("\\u2191", " ")
        // .replaceAll("\\u2193", " ")
        // .replaceAll("/", " ")
        // .replaceAll("'", "")
        .replaceAll("\\p{P}", " ");
    return s;//StringUtils.stripAccents(s);
  }

  /**
   * @param phrase
   * @return
   * @see mitll.langtest.server.scoring.CheckLTS#checkLTS
   */
  //warning -- this will filter out UNKNOWNMODEL - where this matters, add it
  //back in

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_SEGMENT = false;

  /**
   * @param phrase
   * @return
   * @see #getMandarinTokens(String)
   */
  String segmentation(String phrase) {
    Map<String, String> phraseToPrefix = new HashMap<>();
    String s = longest_prefix(phrase, 0, phraseToPrefix);
    boolean failedToSegment = s.trim().isEmpty();
    if (failedToSegment) {
      if (DEBUG_SEGMENT) logger.info("couldn't segment " + phrase + " fall back to character based segmentation.");
      StringBuilder builder = new StringBuilder();

      List<Character> characters = new ArrayList<>(phrase.length());
      for (char c : phrase.toCharArray()) {
        characters.add(c);
      }

      for (int i = 0; i < characters.size(); i++) {
        Character first = characters.get(i);
        Character second = (i < characters.size() - 1) ? characters.get(i + 1) : null;
        Character third = (i < characters.size() - 2) ? characters.get(i + 2) : null;
        boolean found = false;
        if (third != null) {
          String trigram = String.valueOf(first) + second + third;
          if (inDict(trigram)) {
            if (DEBUG_SEGMENT) logger.info("match trigram " + trigram);
            builder.append(trigram).append(" ");
            i++;
            i++;
            found = true;
          }
        }

        if (!found) {
          if (second != null) {
            String bigram = String.valueOf(first) + second;
            if (inDict(bigram)) {
              if (DEBUG_SEGMENT) logger.info("match bigram " + bigram);
              builder.append(bigram).append(" ");
              i++;
            } else {
              builder.append(first).append(" ");
            }
          } else {
            builder.append(first).append(" ");
          }
        }
      }

      String result = builder.toString();
      if (DEBUG_SEGMENT) logger.info("phrase " + phrase + " = " + result);
      return result;
    } else {
      return s;
    }
//    return failedToSegment ? phrase : s;
  }

  private String longest_prefix(String phrase, int i, Map<String, String> phraseToPrefix) {
    if (i == phrase.length())
      return "";
    int endOfPrefix = phrase.length() - i;
    String prefix = phrase.substring(0, endOfPrefix);
    if (inDict(prefix.trim())) {
      if (i == 0) {
        if (DEBUG) logger.debug("longest_prefix : found '" + prefix + "' in '" + phrase + "'");
        return phrase;
      }

      String substring = phrase.substring(endOfPrefix, phrase.length());
      String memo = phraseToPrefix.get(substring);
      if (memo == null) {
        memo = longest_prefix(substring, 0, phraseToPrefix);
        phraseToPrefix.put(substring, memo);
      } else {
//        logger.info("found " + substring + " = " + memo);
      }
      String rest = memo;// longest_prefix(substring, 0,phraseToPrefix);

      if (!rest.isEmpty()) {
        String s = prefix + " " + rest;
        if (DEBUG) {
          logger.debug("longest_prefix : found '" + rest + "' in '" + phrase + "' returning " + s);
        }
        return s;
      } else {
        if (DEBUG) {
          logger.debug("longest_prefix : did not find '" + rest + "' in '" + phrase + "' from  " + endOfPrefix + " to " + phrase.length());
        }
      }
    } else {
      if (DEBUG) logger.debug("longest_prefix : dict doesn't contain " + prefix + " phrase '" + phrase + "' end " + i);
    }
    return longest_prefix(phrase, i + 1, phraseToPrefix);
  }

  private boolean inDict(String token) {
    try {
      scala.collection.immutable.List<?> apply = htkDictionary.apply(token);
      boolean b = (apply != null) && apply.nonEmpty();
//      if (DEBUG && !b) {
//        logger.debug("inDict token '" +token + "' not in " +htkDictionary.size());
//      }

      if (DEBUG && b) {
        logger.debug("inDict token '" + token + "'");
      }
      return b;
    } catch (Exception e) {
      logger.error("isDict for '" + token + "', dict " + (htkDictionary != null) + " got " + e, e);
      return false;
    }
  }
}
