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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.*;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.server.audio.imagewriter.EventAndFileInfo;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.npdata.dao.lts.HTKDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.exercise.Project.WEBSERVICE_HOST_DEFAULT;
import static mitll.langtest.server.scoring.HydraOutput.STATUS_CODES;
import static mitll.langtest.server.scoring.HydraOutput.STATUS_CODES.ERROR;
import static mitll.langtest.server.scoring.HydraOutput.STATUS_CODES.SUCCESS;
import static mitll.langtest.server.scoring.Scores.PHONES;

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

  private static final String DCODR = "dcodr";

  private static final String SEMI = ";";
  private static final String SIL = "sil";

  static final int MAX_FROM_ANY_TOKEN = 10;

  private static final boolean DEBUG = false;

  private static final String WAV1 = ".wav";
  private static final String WAV = WAV1;
  private static final String SCORE = "score";
  private static final String STATUS = "status";
  private static final String LOG = "log";

  private final SLFFile slfFile = new SLFFile();

  // TODO make Scores + phoneLab + wordLab an object so have something more descriptive than Object[]
  private final Cache<String, HydraOutput> decodeAudioToScore; // key => (Scores, wordLab, phoneLab)
  private final Cache<String, HydraOutput> alignAudioToScore; // key => (Scores, wordLab, phoneLab)
  private final Cache<String, Double> fileToDuration; // key => (Scores, wordLab, phoneLab)

  /**
   * Add sils between words
   */
  private static final boolean ADD_SIL = true;
  /**
   * Used in possible trimming
   */
  private static final boolean INCLUDE_SELF_SIL_LINK = false;
  private final Map<String, String> phoneToDisplay;

  /**
   * Normally we delete the tmp dir created by hydec, but if something went wrong, we want to keep it around.
   * If the score was below a threshold, or the magic -1, we keep it around for future study.
   */
  private boolean available = false;
  private final Project project;
  private final IPronunciationLookup pronunciationLookup;

  private final AtomicInteger hydraReqCounter = new AtomicInteger();
  private final TranscriptSegmentGenerator generator;

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
    generator = new TranscriptSegmentGenerator(properties);
    this.project = project;

    int port = getWebservicePort();
    phoneToDisplay = Collections.emptyMap();//properties.getPhoneToDisplay(languageEnum);
