package mitll.langtest.server.scoring;

import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.exercise.Project;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;
import java.util.*;

public class PronunciationLookup implements IPronunciationLookup {
  private static final Logger logger = LogManager.getLogger(PronunciationLookup.class);

  private static final int FOREGROUND_VOCAB_LIMIT = 100;
  private static final int VOCAB_SIZE_LIMIT = 200;
  private static final String UNK = "+UNK+";
  /**
   *
   */
  private static final String SEMI = ";";
  public static final String SIL = "sil";
  private static final int MAX_FROM_ANY_TOKEN = 10;
  private static final String POUND = "#";
  private static final boolean DEBUG = false;


  private SmallVocabDecoder svDecoderHelper = null;
  private final HTKDictionary htkDictionary;
  private final LTS lts;
  private boolean korean, russian, removeAllPunct;

  /**
   * @param dictionary
   * @param lts
   * @param project
   * @see ASRWebserviceScoring#ASRWebserviceScoring
   */
  PronunciationLookup(HTKDictionary dictionary, LTS lts, Project project) {
    this.htkDictionary = dictionary;
    this.lts = lts;
    String language = project.getLanguage();
    korean = language.equalsIgnoreCase("korean");
    russian = language.equalsIgnoreCase("russian");
    removeAllPunct = !language.equalsIgnoreCase("french");
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
   * @param possibleProns
   * @return the dictionary for dcodr
   * @see ASR#getHydraDict
   */
  @Override
  public String createHydraDict(String transcript, String transliteration, List<WordAndProns> possibleProns) {
    if (lts == null) {
      logger.warn(this + " : createHydraDict : LTS is null???");
    }

    String dict = "[";
    dict += getPronunciationsFromDictOrLTS(transcript, transliteration, false, true, possibleProns);
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
   * Don't strip accents from tokens used in dictionary lookup - like for French precisement.
   * <p>
   * It would be good if the accent thing were consistent across dictionaries.
   *
   * @param transcript
   * @param transliteration
   * @param justPhones      true if just want the phones
   * @param makeCandidates
   * @param possible
   * @return
   * @see #createHydraDict
   * @see mitll.langtest.server.audio.AudioFileHelper#getPronunciationsFromDictOrLTS
   */
  @Override
  public String getPronunciationsFromDictOrLTS(String transcript,
                                               String transliteration,
                                               boolean justPhones,
                                               boolean makeCandidates,
                                               List<WordAndProns> possible) {
    StringBuilder dict = new StringBuilder();
    String[] translitTokens = transliteration.toLowerCase().split(" ");

//    logger.info("getPronunciationsFromDictOrLTS ask for pron for '" + transcript + "'");
    List<String> transcriptTokens = svDecoderHelper.getTokens(transcript, removeAllPunct);
    //   logger.info("getPronunciationsFromDictOrLTS got              '" + transcriptTokens + "'");

//    {
//      StringBuilder builder = new StringBuilder();
//      transcriptTokens.forEach(token -> builder.append(token).append(" "));
//      logger.info("getPronunciationsFromDictOrLTS : transcript '" + transcript + "' = " + builder.toString());
//    }

    int numTokens = transcriptTokens.size();
    boolean canUseTransliteration = (transliteration.trim().length() > 0) && ((numTokens == translitTokens.length) || (numTokens == 1));
    int index = 0;

    List<WordAndProns> candidates = new ArrayList<>();

    List<List<String>> wordProns;// = new ArrayList<>();
    //candidates.add("");

    if (numTokens > 50) logger.info("long transcript with " + numTokens + " num tokens " + transcript);
    for (String word : transcriptTokens) {
      String trim = word.trim();
      if (!trim.equals(word)) {
        logger.warn("getPronunciationsFromDictOrLTS trim is different '" + trim + "' != '" + word + "'");
        word = trim;
      }
      if (!word.equals(" ") && !word.isEmpty()) {
        boolean easyMatch, lowerMatch = false, stripMatch = false;

        if (DEBUG) logger.info("getPronunciationsFromDictOrLTS look in dict for '" + word + "'");
        if ((easyMatch = htkDictionary.contains(word)) ||
            (lowerMatch = htkDictionary.contains(word.toLowerCase())) ||
            (russian && (stripMatch = htkDictionary.contains(removeAccents(word))))
            ) {
          candidates.add(addDictMatch(justPhones, dict, word, easyMatch, lowerMatch, stripMatch));
        } else if (!removeAllPunct && !(wordProns = hasParts(word)).isEmpty()) {
          if (DEBUG) logger.info("getPronunciationsFromDictOrLTS add parts for '" + word + "'");
          candidates.add(addDictParts(justPhones, dict, wordProns, word));
        } else {  // not in the dictionary, let's ask LTS
          String optional = russian ? " or " + removeAccents(word) : "";
          logger.info("getPronunciationsFromDictOrLTS NOT found in dict : '" + word + "'" + optional);
          LTS lts = getLTS();
          if (lts == null) {
            logger.warn("getPronunciationsFromDictOrLTS " + this + " : LTS is null???");
          } else {
            if (LTSFactory.isEmpty(lts)) {
              dict.append(getUnkPron(word));
              candidates.add(new WordAndProns(word, UNK));
            } else {
              String word1 = word.toLowerCase();
              //  logger.info("no dict entry for " + word1);
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
                    candidates.add(addTranslitPhones(dict, word, translitprocess));
                  } else {
                    translitWarning(transcript, lts, word1);
                    if (translitprocess != null && (translitprocess.length > 0) && (translitprocess[0].length > 1)) {
                      String defaultPronStringForWord = getDefaultPronStringForWord(word, translitprocess, justPhones);
                      dict.append(defaultPronStringForWord);

                      List<String> prons = new ArrayList<>();
                      for (String[] pron : translitprocess) {
                        prons.add(getPhoneSeq(pron));
                      }
                      candidates.add(new WordAndProns(word, prons));
                    }
                  }
                } else {
//                logger.info("can't use transliteration");
                  if (!seen.contains(key)) {
                    logger.warn("getPronunciationsFromDictOrLTS couldn't get letter to sound map from " + lts + " for '" + word1 + "' in " + transcript);
                  }

                  seen.add(key);
                  logger.warn("getPronunciationsFromDictOrLTS using unk phone for '" + word + "' in " + transcript);
                  dict.append(getUnkPron(word));
                  candidates.add(new WordAndProns(word, UNK));
                }
              } else { // it's ok -use it
                if (process.length > 50) {
                  logger.info("getPronunciationsFromDictOrLTS prons length " + process.length + " for " + word + " in " + transcript);
                }
                int max = MAX_FROM_ANY_TOKEN;
                List<String> prons = new ArrayList<>();
                for (String[] pron : process) {
                  if (max-- == 0) break;
                  boolean allValid = areAllPhonesValid(pron);

                  if (allValid) {
                    String pronStringForWord = getPronStringForWord(word, pron, justPhones);
                    if (korean) {
                      logger.info("getPronunciationsFromDictOrLTS word " + word + " = " + pronStringForWord);
                    }
                    dict.append(pronStringForWord);
                    prons.add(getPhoneSeq(pron));

                  } else {
                    logger.warn("getPronunciationsFromDictOrLTS : skipping invalid pron " + Arrays.asList(pron) + " for " + word);
                  }
                }
                candidates.add(new WordAndProns(word, prons));
              }
            }
          }
        }
      }
    }
//    index += 1;


