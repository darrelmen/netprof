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

import mitll.langtest.shared.project.Language;
import mitll.npdata.dao.lts.HTKDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.text.CharacterIterator;
import java.text.Normalizer;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.regex.Pattern;

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
   * @see #lcToken
   * <p>
   * remove latin capital letter i with dot above - 0130
   * 0x2026 = three dot elipsis
   * FF01 = full width exclamation
   * FF1B - full semi
   * 002D - hyphen
   */
  private static final String REMOVE_ME = "[\\u0130\\u2022\\u2219\\u2191\\u2193\\u2026\\uFF01\\uFF1B\\u002D;~/']";
  private static final String REPLACE_ME_OE = "[\\u0152\\u0153]";

  /**
   * u00B7 = middle dot
   *
   * @see #segmentation
   */
  private static final String JAPANESE_PUNCT = "[\\u3001\\u3002\\uFF0C\\uFF1F\\u2019\\u2026\\u003A\\u0022\\u00B7\\uFF01\\uFF1B\\u300A]";
  private static final String OE = "oe";

  private static final char FULL_WIDTH_ZERO = '\uFF10';
  private static final char ZERO = '0';
  private static final String P_Z = "\\p{Z}+";
  /**
   * @see #getTrimmedLeaveAccents
   */
  private static final String FRENCH_PUNCT = "[,.?!]";
  private static final String TURKISH_CAP_I = "İ";
  private static final boolean WARN_ABOUT_BAD_CHINESE = false;

  private static final String P_P = "\\p{P}";
  private static final String INTERNAL_PUNCT_REGEX = "(?:(?<!\\S)\\p{Punct}+)|(?:\\p{Punct}+(?!\\S))";

  private static final String ONE_SPACE = " ";

  private HTKDictionary htkDictionary;
  private boolean isAsianLanguage;

  private static final int TOO_LONG = 8;

  private final Pattern punctCleaner;
  private final Pattern tickPattern;
  // private final Pattern englishFrenchPattern;
  private final Pattern internalPunctPattern;
  private final Pattern frenchPunct;
  private final Pattern spacepattern;
  private final Pattern oepattern;
  private final Pattern replaceMeOE;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PREFIX = false;
  private static final boolean DEBUG_SEGMENT = false;
  private static final boolean DEBUG_MANDARIN = true;
  private final Language language;

  /**
   * Compiles some handy patterns.
   *
   * @param language
   */
  public SmallVocabDecoder(Language language) {
    // and leading upside down question marks...
    tickPattern = Pattern.compile("['’\\u00bf]");  // "don't" or "mustn't"
    punctCleaner = Pattern.compile(REMOVE_ME + "|" + P_P);
    //englishFrenchPattern = Pattern.compile(REMOVE_ME + "|" + INTERNAL_PUNCT_REGEX);
    internalPunctPattern = Pattern.compile(INTERNAL_PUNCT_REGEX);
    frenchPunct = Pattern.compile(FRENCH_PUNCT);

    replaceMeOE = Pattern.compile(REPLACE_ME_OE);
    spacepattern = Pattern.compile(P_Z);
    oepattern = Pattern.compile(REPLACE_ME_OE);
    this.language = language;
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
   * @see #getTokens(String, boolean, boolean)
   */
  public String toFull(String s) {
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
    sentences.forEach(sent -> getTokens(sent, false, false).forEach(token -> {
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
    Collection<String> tokens = getTokens(longPhrase, removeAllAccents, false);
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
    return isMandarin ? getMandarinTokens(fl) : getTokens(fl, removeAllAccents, false);
  }

  /**
   * @param sentence
   * @param removeAllAccents
   * @param debug
   * @return
   * @see IPronunciationLookup#getPronunciationsFromDictOrLTS
   * @see mitll.langtest.server.audio.SLFFile#createSimpleSLFFile
   */
  public List<String> getTokens(String sentence, boolean removeAllAccents, boolean debug) {
    List<String> all = new ArrayList<>();
    if (sentence.isEmpty()) {
      logger.warn("huh? empty sentence?");
    }
    //  logger.info("getTokens initial    '" + sentence + "'");
    String trimmedSent = getTrimmedSent(sentence, removeAllAccents);

    boolean b = DEBUG || debug;
    if (b && !sentence.equalsIgnoreCase(trimmedSent)) {
      logger.info("getTokens " +
          "\n\tremoveAllAccents " + removeAllAccents + "'" +
          "\n\tbefore           '" + sentence + "'" +
          "\n\tafter trim       '" + trimmedSent + "'");
    }

    for (String untrimedToken : spacepattern.split(trimmedSent)) { // split on spaces
      String token = untrimedToken.trim();  // necessary?
      if (token.length() > 0) {
        String trim = token.trim();
        if (!trim.equalsIgnoreCase("–") &&
            !trim.equalsIgnoreCase("؟") &&
            !trim.equalsIgnoreCase("+"))
          all.add(toFull(token));
//        if (!token.equals("UNKNOWNMODEL")) {
//          logger.debug("\ttoken " + token);
//        }
      }
    }

    if (b) logger.info("getTokens " +
        "\n\tbefore     '" + sentence + "'" +
        "\n\tafter trim '" + trimmedSent + "'" +
        "\n\tall        (" + all.size() + ")" + all
    );

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
   * @see #getTokensAllLanguages
   * @see #segmentation(String)
   */
  private List<String> getMandarinTokens(String foreignLanguage) {
    String segmentation = segmentation(foreignLanguage);
//    if (DEBUG_MANDARIN) logger.info("getMandarinTokens '" + foreignLanguage + "' = '" + segmentation + "'");
    return getTokens(segmentation, false, false);
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
   * @see mitll.langtest.server.trie.ExerciseTrie#addSuffixes
   */
  public String getTrimmed(String sentence) {
    String trim = getTrimmedLeaveLastSpace(sentence).trim();
    //logger.warn("getTrimmed before " + sentence + " after "+ trim);
    return trim;
  }

  /**
   * For the moment we replace the Turkish Cap I with I
   *
   * @param sentence
   * @return
   * @see PronunciationLookup#getPronStringForWord(String, Collection, boolean)
   */
  private String getTrimmedLeaveAccents(String sentence) {
    String alt = frenchPunct.matcher(sentence).replaceAll(" ");
    alt = internalPunctPattern.matcher(alt).replaceAll("");
    alt = replaceMeOE.matcher(alt).replaceAll(OE);

    if (language == Language.TURKISH) {
      alt = alt
          //  .replaceAll(FRENCH_PUNCT, "")
          // .replaceAll(REPLACE_ME_OE, OE)
          .replaceAll(TURKISH_CAP_I, "I")
          //  .replaceAll(INTERNAL_PUNCT_REGEX, ONE_SPACE)
          //.replaceAll("\\s+", " ")
          .trim();
    } else {
      alt = alt.trim();
    }
    //logger.warn("getTrimmedLeaveAccents before " + sentence + " after "+ trim);
    return alt;
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
        getTrimmedLeaveLastSpace(token).toLowerCase() :
        getTrimmedLeaveAccents(token).toLowerCase();
  }

  /**
   * Gosh - we should put these in a table once and never recalc again.
   *
   * We want to keep accents - french accents especially...
   * They are in the dictionary.
   *
   * @param sentence
   * @return
   * @see #getTrimmed
   * @see mitll.langtest.server.trie.ExerciseTrie#getTrimmed
   */
  public String getTrimmedLeaveLastSpace(String sentence) {
/*    String s = sentence
        .replaceAll(REMOVE_ME, ONE_SPACE)
        .replaceAll(P_Z, ONE_SPACE)  // normalize all whitespace

        // .replaceAll(";", " ")
        // .replaceAll("~", " ")
        //  .replaceAll("\\u2191", " ")
        // .replaceAll("\\u2193", " ")
        // .replaceAll("/", " ")
        // .replaceAll("'", "")
        .replaceAll(P_P, ONE_SPACE)
        .replaceAll(REPLACE_ME_OE, OE);*/

    String alt = tickPattern.matcher(sentence).replaceAll("");
    alt = punctCleaner.matcher(alt).replaceAll(ONE_SPACE);

//    if (sentence.length() != alt.length()) {
//      logger.info("'" + sentence + "' = '" + alt + "'");
//    }

    alt = oepattern.matcher(alt).replaceAll(OE);
    alt = spacepattern.matcher(alt).replaceAll(ONE_SPACE);
    alt = alt.trim();

    return alt;
  }

  /**
   * @param phrase
   * @return
   * @see mitll.langtest.server.scoring.CheckLTS#checkLTS
   */
  //warning -- this will filter out UNKNOWNMODEL - where this matters, add it
  //back in

  /**
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
            if (DEBUG_SEGMENT) logger.info("segmentation match trigram " + trigram);
            builder.append(trigram).append(ONE_SPACE);
            i++;
            i++;
            found = true;
          }
        }

        if (!found) {
          if (second != null) {
            String bigram = String.valueOf(first) + second;
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

      String result = builder.toString();
      if (DEBUG_SEGMENT) logger.info("segmentation" +
          "\n\tphrase    " + phrase +
          "\n\tsegmented " + result);
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

  private boolean inDict(String token) {
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
