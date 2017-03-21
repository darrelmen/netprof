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
  private HTKDictionary htkDictionary;

  public SmallVocabDecoder() {
  }

  /**
   * @param htkDictionary
   * @see ASRScoring#makeDecoder()
   */
  public SmallVocabDecoder(HTKDictionary htkDictionary) {
    this.htkDictionary = htkDictionary;
  }

  /**
   * Get the vocabulary to use when generating a language model. <br></br>
   * Very important to limit the vocabulary (less than 300 words) or else the small vocab dcodr will run out of
   * memory and segfault! <br></br>
   * Remember to add special tokens like silence, pause, and unk
   *
   * @param background sentences
   * @return most frequent vocabulary words
   * @see ASRScoring#getUsedTokens
   */
  List<String> getVocab(Collection<String> background, int vocabSizeLimit) {
    return getSimpleVocab(background, vocabSizeLimit);
  }

  /**
   * @param s
   * @return
   * @see #getTokens(String)
   */
  String toFull(String s) {
    StringBuilder builder = new StringBuilder();

    char fullWidthZero = '\uFF10';

    final CharacterIterator it = new StringCharacterIterator(s);
    for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
      if (c >= '0' && c <= '9') {
        int offset = c - '0';
        int full = fullWidthZero + offset;
        Character character = Character.valueOf((char) full);
        builder.append(character.toString());
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
   * @see ASRScoring#getUniqueTokensInLM
   */
  List<String> getSimpleVocab(Collection<String> sentences, int vocabSizeLimit) {
    // childCount the tokens
    final Map<String, Integer> sc = new HashMap<String, Integer>();
    for (String sentence : sentences) {
      for (String token : getTokens(sentence)) {
        //  if (isValid(scoring, token)) {
        Integer c = sc.get(token);
        sc.put(token, (c == null) ? 1 : c + 1);
     /*   } else {
          logger.warn("getSimpleVocab : skipping '" + token + "' which is not in dictionary.");
        }*/
      }
    }

    // sort by frequency
    List<String> vocab = new ArrayList<String>(sc.keySet());
    Collections.sort(vocab, new Comparator<String>() {
      public int compare(String s, String s2) {
        Integer first = sc.get(s);
        Integer second = sc.get(s2);
        return first < second ? +1 : first > second ? -1 : 0;
      }
    });

    // take top n most frequent
    List<String> all = new ArrayList<String>(); // copy list b/c sublist not serializable ???
    if (vocab.size() > vocabSizeLimit)
      logger.warn("truncating vocab size from " + vocab.size() + " to " + vocabSizeLimit);
    all.addAll(vocab.subList(0, Math.min(vocab.size(), vocabSizeLimit)));
    return all;
  }

  /**
   * @param sentence
   * @return
   * @see Scoring#getSegmented(String)
   * @see mitll.langtest.server.audio.SLFFile#createSimpleSLFFile(Collection, boolean, boolean)
   */
  public List<String> getTokens(String sentence) {
    List<String> all = new ArrayList<String>();
    // logger.debug("initial " + sentence);
    String trimmedSent = getTrimmed(sentence);
    // logger.debug("after  trim " + trimmedSent);

    for (String untrimedToken : trimmedSent.split("\\p{Z}+")) { // split on spaces
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

  public Collection<String> getMandarinTokens(String foreignLanguage) {
    return getTokens(segmentation(foreignLanguage));
  }

  /**
   * Tries to remove junky characters from the sentence so hydec won't choke on them.
   * <p>
   * Also removes the chinese unicode bullet character, like in Bill Gates.
   *
   * @param sentence
   * @return
   * @see #getTokens(String)
   * @see mitll.langtest.server.trie.ExerciseTrie#getExercises(String)
   * @see ASRScoring#getScoresFromHydec
   */
  public String getTrimmed(String sentence) {
    //String orig = sentence;
    sentence = sentence.replaceAll("\\u2022", " ").replaceAll("\\u2219", " ").replaceAll("\\p{Z}+", " ").replaceAll(";", " ").replaceAll("~", " ").replaceAll("\\u2191", " ").replaceAll("\\u2193", " ").replaceAll("/", " ");
    // logger.debug("after  convert " + sentence);

    String trim = sentence.replaceAll("'", "").replaceAll("\\p{P}", " ").replaceAll("\\s+", " ").trim();
/*
    logger.debug("from '" +sentence+
        "' => '" +trim+
        "'");
*/

    return trim;
  }

  /**
   * @param phrase
   * @return
   * @see mitll.langtest.server.scoring.CheckLTS#checkLTS
   */
  //warning -- this will filter out UNKNOWNMODEL - where this matters, add it
  //back in
  String segmentation(String phrase) {
    String s = longest_prefix(phrase, 0);
    if (s.trim().isEmpty()) {
      return phrase;
    } else {
      return s;
    }
  }

  private String longest_prefix(String phrase, int i) {
    if (i == phrase.length())
      return "";
    String prefix = phrase.substring(0, phrase.length() - i);
    if (inDict(prefix)) {
      if (i == 0)
        return phrase;
      String rest = longest_prefix(phrase.substring(phrase.length() - i, phrase.length()), 0);
      if (rest.length() > 0)
        return prefix + " " + rest;
    }
    //else {
    //logger.debug("dict doesn't contain " + prefix);
    //}
    return longest_prefix(phrase, i + 1);
  }

  private boolean inDict(String token) {
    try {
      scala.collection.immutable.List<?> apply = htkDictionary.apply(token);
      boolean b = (apply != null) && apply.nonEmpty();
      return b;
    } catch (Exception e) {
      logger.error("isDict - " + token + " got " + e);
      return false;
    }
  }
}
