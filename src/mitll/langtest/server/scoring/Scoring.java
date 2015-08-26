package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.image.TranscriptReader;
import audio.imagewriter.EventAndFileInfo;
import audio.imagewriter.TranscriptWriter;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.scoring.NetPronImageType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
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

  protected static final float SCORE_SCALAR = 1.0f;
  private static final String SCORING = "scoring";

  protected final String scoringDir;
  protected final String deployPath;
  protected final ServerProperties props;

  /**
   * @param deployPath
   * @see ASRScoring#ASRScoring
   */
  protected Scoring(String deployPath, ServerProperties props) {
    this.deployPath = deployPath;
    this.scoringDir = getScoringDir(deployPath);
    this.props = props;
  }

  private static String getScoringDir(String deployPath) {
    return deployPath + File.separator + SCORING;
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
  protected EventAndFileInfo writeTranscripts(String imageOutDir, int imageWidth, int imageHeight,
                                              String audioFileNoSuffix, boolean useScoreToColorBkg,
                                              String prefix, String suffix, boolean decode,
                                              String phoneLab, String wordLab, boolean useWebservice,
                                              boolean usePhoneToDisplay) {
    logger.debug("writeTranscripts - " + audioFileNoSuffix);

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
    logger.debug("writeTranscripts - decode " + decode + " file " + audioFileNoSuffix + " width " + imageWidth);

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
      return getEventInfo(typeToFile, useWebservice,usePhoneToDisplay || props.usePhoneToDisplay()); // if align, don't use webservice regardless
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
   */
  protected EventAndFileInfo writeTranscriptsCached(String imageOutDir, int imageWidth, int imageHeight,
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
      Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap = parseJson(object, "words", "w", usePhoneToDisplay);

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

  /**
   * TODOx : actually use the parsed json to get transcript info
   *
   * @param jsonObject
   * @param words1
   * @param w1
   * @paramx eventScores
   * @see ASRScoring#getCachedScores
   * @see #writeTranscripts(String, int, int, String, boolean, String, String, boolean, boolean, boolean)
   */
  Map<ImageType, Map<Float, TranscriptEvent>> parseJson(JSONObject jsonObject, String words1, String w1, boolean usePhones) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<ImageType, Map<Float, TranscriptEvent>>();
    SortedMap<Float, TranscriptEvent> wordEvents = new TreeMap<Float, TranscriptEvent>();
    SortedMap<Float, TranscriptEvent> phoneEvents = new TreeMap<Float, TranscriptEvent>();

    typeToEvent.put(ImageType.WORD_TRANSCRIPT, wordEvents);
    typeToEvent.put(ImageType.PHONE_TRANSCRIPT, phoneEvents);

    boolean valid = true;
    if (jsonObject.containsKey(words1)) {
      try {
        JSONArray words = jsonObject.getJSONArray(words1);
        for (int i = 0; i < words.size() && valid; i++) {
          JSONObject word = words.getJSONObject(i);
          if (word.containsKey("str")) {
            objectToEvent(wordEvents, w1, word, false);
            JSONArray phones = word.getJSONArray("phones");
            getPhones(phoneEvents, phones, usePhones);
          } else {
            valid = false;
          }
        }
      } catch (Exception e) {
        logger.debug("no json array at " + words1 + " in " + jsonObject, e);
      }
    }

    return valid ? typeToEvent : new HashMap<>();
  }

  private void getPhones(SortedMap<Float, TranscriptEvent> phoneEvents, JSONArray phones, boolean usePhone) {
    getEventsFromJson(phoneEvents, phones, "p", usePhone);
  }

  private void getEventsFromJson(SortedMap<Float, TranscriptEvent> phoneEvents, JSONArray phones, String tokenKey, boolean usePhone) {
    for (int j = 0; j < phones.size(); j++) {
      JSONObject phone = phones.getJSONObject(j);
      objectToEvent(phoneEvents, tokenKey, phone,usePhone);
    }
  }

  private void objectToEvent(SortedMap<Float, TranscriptEvent> phoneEvents, String tokenKey, JSONObject phone,
                             boolean usePhone) {
    String token  = phone.getString(tokenKey);
    double pscore = phone.getDouble("s");
    double pstart = phone.getDouble("str");
    double pend = phone.getDouble("end");
    if (usePhone) token = props.getDisplayPhoneme(token);

    phoneEvents.put((float) pstart, new TranscriptEvent((float) pstart, (float) pend, token, (float) pscore));
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
  protected Map<NetPronImageType, String> getTypeToRelativeURLMap(Map<ImageType, String> typeToImageFile) {
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
}
