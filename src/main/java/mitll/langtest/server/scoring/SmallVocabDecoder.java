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

package mitll.langtest.server.scoring;

import mitll.langtest.shared.project.Language;
import mitll.npdata.dao.lts.HTKDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;
import java.util.*;

public class SmallVocabDecoder extends TextNormalizer {
  private static final Logger logger = LogManager.getLogger(SmallVocabDecoder.class);

  /**
   * u00B7 = middle dot
   *
   * @see #segmentation
   */
  private static final String JAPANESE_PUNCT = "[\\u3001\\u3002\\uFF0C\\uFF1F\\u2019\\u2026\\u003A\\u0022\\u00B7\\uFF01\\uFF1B\\u300A]";

  private static final String P_Z = "\\p{Z}+";
  /**
   * @see #getTrimmedLeaveAccents
   */
  private static final boolean WARN_ABOUT_BAD_CHINESE = false;
  public static final char HIGHEST_ASCII = 'z';

  private HTKDictionary htkDictionary;
  private boolean isAsianLanguage;

  private static final int TOO_LONG = 8;

  private final boolean removeAllAccents;

  private static final boolean DEBUG_PREFIX = false;
  private static final boolean DEBUG_SEGMENT = false;

  /**
   * Compiles some handy patterns.
   *
   * @param language
   */
  public SmallVocabDecoder(Language language) {
    super(language.name());
    removeAllAccents =
        language != Language.FRENCH &&
            language != Language.TURKISH &&
            language != Language.CROATIAN &&
            language != Language.SERBIAN;
  }

  /**
   * @param htkDictionary
   * @param isAsianLanguage
   * @param language
   * @see PronunciationLookup#makeDecoder
   */
  public SmallVocabDecoder(HTKDictionary htkDictionary, boolean isAsianLanguage, Language language) {
    this(language);
    this.htkDictionary = htkDictionary;
    this.isAsianLanguage = isAsianLanguage;

//    logger.info("SmallVocabDecoder dict now " + ((htkDictionary != null) ? htkDictionary.size() : " null dict") + " asian " + isAsianLanguage);
  }

  public List<String> getTokens(String sentence) {
    return getTokens(sentence, removeAllAccents);
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
    tokens.forEach(token -> {
      String trim = token.trim();
      char c = trim.charAt(0);
      String segmentation = trim;

      if (c >= 'A' && c <= 'ž') { // so skip it. it's pinyin
        if (WARN_ABOUT_BAD_CHINESE) logger.info("getSegmented token is not chinese '" + trim + "'");
      } else {
        segmentation = segmentation(trim);
      }
      builder.append(segmentation).append(ONE_SPACE);
    });
    return builder.toString();
  }

  /**
   * @param isMandarin
   * @param fl
   * @param removeAllAccents
   * @return
   * @see mitll.langtest.server.autocrt.DecodeCorrectnessChecker#isCorrect(Collection, String, boolean, boolean)
   */
  public List<String> getTokensAllLanguages(boolean isMandarin, String fl, boolean removeAllAccents) {
    return isMandarin ? getMandarinTokens(fl) : getTokens(fl, removeAllAccents);
  }

  /**
   * @param foreignLanguage
   * @return
   * @see #getTokensAllLanguages
   * @see #segmentation(String)
   */
  private List<String> getMandarinTokens(String foreignLanguage) {
    String segmentation = segmentation(foreignLanguage);
//    if (DEBUG_MANDARIN) logger.info("getMandarinTokens '" + foreignLanguage + "' = '" + segmentation + "'");
    return getTokens(segmentation, false);
  }

  /**
   * @param text
   * @return
   * @see PronunciationLookup#addDictMatch
   */
  String removeAccents(String text) {
    return text == null ? null :
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }

  /**
   * @param token
   * @param removeAllPunct
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#getHydraDict
   */
  public String lcToken(String token, boolean removeAllPunct) {
    return removeAllPunct ?
        getTrimmed(token).toLowerCase() :
        getTrimmedLeaveAccents(token).toLowerCase();
  }

  /**
   * @param phrase
   * @return
   * @see mitll.langtest.server.scoring.CheckLTS#checkLTS
   */
  //warning -- this will filter out UNKNOWNMODEL - where this matters, add it
  //back in