//      logger.info("(" + language + ") phone->display " + phoneToDisplay);

    this.pronunciationLookup = new PronunciationLookup(htkDictionary, getLTS(), project);
    if (port != -1) {
      setAvailable();
    }
  }

  @Override
  public SmallVocabDecoder getSmallVocabDecoder() {
    return getPronunciationLookup().getSmallVocabDecoder();
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

  @Override
  public void setAvailable() {
    new Thread(() -> {
      available = isAvailableCheckNow();
      reportOnHydra();
    }, "setAvailable").start();
  }

  /**
   * @return
   * @see AudioFileHelper#isHydraAvailable
   */
  public boolean isAvailable() {
    return available;
  }

  @Override
  public boolean isAvailableCheckNow() {
    return new HTTPClient().isAvailable(getWebserviceIP(), getWebservicePort(), DCODR);
  }

  private void reportOnHydra() {
    int port = getWebservicePort();
    String ip = getWebserviceIP();
    if (available) {
      logger.debug("ASRWebserviceScoring CAN talk to " + ip + ":" + port);
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
   * @param kaldi
   * @return PretestScore object
   * @seex AudioFileHelper#getASRScoreForAudio(int, String, String, Collection, String, ImageOptions, String, PrecalcScores, DecoderOptions)
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
                                  boolean usePhoneToDisplay,
                                  boolean kaldi) {
    return scoreRepeatExercise(testAudioDir, testAudioFileNoSuffix,
        sentence, lmSentences, transliteration,
        imageOutDir,
        imageOptions,
        decode,

        useCache, prefix, precalcScores, usePhoneToDisplay, kaldi, getWebservicePort());
  }

  /**
   * Use hydra to do scoring<br></br>
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
   * <p>
   * Note we have to worry about to different threads asking for the same file at the same millisecond.
   * In which case we use atomic integer to create non-colliding requests.
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
   * @param useKaldi              true if using the new kaldi protocol!
   * @param port
   * @return score info coming back from alignment/reco
   * @seex ASR#scoreRepeat
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
                                           boolean usePhoneToDisplay,
                                           boolean useKaldi,
                                           int port) {
    long then = System.currentTimeMillis();

    logger.info("scoreRepeatExercise decode =" + decode + " decode/align '" + sentence + "'");
    String noSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String pathname = noSuffix + WAV;

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
    HydraOutput cached = useCache ? (decode ?
        decodeAudioToScore.getIfPresent(filePath) :
        alignAudioToScore.getIfPresent(filePath)) : null;

    if (cached != null) {
      logger.info("scoreRepeatExercise : using cached score for " + filePath);
    }
    Double cachedDuration = getFileDuration(wavFile, filePath);

    // actually run the scoring
    JsonObject jsonObject = null;

    List<WordAndProns> possibleProns = new ArrayList<>();
    TransNormDict transNormDict = getHydraDict(sentence, "", possibleProns);

/*    logger.info("scoreRepeatExercise " +
        "\n\tdict          " + transNormDict.getDict() +
        "\n\tpossibleProns " + possibleProns.size()
    );*/

    if (precalcScores != null && precalcScores.isValid()) {
      logger.info("scoreRepeatExercise got valid precalc " + precalcScores);
      Scores scores = precalcScores.getScores();

      if (cached == null) {
        if (DEBUG) possibleProns.forEach(p -> logger.info("\t" + p));
        cached = new HydraOutput(scores, "", "", possibleProns, transNormDict);
      } else {
        cached.setScores(scores);
      }

      jsonObject = precalcScores.getJsonObject();
    }

    int processDur = 0;
    String hydra = getLabel(useKaldi);
    if (cached == null) {
      Path tempDir = null;
      String rawAudioPath = getUniqueRawAudioPath(filePath);
      try {
        tempDir = Files.createTempDirectory("scoreRepeatExercise_" + languageEnum.getLanguage());

        File tempFile = tempDir.toFile();

        // dcodr can't handle an equals in the file name... duh...
        String wavFilePath = filePath + WAV;
//        logger.info("scoreRepeatExercise : sending " + rawAudioPath + " to hydra (derived from " + wavFilePath + ")");
        boolean wroteIt = useKaldi || AudioConversion.wav2raw(wavFilePath, rawAudioPath);

        if (!wroteIt) {
          logAndNotify.logAndNotifyServerException(null, "couldn't write the raw file to " + rawAudioPath);
          return new PretestScore(0).setStatus("couldn't write the file");
        }


        int end = (int) (cachedDuration * 100.0);
        long now = System.currentTimeMillis();

        if (now - then > 10) {
          logger.info("scoreRepeatExercise : prep for " + filePath + " took " + (now - then));
        }

        then = System.currentTimeMillis();

        if (useKaldi) {
          cached = runKaldi(getTokensForKaldi(sentence, transliteration), filePath + WAV1, port, transNormDict);
          if (cached == null || cached.getScores() == null) {
            logger.warn("scoreRepeatExercise kaldi didn't run properly....");
          } else {
            jsonObject = cached.getScores().getKaldiJsonObject();
//            logger.info("scoreRepeatExercise json object " + jsonObject);
          }
        } else {
          cached = runHydra(rawAudioPath, sentence, transliteration, lmSentences,
              tempFile.getAbsolutePath(), decode, end);
        }

        if (cached == null) {
          return new PretestScore(0).setStatus("couldn't run " + hydra + " service?");
        } else {
          processDur = (int) (System.currentTimeMillis() - then);
          if (cached.getScores() != null && cached.getScores().isValid()) {
            if (cached.getWordLab() != null) {
              if (cached.getWordLab().contains("UNKNOWN")) {
                logger.info("scoreRepeatExercise note : hydra result includes UNKNOWNMODEL : " + cached.getWordLab());
              }
              cacheHydraResult(decode, filePath, cached);
            }
          }
          //else {
          //   logger.warn("scoreRepeatExercise skipping invalid response from " +hydra+ ".");
          // }
        }

      } catch (IOException e) {
        logger.error("got " + e, e);
      } finally {
     /*   if (tempDir != null) {
          if (!tempDir.toFile().delete()) {
            logger.info("scoreRepeatExercise couldn't delete " + tempDir); // clean up temp file
          }
        }*/

        if (!useKaldi) {
          cleanUpRawFile(rawAudioPath);
        }
      }
    }

    if (cached == null || cached.getStatus() != SUCCESS) {
      logger.error("scoreRepeatExercise " + hydra +
          " failed to generate scores : " + cached);
      PretestScore pretestScore = new PretestScore(-1f);
      if (cached != null) {
        pretestScore
            .setStatus(cached.getStatus().toString())
            .setMessage(cached.getMessage());
      }
      return pretestScore;
    } else {
      PretestScore pretestScore = getPretestScore(
          imageOutDir,
          imageOptions,
          decode, prefix, noSuffix,
          cached,
          cachedDuration, processDur, usePhoneToDisplay, jsonObject, useKaldi);

//      logger.info("scoreRepeatExercise got " + pretestScore);
      return pretestScore;
    }
  }

  @NotNull
  private String getLabel(boolean useKaldi) {
    return useKaldi ? "kaldi" : "hydra";
  }

  /**
   * http://hydra-dev.llan.ll.mit.edu:5000/score/%7B%22reqid%22:1234,%22request%22:%22decode%22,%22phrase%22:%22%D8%B9%D8%B1%D8%A8%D9%8A%D9%91%22,%22file%22:%22/opt/netprof/bestAudio/msa/bestAudio/2549/regular_1431731290207_by_511_16K.wav%22%7D
   *
   * @param sentence
   * @param audioPath
   * @param port
   * @return
   * @see #scoreRepeatExercise
   */
  private HydraOutput runKaldi(String sentence, String audioPath, int port, TransNormDict transNormDict) {
    try {
      long then = System.currentTimeMillis();
      String json = callKaldi(sentence, audioPath, port);
      long now = System.currentTimeMillis();
      long processDur = now - then;

//      logger.info("runKaldi took " + processDur + " for " + sentence + " on " + audioPath);

      try {
        JsonObject parse = new JsonParser().parse(json).getAsJsonObject();

        STATUS_CODES status = getStatus(parse);
        String log = parse.has(LOG) ? parse.get(LOG).getAsString() : "";
        float score = -1F;

        if (status == SUCCESS) {
          score = parse.get(SCORE).getAsFloat();
          logger.info("runKaldi " +
              "\n\ttook      " + processDur +
              "\n\tdecoding '" + sentence + "'" +
              "\n\tfile      " + audioPath +
              //"\n\tstatus    " + status +
              "\n\tscore " + score);
        } else {
          logger.warn("runKaldi failed " +
              "\n\tstatus " + status +
              "\n\tlog    " + log.trim()
          );
          parse = new JsonObject();
        }

        return new HydraOutput(
            new Scores(score, new HashMap<>(), (int) processDur)
                .setKaldiJsonObject(parse),
            null,
            null,
            getWordAndProns(sentence), transNormDict)
            .setStatus(status)
            .setLog(log);
      } catch (JsonSyntaxException e) {
        logger.error("got unparseable " +
                "\n\tjson " + json +
                "\n\tmesssage " + e.getMessage(),
            e
        );

        return new HydraOutput(
            new Scores(-1F, new HashMap<>(), 0)
                .setKaldiJsonObject(new JsonObject()),
            null,
            null,
            null, transNormDict)
            .setStatus(ERROR)
            .setLog(e.getMessage());
      }

    } catch (Exception e) {
      logger.error("Got " + e, e);
      return new HydraOutput(new Scores(-1F, new HashMap<>(), 0)
          .setKaldiJsonObject(new JsonObject()), null, null, null, transNormDict)
          .setStatus(ERROR)
          .setMessage(e.getMessage())
          .setLog(e.getMessage());
    }
  }

  @NotNull
  private STATUS_CODES getStatus(JsonObject parse) {
    JsonElement status1 = parse.get(STATUS);
    String status = status1 == null ? STATUS_CODES.ERROR.toString() : status1.getAsString();
    try {
      return STATUS_CODES.valueOf(status);
    } catch (IllegalArgumentException e) {
      logger.warn("getStatus : couldn't parse status " + status);
      return STATUS_CODES.ERROR;
    }
  }

  /**
   * @param sentence
   * @param audioPath
   * @param port
   * @return
   * @throws IOException
   */
  private String callKaldi(String sentence, String audioPath, int port) throws IOException {
    HTTPClient httpClient = new HTTPClient();

    String jsonRequest = getKaldiRequest(sentence, audioPath);
    // String s1 = "{\"reqid\":1234,\"request\":\"decode\",\"phrase\":\"عربيّ\",\"file\":\"/opt/netprof/bestAudio/msa/bestAudio/2549/regular_1431731290207_by_511_16K.wav\"}";

    String prefix = getPrefix(port);
    String encode = URLEncoder.encode(jsonRequest, StandardCharsets.UTF_8.name());
    String url = prefix + encode;
    logger.info("runKaldi " +
        "\n\tsentence  " + sentence +
        "\n\taudioPath " + audioPath +
        //"\n\treq       " + encode +
        "\n\traw       " + (prefix + jsonRequest) +
        "\n\tpost      " + url);

    return httpClient.readFromGET(url);
  }

  @NotNull
  private String getPrefix(int port) {
    String localhost = props.useProxy() ? "hydra-dev" : "localhost";
    return "http://" + localhost + ":" + port + "/score/";
  }

  private String getKaldiRequest(String sentence, String audioPath) {
    JsonObject jsonObject = new JsonObject();

//    logger.info("KALDI " +
//        "\n\tsentence  " + sentence +
//        "\n\taudioPath " + audioPath
//    );
    jsonObject.addProperty("reqid", "1234");
    jsonObject.addProperty("request", "decode");
    jsonObject.addProperty("phrase", sentence.trim());
    jsonObject.addProperty("file", audioPath);

    return jsonObject.toString();
  }

  @NotNull
  private List<WordAndProns> getWordAndProns(String sentence) {
    List<WordAndProns> possibleProns = new ArrayList<>();
    getHydraDict(sentence, "", possibleProns);
    return possibleProns;
  }

  /**
   * @param wavFile
   * @param filePath
   * @return
   * @see #scoreRepeatExercise
   */
  @NotNull
  private Double getFileDuration(File wavFile, String filePath) {
    Double cachedDuration = fileToDuration.getIfPresent(filePath);
    if (cachedDuration == null) {
      cachedDuration = new AudioCheck(props.shouldTrimAudio(), props.getMinDynamicRange()).getDurationInSeconds(wavFile);
      fileToDuration.put(filePath, cachedDuration);
      //    logger.info("fileToDur now has "+fileToDuration.size());
    }
    return cachedDuration;
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
       // logger.info("cleanUpRawFile : deleted " + absolutePath);
      } else {
        String mes = "cleanUpRawFile : huh? couldn't delete raw audio rawFile " + absolutePath;
        logger.error(mes);
        logAndNotify.logAndNotifyServerException(null, mes);
      }
    } else {
      logger.info("cleanUpRawFile : source file doesn't exist = " + absolutePath);
    }
  }

  /**
   * @param filePath
   * @return
   * @see #scoreRepeatExercise
   */
  @NotNull
  private String getUniqueRawAudioPath(String filePath) {
    return getRawAudioPath(filePath, hydraReqCounter.getAndIncrement());
  }

  /**
   * Make sure that two requests on the same file don't collide with each other.
   * I.e. don't write to the same file at the same time, or delete the same file on one thread while another is using it.
   *
   * @param filePath
   * @param unique
   * @return
   * @see #scoreRepeatExercise
   * @see #hydraReqCounter
   */
  @NotNull
  private String getRawAudioPath(String filePath, long unique) {
    return filePath.
        replaceAll("//", "/").
        replaceAll("=", "") + "_" + unique + ".raw";
  }

  private void cacheHydraResult(boolean decode, String key, HydraOutput hydraOutput) {
    Cache<String, HydraOutput> stringCache = decode ? decodeAudioToScore : alignAudioToScore;
    stringCache.put(key, hydraOutput);
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
   * @param duration
   * @param processDur
   * @param usePhoneToDisplay
   * @param useKaldi
   * @return
   * @see #scoreRepeatExercise
   */
  private PretestScore getPretestScore(String imageOutDir,
                                       ImageOptions imageOptions,

                                       boolean decode,
                                       String prefix,
                                       String noSuffix,
                                       HydraOutput result,
                                       double duration,
                                       int processDur,
                                       boolean usePhoneToDisplay,
                                       JsonObject jsonObject,
                                       boolean useKaldi) {
    long then = System.currentTimeMillis();
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

// what if we don't want images?
      int imageWidth = imageOptions.getWidth();
      int imageHeight = imageOptions.getHeight();

      boolean reallyUsePhone = usePhoneToDisplay || props.usePhoneToDisplay(languageEnum);
      EventAndFileInfo eventAndFileInfo = jsonObject == null ?
          writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix,
              useScoreForBkgColor,
              prefix1, "", decode, result.getPhoneLab(), result.getWordLab(), true, usePhoneToDisplay, imageOptions.isWriteImages()) :
          writeTranscriptsCached(imageOutDir, imageWidth, imageHeight, noSuffix,
              useScoreForBkgColor,
              prefix1, decode,
              jsonObject, reallyUsePhone, imageOptions.isWriteImages(), useKaldi);

      Map<NetPronImageType, String> sTypeToImage;
      if (eventAndFileInfo == null) {
        logger.warn("getPretestScore huh? no event and file info? ");
        sTypeToImage = Collections.emptyMap();
      } else {
        Map<ImageType, String> typeToFile = eventAndFileInfo.getTypeToFile();
        // logger.info("getPretestScore type to file " + typeToFile);
        sTypeToImage = getTypeToRelativeURLMap(typeToFile);
      }

      Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent =
          eventAndFileInfo == null ? Collections.emptyMap() : eventAndFileInfo.getTypeToEvent();

      Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = generator.getTypeToSegments(typeToEvent, languageEnum);

/*
      logger.info("getPretestScore sTypeToImage" +
          "\n\tsTypeToImage " + sTypeToImage
      );
*/
      if (typeToEndTimes.isEmpty()) {
        logger.warn("getPretestScore huh? no segments from words " + result);// + " phones " + phoneLab);
      }

/*
      logger.info("getPretestScore typeToEndTimes" +
          "\n\ttypeToEndTimes " + typeToEndTimes
      );
*/

      Map<String, String> phoneToDisplay = Collections.emptyMap();
      if (reallyUsePhone && this.phoneToDisplay != null) {
        phoneToDisplay = this.phoneToDisplay;
        // logger.info("using " + phoneToDisplay.size());
      }

      Scores scores = result.getScores();

      if (useKaldi) {
        if (eventAndFileInfo != null) {
          setPhoneSummaryScores(eventAndFileInfo, scores);
        }
      }

      PretestScore pretestScore = new PretestScore(
          scores.hydraScore,
          getPhoneToScore(scores.getEventScores(), phoneToDisplay),
          getWordToScore(scores.getEventScores()),
          sTypeToImage,
          typeToEndTimes,
          getRecoSentence(eventAndFileInfo),
          (float) duration,
          processDur,
          isMatch(result, typeToEndTimes));
      long now = System.currentTimeMillis();

      if (now - then > 10) {
        logger.info("getPretestScore took " + (now - then));
      }
      return pretestScore;
    } catch (Exception e) {
      logger.error("getPretestScore got " + e, e);
      return new PretestScore(-1).setStatus(e.getMessage());
    }
  }

  /**
   * Put these back so we can see them in the result view.
   *
   * @param eventAndFileInfo
   * @param scores
   * @return
   */
  @NotNull
  private void setPhoneSummaryScores(EventAndFileInfo eventAndFileInfo, Scores scores) {
    Map<String, Float> phoneToTotal = new HashMap<>();
    Map<String, Float> phoneToCount = new HashMap<>();

    eventAndFileInfo.getTypeToEvent().forEach((k, v) -> {
      if (k == ImageType.PHONE_TRANSCRIPT) {
        v.values().forEach(transcriptEvent -> {
          String phone = transcriptEvent.getEvent();
          if (keepEvent(phone)) {
            phoneToTotal.put(phone, phoneToTotal.getOrDefault(phone, 0F) + transcriptEvent.getScore());
            phoneToCount.put(phone, phoneToCount.getOrDefault(phone, 0F) + 1F);
          }
        });
      }
    });

    Map<String, Float> value = new HashMap<>();
    scores.getEventScores().put(PHONES, value);

    phoneToTotal.forEach((k, v) -> value.put(k, v / phoneToCount.get(k)));
  }

  private boolean isMatch(HydraOutput result, Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes) {
    List<WordAndProns> recoPhones = getRecoPhones(typeToEndTimes);

    boolean match = result.isMatch(recoPhones);
    if (!match) {
      logger.info("getPretestScore : reco" +
          "\n\tphones " + recoPhones +
          // "\n\tphones " + pron +
          "\n\texpect " + result.getWordAndProns() +
          "\n\tmatch  " + match
      );
    }
    return match;
  }

  /**
   * English dict is upper case for some reason.
   *
   * @param transcript
   * @param transliteration
   * @return
   */
  private String getTokensForKaldi(String transcript, String transliteration) {
    String wordsFromPossibleProns = getTokensFromTranscript(transcript, transliteration);
    return (languageEnum == Language.ENGLISH) ? wordsFromPossibleProns.toUpperCase() : wordsFromPossibleProns;
  }

  @NotNull
  private String getTokensFromTranscript(String transcript, String transliteration) {
    List<WordAndProns> possibleProns = new ArrayList<>();

    // generate dictionary
    getHydraDict(getTranscriptToPost(transcript, false), transliteration, possibleProns);
    return getWordsFromPossibleProns(possibleProns);
  }

  @NotNull
  private String getWordsFromPossibleProns(List<WordAndProns> possibleProns) {
    StringBuilder builder = new StringBuilder();
    possibleProns.forEach(wordAndProns -> builder.append(wordAndProns.getWord()).append(" "));
    return builder.toString().trim();
  }

  /**
   * @param audioPath
   * @param transcript
   * @param lmSentences if multiple alternatives
   * @param tmpDir      for hydra to run in
   * @param decode
   * @param end         frame number of end of file (I think)
   * @return
   * @see #scoreRepeatExercise
   */
  @Override
  public HydraOutput runHydra(String audioPath,
                              String transcript,
                              String transliteration,
                              Collection<String> lmSentences,
                              String tmpDir,
                              boolean decode,
                              int end) {
    // reference trans
    String cleaned = getTranscriptToPost(transcript, decode);
    boolean removeAllPunct = languageEnum != Language.FRENCH;

    List<WordAndProns> possibleProns = new ArrayList<>();

    // generate dictionary
    TransNormDict transNormDict = getHydraDict(cleaned, transliteration, possibleProns);
    String hydraDict = transNormDict.getDict();

    if (DEBUG) {
      logger.info("runHydra : sending " + possibleProns.size());
      possibleProns.forEach(p -> logger.info("\t" + p));
    }

    String smallLM;

    // generate SLF file (if decoding)
    if (decode) {
      String[] slfOut = getDecodeSLF(lmSentences, removeAllPunct);
      smallLM = "[" + slfOut[0] + "]";
      cleaned = getSmallVocabDecoder().lcToken(slfOut[1], removeAllPunct);
    } else {
      smallLM = getSmallLM(cleaned, removeAllPunct);
    }

    String hydraTranscript = getHydraTranscript(cleaned);
    String hydraInput =
        tmpDir + "/:" +
            audioPath + ":" +
            hydraDict + ":" +
            smallLM + ":" +
            "xxx,0," + end + "," +
            "[<s>" + hydraTranscript + "</s>]";

    logger.info("runHydra : sending " + hydraInput);

    long then = System.currentTimeMillis();

    String resultsStr = runHydra(hydraInput, getDcodr());
    if (resultsStr.startsWith("ERROR")) {
      String message = getFailureMessage(audioPath, transcript, lmSentences, decode);
      message = "hydra said " + resultsStr + " : " + message;
      logger.error(message);
      logAndNotify.logAndNotifyServerException(null, message);
      return null;
    } else {
      String[] results = resultsStr.split("\n");
      // 0th entry-overall score and phone scores,
      // 1st entry-word alignments,
      // 2nd entry-phone alignments
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

      logger.info("runHydra " + languageEnum +
          "\n\ttook      " + timeToRunHydra + " millis to run " + (decode ? "decode" : "align") +
          "\n\ton        " + audioPath +
          "\n\tscore     " + split[0] /*+
          "\n\traw reply " + resultsStr*/);

      // clean up tmp directory if above score threshold
    /*if (Float.parseFloat(split[0]) > lowScoreThresholdKeepTempDir) {   // keep really bad scores for now
      try {
				logger.debug("deleting " + tmpDir + " since score is " + split[0]);
				FileUtils.deleteDirectory(new File(tmpDir));
			} catch (IOException e) {
				logger.error("Deleting dir " + tmpDir + " got " +e,e);
			}
		}*/
      return new HydraOutput(scores,
          results[1].replaceAll("#", ""),
          results[2].replaceAll("#", ""),
          possibleProns,
          transNormDict); // where are the # coming from?
    }
  }

  @NotNull
  private String getTranscriptToPost(String transcript, boolean decode) {
    String cleaned = getLCTranscript(transcript);

    if (isAsianLanguage) {
      cleaned = (decode ? UNKNOWN_MODEL + " " : "") + getSegmented(transcript).trim(); // segmentation method will filter out the UNK model

      if (DEBUG) logger.info("getTranscriptToPost for asian language (" + languageEnum + "): " +
          (decode ? "\n\tdecode     " + decode : "") +
          "\n\ttranscript '" + transcript + "'" +
          "\n\tcleaned    '" + cleaned +"'"
      );
    }
/*    else {
      logger.info("runHydra (" + language + ")" +
          "\n\tdecode     " + decode +
          "\n\ttranscript " + transcript +
          "\n\tcleaned    " + cleaned
      );
    }*/
    return cleaned;
  }


  private String getHydraTranscript(String cleaned) {
    return pronunciationLookup.getCleanedTranscript(cleaned);
  }

  @NotNull
  private String getLCTranscript(String transcript) {
    return getSmallVocabDecoder().lcToken(transcript, removeAllAccents).trim();
  }

  @NotNull
  private String getSmallLM(String cleaned, boolean removeAllPunct) {
    return "[" +
        slfFile.createSimpleSLFFile(Collections.singleton(cleaned), ADD_SIL, false, INCLUDE_SELF_SIL_LINK, removeAllPunct)[0]
        +
        "]";
  }

  /**
   * So the main differences between align and decode are:
   * 1) We include the unk model
   * 2) We allow multiple possible sentences
   * <p>
   * ADD SIL right now is TRUE
   * INCLUDE_SELF_LINK is false here, true only for trimming
   *
   * @param lmSentences
   * @param removeAllPunct
   * @return
   */
  private String[] getDecodeSLF(Collection<String> lmSentences, boolean removeAllPunct) {
    return slfFile.createSimpleSLFFile(lmSentences, ADD_SIL, true, INCLUDE_SELF_SIL_LINK, removeAllPunct);
  }

  /**
   * @param cleaned
   * @param transliteration
   * @param possibleProns
   * @return
   * @see #runHydra(String, String, String, Collection, String, boolean, int)
   */
  @Override
  public TransNormDict getHydraDict(String cleaned, String transliteration, List<WordAndProns> possibleProns) {
    return pronunciationLookup.createHydraDict(cleaned, transliteration, possibleProns);
  }

  public List<String> getTokens(String transcript, String transliteration) {
   // logger.info("getTokens for " +transcript);
    String cleaned = getTranscriptToPost(transcript, false);

    List<WordAndProns> possibleProns = new ArrayList<>();

    // generate dictionary
    getHydraDict(cleaned, transliteration, possibleProns);
    List<String> collect = possibleProns.stream().map(WordAndProns::getWord).collect(Collectors.toList());
   // logger.info("getTokens for " +transcript + " = " +collect);
    return collect;
  }

  /**
   * @param transcript
   * @return
   * @see #runHydra(String, String, String, Collection, String, boolean, int)
   */
  public String getSegmented(String transcript) {
    return pronunciationLookup
        .getSmallVocabDecoder()
        .getSegmented(transcript.trim(), removeAllAccents);
  }

  @NotNull
  private HTTPClient getDcodr() {
    return new HTTPClient(getWebserviceIP(), getWebservicePort(), DCODR);
  }

  private String getFailureMessage(String audioPath, String transcript, Collection<String> lmSentences, boolean decode) {
    String input = decode ? lmSentences == null ? "huh? no sentences to decode???" : lmSentences.toString() : transcript;
    String which = decode ? " DECODING " : " ALIGNMENT ";
    return "Failure during running of hydra " +
        "\n\ton   " + audioPath + which +
        "\n\twith " + input +
        "\n\tat   " + new Date();
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
   * @see AlignDecode#getASRScoreForAudio(int, File, Collection, String, DecoderOptions, PrecalcScores)
   */
/*  public String getUsedTokens(Collection<String> lmSentences, List<String> background) {
    return pronunciationLookup.getUsedTokens(lmSentences, background);
  }*/

  /**
   * Make a map of event type to segment end times (so we can map clicks to which segment is clicked on).<br></br>
   * Note we have to adjust the last segment time to be the audio duration, so we can correct for wav vs mp3 time
   * duration differences (mp3 files being typically about 0.1 seconds longer than wav files).
   * The consumer of this map is at {@linkx mitll.langtest.client.scoring.ScoringAudioPanel.TranscriptEventClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)}
   *
   * @return
   * @paramx eventAndFileInfo
   * @see #getPretestScore
   * @see #scoreRepeatExercise
   */

  private Map<String, Float> getPhoneToScore(Map<String, Map<String, Float>> typeToEventToScore, Map<String, String> phoneToDisplay) {
    return getTokenToScore(typeToEventToScore.get(PHONES), true, phoneToDisplay);
  }

  /**
   * @param typeToEventToScore
   * @return
   * @see #getPretestScore
   */
  private Map<String, Float> getWordToScore(Map<String, Map<String, Float>> typeToEventToScore) {
    return getTokenToScore(typeToEventToScore.get(Scores.WORDS), false, Collections.emptyMap());
  }

  private Map<String, Float> getTokenToScore(
      Map<String, Float> phones,
      boolean expecting,
      Map<String, String> phoneToDisplay) {
    if (phones == null) {
      if (expecting) {
        logger.warn("getTokenToScore no phone scores ");//.eventScores);
      }
      return Collections.emptyMap();
    } else {
      Map<String, Float> phoneToScore = new HashMap<>();

//      logger.info("getTokenToScore " + phones.keySet());

      for (Map.Entry<String, Float> phoneScorePair : phones.entrySet()) {
        String key = phoneScorePair.getKey();

        if (!key.equalsIgnoreCase(SIL)) {
          Float value = phoneScorePair.getValue();
          String s = phoneToDisplay.get(key);

          if (s != null) logger.info("getTokenToScore " + key + " = " + s);

          s = s == null ? key : s;
          //   logger.info("getTokenToScore adding '" + key + "' : " + value);
          phoneToScore.put(s, Math.min(1.0f, value));
        }
//        else {
//          // logger.info("getTokenToScore skipping key '" + key + "'");
//        }

      }
      return phoneToScore;
    }
  }

  @Override
  public IPronunciationLookup getPronunciationLookup() {
    return pronunciationLookup;
  }

  /**
   * @param netPronImageTypeListMap
   * @return
   * @see #isMatch
   */
  private List<WordAndProns> getRecoPhones(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    List<TranscriptSegment> words = netPronImageTypeListMap.get(NetPronImageType.WORD_TRANSCRIPT);
    List<TranscriptSegment> phones = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);
    List<WordAndProns> wordAndProns = new ArrayList<>();

    if (words != null) {
      for (TranscriptSegment segment : words) {
        String wordLabel = segment.getEvent();
        if (keepEvent(wordLabel)) {
          StringBuilder builder = new StringBuilder();
          for (TranscriptSegment pseg : phones) {
            if (pseg.getStart() >= segment.getStart() && pseg.getEnd() <= segment.getEnd()) {
              String phoneLabel = pseg.getEvent();
              if (keepEvent(phoneLabel)) {
                builder.append(phoneLabel);
              }
            }
          }
          wordAndProns.add(new WordAndProns(wordLabel, builder.toString()));
        }
      }

    }
    return wordAndProns;
  }

  private boolean keepEvent(String event) {
    return !event.equals(ASR.UNKNOWN_MODEL) && !toSkip.contains(event);
  }
}