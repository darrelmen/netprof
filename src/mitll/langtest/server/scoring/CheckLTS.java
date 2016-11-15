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
import corpus.LTS;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.SLFFile;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/7/15.
 */
class CheckLTS {
  private static final Logger logger = Logger.getLogger(CheckLTS.class);
  private static final int WARN_LTS_COUNT = 1;

  private final LTS letterToSoundClass;
  private final String languageProperty;

  private final HTKDictionary htkDictionary;
  private final boolean isMandarin;
  private static final boolean DEBUG = false;

  /**
   * @param lts
   * @param htkDictionary
   * @param languageProperty
   * @see Scoring#Scoring(String, ServerProperties, LogAndNotify)
   */
  CheckLTS(LTS lts, HTKDictionary htkDictionary, String languageProperty, boolean hasModel) {
    this.letterToSoundClass = lts;
    this.htkDictionary = htkDictionary;
    if (htkDictionary.isEmpty() && hasModel) {
      logger.warn("\n\n\n dict is empty?");
    }
    String language = languageProperty != null ? languageProperty : "";
    this.languageProperty = language;
    isMandarin = language.equalsIgnoreCase("mandarin");
    if (isMandarin) logger.warn("using mandarin segmentation.");
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see mitll.langtest.server.scoring.Scoring#checkLTS(String)
   */
  Set<String> checkLTS(String foreignLanguagePhrase, String transliteration) {
    return checkLTS(letterToSoundClass, foreignLanguagePhrase, transliteration);
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#checkLTSAndCountPhones
   */
  public ASR.PhoneInfo getBagOfPhones(String foreignLanguagePhrase) {
    return checkLTS2(letterToSoundClass, foreignLanguagePhrase);
  }

  private int shown = 0;

  private boolean hasUsableTransliteration(Collection<String> foreignTokens, Collection<String> translitTokens){
    return (foreignTokens.size() == translitTokens.size()) || (foreignTokens.size() == 1);
  }

  /**
   * So chinese is special -- it doesn't do lts -- it just uses a dictionary
   *
   * @param lts
   * @param foreignLanguagePhrase
   * @return set of oov tokens
   * @see #checkLTS(String)
   */
  private Set<String> checkLTS(LTS lts, String foreignLanguagePhrase, String transliteration) {
    if (htkDictionary.isEmpty() && LTSFactory.isEmpty(lts)) {
      if (shown++ < WARN_LTS_COUNT) {
        logger.debug("skipping LTS since dict is empty and using the empty LTS : " + lts);
      }
      return Collections.emptySet();
    }

    SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder(htkDictionary);
    Collection<String> tokens = smallVocabDecoder.getTokens(foreignLanguagePhrase);
    Collection<String> translitTokens = smallVocabDecoder.getTokens(transliteration);
    boolean translitOk = true;
    if (hasUsableTransliteration(tokens, translitTokens)) {
      try {
          int i = 0;
          for (String translitToken : translitTokens) {
            String trim = translitToken.trim();
            String[][] process = lts.process(trim);
            //if any of the words in the transliteration fails, we won't use the transliteration
            translitOk &= (!(process == null || process.length == 0 || process[0].length == 0 ||
                    process[0][0].length() == 0 || (StringUtils.join(process[0], "-")).contains("#")));
          }
        }
      catch (Exception e){
        if (DEBUG)  logger.debug("transliteration not usable in checkLTS with lts "+lts+" and transliteration: "+transliteration);
        translitOk = false;
      }
    } else{
      translitOk = false;
    }

    String language = isMandarin ? " MANDARIN " : "";

    if (DEBUG) logger.debug("checkLTS '" + language + "' tokens : '" + tokens + "' lts " + lts + " dict size " + htkDictionary.size());

    Set<String> oov = new HashSet<>();
    Set<String> inlts = new HashSet<>();
    Set<String> indict = new HashSet<>();
    try {
      int i = 0;
      for (String token : tokens) {
        String trim = token.trim();
        if (token.equalsIgnoreCase(SLFFile.UNKNOWN_MODEL))
          return oov;
        if (isMandarin) {
          String segmentation = smallVocabDecoder.segmentation(trim);
          if (segmentation.isEmpty() && !translitOk) {
            logger.warn("checkLTSOnForeignPhrase: mandarin token : '" + token + "' invalid!");
            oov.add(trim);
          }
        } else {
          String[][] process = lts.process(token);
          if (!translitOk && (process == null || process.length == 0 || process[0].length == 0 ||
              process[0][0].length() == 0 || (process.length == 1 && process[0].length == 1 && process[0][0].equals("#")))) {
            boolean htkEntry = htkDictionary.contains(token);
            if (!htkEntry && !htkDictionary.isEmpty()) {
              if (!(lts instanceof corpus.EmptyLTS)) {
                logger.warn("checkLTS with " + lts + "/" + languageProperty + " token #" + i +
                    " : '" + token + "' hash " + token.hashCode() +
                    " is invalid in '" + foreignLanguagePhrase +
                    "' and not in dictionary of size " + htkDictionary.size()
                );
              }
              oov.add(trim);
            } else {
              indict.add(trim);
            }
          } else {
            //  logger.info("for " + token + " got " + (process.length));
//            for (String [] first : process) {
//              for (String  second : first) logger.info("\t" + second);
//            }
            inlts.add(trim);
          }
        }
        i++;
      }
    } catch (Exception e) {
      logger.error("lts " + language + "/" + lts + " failed on '" + foreignLanguagePhrase + "'", e);
      oov.add(e.getMessage());
      return oov;
    }
    if (foreignLanguagePhrase.trim().isEmpty()) {
      //logger.warn("huh fl is empty?");
    } else {
      if (DEBUG) logger.info("for " + foreignLanguagePhrase + " : inlts " + inlts + " indict " + indict);
    }
    if (DEBUG)  logger.debug("checkLTS '" + language + "' tokens : '" + tokens + "' oov " + oov + " for " + foreignLanguagePhrase + " : inlts " + inlts + " indict " + indict);

    return oov;
  }

  //private int multiple = 0;

  /**
   * Might be n1 x n2 x n3 different possible combinations of pronunciations of a phrase
   * Consider running ASR on all ref audio to get actual phone sequence.
   *
   * @param lts
   * @param foreignLanguagePhrase
   */
  //this seems to be dead code - it's called by a method that isn't so far as I can tell, called by anything else. Going to not mess with trying to get the transliteration in here
  private ASR.PhoneInfo checkLTS2(LTS lts, String foreignLanguagePhrase) {
    SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder(htkDictionary);
    Collection<String> tokens = smallVocabDecoder.getTokens(foreignLanguagePhrase);

    List<String> firstPron = new ArrayList<>();
    Set<String> uphones = new TreeSet<>();

    if (isMandarin) {
      List<String> token2 = new ArrayList<>();
      for (String token : tokens) {
        String segmentation = smallVocabDecoder.segmentation(token.trim());
        if (segmentation.isEmpty()) {
          logger.warn("no segmentation for " + foreignLanguagePhrase + " token " + token + " trying transliteration");
        } else {
          Collections.addAll(token2, segmentation.split(" "));
        }
      }
      //    logger.debug("Tokens were " + tokens + " now " + token2);
      tokens = token2;
    }

    for (String token : tokens) {
      if (token.equalsIgnoreCase(SLFFile.UNKNOWN_MODEL))
        return new ASR.PhoneInfo(firstPron, uphones);
      // either lts can handle it or the dictionary can...

      boolean htkEntry = htkDictionary.contains(token);
      if (htkEntry) {
        scala.collection.immutable.List<String[]> pronunciationList = htkDictionary.apply(token);
        //   logger.debug("token " + pronunciationList);
        scala.collection.Iterator iter = pronunciationList.iterator();
        boolean first = true;
        while (iter.hasNext()) {
          Object next = iter.next();
          //    logger.debug(next);

          String[] tt = (String[]) next;
          for (String t : tt) {
            //logger.debug(t);
            uphones.add(t);

            if (first) {
              firstPron.add(t);
            }
          }
          //    if (!first) multiple++;
          first = false;
        }
      } else {
        String[][] process = lts.process(token);
        //logger.debug("token " + token);
        if (process != null) {

          boolean first = true;
          for (String[] onePronunciation : process) {
            // each pronunciation
            //          ArrayList<String> pronunciation = new ArrayList<String>();
            //        pronunciations.add(pronunciation);
            for (String phoneme : onePronunciation) {
              //logger.debug("phoneme " +phoneme);
              //        pronunciation.add(phoneme);
              uphones.add(phoneme);

              if (first) {
                firstPron.add(phoneme);
              }
            }
            //        if (!first) multiple++;
            first = false;
          }
        }
      }
    }
    //if (multiple % 1000 == 0) logger.debug("mult " + multiple);
    return new ASR.PhoneInfo(firstPron, uphones);
  }
}