    if (candidates != null) {
      possible.addAll(candidates);
    }
    return dict.toString();
  }

  private WordAndProns addDictParts(boolean justPhones, StringBuilder dict, List<List<String>> wordProns, String word) {
    List<String> possibleProns = new ArrayList<>(wordProns.size());
    for (List<String> phoneSequence : wordProns) {
      if (DEBUG) logger.warn("\taddDictParts got " + word + " : " + phoneSequence);
      addPhoneSeq(possibleProns, phoneSequence);
      String pronStringForWord = getPronStringForWord(word, phoneSequence, justPhones);
      if (DEBUG)  logger.warn("\taddDictParts add dict " + pronStringForWord);
      dict.append(pronStringForWord);
    }
    WordAndProns wordAndProns = new WordAndProns(word, possibleProns);
    logger.warn("\taddDictParts for " +word+ " made " + wordAndProns);
    return wordAndProns;
  }

  private WordAndProns addTranslitPhones(StringBuilder dict,
                                         String word,
                                         String[][] translitprocess) {
    List<String> prons = new ArrayList<>();
    for (String[] pron : translitprocess) {
      dict.append(getPronStringForWord(word, pron, false));
      prons.add(getPhoneSeq(pron));
    }
    return new WordAndProns(word, prons);
  }

  private List<List<String>> hasParts(String token) {
    String[] split = token.split("'");

    boolean match = true;

    List<List<String>> candidates = new ArrayList<>();
    candidates.add(new ArrayList<>());

    if (split.length > 1) {
      List<String> parts = Arrays.asList(split);

      for (String part : parts) {
        boolean easyMatch;
        match &=
            (easyMatch = htkDictionary.contains(part)) ||
                (htkDictionary.contains(part.toLowerCase()));

        if (match) {
          String lookupToken = easyMatch ? part : part.toLowerCase();

          scala.collection.immutable.List<String[]> prons = htkDictionary.apply(lookupToken);

          int size = prons.size();
          List<List<String>> possibleProns = new ArrayList<>(size);
          logger.warn("hasParts for " + lookupToken + " found " + size + " possible prons.");
          for (int i = 0; i < size; i++) {
            String[] phoneSequence = prons.apply(i);
            List<String> e = Arrays.asList(phoneSequence);
            logger.warn("hasParts adding " + lookupToken + " : " + e);
            possibleProns.add(e);
          }

          candidates = getPermutations(candidates, possibleProns);

        } else {
          logger.warn("hasParts for " + token + " found no match.");
          match = false;
          break;
        }
      }
    } else {
      logger.warn("hasParts for " + token + " split " + split.length);
      match = false;
    }
    return match ? candidates : Collections.emptyList();
  }

