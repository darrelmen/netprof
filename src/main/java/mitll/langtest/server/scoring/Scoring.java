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

import com.google.gson.JsonObject;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.server.audio.imagewriter.EventAndFileInfo;
import mitll.langtest.server.audio.imagewriter.TranscriptWriter;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.npdata.dao.lts.EmptyLTS;
import mitll.npdata.dao.lts.HTKDictionary;
import mitll.npdata.dao.lts.LTS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.Collator;
import java.util.*;

public abstract class Scoring {
  private static final Logger logger = LogManager.getLogger(Scoring.class);

  private static final float SCORE_SCALAR = 1.0f;
  private static final String SCORING = "scoring";
  private static final String MANDARIN = "mandarin";
  private static final String PHONES_LAB = ".phones.lab";
  // private static final String WORDS_LAB = ".words.lab";

  private static final String START_SIL = "<s>";
  private static final String END_SIL = "</s>";
  private static final String SIL = "sil";
  private static final String CAP_SIL = "SIL";

  private static final String JAPANESE = "Japanese";
  public static final String KOREAN = "Korean";

  final Collection<String> toSkip = new HashSet<>(Arrays.asList(START_SIL, END_SIL, SIL, CAP_SIL));

  private final String deployPath;
  final ServerProperties props;
  final LogAndNotify logAndNotify;

  /**
   * @see SLFFile#createSimpleSLFFile
   */
//  public static final String SMALL_LM_SLF = "smallLM.slf";

  private final CheckLTS checkLTSHelper;

  final boolean isAsianLanguage;
  final boolean removeAllAccents;

