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
import mitll.npdata.dao.lts.LTS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static mitll.langtest.server.scoring.ASRWebserviceScoring.MAX_FROM_ANY_TOKEN;

class CheckLTS {
  private static final Logger logger = LogManager.getLogger(CheckLTS.class);
  private static final int WARN_LTS_COUNT = 1;

  private final LTS letterToSoundClass;
  // private final String language;

  private final HTKDictionary htkDictionary;
  private final SmallVocabDecoder smallVocabDecoder;

  private final boolean isAsianLanguage, removeAllAccents;
  private final Language languageInfo;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_OOV = false;
  private static final String POUND = "#";

  /**
   * @param lts
   * @param htkDictionary
   * @param isAsianLanguage
   * @see Scoring#Scoring
   */
  CheckLTS(LTS lts, HTKDictionary htkDictionary, Language languageInfo, boolean hasModel, boolean isAsianLanguage) {
    this.letterToSoundClass = lts;
    this.htkDictionary = htkDictionary;
    this.languageInfo = languageInfo;
    if (htkDictionary == null || (htkDictionary.isEmpty() && hasModel)) {
      logger.warn("CheckLTS : dict is empty? lts = " + lts);
    }
    smallVocabDecoder = new SmallVocabDecoder(htkDictionary, isAsianLanguage, languageInfo);
    //  this.language = languageInfo != null ? languageInfo.getLanguage() : "";
    this.isAsianLanguage = isAsianLanguage;
    removeAllAccents =
        languageInfo != Language.FRENCH &&
            languageInfo != Language.TURKISH &&
            languageInfo != Language.CROATIAN &&
            languageInfo != Language.SERBIAN
    ;

//    logger.info("lang " + languageProperty  + " asian " + isAsianLanguage);
//    if (isAsianLanguage) logger.warn("using mandarin segmentation.");
  }

  private boolean areAllPhonesValid(String[] pron) {
    boolean allValid = true;
    for (String p : pron) {
      if (checkInvalidPhone(p)) allValid = false;
    }
    return allValid;
  }

