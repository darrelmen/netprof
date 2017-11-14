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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import corpus.HTKDictionary;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.*;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.server.audio.imagewriter.EventAndFileInfo;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static mitll.langtest.server.database.exercise.Project.WEBSERVICE_HOST_DEFAULT;

/**
 * Does ASR scoring using hydra.
 * <p>
 * Results in either alignment or decoding, depending on the mode.
 * Decoding is used with autoCRT of audio.
 * <p>
 * Takes the label files and generates transcript images for display in the client.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/10/12
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRWebserviceScoring extends Scoring implements ASR {
  private static final Logger logger = LogManager.getLogger(ASRWebserviceScoring.class);
  private static final int FOREGROUND_VOCAB_LIMIT = 100;
  private static final int VOCAB_SIZE_LIMIT = 200;
  private static final String DCODR = "dcodr";
  private static final String UNK = "+UNK+";
  private static final String SEMI = ";";
  public static final String SIL = "sil";

  private final SLFFile slfFile = new SLFFile();

  // TODO make Scores + phoneLab + wordLab an object so have something more descriptive than Object[]
  private final Cache<String, Object[]> decodeAudioToScore; // key => (Scores, wordLab, phoneLab)
  private final Cache<String, Object[]> alignAudioToScore; // key => (Scores, wordLab, phoneLab)
  private final Cache<String, Double> fileToDuration; // key => (Scores, wordLab, phoneLab)

  /**
   * Tell dcodr what grammar to use - include optional sils between words
   */
  private static final boolean SEND_GRAMMER_WITH_ALIGNMENT = true;
  /**
   * Add sils between words
   */
  private static final boolean ADD_SIL = true;
  /**
   * Used in possible trimming
   */
  private static final boolean INCLUDE_SELF_SIL_LINK = false;

  /**
   * Normally we delete the tmp dir created by hydec, but if something went wrong, we want to keep it around.
   * If the score was below a threshold, or the magic -1, we keep it around for future study.
   */
  private boolean available = false;
  private final Project project;

  /**
   * @param deployPath
   * @param properties
   * @param project
   * @see mitll.langtest.server.services.ScoringServiceImpl#getASRScoreForAudio
   * @see mitll.langtest.server.audio.AudioFileHelper#makeASRScoring
   */
  public ASRWebserviceScoring(String deployPath,
                              ServerProperties properties,
                              LogAndNotify langTestDatabase,
                              HTKDictionary htkDictionary,
                              Project project) {
    super(deployPath, properties, langTestDatabase, htkDictionary, project);
    decodeAudioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();
    alignAudioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();
    fileToDuration = CacheBuilder.newBuilder().maximumSize(100000).build();

    this.project = project;
    int port = getWebservicePort();

    if (port != -1) {
      setAvailable();
    }
  }

  private int getWebservicePort() {
    return project.getWebservicePort();
  }

  /**
   * This is always localhost, since we can't really run on a separate host - we have to be able to
   * read from the audio file, which for now must be local.
   *
   * @return
   */
  private String getWebserviceIP() {
    return WEBSERVICE_HOST_DEFAULT;
  }

  public void setAvailable() {
    new Thread(() -> {
      available = isAvailableCheckNow();
      reportOnHydra();
    }
    ).start();
  }

  /**
   * @return
   * @see AudioFileHelper#isHydraAvailable
   */
  public boolean isAvailable() {
    return available;
  }

  public boolean isAvailableCheckNow() {
    return new HTTPClient().isAvailable(getWebserviceIP(), getWebservicePort(), DCODR);
  }

  private void reportOnHydra() {
    int port = getWebservicePort();
    String ip = getWebserviceIP();
    if (available) {
      logger.info("ASRWebserviceScoring CAN talk to " + ip + ":" + port);
    } else {
      logger.warn("ASRWebserviceScoring can't talk to " + ip + ":" + port + " : this is only a problem on a hydra machine.");
    }
  }

  /**
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence              that should be what the test audio contains
   * @param imageOutDir
   * @param useCache
   * @param prefix
   * @param precalcScores
   * @param usePhoneToDisplay
   * @return PretestScore object
   */
  public PretestScore scoreRepeat(String testAudioDir,
                                  String testAudioFileNoSuffix,
                                  String sentence,
                                  Collection<String> lmSentences,
                                  String transliteration,

                                  String imageOutDir,
                                  ImageOptions imageOptions,

                                  boolean decode,
                                  boolean useCache,

                                  String prefix,

                                  PrecalcScores precalcScores,
                                  boolean usePhoneToDisplay) {
    return scoreRepeatExercise(testAudioDir, testAudioFileNoSuffix,
        sentence, lmSentences, transliteration,
        imageOutDir,
        imageOptions,
        decode,

        useCache, prefix, precalcScores, usePhoneToDisplay);
  }

  /**
   * Use hydec to do scoring<br></br>
   * <p>
   * Some magic happens in {@link Scoring#writeTranscripts } where .lab files are
   * parsed to determine the start and end times for each event, which lets us both create images that
   * show the location of the words and phonemes, and for decoding, the actual reco sentence returned. <br></br>
   * <p>
   * For alignment, of course, the reco sentence is just the given sentence echoed back (unless alignment fails to
   * generate any alignments (e.g. for audio that's complete silence or when the
   * spoken sentence is utterly unrelated to the reference.)).
   * <p>
   * Audio file must be a wav file, but can be any sample rate - if not 16K will be sampled down to 16K.
   *
   * @param testAudioDir          where the audio is
   * @param testAudioFileNoSuffix file name without a suffix - wav file, any sample rate
   * @param sentence              to align
   * @param imageOutDir           where to write the images (audioImage)
   * @param decode                if true, skips writing image files
   * @param useCache              cache scores so subsequent requests for the same audio file will get the cached score
   * @param prefix                on the names of the image files, if they are written
   * @param precalcScores
   * @param usePhoneToDisplay
   * @return score info coming back from alignment/reco
   * @see ASR#scoreRepeat
   */
  private PretestScore scoreRepeatExercise(String testAudioDir,
                                           String testAudioFileNoSuffix,
                                           String sentence,
                                           Collection<String> lmSentences, // TODO make two params, transcript and lm (null if no slf)
                                           String transliteration,
                                           String imageOutDir,
                                           ImageOptions imageOptions,

                                           boolean decode,
                                           boolean useCache,
                                           String prefix,
                                           PrecalcScores precalcScores,
                                           boolean usePhoneToDisplay) {
    String noSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String pathname = noSuffix + ".wav";

    boolean b = validLTS(sentence, transliteration);
    // audio conversion stuff
    File wavFile = new File(pathname);
    boolean mustPrepend = false;
    if (!wavFile.exists()) {
      wavFile = new File(props.getAudioBaseDir() + File.separator + pathname);
      mustPrepend = true;
    }
    if (!wavFile.exists()) {
      logger.error("scoreRepeatExercise : Can't find audio wav file at : " + wavFile.getAbsolutePath());
      return new PretestScore();
    }
    long uniqueTimestamp = System.currentTimeMillis();
    try {
      String audioDir = testAudioDir;
      if (mustPrepend) {
        audioDir = props.getAudioBaseDir() + File.separator + audioDir;
        if (!new File(audioDir).exists()) logger.error("Couldn't find " + audioDir);
        else {
          testAudioDir = audioDir;
        }
      }
      testAudioFileNoSuffix = new AudioConversion(props.shouldTrimAudio(), props.getMinDynamicRange())
          .convertTo16Khz(audioDir, testAudioFileNoSuffix, uniqueTimestamp);
    } catch (UnsupportedAudioFileException e) {
      logger.error("Got " + e, e);
    }

    if (testAudioFileNoSuffix.contains(AudioConversion.SIXTEEN_K_SUFFIX)) {
      noSuffix += AudioConversion.SIXTEEN_K_SUFFIX + "_" + uniqueTimestamp;
    }
    String filePath = testAudioDir + File.separator + testAudioFileNoSuffix;

    // check the cache...
    Object[] cached = useCache ? (decode ?
        decodeAudioToScore.getIfPresent(filePath) :
        alignAudioToScore.getIfPresent(filePath)) : null;
    Scores scores = null;
    String phoneLab = "";
    String wordLab = "";

    Double cachedDuration = fileToDuration.getIfPresent(filePath);
    if (cachedDuration == null) {
      cachedDuration = new AudioCheck(props.shouldTrimAudio(), props.getMinDynamicRange()).getDurationInSeconds(wavFile);
      fileToDuration.put(filePath, cachedDuration);
      //    logger.info("fileToDur now has "+fileToDuration.size());
    }
    if (cached != null) {
      scores = (Scores) cached[0];
      wordLab = (String) cached[1];
      phoneLab = (String) cached[2];
    }
    // actually run the scoring
    //logger.debug("Converting: " + (testAudioDir + File.separator + testAudioFileNoSuffix + ".wav to: " + rawAudioPath));
    // TODO remove the 16k hardcoding?

    JsonObject jsonObject = null;

    if (precalcScores != null && precalcScores.isValid()) {
//      logger.info("scoreRepeatExercise got valid precalc  " + precalcScores);
      scores = precalcScores.getScores();
      jsonObject = precalcScores.getJsonObject();
    }

    int processDur = 0;
    if (scores == null) {
      long then = System.currentTimeMillis();
      int end = (int) (cachedDuration * 100.0);
      Path tempDir = null;
      long uniqueTimestamp2 = System.currentTimeMillis();

      String rawAudioPath = getRawAudioPath(filePath, uniqueTimestamp2);
      try {
        tempDir = Files.createTempDirectory("scoreRepeatExercise_" + languageProperty);

        File tempFile = tempDir.toFile();

        // dcodr can't handle an equals in the file name... duh...
        String wavFile1 = filePath + ".wav";
        logger.info("scoreRepeatExercise : sending " + rawAudioPath + " to hydra (derived from " + wavFile1 + ")");
        boolean wroteIt = AudioConversion.wav2raw(wavFile1, rawAudioPath);

        if (!wroteIt) {
          logAndNotify.logAndNotifyServerException(null, "couldn't write the raw file to " + rawAudioPath);
          return new PretestScore(0);
        }

        Object[] result = runHydra(rawAudioPath, sentence, transliteration, lmSentences,
            tempFile.getAbsolutePath(), decode, end);
        if (result == null) {
          return new PretestScore(0);
        } else {
          processDur = (int) (System.currentTimeMillis() - then);
          scores = (Scores) result[0];
          wordLab = (String) result[1];
          phoneLab = (String) result[2];
          if (scores.isValid()) {
            if (wordLab.contains("UNKNOWN")) {
              logger.info("note : hydra result includes UNKNOWNMODEL : " + wordLab);
            }
            cacheHydraResult(decode, filePath, scores, phoneLab, wordLab);
          } else {
            logger.warn("scoreRepeatExercise skipping invalid response from hydra.");
          }
        }

      } catch (IOException e) {
        logger.error("got " + e, e);
      } finally {
        if (tempDir != null) {
          //  tempDir.toFile().deleteOnExit(); // clean up temp file

          if (!tempDir.toFile().delete()) {
            logger.debug("couldn't delete " + tempDir); // clean up temp file
          }
        }

        cleanUpRawFile(rawAudioPath);
      }
    }
    if (scores == null) {
      logger.error("scoreRepeatExercise hydra failed to generate scores.");
      return new PretestScore(-1f);
    }
    return getPretestScore(
        imageOutDir,
        imageOptions,
        decode, prefix, noSuffix,
        scores, phoneLab, wordLab,
        cachedDuration, processDur, usePhoneToDisplay, jsonObject);
  }

  /**
   * We don't need it after sending it to dcodr
   *
   * @param rawAudioPath
   */
  private void cleanUpRawFile(String rawAudioPath) {
    File rawFile = new File(rawAudioPath);
    String absolutePath = rawFile.getAbsolutePath();
    if (rawFile.exists()) {
      if (rawFile.delete()) {
        logger.info("cleanUpRawFile : deleted " + absolutePath);
      } else {
        String mes = "cleanUpRawFile : huh? couldn't delete raw audio rawFile " + absolutePath;
        logger.error(mes);
        logAndNotify.logAndNotifyServerException(null, mes);
      }
    } else {
      logger.info("cleanUpRawFile : source file doesn't exist = " + absolutePath);
    }
  }

  @NotNull
  private String getRawAudioPath(String filePath, long unique) {
    return filePath.replaceAll("\\=", "") + "_" + unique + ".raw";
  }

  private void cacheHydraResult(boolean decode, String key, Scores scores, String phoneLab, String wordLab) {
    Cache<String, Object[]> stringCache = decode ? decodeAudioToScore : alignAudioToScore;
    stringCache.put(key, new Object[]{scores, wordLab, phoneLab});
  }

  /**
   * TODO : don't copy this method in both ASRScoring and ASRWebserviceScoring
   * <p>
   * TODO : don't write images unless we really want them
   *
   * @param imageOutDir
   * @param decode
   * @param prefix
   * @param noSuffix
   * @param scores
   * @param phoneLab
   * @param wordLab
   * @param duration
   * @param processDur
   * @param usePhoneToDisplay
   * @return
   * @see #scoreRepeatExercise
   */
  private PretestScore getPretestScore(String imageOutDir,
                                       ImageOptions imageOptions,

                                       boolean decode, String prefix, String noSuffix,
                                       Scores scores,
                                       String phoneLab,
                                       String wordLab,
                                       double duration, int processDur, boolean usePhoneToDisplay,
                                       JsonObject jsonObject
  ) {
    try {
      boolean useScoreForBkgColor = imageOptions.isUseScoreToColorBkg();
      String prefix1 = prefix + (useScoreForBkgColor ? "bkgColorForRef" : "") + (usePhoneToDisplay ? "_phoneToDisplay" : "");

/*
      logger.info("getPretestScore write images to" +
          "\n\tout " + imageOutDir +
          "\n\tnoSuffix " + noSuffix +
          "\n\tprefix1 " + prefix1
      );
*/

      int imageWidth = imageOptions.getWidth();
      int imageHeight = imageOptions.getHeight();

      boolean reallyUsePhone = usePhoneToDisplay || props.usePhoneToDisplay();
      EventAndFileInfo eventAndFileInfo = jsonObject == null ?
          writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix,
              useScoreForBkgColor,
              prefix1, "", decode, phoneLab, wordLab, true, usePhoneToDisplay) :
          writeTranscriptsCached(imageOutDir, imageWidth, imageHeight, noSuffix,
              useScoreForBkgColor,
              prefix1, "", decode, false, jsonObject, reallyUsePhone);
      Map<NetPronImageType, String> sTypeToImage = getTypeToRelativeURLMap(eventAndFileInfo.typeToFile);
      Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = getTypeToEndTimes(eventAndFileInfo);

/*
      logger.info("getPretestScore sTypeToImage" +
          "\n\tsTypeToImage " + sTypeToImage
      );
*/

      if (typeToEndTimes.isEmpty()) {
        logger.warn("getPretestScore huh? no segments from words " + wordLab + " phones " + phoneLab);
      }
/*      logger.info("getPretestScore typeToEndTimes" +
          "\n\ttypeToEndTimes " + typeToEndTimes
      );*/

      return new PretestScore(scores.hydraScore,
          getPhoneToScore(scores),
          getWordToScore(scores),
          sTypeToImage,
          typeToEndTimes,
          getRecoSentence(eventAndFileInfo),
          (float) duration,
          processDur);

    } catch (Exception e) {
      logger.error("Got " + e, e);
      return new PretestScore(-1);
    }
  }

  ////////////////////////////////
  ////////////////////////////////

  private boolean ltsOutputOk(String[][] process) {
    return !(
        process == null ||
            process.length == 0 ||
            process[0].length == 0 ||
            process[0][0].length() == 0 ||
            (StringUtils.join(process[0], "-")).contains("#"));
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
   * @see #runHydra
   */
  @Override
  public String createHydraDict(String transcript, String transliteration) {
    if (getLTS() == null) {
      logger.warn(this + " : createHydraDict : LTS is null???");
    }

    String dict
        = "[";
    String pronunciations = getPronunciationsFromDictOrLTS(transcript, transliteration, false);

/*    logger.info("createHydraDict" +
        "\n\ttranscript     " + transcript +
        "\n\tpronunciations " + pronunciations
    );*/

    dict += pronunciations;
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
   * @param justPhones
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#getPronunciationsFromDictOrLTS
   */
  public String getPronunciationsFromDictOrLTS(String transcript, String transliteration, boolean justPhones) {
    StringBuilder dict = new StringBuilder();
    String[] translitTokens = transliteration.toLowerCase().split(" ");

    //String[] transcriptTokens = transcript.split(" ");
    List<String> transcriptTokens = svDecoderHelper.getTokens(transcript);

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
          scala.collection.immutable.List<String[]> prons = htkDictionary.apply(easyMatch ? word : word.toLowerCase());


          for (int i = 0; i < prons.size(); i++) {
            dict.append(getPronStringForWord(word, prons.apply(i), justPhones));
          }
        } else {  // not in the dictionary, let's ask LTS
          if (getLTS() == null) {
            logger.warn("getPronunciationsFromDictOrLTS " + this + " " + languageProperty + " : LTS is null???");
          } else {
            String word1 = word.toLowerCase();
            String[][] process = getLTS().process(word1);
            if (!ltsOutputOk(process)) {
              String key = transcript + "-" + transliteration;
              if (canUseTransliteration) {
                //              logger.info("trying transliteration LTS");
                if (!seen.contains(key)) {
                  logger.warn("getPronunciationsFromDictOrLTS (transliteration) couldn't get letter to sound map from " +
                      getLTS() + " for " + word1 + " in " + transcript);
                }

                String[][] translitprocess = (numTokens == 1) ?
                    getLTS().process(StringUtils.join(translitTokens, "")) :
                    getLTS().process(translitTokens[index]);

                if (ltsOutputOk(translitprocess)) {
                  logger.info("getPronunciationsFromDictOrLTS got pronunciation from transliteration");
                  for (String[] pron : translitprocess) {
                    dict.append(getPronStringForWord(word, pron, false));
                  }
                } else {
                  logger.info("getPronunciationsFromDictOrLTS transliteration LTS failed");
                  logger.warn("getPronunciationsFromDictOrLTS couldn't get letter to sound map from " + getLTS() + " for " + word1 + " in " + transcript);
                  logger.info("getPronunciationsFromDictOrLTS attempting to fall back to default pronunciation");
                  if (translitprocess != null && (translitprocess.length > 0) && (translitprocess[0].length > 1)) {
                    dict.append(getDefaultPronStringForWord(word, translitprocess, justPhones));
                  }
                }
              } else {
//                logger.info("can't use transliteration");
                if (!seen.contains(key)) {
                  logger.warn("getPronunciationsFromDictOrLTS couldn't get letter to sound map from " + getLTS() + " for '" + word1 + "' in " + transcript);
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

                logger.info("using unk phone for " + word);
                dict.append(getUnkPron(word));
              }
            } else { // it's ok -use it
              if (process.length > 50) logger.info("prons length " + process.length + " for " + transcript);
              for (String[] pron : process) {
                dict.append(getPronStringForWord(word, pron, justPhones));
              }
            }
          }
        }
      }
      index += 1;
    }
    return dict.toString();
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
  private String getPronStringForWord(String word, String[] apply, boolean justPhones) {
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
      if (!p.contains("#"))
        builder.append(p).append(" ");
    }
    return builder.toString().trim();
  }

  private String listToSpaceSepSequence(String[] pron) {
    StringBuilder builder = new StringBuilder();
    for (String p : pron) builder.append(p).append(" ");
    return builder.toString().trim();
  }

  /**
   * @param audioPath
   * @param transcript
   * @param lmSentences
   * @param tmpDir
   * @param decode
   * @param end         frame number of end of file (I think)
   * @return
   * @see #scoreRepeatExercise
   */
  public Object[] runHydra(String audioPath,
                           String transcript,
                           String transliteration,
                           Collection<String> lmSentences,
                           String tmpDir,
                           boolean decode,
                           int end) {
    // reference trans
//    logger.info("transcript " + transcript);
    String cleaned = slfFile.cleanToken(transcript).trim();
//    logger.info("cleaned    " + cleaned);
    if (isMandarin) {
      cleaned = (decode ? SLFFile.UNKNOWN_MODEL + " " : "") + svDecoderHelper.getSegmented(transcript.trim()); // segmentation method will filter out the UNK model
    }

    // generate dictionary
    String hydraDict = createHydraDict(cleaned, transliteration);
    String smallLM = "[" +
        (SEND_GRAMMER_WITH_ALIGNMENT ? slfFile.createSimpleSLFFile(Collections.singleton(cleaned), ADD_SIL, false, INCLUDE_SELF_SIL_LINK)[0] : "") +
        "]";

    // generate SLF file (if decoding)
    if (decode) {
      String[] slfOut = slfFile.createSimpleSLFFile(lmSentences, ADD_SIL, true, INCLUDE_SELF_SIL_LINK);
      smallLM = "[" + slfOut[0] + "]";
      cleaned = slfFile.cleanToken(slfOut[1]);
    }

    // String cleanedTranscript = getCleanedTranscript(cleaned, SEMI);

    String hydraInput =
        tmpDir + "/:" +
            audioPath + ":" +
            hydraDict + ":" +
            smallLM + ":" +
            "xxx,0," + end + "," +
            "[<s>" + getCleanedTranscript(cleaned, SEMI) + "</s>]";

    long then = System.currentTimeMillis();

    HTTPClient dcodr = getDcodr();

    String resultsStr = runHydra(hydraInput, dcodr);
    if (resultsStr.startsWith("ERROR")) {
      String message = getFailureMessage(audioPath, transcript, lmSentences, decode);
      message = "hydra said " + resultsStr + " : " + message;
      logger.error(message);
      logAndNotify.logAndNotifyServerException(null, message);
      return null;
    } else {
      String[] results = resultsStr.split("\n"); // 0th entry-overall score and phone scores, 1st entry-word alignments, 2nd entry-phone alignments
      long timeToRunHydra = System.currentTimeMillis() - then;

      if (results[0].isEmpty()) {
        String message = getFailureMessage(audioPath, transcript, lmSentences, decode);
        logger.error(message);
        if (logAndNotify != null) {  // skip during testing
          logAndNotify.logAndNotifyServerException(null, message);
        }
        return null;
      }
      // TODO makes this a tuple3 type
      String[] split = results[0].split(SEMI);
      Scores scores = new Scores(split);
      // clean up tmp directory if above score threshold
      logger.debug(languageProperty + " : Took " + timeToRunHydra + " millis to run " + (decode ? "decode" : "align") +
          " hydra on " + audioPath + " - score: " + split[0] + " raw reply : " + resultsStr);
    /*if (Float.parseFloat(split[0]) > lowScoreThresholdKeepTempDir) {   // keep really bad scores for now
      try {
				logger.debug("deleting " + tmpDir + " since score is " + split[0]);
				FileUtils.deleteDirectory(new File(tmpDir));
			} catch (IOException e) {
				logger.error("Deleting dir " + tmpDir + " got " +e,e);
			}
		}*/
      return new Object[]{scores, results[1].replaceAll("#", ""), results[2].replaceAll("#", "")}; // where are the # coming from?
    }
  }

  @NotNull
  private HTTPClient getDcodr() {
    return new HTTPClient(getWebserviceIP(), getWebservicePort(), DCODR);
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

  private String getFailureMessage(String audioPath, String transcript, Collection<String> lmSentences, boolean decode) {
    String input = decode ? lmSentences == null ? "huh? no sentences to decode???" : lmSentences.toString() : transcript;
    String which = decode ? " DECODING " : " ALIGNMENT ";
    return "Failure during running of hydra on " + audioPath + which + " with " + input;
  }

  /**
   * @param hydraInput
   * @param httpClient
   * @return
   * @see #runHydra
   */
  private String runHydra(String hydraInput, HTTPClient httpClient) {
    try {
      String resultsStr;
      try {
        resultsStr = httpClient.sendAndReceive(hydraInput);
      } catch (IOException e) {
        logger.error("Error closing http connection " + e, e);
        logAndNotify.logAndNotifyServerException(e, "running hydra on" +
            "\n\thost  " + getHostName() +
            "\n\tinput " + hydraInput);
        resultsStr = "";
      }
      return resultsStr;
    } catch (Exception e) {
      logger.error("runHydra : running on port " + getWebservicePort() + " sent " + hydraInput + " got " + e, e);
      return "";
    }
  }

  private String getHostName() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown host?";
    }
  }

  ////////////////////////////////
  ////////////////////////////////

  /**
   * @param lmSentences
   * @param background
   * @return
   * @see AlignDecode#getASRScoreForAudio
   * @see AlignDecode#getASRScoreForAudio(File, Collection, String, DecoderOptions, PrecalcScores)
   */
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

  /**
   * Make a map of event type to segment end times (so we can map clicks to which segment is clicked on).<br></br>
   * Note we have to adjust the last segment time to be the audio duration, so we can correct for wav vs mp3 time
   * duration differences (mp3 files being typically about 0.1 seconds longer than wav files).
   * The consumer of this map is at {@link mitll.langtest.client.scoring.ScoringAudioPanel.TranscriptEventClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)}
   *
   * @param eventAndFileInfo
   * @return
   * @see #scoreRepeatExercise
   */
  private Map<NetPronImageType, List<TranscriptSegment>> getTypeToEndTimes(EventAndFileInfo eventAndFileInfo) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = eventAndFileInfo.typeToEvent;
    return getTypeToSegments(typeToEvent);
  }

  @NotNull
  private Map<NetPronImageType, List<TranscriptSegment>> getTypeToSegments(Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<>();

    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<TranscriptSegment> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) {
        typeToEndTimes.put(key, endTimes = new ArrayList<>());
      }
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        endTimes.add(new TranscriptSegment(value.start, value.end, value.event, value.score));
      }
    }

    return typeToEndTimes;
  }

  private Map<String, Float> getPhoneToScore(Scores scores) {
    Map<String, Float> phones = scores.eventScores.get(Scores.PHONES);
    return getTokenToScore(scores, phones, true);
  }

  /**
   * @param scores
   * @return
   * @see #getPretestScore
   */
  private Map<String, Float> getWordToScore(Scores scores) {
    Map<String, Float> phones = scores.eventScores.get(Scores.WORDS);
    return getTokenToScore(scores, phones, false);
  }

  private Map<String, Float> getTokenToScore(Scores scores, Map<String, Float> phones, boolean expecting) {
    if (phones == null) {
      if (expecting) {
        logger.warn("getTokenToScore no phone scores in " + scores.eventScores);
      }
      return Collections.emptyMap();
    } else {
      Map<String, Float> phoneToScore = new HashMap<>();
      for (Map.Entry<String, Float> phoneScorePair : phones.entrySet()) {
        String key = phoneScorePair.getKey();

        if (!key.equalsIgnoreCase(SIL)) {
          Float value = phoneScorePair.getValue();
          //   logger.info("getTokenToScore adding '" + key + "' : " + value);
          phoneToScore.put(key, Math.min(1.0f, value));
        }
//        else {
//          // logger.info("getTokenToScore skipping key '" + key + "'");
//        }

      }
      return phoneToScore;
    }
  }
}