  private LTSFactory ltsFactory;
  final Language languageEnum;
//  private Map<String, String> phoneToDisplay;

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
    this.languageEnum = project.getLanguageEnum();
    removeAllAccents = languageEnum != Language.FRENCH;
    isAsianLanguage = isAsianLanguage(languageEnum);
    //   phoneToDisplay = props.getPhoneToDisplay(languageEnum);

//    logger.info("isAsian " + isAsianLanguage + " lang " + language);
//    if (isAsianLanguage) {
//      logger.warn("using mandarin segmentation.");
//    }
    setLTSFactory();
    checkLTSHelper = new CheckLTS(getLTS(), htkDictionary, project.getLanguageEnum(), project.hasModel(), isAsianLanguage);
  }

  private void setLTSFactory() {
    try {
//      logger.debug("\n" + this + " : Factory for " + language);
      ltsFactory = new LTSFactory(languageEnum);
    } catch (Exception e) {
      ltsFactory = null;
      logger.error("\n" + this + " : Scoring for " + languageEnum + " got " + e);
    }
  }

  private boolean isAsianLanguage(Language language) {
    return language == Language.MANDARIN ||
        language == Language.JAPANESE
//        ||
//        language.equalsIgnoreCase(KOREAN)
        ;
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
   * @param writeImages
   * @return map of image type to image path, suitable using in setURL on a GWT Image (must be relative to deploy location)
   * @see ASRWebserviceScoring#getPretestScore
   */
  EventAndFileInfo writeTranscripts(String imageOutDir, int imageWidth, int imageHeight,
                                    String audioFileNoSuffix, boolean useScoreToColorBkg,
                                    String prefix, String suffix,
                                    String phoneLab, String wordLab,
                                    boolean usePhoneToDisplay, boolean writeImages) {
    // logger.info("writeTranscripts - " + audioFileNoSuffix + " prefix " + prefix);
    Map<ImageType, String> typeToFile = getTypeToFile(audioFileNoSuffix, phoneLab, wordLab);

    boolean usePhone = usePhoneToDisplay || props.usePhoneToDisplay(languageEnum);
    if (writeImages) {
      String pathname = getAudioPath(audioFileNoSuffix);
      if (!new File(pathname).exists()) {
        logger.error("writeTranscripts : can't find " + pathname);
        return new EventAndFileInfo();
      }

      return new TranscriptWriter().writeTranscripts(pathname,
          deployPath + File.separator + imageOutDir, imageWidth, imageHeight, typeToFile, SCORE_SCALAR, useScoreToColorBkg, prefix, suffix,
          usePhone, Collections.emptyMap());
    } else { //  skip image generation
      return new EventAndFileInfo(Collections.emptyMap(), new TranscriptWriter().getImageTypeMapMap(typeToFile, usePhone, Collections.emptyMap()));
    }
  }

  @NotNull
  private Map<ImageType, String> getTypeToFile(String audioFileNoSuffix, String phoneLab, String wordLab) {
    boolean foundATranscript = false;
    // These may not all exist. The speech file is created only by multisv right now.
    Map<ImageType, String> typeToFile = new HashMap<>();

    if (phoneLab != null) {
      //  logger.info("phoneLab: " + phoneLab);
      typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLab);
      foundATranscript = true;
    }
    if (wordLab != null) {
      //  logger.info("wordLab: " + wordLab);
      typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLab);
      foundATranscript = true;
    }

    if (!foundATranscript) {
      String phoneLabFile = prependDeploy(audioFileNoSuffix + PHONES_LAB);
      logger.error("no label files found, e.g. " + phoneLabFile);
    }
    return typeToFile;
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
   * @param object
   * @param writeImages
   * @return
   * @see ASRWebserviceScoring#getPretestScore
   */
  EventAndFileInfo writeTranscriptsCached(String imageOutDir,
                                          int imageWidth, int imageHeight,
                                          String audioFileNoSuffix, boolean useScoreToColorBkg,
                                          String prefix,
                                          JsonObject object,
                                          boolean usePhoneToDisplay,
                                          boolean writeImages,
                                          boolean useKaldi) {
    if (writeImages) {
      Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap =
          getTypeToTranscriptEvents(object, usePhoneToDisplay, useKaldi);
      String pathname = getAudioPath(audioFileNoSuffix);
      if (!new File(pathname).exists()) {
        logger.error("writeTranscripts : can't find " + pathname);
        return new EventAndFileInfo();
      }
      imageOutDir = deployPath + File.separator + imageOutDir;
//      logger.info("writeTranscriptsCached " + " writing to " + deployPath + " " + imageOutDir);
      Collection<ImageType> expectedTypes = Arrays.asList(ImageType.PHONE_TRANSCRIPT, ImageType.WORD_TRANSCRIPT);
      return new TranscriptWriter().getEventAndFileInfo(pathname,
          imageOutDir, imageWidth, imageHeight, expectedTypes, SCORE_SCALAR, useScoreToColorBkg, prefix, "",
          imageTypeMapMap);
    } else {
      return new EventAndFileInfo(Collections.emptyMap(), getTypeToTranscriptEvents(object, usePhoneToDisplay, useKaldi));
    }
  }

  private String getAudioPath(String audioFileNoSuffix) {
    String pathname = audioFileNoSuffix + ".wav";
    pathname = prependDeploy(pathname);
    return pathname;
  }

  private Map<ImageType, Map<Float, TranscriptEvent>> getTypeToTranscriptEvents(JsonObject object,
                                                                                boolean usePhoneToDisplay,
                                                                                boolean useKaldi) {
    String words = useKaldi ? "word_align" : "words";
    String w = useKaldi ? "word" : "w";

    return
        new ParseResultJson(props, languageEnum)
            .readFromJSON(object, words, w, usePhoneToDisplay, null, useKaldi);
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
   * @seex mitll.langtest.server.audio.AudioFileHelper#checkLTSOnForeignPhrase
   * @seex mitll.langtest.server.audio.AudioFileHelper#isInDictOrLTS
   */
  public boolean validLTS(String fl, String transliteration) {
    if (fl.isEmpty()) {
      return false;
    } else {
      Collection<String> oovForFL = getOOV(fl, transliteration);
      return oovForFL.isEmpty();
    }
  }

  /**
   * @see AudioFileHelper#isValidForeignPhrase(Set, Set, CommonExercise, Set, boolean)
   * @see AudioFileHelper#checkLTSOnForeignPhrase(String, String)
   *
   * @param fl
   * @param transliteration
   * @return
   */
  @NotNull
  public Collection<String> getOOV(String fl, String transliteration) {
    Set<String> oovForFL = checkLTSHelper.checkLTS(fl, transliteration);

 /*   List<String> inOrder = new ArrayList<>(oovForFL);

    //if (oov.addAll(oovForFL)) {
    // logger.info("validLTS : For " + fl + " got " + oovForFL + " now " + oov.size() + " set = " + oov.hashCode());
    //}

    Iterator<String> iterator = inOrder.iterator();

    while (iterator.hasNext()) {
      String oov = iterator.next();
      try {
        Integer.parseInt(oov);
        iterator.remove();
      } catch (NumberFormatException e) {
        // ok not an int
      }
    }*/
    return oovForFL;
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

  /**
   * @return
   * @see AudioFileHelper#getCollator
   */
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
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = eventAndFileInfo.getTypeToEvent();
    if (typeToEvent == null) logger.warn("no type to event in " + eventAndFileInfo);
    else {
      for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
        NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
        if (key == NetPronImageType.WORD_TRANSCRIPT) {
          Map<Float, TranscriptEvent> timeToEvent = typeToEvents.getValue();
          for (Float timeStamp : timeToEvent.keySet()) {
            String event = timeToEvent.get(timeStamp).getEvent();
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
    }

    return b.toString().trim();
  }

}