  /**
   * check to see if all characters in a token are ASCII and if so doesn't attempt to split them into characters.
   * @param phrase
   * @return
   * @see CheckLTS#checkLTS
   * @see CheckLTS#checkLTS2
   * @see #getMandarinTokens(String)
   */
  String segmentation(String phrase) {
    Map<String, String> phraseToPrefix = new HashMap<>();

    phrase = phrase.replaceAll(JAPANESE_PUNCT, "");
    String s = longest_prefix(phrase, 0, phraseToPrefix);
    boolean failedToSegment = s.trim().isEmpty();
    if (failedToSegment) {
      if (DEBUG_SEGMENT) {
        logger.info("segmentation couldn't segment '" + phrase + "' fall back to character based segmentation.");
      }

      StringBuilder builder = new StringBuilder();

      List<Character> characters = new ArrayList<>(phrase.length());
      boolean allASCII = true;
      for (char c : phrase.toCharArray()) {
        characters.add(c);
        if (c > HIGHEST_ASCII) allASCII = false;
      }

      if (allASCII) {
        //logger.info("segmentation : is all ascii " + phrase);
        builder.append(phrase).append(ONE_SPACE);
      } else {
        //logger.info("segmentation : *not* all ascii '" + phrase +"'");

        for (int i = 0; i < characters.size(); i++) {
          Character first = characters.get(i);
          Character second = (i < characters.size() - 1) ? characters.get(i + 1) : null;
          Character third = (i < characters.size() - 2) ? characters.get(i + 2) : null;
          boolean found = false;
          String s1 = String.valueOf(first);
          if (third != null) {
            String trigram = s1 + second + third;
            if (inDict(trigram)) {
              if (DEBUG_SEGMENT) logger.info("segmentation match trigram " + trigram);
              builder.append(trigram).append(ONE_SPACE);
              i++;
              i++;
              found = true;
            }
          }

          if (!found) {
            if (second != null) {
              String bigram = s1 + second;
              if (inDict(bigram)) {
                if (DEBUG_SEGMENT) logger.info("segmentation match bigram " + bigram);
                builder.append(bigram).append(ONE_SPACE);
                i++;
              } else {
                builder.append(first).append(ONE_SPACE);
              }
            } else {
              builder.append(first).append(ONE_SPACE);
            }
          }
        }
      }
      String result = builder.toString();
      if (DEBUG_SEGMENT) logger.info("segmentation" +
          "\n\tphrase    " + phrase +
          "\n\tsegmented " + result);
      return result;
    } else {
//      if (DEBUG_SEGMENT || true) logger.info("segmentation : " +
//          "\n\tphrase    " + phrase +
//          "\n\tsegmented " + s);
      return s;
    }
  }

  private String longest_prefix(String phrase, int i, Map<String, String> phraseToPrefix) {
    if (i == phrase.length())
      return "";
    int endOfPrefix = phrase.length() - i;
    String prefix = phrase.substring(0, endOfPrefix);
    String trim = prefix.trim();
    boolean prefixTooLong = false;
    if (isAsianLanguage) {
      prefixTooLong = prefix.length() > TOO_LONG;
      if (DEBUG_PREFIX && prefixTooLong) {
        logger.warn("longest_prefix not looking in dict for long asian prefix " + prefix);
      }
    }

    if (!prefixTooLong && inDict(trim)) {
      if (i == 0) {
        if (DEBUG_PREFIX) logger.debug("longest_prefix : found '" + prefix + "' in '" + phrase + "'");
        return phrase;
      }

      String substring = phrase.substring(endOfPrefix, phrase.length());
      String memo = phraseToPrefix.get(substring);
      if (memo == null) {
        memo = longest_prefix(substring, 0, phraseToPrefix);
        phraseToPrefix.put(substring, memo);
      }
      //else {
//        logger.info("found " + substring + " = " + memo);
      // }
      String rest = memo;

      if (!rest.isEmpty()) {
        String s = prefix + ONE_SPACE + rest;
        if (DEBUG_PREFIX) {
          logger.debug("longest_prefix : found '" + rest + "' in '" + phrase + "' returning " + s);
        }
        return s;
      } else {
        if (DEBUG_PREFIX) {
          logger.debug("longest_prefix : did not find '" + rest + "' in '" + phrase + "' from  " + endOfPrefix + " to " + phrase.length());
        }
      }
    } else {
      if (DEBUG_PREFIX)
        logger.debug("longest_prefix : dict doesn't contain " + prefix + " phrase '" + phrase + "' end " + i);
    }
    return longest_prefix(phrase, i + 1, phraseToPrefix);
  }

  /**
   * @param token
   * @return
   * @see #segmentation(String)
   * @see #longest_prefix(String, int, Map)
   */
  boolean inDict(String token) {
    try {
      scala.collection.immutable.List<?> apply = htkDictionary.apply(token);
      boolean b = (apply != null) && apply.nonEmpty();
//      if (DEBUG && !b) {
//        logger.debug("inDict token '" +token + "' not in " +htkDictionary.size());
//      }

      if (DEBUG_PREFIX && b) {
        logger.debug("---> inDict '" + token + "'");
      }
      return b;
    } catch (Exception e) {
      logger.error("isDict for '" + token + "', dict " + (htkDictionary != null) + " got " + e, e);
      return false;
    }
  }
}
