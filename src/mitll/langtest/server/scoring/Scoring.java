package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.image.TranscriptReader;
import audio.imagewriter.EventAndFileInfo;
import audio.imagewriter.TranscriptWriter;
import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.scoring.NetPronImageType;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.*;

/**
 * Base class for the two types of scoring : DTW (Dynamic Time Warping, using "sv") and ASR (Speech Recognition).
 * <p>
 * Scoring (for now) goes through a thin layer in pronz (scala) to run a config file in hydec.
 * <p>
 * TODO : In the future, we may want to make a new project that has just the hydec interface code in pronz.
 * <p>
 * User: go22670
 * Date: 9/12/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Scoring {
  private static final Logger logger = Logger.getLogger(Scoring.class);

  private static final float SCORE_SCALAR = 1.0f;
  private static final String SCORING = "scoring";

  final String scoringDir;
  final String deployPath;
  final ServerProperties props;
  final LogAndNotify langTestDatabase;

  private static final double KEEP_THRESHOLD = 0.3;

  static final int FOREGROUND_VOCAB_LIMIT = 100;
  static final int VOCAB_SIZE_LIMIT = 200;

  /**
   * @see SLFFile#createSimpleSLFFile(Collection, String, float)
   */
  public static final String SMALL_LM_SLF = "smallLM.slf";

  private static SmallVocabDecoder svDecoderHelper = null;
  private final CheckLTS checkLTSHelper;
  final SmallVocabDecoder svd = new SmallVocabDecoder();

  /**
   * By keeping these here, we ensure that we only ever read the dictionary once
   */
  HTKDictionary htkDictionary;
  final LTS letterToSoundClass;
  final ConfigFileCreator configFileCreator;
  final boolean isMandarin;

  /**
   * Normally we delete the tmp dir created by hydec, but if something went wrong, we want to keep it around.
   * If the score was below a threshold, or the magic -1, we keep it around for future study.
   */
  double lowScoreThresholdKeepTempDir = KEEP_THRESHOLD;
  private final LTSFactory ltsFactory;
  final String languageProperty;

  /**
   * @param deployPath
   * @see ASRScoring#ASRScoring
   */
  Scoring(String deployPath, ServerProperties props, LogAndNotify langTestDatabase) {
    this.deployPath = deployPath;
    this.scoringDir = getScoringDir(deployPath);
    this.props = props;
    this.langTestDatabase = langTestDatabase;

    // logger.debug("Creating ASRScoring object");
    lowScoreThresholdKeepTempDir = KEEP_THRESHOLD;

    Map<String, String> properties = props.getProperties();
    languageProperty = properties.get("language");
    String language = languageProperty != null ? languageProperty : "";

    isMandarin = language.equalsIgnoreCase("mandarin");
    if (isMandarin) logger.warn("using mandarin segmentation.");
    ltsFactory = new LTSFactory(languageProperty);
    this.letterToSoundClass = ltsFactory.getLTSClass(language);

    this.configFileCreator = new ConfigFileCreator(properties, letterToSoundClass, scoringDir);

    readDictionary();
    makeDecoder();
    checkLTSHelper = new CheckLTS(letterToSoundClass, htkDictionary, language);
  }

  private static String getScoringDir(String deployPath) {
    return deployPath + File.separator + SCORING;
  }

  /**
   * For chinese, maybe later other languages.
   *
   * @param longPhrase
   * @return
   * @seex AutoCRT#getRefs
   * @see ASRScoring#getScoreForAudio
   */
  public static String getSegmented(String longPhrase) {
    Collection<String> tokens = svDecoderHelper.getTokens(longPhrase);
/*    System.err.println("got '" + longPhrase +
        "' -> '" +tokens +
        "'");*/
    StringBuilder builder = new StringBuilder();
    for (String token : tokens) {
      builder.append(svDecoderHelper.segmentation(token.trim()));
      builder.append(" ");
    }
    return builder.toString();
  }

  /**
   * Given an audio file without a suffix, check if there are label files, and if so, for each one,
   * write out a transcript image file.  Write them to the imageOutDir, and use the specified width and height.
   *
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param audioFileNoSuffix
   * @param useScoreToColorBkg
   * @param prefix
   * @param suffix
   * @param decode             if true don't bother to write out images for word and phone
   * @return map of image type to image path, suitable using in setURL on a GWT Image (must be relative to deploy location)
   * @see ASRScoring#scoreRepeatExercise
   */
  EventAndFileInfo writeTranscripts(String imageOutDir, int imageWidth, int imageHeight,
                                    String audioFileNoSuffix, boolean useScoreToColorBkg,
                                    String prefix, String suffix, boolean decode,
                                    String phoneLab, String wordLab, boolean useWebservice,
                                    boolean usePhoneToDisplay) {
    // logger.debug("writeTranscripts - " + audioFileNoSuffix + " prefix " + prefix);
    boolean foundATranscript = false;
    // These may not all exist. The speech file is created only by multisv right now.
    String phoneLabFile = prependDeploy(audioFileNoSuffix + ".phones.lab");
    Map<ImageType, String> typeToFile = new HashMap<ImageType, String>();

    if (phoneLab != null) {
      logger.debug("phoneLab: " + phoneLab);
      typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLab);
      foundATranscript = true;
    }
    if (wordLab != null) {
      logger.debug("wordLab: " + wordLab);
      typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLab);
      foundATranscript = true;
    }

    if (!foundATranscript) {
      logger.error("no label files found, e.g. " + phoneLabFile);
    }

    boolean usePhone = usePhoneToDisplay || props.usePhoneToDisplay();
    if (decode || imageWidth < 0) {  // hack to skip image generation
      return getEventInfo(typeToFile, useWebservice, usePhone);
    } else {
      String pathname = audioFileNoSuffix + ".wav";
      pathname = prependDeploy(pathname);
      if (!new File(pathname).exists()) {
        logger.error("writeTranscripts : can't find " + pathname);
        return new EventAndFileInfo();
      }
      imageOutDir = deployPath + File.separator + imageOutDir;

      return new TranscriptWriter().writeTranscripts(pathname,
          imageOutDir, imageWidth, imageHeight, typeToFile, SCORE_SCALAR, useScoreToColorBkg, prefix, suffix, useWebservice,
          usePhone, props.getPhoneToDisplay());
    }
  }

  /**
   * Parse the .lab files that are put into the audio directory as a side effect of alignment/decoding.
   *
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param audioFileNoSuffix
   * @param useScoreToColorBkg
   * @param prefix
   * @param suffix
   * @param decode
   * @param useWebservice
   * @param usePhoneToDisplay
   * @return
   * @see ASRScoring#getPretestScore
   */
  EventAndFileInfo writeTranscripts(String imageOutDir, int imageWidth, int imageHeight,
                                    String audioFileNoSuffix, boolean useScoreToColorBkg,
                                    String prefix, String suffix, boolean decode, boolean useWebservice,
                                    boolean usePhoneToDisplay) {
    logger.debug("writeTranscripts - decode " + decode + " file " + audioFileNoSuffix + " width " + imageWidth + " prefix " + prefix);

    boolean foundATranscript = false;
    // These may not all exist. The speech file is created only by multisv right now.
    String phoneLabFile = prependDeploy(audioFileNoSuffix + ".phones.lab");
    Map<ImageType, String> typeToFile = new HashMap<ImageType, String>();
    if (new File(phoneLabFile).exists()) {
      typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLabFile);
      foundATranscript = true;
    } else {
      logger.warn("no phones " + phoneLabFile);
    }

    String wordLabFile = prependDeploy(audioFileNoSuffix + ".words.lab");
    if (new File(wordLabFile).exists()) {
      typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLabFile);
      foundATranscript = true;
    } else {
      logger.warn("no words " + wordLabFile);
    }

    if (!foundATranscript) {
      logger.error("no label files found, e.g. " + phoneLabFile);
    }

    // logger.debug("typeToFile " + typeToFile);

    if (decode || imageWidth < 0) {  // hack to skip image generation
      return getEventInfo(typeToFile, useWebservice, usePhoneToDisplay || props.usePhoneToDisplay()); // if align, don't use webservice regardless
    } else {
      String pathname = audioFileNoSuffix + ".wav";
      pathname = prependDeploy(pathname);
      if (!new File(pathname).exists()) {
        logger.error("writeTranscripts : can't find " + pathname);
        return new EventAndFileInfo();
      }
      imageOutDir = deployPath + File.separator + imageOutDir;
      return new TranscriptWriter().writeTranscripts(pathname,
          imageOutDir, imageWidth, imageHeight, typeToFile, SCORE_SCALAR, useScoreToColorBkg, prefix, suffix,
          useWebservice,
          usePhoneToDisplay || props.usePhoneToDisplay(),
          props.getPhoneToDisplay());
    }
  }

  /**
   * TODO : actually use the json to
   *
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param audioFileNoSuffix
   * @param useScoreToColorBkg
   * @param prefix
   * @param suffix
   * @param decode
   * @param useWebservice
   * @param object             TODO Actually use it
   * @return
   * @see ASRScoring#getPretestScore(String, int, int, boolean, boolean, String, String, File, Scores, JSONObject, boolean)
   */
  EventAndFileInfo writeTranscriptsCached(String imageOutDir, int imageWidth, int imageHeight,
                                          String audioFileNoSuffix, boolean useScoreToColorBkg,
                                          String prefix, String suffix, boolean decode, boolean useWebservice,
                                          JSONObject object,
                                          boolean usePhoneToDisplay) {
    // logger.debug("writeTranscriptsCached " + object);
    if (decode || imageWidth < 0) {  // hack to skip image generation
      // These may not all exist. The speech file is created only by multisv right now.
      String phoneLabFile = prependDeploy(audioFileNoSuffix + ".phones.lab");
      Map<ImageType, String> typeToFile = new HashMap<ImageType, String>();
      if (new File(phoneLabFile).exists()) {
        typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLabFile);
      }
      String wordLabFile = prependDeploy(audioFileNoSuffix + ".words.lab");
      if (new File(wordLabFile).exists()) {
        typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLabFile);
      }
      return getEventInfo(typeToFile, useWebservice, usePhoneToDisplay); // if align, don't use webservice regardless
    } else {
      Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap = getTypeToTranscriptEvents(object, usePhoneToDisplay);

      String pathname = audioFileNoSuffix + ".wav";
      pathname = prependDeploy(pathname);
      if (!new File(pathname).exists()) {
        logger.error("writeTranscripts : can't find " + pathname);
        return new EventAndFileInfo();
      }
      imageOutDir = deployPath + File.separator + imageOutDir;

      Collection<ImageType> expectedTypes = Arrays.asList(ImageType.PHONE_TRANSCRIPT, ImageType.WORD_TRANSCRIPT);
      return new TranscriptWriter().getEventAndFileInfo(pathname,
          imageOutDir, imageWidth, imageHeight, expectedTypes, SCORE_SCALAR, useScoreToColorBkg, prefix, suffix, imageTypeMapMap);
    }
  }

  private Map<ImageType, Map<Float, TranscriptEvent>> getTypeToTranscriptEvents(JSONObject object, boolean usePhoneToDisplay) {
    return new ParseResultJson(props).parseJson(object, "words", "w", usePhoneToDisplay);
  }

  // JESS reupdate here
  private EventAndFileInfo getEventInfo(Map<ImageType, String> imageTypes, boolean useWebservice,
                                        boolean usePhoneToDisplay) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<ImageType, Map<Float, TranscriptEvent>>();
    try {
      for (Map.Entry<ImageType, String> o : imageTypes.entrySet()) {
        ImageType imageType = o.getKey();
        boolean isPhone = imageType.equals(ImageType.PHONE_TRANSCRIPT) && usePhoneToDisplay;
        TranscriptReader transcriptReader = new TranscriptReader();
        typeToEvent.put(imageType, useWebservice ? transcriptReader.readEventsFromString(o.getValue(), isPhone, props.getPhoneToDisplay()) :
            transcriptReader.readEventsFromFile(o.getValue(), isPhone, props.getPhoneToDisplay()));

      }
      return new EventAndFileInfo(new HashMap<ImageType, String>(), typeToEvent);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Make sure the paths for each image are relative (don't include the deploy path prefix) and have
   * the slashes going in the right direction.<br></br>
   * I.e. make valid URLs.
   *
   * @param typeToImageFile
   * @return map of image type to URL
   */
  Map<NetPronImageType, String> getTypeToRelativeURLMap(Map<ImageType, String> typeToImageFile) {
    Map<NetPronImageType, String> sTypeToImage = new HashMap<NetPronImageType, String>();
    if (typeToImageFile == null) {
      logger.error("huh? typeToImageFile is null?");
    } else {
      for (Map.Entry<ImageType, String> kv : typeToImageFile.entrySet()) {
        String name = kv.getKey().toString();
        NetPronImageType key = NetPronImageType.valueOf(name);
        String filePath = kv.getValue();
        if (filePath.startsWith(deployPath)) {
          filePath = filePath.substring(deployPath.length()); // make it a relative path
        } else {
          logger.error("expecting image " + filePath + "\tto be under " + deployPath);
        }

        filePath = filePath.replaceAll("\\\\", "/");
        if (filePath.startsWith("/")) {
          //System.out.println("removing initial slash from " + filePath);
          filePath = filePath.substring(1);
        }
        sTypeToImage.put(key, filePath);
      }
    }
    return sTypeToImage;
  }

  private String prependDeploy(String pathname) {
    if (!new File(pathname).exists()) {
      pathname = deployPath + File.separator + pathname;
    }
    return pathname;
  }

  private void makeDecoder() {
    if (svDecoderHelper == null && htkDictionary != null) {
      svDecoderHelper = new SmallVocabDecoder(htkDictionary);
    }
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#inDictOrLTS(String)
   */
  Set<String> checkLTS(String foreignLanguagePhrase) {
    return checkLTSHelper.checkLTS(foreignLanguagePhrase);
  }

  public boolean validLTS(String fl) {
    return checkLTS(fl).isEmpty();
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#checkLTS
   */
  public ASR.PhoneInfo getBagOfPhones(String foreignLanguagePhrase) {
    return checkLTSHelper.getBagOfPhones(foreignLanguagePhrase);
  }

  /**
   * @see #ASRScoring
   */
  private void readDictionary() {
    htkDictionary = makeDict();
    //logger.info(this + " dict now " + htkDictionary);
  }

  /**
   * @return
   * @see #readDictionary()
   */
  private HTKDictionary makeDict() {
    String dictFile = configFileCreator.getDictFile();
    if (new File(dictFile).exists()) {
      long then = System.currentTimeMillis();
      HTKDictionary htkDictionary = new HTKDictionary(dictFile);
      long now = System.currentTimeMillis();
      int size = htkDictionary.size(); // force read from lazy val
      if (now - then > 300) {
        logger.info("for " + languageProperty + " read dict " + dictFile + " of size " + size + " took " + (now - then) + " millis");
      }
      return htkDictionary;
    } else {
      logger.error("\n\n\n---> makeDict : Can't find dict file at " + dictFile);
      return new HTKDictionary();
    }
  }

  public SmallVocabDecoder getSmallVocabDecoder() {
    return svDecoderHelper;
  }

  /**
   * @param toSort
   * @param <T>
   * @see ASR#sort(List)
   */
  public <T extends CommonExercise> void sort(List<T> toSort) {
    ltsFactory.sort(toSort);
  }

  public Collator getCollator() {
    return ltsFactory.getCollator();
  }
}
