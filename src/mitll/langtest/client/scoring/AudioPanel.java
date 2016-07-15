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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.exercise.Shell;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static mitll.langtest.server.audio.AudioConversion.FILE_MISSING;

/**
 * Does audio playback and fetches and shows various audio images (waveform, spectrogram, etc.) with a red line
 * indicating audio position during playback.<br></br>
 * <p>
 * Subclasses include the capability to record audio and post it to the server.<br></br>
 * <p>
 * On window resize, requests new images compatible with current window size.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class AudioPanel<T extends Shell> extends VerticalPanel implements RequiresResize {
  private final Logger logger = Logger.getLogger("AudioPanel");
  private static final int TRANSCRIPT_IMAGE_HEIGHT = 20;

  private static final int LEFT_COLUMN_WIDTH = PagingContainer.MAX_WIDTH;

  static final int MIN_WIDTH = 256;
  private static final float WAVEFORM_HEIGHT = 80f;//96;
  private static final float SPECTROGRAM_HEIGHT = 50f;//96;
  private static final String WAVEFORM = "Waveform";
  private static final String SPECTROGRAM = "Spectrogram";
  private static final String WAVEFORM_TOOLTIP = "The waveform should only be used to determine when periods of silence" +
      " and speech occur, or whether the mic is working properly.";

  private static final boolean WARN_ABOUT_MISSING_AUDIO = false;
  private static final int WINDOW_SIZE_CHANGE_THRESHOLD = 50;
  private static final int IMAGE_WIDTH_SLOP = 70 + WINDOW_SIZE_CHANGE_THRESHOLD / 2;

  private final ScoreListener gaugePanel;
 // protected final int exerciseID;
  protected String audioPath;
  private final Map<String, Integer> reqs = new HashMap<String, Integer>();
  private int reqid;

  private ImageAndCheck waveform;
  private ImageAndCheck spectrogram;
  ImageAndCheck words;
  ImageAndCheck phones;

  private int lastWidth = 0;
  private AudioPositionPopup audioPositionPopup;
  protected final LangTestDatabaseAsync service;
  protected final SoundManagerAPI soundManager;
  private PlayAudioPanel playAudio;
  private float screenPortion = 1.0f;
  private final boolean logMessages;
  protected final ExerciseController controller;
  private boolean showSpectrogram = false;
  private final int rightMargin;

  protected final T exercise;
  final String instance;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_GET_IMAGES = false;

  /**
   * @param service
   * @param showSpectrogram
   * @param gaugePanel
   * @param rightMargin
   * @param playButtonSuffix
   * @param exercise
   * @param instance
   * @see ScoringAudioPanel#ScoringAudioPanel
   */
  public AudioPanel(String path, LangTestDatabaseAsync service,
                    ExerciseController controller, boolean showSpectrogram, ScoreListener gaugePanel, int rightMargin,
                    String playButtonSuffix,
                    T exercise, String instance) {
    this(service, controller, showSpectrogram, gaugePanel, 1.0f, rightMargin, /*exercise.getID(), */exercise, instance);
    this.audioPath = path;

    addWidgets(playButtonSuffix, "Record");
    if (playAudio != null) {
      controller.register(getPlayButton(), exercise.getID());
    }
  }

  public Button getPlayButton() {
    return hasAudio() ? playAudio.getPlayButton() : null;
  }

  /**
   * @param service
   * @param controller
   * @param showSpectrogram
   * @param gaugePanel
   * @param screenPortion
   * @param rightMargin
   * @paramx exerciseID
   * @param exercise
   * @param instance
   * @see mitll.langtest.client.exercise.RecordAudioPanel#RecordAudioPanel
   */
  protected AudioPanel(LangTestDatabaseAsync service,
                       ExerciseController controller, boolean showSpectrogram, ScoreListener gaugePanel,
                       float screenPortion, int rightMargin,
                       //int exerciseID,
                       T exercise,
                       String instance) {
    this.screenPortion = screenPortion;
    this.soundManager = controller.getSoundManager();
    this.service = service;
    this.logMessages = controller.isLogClientMessages();
    this.controller = controller;
    this.gaugePanel = gaugePanel;
    if (DEBUG) logger.info("AudioPanel : gauge panel " + gaugePanel);
    this.showSpectrogram = showSpectrogram;
    this.rightMargin = rightMargin;
   // this.exerciseID = exerciseID;
    this.exercise = exercise;
    this.instance = instance;
    getElement().setId("AudioPanel_exercise_" + exercise.getID());
  }

  public void onResize() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        getImages();
      }
    });
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @param playButtonSuffix
   * @param recordButtonTitle
   * @return
   * @see mitll.langtest.client.exercise.RecordAudioPanel#RecordAudioPanel
   * @see mitll.langtest.client.scoring.AudioPanel#AudioPanel
   */
  protected void addWidgets(String playButtonSuffix, String recordButtonTitle) {
//    logger.info("AudioPanel.addWidgets " + audioType + " title " + recordButtonTitle +
//        " suffix = " + playButtonSuffix + " has audio " + hasAudio());

    DivWidget divWithRelativePosition = new DivWidget();  // need this for audio position div to work properly
    divWithRelativePosition.getElement().getStyle().setPosition(Style.Position.RELATIVE);
    Panel imageContainer = new VerticalPanel();
    divWithRelativePosition.add(imageContainer);
    imageContainer.getElement().setId("AudioPanel_imageContainer");
   // float totalHeight = getWaveformHeight() + (2 * TRANSCRIPT_IMAGE_HEIGHT);
   // imageContainer.setHeight(totalHeight +"px");
  //  imageContainer.setWidth(getImageWidth()+"px");

    HorizontalPanel hp = new HorizontalPanel();
    hp.setVerticalAlignment(ALIGN_MIDDLE);
    hp.getElement().setId("AudioPanel_hp");

    // add widgets to left of play button
    Widget toTheRightWidget = getAfterPlayWidget();
    audioPositionPopup = new AudioPositionPopup(imageContainer);
    imageContainer.add(audioPositionPopup);

    if (hasAudio()) {
      playAudio = getPlayButtons(toTheRightWidget, playButtonSuffix, recordButtonTitle);
      hp.add(playAudio);
      hp.setCellHorizontalAlignment(playAudio, HorizontalPanel.ALIGN_LEFT);
    } else {
      hp.add(toTheRightWidget);
      audioPositionPopup.setVisible(false);
    }

    waveform = new ImageAndCheck();
    imageContainer.add(getWaveformImage());

    spectrogram = new ImageAndCheck();
    if (showSpectrogram) {
      imageContainer.add(getSpectrogram().getImage());
    }

    words = new ImageAndCheck();
    Image wordsImage = words.getImage();
    imageContainer.add(wordsImage);
    wordsImage.getElement().setId("Transcript_Words");
    wordsImage.setHeight(TRANSCRIPT_IMAGE_HEIGHT +"px");

    phones = new ImageAndCheck();
    Image phonesImage = phones.getImage();
    imageContainer.add(phonesImage);
    phonesImage.setHeight(TRANSCRIPT_IMAGE_HEIGHT +"px");
    phonesImage.getElement().setId("Transcript_Phones");

   // hp.setWidth("100%");

    add(hp);
    hp.addStyleName("bottomFiveMargin");

    add(divWithRelativePosition);
  }

  private Image getWaveformImage() {
    Image waveformImage = getWaveform().getImage();
    waveformImage.getElement().setId("waveformImage");
    // waveformImage.setHeight(getWaveformHeight()+"px");
    waveformImage.setAltText(WAVEFORM_TOOLTIP);
    waveformImage.setTitle(WAVEFORM_TOOLTIP);
    return waveformImage;
  }

  boolean hasAudio() {
    return hasAudio(exercise);
  }

  boolean hasAudio(T exercise) {
    return true;
  }

  public boolean isAudioPathSet() {
    return audioPath != null;
  }

  void doPause() {
    if (playAudio != null) playAudio.doPause();
  }

  /**
   * This is sort of a hack --
   *
   * @return
   */
  protected Widget getAfterPlayWidget() {
    return null;
  }

  @Override
  public void onLoad() {
    if (DEBUG) logger.info("onLoad : id=" + getElement().getId() + " audio path is " + audioPath);
    if (audioPath != null) {
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        public void execute() {
          getImagesForPath(audioPath);
        }
      });
    }
  }

  @Override
  protected void onUnload() {
    audioPositionPopup.reinitialize();
  }

  public void setScreenPortion(float screenPortion) {
    if (DEBUG) logger.info("AudioPanel.setScreenPortion : screenPortion " + screenPortion);
    this.screenPortion = screenPortion;
  }

  public ImageAndCheck getWaveform() {
    return waveform;
  }

  public ImageAndCheck getSpectrogram() {
    return spectrogram;
  }

  public void addPlayListener(PlayListener playListener) {
    if (playAudio != null) {
      playAudio.addPlayListener(playListener);
    }
  }

  public static class ImageAndCheck {
    private final Image image;

    ImageAndCheck() {
      image = new Image();
      getImage().setVisible(false);
    }

    /**
     * @see WaveformPostAudioRecordButton#stopRecording()
     * @param visible
     */
    public void setVisible(boolean visible) {
      getImage().setVisible(visible);
    }

    public boolean isVisible() {
      return getImage().isVisible();
    }

    /**
     * @see WaveformPostAudioRecordButton#stopRecording()
     * @param url
     */
    public void setUrl(String url) {
      getImage().setUrl(url);
      setVisible(true);
    }

    /**
     * @see ScoringAudioPanel#scoreAudio(String, int, String, ImageAndCheck, ImageAndCheck, int, int, int)
     * @return
     */
    public Image getImage() {
      return image;
    }
  }

  /**
   * Ask the server for the URLs to the mp3 for this audio
   *
   * @param path to audio on the server
   * @see #onLoad()
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @see mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#showAudio
   * @see mitll.langtest.client.result.ResultManager#getAsyncTable(int, Widget)
   */
  public String getImagesForPath(String path) {
    path = getPath(path);
    if (DEBUG) logger.info("AudioPanel : " + getElement().getId() + " getImagesForPath " + path);
    if (path != null) {
      this.audioPath = path;
    }
    lastWidth = 0;
    if (playAudio != null) {
      playAudio.startSong(path);
    }

    // wait for rest of page to do layout before asking for the images
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        getImages();
      }
    });

    return path;
  }

  //private CompressedAudio compressedAudio = new CompressedAudio();

  private String getPath(String path) { return CompressedAudio.getPath(path);  }

  /**
   * Note this is currently not very accurate with soundmanager2.
   *
   * @param start from here
   * @param end   to here
   * @paramx waveDurInSeconds
   * @see ScoringAudioPanel.TranscriptEventClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   */
  void playSegment(float start, float end) {
    if (start >= end) {
      logger.warning("bad segment " + start + "-" + end);
    } else {
//      logger.info("playSegment segment " + start + "-" + end);
      playAudio.repeatSegment(start, end);
    }
  }

  /**
   * Adds a listener to the audio panel - this way it can update its position as the audio plays
   *
   * @return PlayAudioPanel
   * @see #addWidgets(String, String)
   */
  private PlayAudioPanel getPlayButtons(Widget toTheRightWidget, String playButtonSuffix, String recordButtonTitle) {
    PlayAudioPanel playAudio = makePlayAudioPanel(toTheRightWidget, playButtonSuffix, recordButtonTitle);
    playAudio.addListener(audioPositionPopup);
    return playAudio;
  }

  protected PlayAudioPanel makePlayAudioPanel(final Widget toTheRightWidget, String buttonTitle,
                                              String recordButtonTitle) {
    return new PlayAudioPanel(soundManager, buttonTitle, toTheRightWidget);
  }

  /**
   * Ask the server for images of the waveform, spectrogram, and transcripts.
   * <p>
   * Complicated -- could be improved -- tries to ask for an image of that would fit horizontally.
   * <p>
   * TODO uses the window width to determine the image width -- ideally we could find out what the width
   * of this component is and then use that.  getOffsetWidth doesn't work well, although perhaps using a parent
   * component's getOffsetWidth would.
   *
   * @see #getImagesForPath(String)
   * @see #onResize()
   */
  private void getImages() {
    //int leftColumnWidth1 = controller.getLeftColumnWidth();
    int width = getImageWidth();

    int diff = Math.abs(Window.getClientWidth() - lastWidth);
    if (lastWidth == 0 || diff > WINDOW_SIZE_CHANGE_THRESHOLD) {
      lastWidth = Window.getClientWidth();

      if (DEBUG_GET_IMAGES) {
        logger.info("\t---> AudioPanel.getImages : offset width " + getOffsetWidth() +
            " request width " + width + " path " + audioPath);
      }
      getEachImage(width);
    } else {
      if (DEBUG_GET_IMAGES) logger.info("\tAudioPanel.getImages : not updating, offset width " +
          getOffsetWidth() + " width " + width + " path " + audioPath + " diff " + diff + " last " + lastWidth);
    }
  }

  private int getImageWidth() {
    int leftColumnWidth = LEFT_COLUMN_WIDTH + IMAGE_WIDTH_SLOP;
    int rightSide = gaugePanel != null ? gaugePanel.getOffsetWidth() : rightMargin;
    if (gaugePanel != null && rightSide == 0 /*&& !controller.getProps().isNoModel()*/) {
      rightSide = 180; // TODO : hack!!!
    }
    return getWidthForWaveform(LEFT_COLUMN_WIDTH, leftColumnWidth, rightSide);
  }

  int getWidthForWaveform(int leftColumnWidth1, int leftColumnWidth, int rightSide) {
    int width = (int) ((screenPortion * ((float) Window.getClientWidth())) - leftColumnWidth) - rightSide;
    int i = width / 5;
    width = (i - 1) * 5;
    width -= 4;
    if (DEBUG_GET_IMAGES) {
      logger.info("AudioPanel.getImages : leftColumnWidth " + leftColumnWidth + "(" + leftColumnWidth1 +
          ") width " + width + " (screen portion = " + screenPortion +
          ") vs window width " + Window.getClientWidth() + " right side " + rightSide);
    }
    return width;
  }

  /**
   * Get images of all types.
   *
   * @param width
   * @see #getImages()
   */
  protected void getEachImage(int width) {
    //logger.info("AudioPanel.getEachImage : " + getElement().getId()+ " path " + audioPath);
    getImageURLForAudio(audioPath, WAVEFORM, width, getWaveform());
    if (showSpectrogram) {
      getImageURLForAudio(audioPath, SPECTROGRAM, width, getSpectrogram());
    }
  }

  /**
   * Worries about responses returning out of order for the same type of image request (as the window is resized)
   * and ignores all but the most recent request.
   * <p>
   * Call to controller getImage checks if the requested image is in the cache before requesting it.
   *
   * @param path
   * @param type
   * @param width
   * @param imageAndCheck
   * @see mitll.langtest.client.LangTest#getImage
   */
  private void getImageURLForAudio(final String path, final String type, int width, final ImageAndCheck imageAndCheck) {
    final int toUse = Math.max(MIN_WIDTH, width);
    float heightForType = type.equals(WAVEFORM) ? getWaveformHeight() : SPECTROGRAM_HEIGHT;
    int height = Math.max(10, (int) (((float) Window.getClientHeight()) / 1200f * heightForType));
    if (path != null && !path.equals(FILE_MISSING)) {
      final long then = System.currentTimeMillis();
//      logger.info("getImageURLForAudio : req " + reqid + " path " + path + " type " + type + " width " + width);
      controller.getImage(getReqID(type), path, type, toUse, height, exercise.getID(), new AsyncCallback<ImageResponse>() {
        public void onFailure(Throwable caught) {
          long now = System.currentTimeMillis();
          logger.info("getImageURLForAudio : (failure) took " + (now - then) + " millis");
          if (!caught.getMessage().trim().equals("0")) {
            // Window.alert("getImageForAudioFile Couldn't contact server. Please check network connection.");
            controller.logMessageOnServer("getImageFailed for " + path + " " + type + " width" + toUse, "onFailure");
          }
          logger.info("message " + caught.getMessage() + " " + caught);
        }

        public void onSuccess(ImageResponse result) {
          long now = System.currentTimeMillis();
          long roundtrip = now - then;

          if (!result.successful) {
            logger.warning("getImageURLForAudio : got error for request for type " + type + " and " +path + " and exid " + exercise.getID());
            if (WARN_ABOUT_MISSING_AUDIO) Window.alert("missing audio file on server " + path);
          } else if (result.req == -1 || isMostRecentRequest(type, result.req)) { // could be cached
            showResult(result, imageAndCheck);
          } else {
            logger.info("\tgetImageURLForAudio : ignoring out of sync response " + result.req + " for " + type);
          }
          if (logMessages) {
            logMessage(result, roundtrip);
          }
        }
      });
    }
  }

  protected float getWaveformHeight() {
    return WAVEFORM_HEIGHT;
  }

  private void showResult(ImageResponse result, final ImageAndCheck imageAndCheck) {
    // logger.info("\tgetImageURLForAudio : using new url " + result.imageURL);
    imageAndCheck.getImage().setUrl(result.imageURL);
    imageAndCheck.getImage().setVisible(true);
    audioPositionPopup.reinitialize(result.durationInSeconds);
  }

  private void logMessage(ImageResponse result, long roundtrip) {
    String message = "getImageURLForAudio : (success) " + result + " took " + roundtrip + " millis, audio dur " +
        (result.durationInSeconds * 1000f) + " millis, " +
        " " + ((float) roundtrip / (float) (result.durationInSeconds * 1000f)) + " roundtrip/audio duration ratio.";
    service.logMessage(message, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  int getReqID(String type) {
    synchronized (this) {
      int current = reqid++;
      reqs.put(type, current);
      return current;
    }
  }

  boolean isMostRecentRequest(String type, int responseReqID) {
    synchronized (this) {
      Integer mostRecentIDForType = reqs.get(type);
      if (mostRecentIDForType == null) {
        logger.warning("huh? couldn't find req " + reqid + " in " + reqs);
        return false;
      } else {
        //logger.info("\tisMostRecentRequest: req for " + type + " = " + mostRecentIDForType + " compared to " + responseReqID);
        return mostRecentIDForType == responseReqID;
      }
    }
  }
}
