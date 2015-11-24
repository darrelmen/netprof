/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.scoring;

import corpus.HTKDictionary;
import org.apache.log4j.Logger;
import pronz.speech.Audio;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;

/**
 * Creates the LM for a small vocab decoder from foreground and background sentences.
 *
 * User: GO22670
 * Date: 2/7/13
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class SmallVocabDecoder {
  private static final Logger logger = Logger.getLogger(SmallVocabDecoder.class);
  private HTKDictionary htkDictionary;

  public SmallVocabDecoder() {}

  /**
   *
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
   * @see ASRScoring#getUsedTokens
   * @param background sentences
   * @return most frequent vocabulary words
   */
  public List<String> getVocab(Collection<String> background, int vocabSizeLimit) {
    return getSimpleVocab(background, vocabSizeLimit);
  }

  /**
   * @see ASRScoring#getValidSentences(Collection)
   * @param s
   * @return
   */
  private String toFull(String s) {
    StringBuilder builder = new StringBuilder();

    char fullWidthZero = '\uFF10';

    final CharacterIterator it = new StringCharacterIterator(s);
    for(char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
      if (c >= '0' && c <= '9') {
        int offset = c-'0';
        int full = fullWidthZero + offset;
        Character character = Character.valueOf((char)full);
        builder.append(character.toString());
      }
      else {
        builder.append(c);
      }
    }
    return builder.toString();
  }

  /**
   * @see ASRScoring#getUniqueTokensInLM
   * @param sentences
   * @param vocabSizeLimit
   * @return
   */
  public List<String> getSimpleVocab(Collection<String> sentences, int vocabSizeLimit) {
    // count the tokens
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
    if (vocab.size() > vocabSizeLimit) logger.warn("truncating vocab size from " + vocab.size() + " to " + vocabSizeLimit);
    all.addAll(vocab.subList(0, Math.min(vocab.size(), vocabSizeLimit)));
    return all;
  }

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

  public Collection<String> getMandarinTokens( String foreignLanguage) {
  //  String foreignLanguage = e.getForeignLanguage();
    return getTokens(segmentation(foreignLanguage));
  }

  /**
   * Tries to remove junky characters from the sentence so hydec won't choke on them.
   *
   * @param sentence
   * @return
   * @see #getTokens(String)
   * @see mitll.langtest.server.ExerciseTrie#getExercises(String, SmallVocabDecoder)
   * @see ASRScoring#getScoresFromHydec(Audio, String, String)
   */
  public String getTrimmed(String sentence) {
    //String orig = sentence;
    sentence = sentence.replaceAll("\\u2022", " ").replaceAll("\\p{Z}+", " ").replaceAll(";", " ").replaceAll("~", " ").replaceAll("\\u2191", " ").replaceAll("\\u2193", " ").replaceAll("/", " ");
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
   * @see mitll.langtest.server.scoring.ASRScoring#checkLTS(corpus.LTS, String)
   * @see mitll.langtest.server.ExerciseTrie#getFLTokens(SmallVocabDecoder, CommonExercise)
   */
  //warning -- this will filter out UNKNOWNMODEL - where this matters, add it 
  //back in
  public String segmentation(String phrase) {
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
