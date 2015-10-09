package mitll.langtest.server.scoring;

import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.audio.SLFFile;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by go22670 on 10/7/15.
 */
class CheckLTS {
  private static final Logger logger = Logger.getLogger(CheckLTS.class);

  private final LTS letterToSoundClass;
  private final String languageProperty;

  private final HTKDictionary htkDictionary;
  private final boolean isMandarin;
  private static final boolean DEBUG = false;

  public CheckLTS(LTS lts, HTKDictionary htkDictionary, String languageProperty) {
    this.letterToSoundClass = lts;
    this.htkDictionary = htkDictionary;
    if (htkDictionary.isEmpty()) logger.warn("\n\n\n dict is empty?");
    String language = languageProperty != null ? languageProperty : "";
    this.languageProperty = language;
    isMandarin = language.equalsIgnoreCase("mandarin");
    if (isMandarin) logger.warn("using mandarin segmentation.");
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#inDictOrLTS(String)
   */
  public Set<String> checkLTS(String foreignLanguagePhrase) {
    return checkLTS(letterToSoundClass, foreignLanguagePhrase);
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#checkLTS
   */
  public ASR.PhoneInfo getBagOfPhones(String foreignLanguagePhrase) {
    return checkLTS2(letterToSoundClass, foreignLanguagePhrase);
  }

  /**
   * So chinese is special -- it doesn't do lts -- it just uses a dictionary
   *
   * @param lts
   * @param foreignLanguagePhrase
   * @return set of oov tokens
   * @see #checkLTS(String)
   */
  private Set<String> checkLTS(LTS lts, String foreignLanguagePhrase) {
    SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder(htkDictionary);
    Collection<String> tokens = smallVocabDecoder.getTokens(foreignLanguagePhrase);

    String language = isMandarin ? " MANDARIN " : "";
    //logger.debug("checkLTS '" + language + "' tokens : '" +tokens +"'");
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
          if (segmentation.isEmpty()) {
            logger.warn("checkLTS: mandarin token : '" + token + "' invalid!");
            oov.add(trim);
          }
        } else {
          String[][] process = lts.process(token);
          if (process == null || process.length == 0 || process[0].length == 0 ||
              process[0][0].length() == 0 || (process.length == 1 && process[0].length == 1 && process[0][0].equals("aa"))) {
            boolean htkEntry = htkDictionary.contains(token);
            if (!htkEntry && !htkDictionary.isEmpty()) {
              logger.warn("checkLTS with " + lts + "/" + languageProperty + " token #" + i +
                      " : '" + token + "' hash " + token.hashCode() +
                      " is invalid in '" + foreignLanguagePhrase +
                      "' and not in dictionary of size " + htkDictionary.size()
              );
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
  private ASR.PhoneInfo checkLTS2(LTS lts, String foreignLanguagePhrase) {
    SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder(htkDictionary);
    Collection<String> tokens = smallVocabDecoder.getTokens(foreignLanguagePhrase);

    List<String> firstPron = new ArrayList<String>();
    Set<String> uphones = new TreeSet<String>();

    if (isMandarin) {
      List<String> token2 = new ArrayList<String>();
      for (String token : tokens) {
        String segmentation = smallVocabDecoder.segmentation(token.trim());
        if (segmentation.isEmpty()) {
          logger.warn("no segmentation for " + foreignLanguagePhrase + " token " + token);
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