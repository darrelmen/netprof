package mitll.langtest.server.scoring;

import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.exercise.Project;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PronunciationLookup implements IPronunciationLookup {
  private static final Logger logger = LogManager.getLogger(PronunciationLookup.class);

  private static final int FOREGROUND_VOCAB_LIMIT = 100;
  private static final int VOCAB_SIZE_LIMIT = 200;
  private static final String UNK = "+UNK+";
  private static final String SEMI = ";";
  public static final String SIL = "sil";
  private static final int MAX_FROM_ANY_TOKEN = 10;
  private static final String POUND = "#";

  private SmallVocabDecoder svDecoderHelper = null;
  private final HTKDictionary htkDictionary;
  private final LTS lts;
  private boolean korean;

  /**
   * @param dictionary
   * @param lts
   * @param project
   */
  PronunciationLookup(HTKDictionary dictionary, LTS lts, Project project) {
    this.htkDictionary = dictionary;
    this.lts = lts;
    korean = project.getLanguage().equalsIgnoreCase("korean");
    makeDecoder();
  }

  private void makeDecoder() {
    if (svDecoderHelper == null && htkDictionary != null) {
      svDecoderHelper = new SmallVocabDecoder(htkDictionary);
    }
  }

  /**
   * TODO : Some phrases seem to break lts process?
   * This will work for both align and decode modes, although align will ignore the unknownmodel.
   * <p>
   * Create a dcodr input string dictionary, a sequence of words and their phone sequence:
   * e.g. [distra?do,d i s t rf a i d o sp;UNKNOWNMODEL,+UNK+;<s>,sil;</s>,sil]
   *
   * @param transcript
   * @return the dictionary for dcodr
   * @see ASRWebserviceScoring#runHydra
   */
  // @Override
  @Override
  public String createHydraDict(String transcript, String transliteration) {
    if (lts == null) {
      logger.warn(this + " : createHydraDict : LTS is null???");
    }

    String dict = "[";
    dict += getPronunciationsFromDictOrLTS(transcript, transliteration, false);
    dict += "UNKNOWNMODEL,+UNK+;<s>,sil;</s>,sil;SIL,sil";
    dict += "]";
    return dict;
  }

  private final Set<String> seen = new HashSet<>();

  /**
   * TODO : Why this method here too? Duplicate?
   * TODO : add failure case back in
   *
   * @param transcript
   * @param transliteration
   * @return
   * @see AudioFileHelper#getNumPhonesFromDictionary
   */
  @Override
  public int getNumPhonesFromDictionaryOrLTS(String transcript, String transliteration) {
    //  String[] translitTokens = transliteration.toLowerCase().split(" ");
    String[] transcriptTokens = transcript.split(" ");
    //  boolean canUseTransliteration = (transliteration.trim().length() > 0) && ((transcriptTokens.length == translitTokens.length) || (transcriptTokens.length == 1));

    int total = 0;
    for (String word : transcriptTokens) {
      boolean easyMatch;
      if ((easyMatch = htkDictionary.contains(word)) || (htkDictionary.contains(word.toLowerCase()))) {
        scala.collection.immutable.List<String[]> prons = htkDictionary.apply(easyMatch ? word : word.toLowerCase());

        int numForThisWord = 0;
        for (int i = 0; i < prons.size(); i++) {
          String[] apply = prons.apply(i);
          numForThisWord = Math.max(numForThisWord, apply.length);
/*
          StringBuilder builder = new StringBuilder();
          for (String s:apply) builder.append(s).append("-");
          logger.info("\t" +word + " = " + builder);
*/
        }
        total += numForThisWord;

//        logger.info(transcript + " token "+ word + " num " + numForThisWord + " total " + total);
      } else {
        String word1 = word.toLowerCase();
        String[][] process = getLTS().process(word1);

        if (ltsOutputOk(process)) {
          int max = 0;
          for (String[] pc : process) {
//            int c = 0;
//            for (String p : pc) {
//              if (!p.contains("#")) c++;
//            }
            max = Math.max(max, pc.length);
          }
          total += max;
        }
      }
    }
    return total;
  }

  /**
   * @param transcript
   * @param transliteration
   * @param justPhones      true if just want the phones
   * @return
   * @see #createHydraDict
   * @see mitll.langtest.server.audio.AudioFileHelper#getPronunciationsFromDictOrLTS
   */
  @Override
  public String getPronunciationsFromDictOrLTS(String transcript, String transliteration, boolean justPhones) {
    StringBuilder dict = new StringBuilder();
    String[] translitTokens = transliteration.toLowerCase().split(" ");

    List<String> transcriptTokens = svDecoderHelper.getTokens(transcript);

    {
      StringBuilder builder = new StringBuilder();
      transcriptTokens.forEach(token -> builder.append(token).append(" "));
      logger.info("getPronunciationsFromDictOrLTS : transcript '" + transcript + "' = " + builder.toString());
    }

    int numTokens = transcriptTokens.size();
    boolean canUseTransliteration = (transliteration.trim().length() > 0) && ((numTokens == translitTokens.length) || (numTokens == 1));
    int index = 0;

    if (numTokens > 50) logger.info("long transcript with " + numTokens + " num tokens " + transcript);
    for (String word : transcriptTokens) {
      String trim = word.trim();
      if (!trim.equals(word)) {
        logger.warn("getPronunciationsFromDictOrLTS trim is different '" + trim + "' != '" + word + "'");
        word = trim;
      }
      if (!word.equals(" ") && !word.isEmpty()) {
        boolean easyMatch;

        if ((easyMatch = htkDictionary.contains(word)) || (htkDictionary.contains(word.toLowerCase()))) {
          addDictMatches(justPhones, dict, word, easyMatch);
        } else {  // not in the dictionary, let's ask LTS
          LTS lts = getLTS();
          if (lts == null) {
            logger.warn("getPronunciationsFromDictOrLTS " + this + " : LTS is null???");
          } else {
            if (LTSFactory.isEmpty(lts)) {
              dict.append(getUnkPron(word));
            } else {
              String word1 = word.toLowerCase();
              String[][] process = lts.process(word1);

              if (!ltsOutputOk(process)) {
                String key = transcript + "-" + transliteration;
                if (canUseTransliteration) {
                  //              logger.info("trying transliteration LTS");
                  if (!seen.contains(key)) {
                    logger.warn("getPronunciationsFromDictOrLTS (transliteration) couldn't get letter to sound map from " +
                        lts + " for " + word1 + " in " + transcript);
                  }

                  String[][] translitprocess = (numTokens == 1) ?
                      lts.process(StringUtils.join(translitTokens, "")) :
                      lts.process(translitTokens[index]);

                  if (ltsOutputOk(translitprocess)) {
                    logger.info("getPronunciationsFromDictOrLTS got pronunciation from transliteration");
                    for (String[] pron : translitprocess) {
                      dict.append(getPronStringForWord(word, pron, false));
                    }
                  } else {
                    logger.info("getPronunciationsFromDictOrLTS transliteration LTS failed");
                    logger.warn("getPronunciationsFromDictOrLTS couldn't get letter to sound map from " + lts + " for " + word1 + " in " + transcript);
                    logger.info("getPronunciationsFromDictOrLTS attempting to fall back to default pronunciation");
                    if (translitprocess != null && (translitprocess.length > 0) && (translitprocess[0].length > 1)) {
                      dict.append(getDefaultPronStringForWord(word, translitprocess, justPhones));
                    }
                  }
                } else {
//                logger.info("can't use transliteration");
                  if (!seen.contains(key)) {
                    logger.warn("getPronunciationsFromDictOrLTS couldn't get letter to sound map from " + lts + " for '" + word1 + "' in " + transcript);
                  }

                  seen.add(key);
//                logger.info("attempting to fall back to default pronunciation");

                  // THIS is going to be the "a" pron...
           /*     if (process != null && process.length > 0) {
                  dict += getDefaultPronStringForWord(word, process, justPhones);
                }
                else {
                  logger.info("using unk phone for " +word);
                  dict += getUnkPron(word);
                }*/

                  logger.warn("using unk phone for '" + word + "' in " + transcript);
                  dict.append(getUnkPron(word));
                }
              } else { // it's ok -use it
                if (process.length > 50)
                  logger.info("getPronunciationsFromDictOrLTS prons length " + process.length + " for " + word + " in " + transcript);
                int max = MAX_FROM_ANY_TOKEN;
                for (String[] pron : process) {
                  if (max-- == 0) break;


         /*       if (korean) {
                  for (String p : pron) {
                    logger.info("got from lts '" + p + "'");
                  }
                }*/

                  String pronStringForWord = getPronStringForWord(word, pron, justPhones);
                  if (korean) {
                    logger.info("getPronunciationsFromDictOrLTS word " + word + " = " + pronStringForWord);
                  }
                  dict.append(pronStringForWord);
                }
              }
            }
          }
        }
      }
      index += 1;
    }
    return dict.toString();
  }

  private void addDictMatches(boolean justPhones, StringBuilder dict, String word, boolean easyMatch) {
    scala.collection.immutable.List<String[]> prons = htkDictionary.apply(easyMatch ? word : word.toLowerCase());
    for (int i = 0; i < prons.size(); i++) {
      String[] apply = prons.apply(i);
    /*  if (project.getLanguage().equalsIgnoreCase("korean")) {
        for (String p : apply) {
          logger.info("got from dict  " + p);
        }
      }*/
      dict.append(getPronStringForWord(word, apply, justPhones));
    }
  }

  public String getCleanedTranscript(String cleaned) {
    return getCleanedTranscript(cleaned, SEMI);
  }

  private String getCleanedTranscript(String cleaned, String sep) {
    String s = cleaned.replaceAll("\\p{Z}", sep);
    String transcriptCleaned = sep + s.trim();

    if (!transcriptCleaned.endsWith(sep)) {
      transcriptCleaned = transcriptCleaned + sep;
    }

    String after = transcriptCleaned.replaceAll(";;", sep);
    return after;
  }

  /**
   * TODO : (3/20/16) sp breaks wsdcodr when sent directly
   * wsdcodr expects a pronunciation like : distraido,d i s t rf a i d o sp;
   *
   * @param word
   * @param apply
   * @param justPhones
   * @return
   */
  @Override
  public String getPronStringForWord(String word, String[] apply, boolean justPhones) {
    String s = listToSpaceSepSequence(apply);
    return justPhones ? s + " " : word + "," + s + " sp" + SEMI;
  }

  /**
   * last resort, if we can't even use the transliteration to get some kind of pronunciation
   *
   * @param word
   * @param apply
   * @param justPhones
   * @return
   * @see #getPronunciationsFromDictOrLTS
   */
  private String getDefaultPronStringForWord(String word, String[][] apply, boolean justPhones) {
    for (String[] pc : apply) {
      String result = getPhones(pc);
      if (result.length() > 0) {
        return justPhones ? result + " " : word + "," + result + " sp" + SEMI;
      }
    }
    return justPhones ? "" : getUnkPron(word); //hopefully we never get here...
  }

  @NotNull
  private String getUnkPron(String word) {
    return word + "," + UNK + " sp" + SEMI;
  }

  private String getPhones(String[] pc) {
    StringBuilder builder = new StringBuilder();
    for (String p : pc) {
      if (!p.contains(POUND))
        builder.append(p).append(" ");
    }
    return builder.toString().trim();
  }

  private String listToSpaceSepSequence(String[] pron) {
    StringBuilder builder = new StringBuilder();
    for (String p : pron) {
/*      if (korean && p.equalsIgnoreCase("aa")) {
        p = UNK;
      }*/
      builder.append(p).append(" ");
    }
    return builder.toString().trim();
  }

  private boolean ltsOutputOk(String[][] process) {
    return !(
        process == null ||
            process.length == 0 ||
            process[0].length == 0 ||
            process[0][0].length() == 0 ||
            (StringUtils.join(process[0], "-")).contains(POUND));
  }

  private LTS getLTS() {
    return lts;
  }

  @Override
  public String getUsedTokens(Collection<String> lmSentences, List<String> background) {
    List<String> backgroundVocab = svDecoderHelper.getVocab(background, VOCAB_SIZE_LIMIT);
    return getUniqueTokensInLM(lmSentences, backgroundVocab);
  }

  /**
   * Get the unique set of tokens to use to filter against our full dictionary.
   * We check all these words for existence in the dictionary.
   * <p>
   * Any OOV words have letter-to-sound called to create word->phoneme mappings.
   * This happens in {@see pronz.speech.Audio#hscore}
   *
   * @param lmSentences
   * @param backgroundVocab
   * @return
   * @see #getUsedTokens
   */
  private String getUniqueTokensInLM(Collection<String> lmSentences, List<String> backgroundVocab) {
    String sentence;
    Set<String> backSet = new HashSet<>(backgroundVocab);
    List<String> mergedVocab = new ArrayList<>(backgroundVocab);
    List<String> foregroundVocab = svDecoderHelper.getSimpleVocab(lmSentences, FOREGROUND_VOCAB_LIMIT);
    for (String foregroundToken : foregroundVocab) {
      if (!backSet.contains(foregroundToken)) {
        mergedVocab.add(foregroundToken);
      }
    }
    StringBuilder builder = new StringBuilder();

    for (String token : mergedVocab) builder.append(token).append(" ");

    sentence = builder.toString();
    return sentence;
  }

  @Override
  public SmallVocabDecoder getSmallVocabDecoder() {
    return svDecoderHelper;
  }
}
