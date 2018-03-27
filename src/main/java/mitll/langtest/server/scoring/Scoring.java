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

import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.server.audio.image.TranscriptReader;
import com.google.gson.JsonObject;
import corpus.EmptyLTS;
import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.audio.imagewriter.EventAndFileInfo;
import mitll.langtest.server.audio.imagewriter.TranscriptWriter;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/12/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Scoring {
  private static final Logger logger = LogManager.getLogger(Scoring.class);

  private static final float SCORE_SCALAR = 1.0f;
  private static final String SCORING = "scoring";
  private static final String MANDARIN = "mandarin";
  private static final String PHONES_LAB = ".phones.lab";
  private static final String WORDS_LAB = ".words.lab";

  private static final String START_SIL = "<s>";
  private static final String END_SIL = "</s>";
  private static final String SIL = "sil";
  private static final String CAP_SIL = "SIL";
  private final Collection<String> toSkip = new HashSet<>(Arrays.asList(START_SIL, END_SIL, SIL, CAP_SIL));

  private final String deployPath;
  final ServerProperties props;
  final LogAndNotify logAndNotify;

  /**
   * @see SLFFile#createSimpleSLFFile(Collection, String, float)
   */
  public static final String SMALL_LM_SLF = "smallLM.slf";

  private final CheckLTS checkLTSHelper;

  final boolean isAsianLanguage;

  private LTSFactory ltsFactory;
  final String language;

  /**
   * @param deployPath
   * @see ASRWebserviceScoring#ASRWebserviceScoring(String, ServerProperties, LogAndNotify, HTKDictionary, Project)
   */
  Scoring(String deployPath,
          ServerProperties props,
          LogAndNotify langTestDatabase,
          HTKDictionary htkDictionary,
          Project project) {
    this.deployPath = deployPath;
    this.props = props;
    this.logAndNotify = langTestDatabase;
    String language = project.getLanguage();
    this.language = language;
    isAsianLanguage = isAsianLanguage(language);
//    if (isAsianLanguage) {
//      logger.warn("using mandarin segmentation.");
//    }
    setLTSFactory();
    checkLTSHelper = new CheckLTS(getLTS(), htkDictionary, language, project.hasModel(), isAsianLanguage);
  }

  private void setLTSFactory() {
    try {
//      logger.debug("\n" + this + " : Factory for " + language);
      ltsFactory = new LTSFactory(language);
    } catch (Exception e) {
      ltsFactory = null;
      logger.error("\n" + this + " : Scoring for " + language + " got " + e);
    }
  }

  private boolean isAsianLanguage(String language) {
    return language.equalsIgnoreCase(MANDARIN) ||
        language.equalsIgnoreCase("Japanese") ||
        language.equalsIgnoreCase("Korean");
  }

  LTS getLTS() {
    return ltsFactory == null ? new EmptyLTS() : ltsFactory.getLTSClass();
  }

  public static String getScoringDir(String deployPath) {
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
   * @param writeImages
   * @return map of image type to image path, suitable using in setURL on a GWT Image (must be relative to deploy location)
   * @see ASRWebserviceScoring#getPretestScore
   */
  EventAndFileInfo writeTranscripts(String imageOutDir, int imageWidth, int imageHeight,
                                    String audioFileNoSuffix, boolean useScoreToColorBkg,
                                    String prefix, String suffix, boolean decode,
                                    String phoneLab, String wordLab, boolean useWebservice,
                                    boolean usePhoneToDisplay, boolean writeImages) {
    // logger.debug("writeTranscripts - " + audioFileNoSuffix + " prefix " + prefix);
    boolean foundATranscript = false;
    // These may not all exist. The speech file is created only by multisv right now.
    String phoneLabFile = prependDeploy(audioFileNoSuffix + PHONES_LAB);
    Map<ImageType, String> typeToFile = new HashMap<>();

    if (phoneLab != null) {
//      logger.debug("phoneLab: " + phoneLab);
      typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLab);
      foundATranscript = true;
    }
    if (wordLab != null) {
      //     logger.debug("wordLab: " + wordLab);
      typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLab);
      foundATranscript = true;
    }

    if (!foundATranscript) {
      logger.error("no label files found, e.g. " + phoneLabFile);
    }

    boolean usePhone = usePhoneToDisplay || props.usePhoneToDisplay();
    if (decode || !writeImages) {  //  skip image generation
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
          usePhone, props.getPhoneToDisplay(language));
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
   * @param object
   * @param writeImages
   * @return
   * @see ASRWebserviceScoring#getPretestScore
   */
  EventAndFileInfo writeTranscriptsCached(String imageOutDir, int imageWidth, int imageHeight,
                                          String audioFileNoSuffix, boolean useScoreToColorBkg,
                                          String prefix,
                                          String suffix,
                                          boolean decode,
                                          boolean useWebservice,
                                          JsonObject object,
                                          boolean usePhoneToDisplay, boolean writeImages) {
    //logger.debug("writeTranscriptsCached " + object);
    if (decode || !writeImages) {  //  skip image generation
      // These may not all exist. The speech file is created only by multisv right now.
      String phoneLabFile = prependDeploy(audioFileNoSuffix + PHONES_LAB);
      Map<ImageType, String> typeToFile = new HashMap<>();
      if (new File(phoneLabFile).exists()) {
        typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLabFile);
      }
      String wordLabFile = prependDeploy(audioFileNoSuffix + WORDS_LAB);
      if (new File(wordLabFile).exists()) {
        typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLabFile);
      }
      // logger.debug("writeTranscriptsCached got " + typeToFile);

      if (typeToFile.isEmpty()) {
        Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap =
            getTypeToTranscriptEvents(object, usePhoneToDisplay);
        return new EventAndFileInfo(typeToFile, imageTypeMapMap);
      } else {
        return getEventInfo(typeToFile, useWebservice, usePhoneToDisplay); // if align, don't use webservice regardless
      }
    } else {
      Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap = getTypeToTranscriptEvents(object, usePhoneToDisplay);

      // logger.info("imageTypeMapMap " + imageTypeMapMap);
      String pathname = audioFileNoSuffix + ".wav";
      pathname = prependDeploy(pathname);
      if (!new File(pathname).exists()) {
        logger.error("writeTranscripts : can't find " + pathname);
        return new EventAndFileInfo();
      }
      imageOutDir = deployPath + File.separator + imageOutDir;
//      logger.info("writeTranscriptsCached " + " writing to " + deployPath + " " + imageOutDir);

      Collection<ImageType> expectedTypes = Arrays.asList(ImageType.PHONE_TRANSCRIPT, ImageType.WORD_TRANSCRIPT);
      return new TranscriptWriter().getEventAndFileInfo(pathname,
          imageOutDir, imageWidth, imageHeight, expectedTypes, SCORE_SCALAR, useScoreToColorBkg, prefix, suffix,
          imageTypeMapMap);
    }
  }

  private Map<ImageType, Map<Float, TranscriptEvent>> getTypeToTranscriptEvents(JsonObject object, boolean usePhoneToDisplay) {
    return
        new ParseResultJson(props, language)
            .readFromJSON(object, "words", "w", usePhoneToDisplay, null);
  }

  /**
   * @param imageTypes
   * @param useWebservice
   * @param usePhoneToDisplay
   * @return
   * @see #writeTranscriptsCached(String, int, int, String, boolean, String, String, boolean, boolean, JsonObject, boolean, boolean)
   */
  // JESS reupdate here
  private EventAndFileInfo getEventInfo(Map<ImageType, String> imageTypes,
                                        boolean useWebservice,
                                        boolean usePhoneToDisplay) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<>();
    try {
      for (Map.Entry<ImageType, String> o : imageTypes.entrySet()) {
        ImageType imageType = o.getKey();
        boolean isPhone = imageType.equals(ImageType.PHONE_TRANSCRIPT) && usePhoneToDisplay;
        TranscriptReader transcriptReader = new TranscriptReader();
        Map<String, String> phoneToDisplay = props.getPhoneToDisplay(language);
        typeToEvent.put(imageType,
            useWebservice ?
                transcriptReader.readEventsFromString(o.getValue(), isPhone, phoneToDisplay) :
                transcriptReader.readEventsFromFile(o.getValue(), isPhone, phoneToDisplay));

      }
      return new EventAndFileInfo(new HashMap<>(), typeToEvent);
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
    Map<NetPronImageType, String> sTypeToImage = new HashMap<>();
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
      pathname = props.getAudioBaseDir() + File.separator + pathname;
    }
    return pathname;
  }


  /**
   * @param fl
   * @param transliteration
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#checkLTSOnForeignPhrase
   * @see mitll.langtest.server.audio.AudioFileHelper#isInDictOrLTS
   */
  public boolean validLTS(String fl, String transliteration) {
    if (fl.isEmpty()) return false;
    Set<String> strings = checkLTSHelper.checkLTS(fl, transliteration);
//    logger.info("validLTS : For " + fl + " got " + strings);
    return strings.isEmpty();
  }

  /**
   * Must be public.
   *
   * @return
   */
  public boolean isDictEmpty() {
    return checkLTSHelper.isDictEmpty();
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#checkLTSAndCountPhones
   */
  public PhoneInfo getBagOfPhones(String foreignLanguagePhrase) {
    return checkLTSHelper.getBagOfPhones(foreignLanguagePhrase);
  }

  public abstract SmallVocabDecoder getSmallVocabDecoder();

  public Collator getCollator() {
    return ltsFactory.getCollator();
  }

  /**
   * Take the events (originally from a .lab file generated in pronz) for WORDS and string them together into a
   * sentence.
   * We might consider defensively sorting the events by time.
   *
   * @param eventAndFileInfo
   * @return
   * @see ASRWebserviceScoring#getPretestScore
   */
  String getRecoSentence(EventAndFileInfo eventAndFileInfo) {
    StringBuilder b = new StringBuilder();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : eventAndFileInfo.typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      if (key == NetPronImageType.WORD_TRANSCRIPT) {
        Map<Float, TranscriptEvent> timeToEvent = typeToEvents.getValue();
        for (Float timeStamp : timeToEvent.keySet()) {
          String event = timeToEvent.get(timeStamp).event;
          String trim = event.trim();
          if (!trim.isEmpty() && !toSkip.contains(event)) {
//            logger.debug("getRecoSentence including " + event + " trim '" + trim + "'");
            b.append(trim);
            b.append(" ");
          } else {
            //          logger.debug("getRecoSentence skipping  " + event + " trim '" + trim + "'");
          }
        }
      }
    }

    return b.toString().trim();
  }

  List<String> getRecoPhones(EventAndFileInfo eventAndFileInfo) {
    List<String> phones = new ArrayList<>();

    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : eventAndFileInfo.typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      if (key == NetPronImageType.PHONE_TRANSCRIPT) {
        Map<Float, TranscriptEvent> timeToEvent = typeToEvents.getValue();
        timeToEvent.values().forEach(transcriptEvent -> phones.add(transcriptEvent.event));
      }
    }
    return phones;
  }
}