  private boolean checkInvalidPhone(String p) {
    return p.equalsIgnoreCase(POUND);
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see Scoring#validLTS(String, String)
   */
  Set<String> checkLTS(String foreignLanguagePhrase, String transliteration) {
    return checkLTS(letterToSoundClass, foreignLanguagePhrase, transliteration, removeAllAccents);
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see Scoring#getBagOfPhones
   */
  PhoneInfo getBagOfPhones(String foreignLanguagePhrase) {
    return checkLTS2(letterToSoundClass, foreignLanguagePhrase);
  }

  private int shown = 0;

  private boolean hasUsableTransliteration(Collection<String> foreignTokens, Collection<String> translitTokens) {
    return (foreignTokens.size() == translitTokens.size()) || (foreignTokens.size() == 1);
  }

  int spew =0;
  /**
   * So chinese is special -- it doesn't do lts -- it just uses a dictionary
   *
   * @param lts
   * @param foreignLanguagePhrase
   * @param removeAllAccents
   * @return set of oov tokens
   * @see #checkLTS(String, String)
   */
  private Set<String> checkLTS(LTS lts, String foreignLanguagePhrase, String transliteration, boolean removeAllAccents) {
    boolean isEmptyLTS = LTSFactory.isEmpty(lts);
    if (isDictEmpty() && isEmptyLTS) {
      if (shown++ < WARN_LTS_COUNT) {
        logger.debug("checkLTS skipping LTS since dict is empty and using the empty LTS : " + lts);
      }
      return Collections.emptySet();
    }

//    if (removeAllAccents) logger.info("checkLTS " +foreignLanguagePhrase);

    Collection<String> tokens = smallVocabDecoder.getTokens(foreignLanguagePhrase, removeAllAccents, false);
    Collection<String> translitTokens = transliteration.isEmpty() ? Collections.emptyList() : smallVocabDecoder.getTokens(transliteration, removeAllAccents, false);

    boolean translitOk = isTranslitOk(lts, transliteration, tokens, translitTokens);

    //   String language = isAsianLanguage ? " MANDARIN " : "";

    if (DEBUG) {
      logger.info("checkLTS '" + languageInfo + "'" +
          "\n\tfl          " + foreignLanguagePhrase +
          "\n\ttokens      " + tokens
      );
    }

    Set<String> oov = new HashSet<>();
    Set<String> inlts = new HashSet<>();
    Set<String> indict = new HashSet<>();

    Map<String, String[][]> tokenToLTSResult = new HashMap<>();
    try {
      int i = 0;
      for (String token : tokens) {
        String trim = token.trim();
        if (token.equalsIgnoreCase(ASR.UNKNOWN_MODEL))
          return oov;

        if (isAsianLanguage) {
          String segmentation = smallVocabDecoder.segmentation(trim);
          if (segmentation.isEmpty() && !translitOk) {
            logger.warn("checkLTSOnForeignPhrase: mandarin token : '" + token + "' invalid!");
            oov.add(trim);
          }
        } else {
          boolean htkEntry = htkDictionary.contains(token) || htkDictionary.contains(token.toLowerCase());
          if (DEBUG && !htkEntry)
            logger.info("checkLTS NOT IN DICT for '" + token + "' and not '" + token.toLowerCase() + "'");

          if (!htkEntry && isSpanish() && token.equalsIgnoreCase("Sί")) {
            htkEntry = htkDictionary.contains("si");
          }

          if (htkEntry) {
            // we don't accept tokens when the dict is empty...
            indict.add(trim);
          } else {
            String[][] process = null;

            if (!isEmptyLTS) {
              String[][] cached = tokenToLTSResult.get(token);
              if (cached == null) {
                tokenToLTSResult.put(token, cached = lts.process(token));
              }
              process = cached;
            }

            if (DEBUG) {
              if (process != null) {
                if (process.length > 0) {
                  logger.info("in dict for " + process[0].length);
                  if (process[0].length > 0) {
                    logger.info("in dict for " + process[0][0].length());
                  }
                }
              }
            }

            // so we've checked with LTS - why first ?
            // but if it's not in LTS, check in dict
            boolean legitLTS = isLegitLTS(process, token);
            if (!legitLTS) { // deal with upper case better
              process = (!isEmptyLTS) ? lts.process(token.toLowerCase()) : null;
              legitLTS = isLegitLTS(process, token);
            }

            if (!translitOk && !legitLTS) {
              if (!isEmptyLTS) {
                logger.warn(getDebugInfo(lts, foreignLanguagePhrase, i, token) + " translitOk " + translitOk + " legitLTS " + legitLTS);
//                if (spew++<5) {
//                  logger.info(getDebugInfo(lts, foreignLanguagePhrase, i, token) + " translitOk " + translitOk + " legitLTS " + legitLTS, new Exception("why?"));
//                }
              } else if (DEBUG) {
                logger.info(getDebugInfo(lts, foreignLanguagePhrase, i, token));
              }

              oov.add(trim);
            } else if (!isEmptyLTS && legitLTS) {
              if (DEBUG && process != null) {
                logger.info("checkLTS for " + token + " got " + (process.length));
                for (String[] first : process) {
                  for (String second : first) logger.info("\t" + second);
                }
              }
              inlts.add(trim);
            } else {
              if (DEBUG) logger.info("checkLTS oov for " + token + " = " + trim);
              oov.add(trim);
            }
          }
        }
        i++;
      }
    } catch (Exception e) {
      logger.error("lts " + languageInfo + "/" + lts + " failed on '" + foreignLanguagePhrase + "'", e);
      oov.add(e.getMessage());
      return oov;
    }
    if (foreignLanguagePhrase.trim().isEmpty()) {
      //logger.warn("huh fl is empty?");
    } else {
      if (DEBUG)
        logger.info("checkLTS : for phrase '" + foreignLanguagePhrase + "' : inlts " + inlts + " indict " + indict);
    }
    if (DEBUG || (DEBUG_OOV && !oov.isEmpty()))
      logger.info("checkLTS '" + languageInfo + "' tokens : '" + tokens + "' oov " + oov.size() + " : " + oov +
          " for " + foreignLanguagePhrase + " : inlts " + inlts + " indict " + indict);

    return oov;
  }

  private boolean isSpanish() {
    return languageInfo == Language.SPANISH;
  }

  private String getDebugInfo(LTS lts, String foreignLanguagePhrase, int i, String token) {
    return "checkLTS with " + lts + "/" + languageInfo + " token #" + i +
        " : '" + token + "' hash " + token.hashCode() +
        " is invalid in '" + foreignLanguagePhrase +
        "' and not in dictionary of size " + htkDictionary.size();
  }

  /**
   * @return
   * @see Scoring#isDictEmpty
   */
  boolean isDictEmpty() {
    return htkDictionary == null || htkDictionary.isEmpty();
  }

  private boolean isTranslitOk(LTS lts, String transliteration, Collection<String> tokens, Collection<String> translitTokens) {
    boolean isEmptyLTS = LTSFactory.isEmpty(lts);
    boolean translitOk = true;
    if (hasUsableTransliteration(tokens, translitTokens) && !isEmptyLTS) {
      try {
        int i = 0;
        for (String translitToken : translitTokens) {
          String trim = translitToken.trim();
          String[][] process = lts.process(trim);
          //if any of the words in the transliteration fails, we won't use the transliteration
          translitOk &= isLegitLTS(process, trim);
        }
      } catch (Exception e) {
        if (DEBUG) {
          logger.debug("checkLTS transliteration not usable in checkLTS with lts " + lts + " and transliteration: " + transliteration);
        }
        translitOk = false;
      }
    } else {
      translitOk = false;
    }
    return translitOk;
  }

  private String getCons(String[] tokens) {
    return String.join("-", tokens);
  }

  private boolean isLegitLTS(String[][] process, String token) {
    boolean b = !(process == null ||
        process.length == 0 ||
        process[0].length == 0 ||
        process[0][0].length() == 0 ||
        (getCons(process[0])).contains("#"));

    boolean valid = true;
    if (b) {
      int max = MAX_FROM_ANY_TOKEN;
      int n = process.length;
      int c = 0;
//      logger.warn("isLegitLTS " + n + " for " + token);

      int numBad = 0;
      for (String[] pron : process) {
        if (max-- == 0) break;
        boolean allValid = areAllPhonesValid(pron);
        if (!allValid) {
          if (DEBUG) logger.warn("isLegitLTS bad  " + c + "/" + n + " for " + token);
          numBad++;
        } else {
          // logger.info("good " + c + "/" + n + " for " + token);
        }
        c++;
        //  valid &= allValid;
      }
      if (numBad == n) {
        valid = false;
        //logger.info("isLegitLTS all bad " + numBad + " out of " + n + " for " + token);
      } else if (numBad > 0) {
        if (DEBUG) logger.info("isLegitLTS not all bad " + numBad + " out of " + n + " for " + token);
      }
    }
    return b && valid;
  }

  /**
   * Might be n1 x n2 x n3 different possible combinations of pronunciations of a phrase
   * Consider running ASR on all ref audio to get actual phone sequence.
   *
   * @param lts
   * @param foreignLanguagePhrase
   */
  //this seems to be dead code - it's called by a method that isn't so far as I can tell, called by anything else. Going to not mess with trying to get the transliteration in here
  private PhoneInfo checkLTS2(LTS lts, String foreignLanguagePhrase) {
    //   logger.info("lang  " + language + " is asian " + isAsianLanguage);
    SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder(htkDictionary, isAsianLanguage, languageInfo);
    Collection<String> tokens = smallVocabDecoder.getTokens(foreignLanguagePhrase, languageInfo != Language.FRENCH, false);

    List<String> firstPron = new ArrayList<>();
    Set<String> uphones = new TreeSet<>();

    if (isAsianLanguage) {
      List<String> token2 = new ArrayList<>();
      for (String token : tokens) {
        String segmentation = smallVocabDecoder.segmentation(token.trim());
        if (segmentation.isEmpty()) {
          logger.warn("checkLTS2 : no segmentation for " + foreignLanguagePhrase + " token " + token + " trying transliteration");
        } else {
          Collections.addAll(token2, segmentation.split(" "));
        }
      }
      //    logger.debug("Tokens were " + tokens + " now " + token2);
      tokens = token2;
    }

    for (String token : tokens) {
      if (token.equalsIgnoreCase(ASR.UNKNOWN_MODEL))
        return new PhoneInfo(firstPron, uphones);
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
    return new PhoneInfo(firstPron, uphones);
  }
}