/*
  private WordAndProns addDictMatch2(boolean justPhones,
                                     StringBuilder dict,
                                     String word, boolean easyMatch, boolean lowerMatch, boolean stripMatch) {
    String lookupToken =
        easyMatch ? word :
            lowerMatch ? word.toLowerCase() :
                stripMatch ? removeAccents(word) : word;

    if (DEBUG) {
      logger.info("getPronunciationsFromDictOrLTS found in dict : '" + word + "' : '" + lookupToken + "'");
    }

    List<String> pronsForWord = addDictMatches(justPhones, dict, word, lookupToken);
    return new WordAndProns(word, pronsForWord);
  }
*/


  private WordAndProns addDictMatch(boolean justPhones, StringBuilder dict, String word, boolean easyMatch, boolean lowerMatch, boolean stripMatch) {
    String lookupToken =
        easyMatch ? word :
            lowerMatch ? word.toLowerCase() :
                stripMatch ? removeAccents(word) : word;

    if (DEBUG) {
      logger.info("getPronunciationsFromDictOrLTS found in dict : '" + word + "' : '" + lookupToken + "'");
    }

    return new WordAndProns(word, addDictMatches(justPhones, dict, word, lookupToken));
  }

  /**
   * @param justPhones
   * @param dict
   * @param word
   * @param lookupToken
   * @return
   */
  private List<String> addDictMatches(boolean justPhones,
                                      StringBuilder dict,
                                      String word,
                                      String lookupToken) {
    scala.collection.immutable.List<String[]> prons = htkDictionary.apply(lookupToken);
    int size = prons.size();
    List<String> possibleProns = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      String[] phoneSequence = prons.apply(i);
      addPhoneSeq(possibleProns, Arrays.asList(phoneSequence));
      String pronStringForWord = getPronStringForWord(word, phoneSequence, justPhones);
//      logger.warn("addDictMatches for word " + lookupToken + "\n\t" + pronStringForWord);
      dict.append(pronStringForWord);
    }
    /*    logger.info("addDictMatches for" +
        "\n\teasyMatch " + easyMatch +
        "\n\tword " + word +
        "\n\tdict " + dict);*/

    return possibleProns;
  }

  private void translitWarning(String transcript, LTS lts, String word1) {
    logger.info("getPronunciationsFromDictOrLTS transliteration LTS failed");
    logger.warn("getPronunciationsFromDictOrLTS couldn't get letter to sound map from " + lts + " for " + word1 + " in " + transcript);
    logger.info("getPronunciationsFromDictOrLTS attempting to fall back to default pronunciation");
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


//
//  List<String> getPhoneSeqs(String[] phoneSequence) {
//    List<String> possibleProns = new ArrayList<>(1);
//    addPhoneSeq(possibleProns, phoneSequence);
//    return possibleProns;
//  }

  private void addPhoneSeq(List<String> possibleProns, Collection<String> phoneSequence) {
    possibleProns.add(getPhoneSeq(phoneSequence));
  }

  @NotNull
  private List<List<String>> getPermutations(List<List<String>> candidates, List<List<String>> pronsForWord) {
    List<List<String>> nc = new ArrayList<>();
    for (List<String> curr : candidates) {
      for (List<String> pronForWord : pronsForWord) {
        // StringBuilder builder = new StringBuilder();
        // builder.append(curr);
        // builder.append(pronForWord);
        List<String> copy = new ArrayList<>(curr);
        copy.addAll(pronForWord);
        nc.add(copy);
      }
    }
    return nc;
  }

  private String getPhoneSeq(String[] phoneSequence) {
    return getPhoneSeq(Arrays.asList(phoneSequence));
  }

  /**
   * Mush them all together
   *
   * @param phoneSequence
   * @return
   */
  @NotNull
  private String getPhoneSeq(Collection<String> phoneSequence) {
    StringBuilder builder = new StringBuilder();
    for (String p : phoneSequence) builder.append(p);
    return builder.toString();
  }

  private String removeAccents(String text) {
    return text == null ? null :
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }

  /**
   * @param word
   * @param apply
   * @param justPhones
   * @return
   */

  private String getPronStringForWord(String word, String[] apply, boolean justPhones) {
    return getPronStringForWord(word, Arrays.asList(apply), justPhones);
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
  public String getPronStringForWord(String word, Collection<String> apply, boolean justPhones) {
    String s = listToSpaceSepSequence(apply);
    String word1 = getSmallVocabDecoder().getTrimmed(removeAccents(word));
/*    if (!word.equals(word1)) {
      logger.warn("from :\n\t'" + word +
          "' to " +
          "\n\t'" + word1 +
          "'");
    }*/
    return justPhones ?
        (s + " ") :
        word1 + "," + s + " sp" + SEMI;
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

    return transcriptCleaned.replaceAll(";;", sep);
  }

  /**
   * last resort, if we can't even use the transliteration to get some kind of pronunciation
   *
   * @param word
   * @param apply
   * @param justPhones
   * @return
   * @see IPronunciationLookup#getPronunciationsFromDictOrLTS
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

  private String getPhones(String[] pc) {
    StringBuilder builder = new StringBuilder();
    for (String p : pc) {
      if (!p.contains(POUND))
        builder.append(p).append(" ");
    }
    return builder.toString().trim();
  }

  @NotNull
  private String getUnkPron(String word) {
    return word + "," + UNK + " sp" + SEMI;
  }

  private String listToSpaceSepSequence(Collection<String> pron) {
    StringBuilder builder = new StringBuilder();
    pron.forEach(p -> builder.append(p).append(" "));
    return builder.toString().trim();
  }

  /**
   * @param process
   * @return
   * @see #getNumPhonesFromDictionaryOrLTS
   */
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
