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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.image.ImageResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static mitll.langtest.server.audio.AudioConversion.FILE_MISSING;


public class AudioPanel<T extends HasID> extends DivWidget implements RequiresResize {
  private final Logger logger = Logger.getLogger("AudioPanel");

  private static final String RECORD = "Record";
  private static final int TRANSCRIPT_IMAGE_HEIGHT = 22;

  static final int LEFT_COLUMN_WIDTH = SimplePagingContainer.MAX_WIDTH;

  static final int MIN_WIDTH = 256;
  private static final float WAVEFORM_HEIGHT = 60F;//70F;//80f;//96;
  private static final float SPECTROGRAM_HEIGHT = 50f;//96;
  private static final String WAVEFORM = "Waveform";
  private static final String SPECTROGRAM = "Spectrogram";
  private static final String WAVEFORM_TOOLTIP = "The waveform should only be used to determine when periods of silence" +
      " and speech occur, or whether the mic is working properly.";

  private static final boolean WARN_ABOUT_MISSING_AUDIO = false;
  private static final int WINDOW_SIZE_CHANGE_THRESHOLD = 50;
  static final int IMAGE_WIDTH_SLOP = 70 + WINDOW_SIZE_CHANGE_THRESHOLD / 2;

  protected String audioPath = null;
  private final Map<String, Integer> reqs = new HashMap<>();
  private int reqid;

  private ImageAndCheck waveform;
  private ImageAndCheck spectrogram;
  /**
   * @see #addWidgets(String, String)
   */
  ImageAndCheck words;
  ImageAndCheck phones;

  private int lastWidth = 0;
  private int lastWidthOuter = 0;
  private AudioPositionPopup audioPositionPopup;

  PlayAudioPanel playAudio;

  private final boolean logMessages;
  protected final ExerciseController controller;
  private boolean showSpectrogram;
  private final int rightMargin;

  protected final T exercise;

  private final int exerciseID;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_GET_IMAGES = false;

  /**
   * @param showSpectrogram
   * @param rightMargin
   * @param playButtonSuffix
   * @param exercise
   * @param exerciseID
   * @see ScoringAudioPanel#ScoringAudioPanel
   */
  public AudioPanel(String path,
                    ExerciseController controller,
                    boolean showSpectrogram,
                    int rightMargin,
                    String playButtonSuffix,
                    T exercise,
                    int exerciseID) {
    this(controller, showSpectrogram, rightMargin, exercise, exerciseID);
    this.audioPath = path;

   // logger.info("\n\n\nbutton title is " + RECORD);
    addWidgets(playButtonSuffix, RECORD);
    if (playAudio != null) {
      if (exercise == null) {
        // logger.info("hmm exercise is null for " + instance + " and " + exerciseID);
      } else {
        controller.register(getPlayButton(), exercise.getID());
      }
    }
  }

  public Widget getPlayButton() {
    return playAudio.getPlayButton();
  }

  /**
   * @param controller
   * @param showSpectrogram
   * @param rightMargin
   * @param exercise
   * @param exerciseID
   * @paramx exerciseID
   * @see mitll.langtest.client.exercise.RecordAudioPanel#RecordAudioPanel
   */
  protected AudioPanel(ExerciseController controller,
                       boolean showSpectrogram,
                       int rightMargin,
                       T exercise,
                       int exerciseID) {
    this.logMessages = controller.isLogClientMessages();
    this.controller = controller;
    this.showSpectrogram = showSpectrogram;
    this.rightMargin = rightMargin;
    this.exerciseID = exerciseID;
    this.exercise = exercise;
    getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

    int id = getExerciseID();
    getElement().setId("AudioPanel_exercise_" + id);

    int width = getImageWidth();
    //  logger.info("AudioPanel " + getElement().getId() + " width " + width);
    setWidth(width + "px");
  }

  public void onResize() {
    Scheduler.get().scheduleDeferred(() -> {
      int images = getImages();
//        logger.info(getElement().getID() + " gotResize " + images);
      if (images != 0) {
        int diff = Math.abs(Window.getClientWidth() - lastWidthOuter);
        if (lastWidthOuter == 0 || diff > WINDOW_SIZE_CHANGE_THRESHOLD) {
          lastWidthOuter = Window.getClientWidth();

          //  logger.info("width is " + images);
          setWidth(images + "px");
        }
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
   * @see #AudioPanel
   */
  protected void addWidgets(String playButtonSuffix, String recordButtonTitle) {
//    logger.info("AudioPanel.addWidgets " + audioType + " title " + recordButtonTitle +
//        " suffix = " + playButtonSuffix + " has audio " + hasAudioNonContext());

    DivWidget divWithRelativePosition = new DivWidget();  // need this for audio position div to work properly
    divWithRelativePosition.getElement().getStyle().setPosition(Style.Position.RELATIVE);
    divWithRelativePosition.addStyleName("floatLeft");

    Panel imageContainer = new VerticalPanel();
    divWithRelativePosition.add(imageContainer);
    imageContainer.getElement().setId("AudioPanel_imageContainer");

    int heightForTranscripts = rightMargin > 0 ? 2 * TRANSCRIPT_IMAGE_HEIGHT : 0;
    float totalHeight = getScaledImageHeight(WAVEFORM) + heightForTranscripts + 6;
    imageContainer.setHeight(totalHeight + "px");
    //  imageContainer.setWidth(getImageWidth()+"px");

    Panel hp = new DivWidget();
    // hp.setVerticalAlignment(ALIGN_MIDDLE);
    hp.getElement().setId("AudioPanel_hp");
    hp.addStyleName("floatLeft");
    // add widgets to left of play button
    Widget toTheRightWidget = getAfterPlayWidget();
    audioPositionPopup = new AudioPositionPopup(imageContainer);
    imageContainer.add(audioPositionPopup);


    playAudio = getPlayButtons(toTheRightWidget, playButtonSuffix, recordButtonTitle, exercise);
    hp.add(playAudio);
    playAudio.addStyleName("floatLeftAndClear");
    //hp.setCellHorizontalAlignment(playAudio, HorizontalPanel.ALIGN_LEFT);
//    } else {
//      hp.add(toTheRightWidget);
//      audioPositionPopup.setVisible(false);
//    }

    waveform = new ImageAndCheck();
    imageContainer.add(getWaveformImage());

    spectrogram = new ImageAndCheck();
    if (showSpectrogram) {
      imageContainer.add(getSpectrogram().getImage());
    }

    {
      words = new ImageAndCheck();
      Image wordsImage = words.getImage();
      imageContainer.add(wordsImage);
      wordsImage.getElement().setId("Transcript_Words");
    }
    //   wordsImage.setHeight(TRANSCRIPT_IMAGE_HEIGHT + "px");

    {
      phones = new ImageAndCheck();
      Image phonesImage = phones.getImage();
      imageContainer.add(phonesImage);
      // for some reason this totally screws up max width for transcript images ????
      // phonesImage.setHeight(TRANSCRIPT_IMAGE_HEIGHT + "px");
      phonesImage.getElement().setId("Transcript_Phones");
    }
    // hp.setWidth("100%");

    add(hp);
    hp.addStyleName("bottomFiveMargin");

    add(divWithRelativePosition);
  }

  private Image getWaveformImage() {
    Image waveformImage = getWaveform().getImage();
    waveformImage.getElement().setId("waveformImage");
    //  waveformImage.setAltText(WAVEFORM_TOOLTIP);
    waveformImage.setTitle(WAVEFORM_TOOLTIP);
    return waveformImage;
  }

  public boolean isAudioPathSet() {
    return audioPath != null;
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
      Scheduler.get().scheduleDeferred(() -> getImagesForPath(audioPath));
    }
  }

  @Override
  protected void onUnload() {
    audioPositionPopup.reinitialize();
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
     * @param visible
     * @see RecordButton.RecordingListener#stopRecording(long, boolean)
     */
    public void setVisible(boolean visible) {
      getImage().setVisible(visible);
    }

    boolean isVisible() {
      return getImage().isVisible();
    }

    /**
     * @param url
     * @see RecordButton.RecordingListener#stopRecording(long, boolean)
     */
    public void setUrl(String url) {
      getImage().setUrl(url);
      setVisible(true);
    }

    /**
     * @return
     * @see ScoringAudioPanel#scoreAudio
     * @see ASRScoringAudioPanel#scoreAudio
     */
    public Image getImage() {
      return image;
    }
  }

  /**
   * Ask the server for the URLs to the mp3 for this audio
   *
   * @param path to audio on the server
   * @seex mitll.langtest.client.scoring.ASRRecordAudioPanel.MyPostAudioRecordButton#useResult
   * @see #onLoad()
   * @see RecordButton.RecordingListener#stopRecording(long, boolean)
   * @see mitll.langtest.client.result.ResultManager#getAsyncTable
   */
  public String getImagesForPath(String path) {
    path = getReadyToPlayAudio(path);

    // wait for rest of page to do layout before asking for the images
    Scheduler.get().scheduleDeferred(this::getImages);

    return path;
  }

  private String getReadyToPlayAudio(String path) {
    path = getPath(path);
    if (DEBUG) {
      logger.info("getReadyToPlayAudio : " + getElement().getId() + " getImagesForPath " + path);
    }

    if (path != null) {
      this.audioPath = path;
    }
    lastWidth = 0;
    if (playAudio != null) {
      playAudio.startSong(path, true);
    }
    return path;
  }

  private String getPath(String path) {
    return CompressedAudio.getPath(path);
  }

  /**
   * Note this is currently not very accurate with soundmanager2.
   *
   * @param start from here
   * @param end   to here
   * @paramx waveDurInSeconds
   * @seex ScoringAudioPanel.TranscriptEventClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   */
/*
  void playSegment(float start, float end) {
    if (start >= end) {
      logger.warning("bad segment " + start + "-" + end);
    } else {
//      logger.info("playSegment segment " + start + "-" + end);
      loadAndPlayOrPlayAudio.loadAndPlaySegment(start, end);
    }
  }
*/

  /**
   * Adds a listener to the audio panel - this way it can update its position as the audio plays
   *
   * @return PlayAudioPanel
   * @see #addWidgets(String, String)
   */
  private PlayAudioPanel getPlayButtons(Widget toTheRightWidget, String playButtonSuffix, String recordButtonTitle, HasID exercise) {
    PlayAudioPanel playAudio = makePlayAudioPanel(toTheRightWidget, playButtonSuffix, recordButtonTitle, exercise);
    playAudio.setListener(audioPositionPopup);
    return playAudio;
  }

  protected PlayAudioPanel makePlayAudioPanel(final Widget toTheRightWidget, String buttonTitle,
                                              String recordButtonTitle, HasID exercise) {
    return new PlayAudioPanel(buttonTitle, toTheRightWidget, false, controller, getExerciseID(), true);
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
  private int getImages() {
    int width = getImageWidth();

    int diff = Math.abs(Window.getClientWidth() - lastWidth);
    if (lastWidth == 0 || diff > WINDOW_SIZE_CHANGE_THRESHOLD) {
      lastWidth = Window.getClientWidth();

      if (DEBUG_GET_IMAGES) {
        logger.info("\t---> AudioPanel.getImages : offset width " + getOffsetWidth() +
            " request width " + width + " path " + audioPath);
      }
      getEachImage(width);
      return width;
    } else {
      if (DEBUG_GET_IMAGES) logger.info("\tAudioPanel.getImages : not updating, offset width " +
          getOffsetWidth() + " width " + width + " path " + audioPath + " diff " + diff + " last " + lastWidth);
      return 0;
    }
  }

  protected int getImageWidth() {
    try {
      int leftColumnWidth = LEFT_COLUMN_WIDTH + IMAGE_WIDTH_SLOP;
      int widthForWaveform = getWidthForWaveform(LEFT_COLUMN_WIDTH, leftColumnWidth);

      //  logger.info("left " + leftColumnWidth + " width " + widthForWaveform);

      return Math.min(770, widthForWaveform);//, rightSide);
    } catch (Exception e) {
      // OK, ignore it
      return 200;
    }
  }

  /**
   * @param leftColumnWidth1
   * @param leftColumnWidth
   * @return
   * @paramx rightSide
   */
  int getWidthForWaveform(int leftColumnWidth1, int leftColumnWidth) {
    int width = (int) ((((float) Window.getClientWidth())) - leftColumnWidth);// - rightSide;
    int i = width / 5;
    width = (i - 1) * 5;
    width -= 4;
    if (DEBUG_GET_IMAGES) {
      logger.info("AudioPanel.getImages : leftColumnWidth " + leftColumnWidth + "(" + leftColumnWidth1 +
              ") width " + width +
              " (" +
//              "screen portion = " + screenPortion +
              ") vs window width " + Window.getClientWidth()
          //+ " right side " + rightSide
      );
    }
    return width;
  }

  /**
   * Get images of all types.
   *
   * @param width
   * @see #getImages()
   */
  void getEachImage(int width) {
    // logger.info("AudioPanel.getEachImage : " + getElement().getId()+ " path " + audioPath + " width " +width);
    if (audioPath == null || audioPath.isEmpty()) {
      logger.warning("getEachImage audio path is " + audioPath);

//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception());
//      logger.info("logException stack " + exceptionAsString);
    } else {
      getImageURLForAudio(audioPath, WAVEFORM, width, getWaveform());
      if (showSpectrogram) {
        getImageURLForAudio(audioPath, SPECTROGRAM, width, getSpectrogram());
      }
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
   * @see AudioPanel#getEachImage
   * @see mitll.langtest.client.LangTest#getImage
   */
  private void getImageURLForAudio(final String path, final String type, int width, final ImageAndCheck imageAndCheck) {
    if (path != null && !path.equals(FILE_MISSING)) {
      final long then = System.currentTimeMillis();

   //   logger.info("getImageURLForAudio : req " + reqid + " path " + path + " type " + type + " width " + width);

      final int toUse = Math.max(MIN_WIDTH, width);
      int height = getScaledImageHeight(type);
      final int id = getExerciseID();
      controller.getImage(getReqID(type), path, type, toUse, height, id, new AsyncCallback<ImageResponse>() {
        public void onFailure(Throwable caught) {
          long now = System.currentTimeMillis();
          logger.info("getImageURLForAudio : (failure) took " + (now - then) + " millis");
          if (!caught.getMessage().trim().equals("0")) {
            // Window.alert("getImageForAudioFile Couldn't contact server. Please check network connection.");
            controller.logMessageOnServer("getImageFailed for " + path + " " + type + " width" + toUse, "onFailure", true);
          }
          logger.info("message " + caught.getMessage() + " " + caught);
          controller.handleNonFatalError("getting image", caught);
        }

        public void onSuccess(ImageResponse result) {
//          logger.info("getImageURLForAudio : result " + result);
          long now = System.currentTimeMillis();
          long roundtrip = now - then;

          if (!result.isSuccessful()) {
            logger.warning("getImageURLForAudio : got error for request for type " + type + " and " + path + " and exid " + id);
            waveform.setUrl(LangTest.RED_X_URL.asString());
            if (WARN_ABOUT_MISSING_AUDIO) Window.alert("missing audio file on server " + path);
          } else if (result.getReq() == -1 || isMostRecentRequest(type, result.getReq())) { // could be cached
            showResult(result, imageAndCheck);
          } else {
            logger.info("\tgetImageURLForAudio : ignoring out of sync response " + result.getReq() + " for " + type);
          }
          if (logMessages) {
            logMessage(result, roundtrip);
          }
        }
      });
    }
  }

  int getExerciseID() {
    return exercise == null ? exerciseID : exercise.getID();
  }

  protected int getScaledImageHeight(String type) {
    float heightForType = type.equals(WAVEFORM) ? getWaveformHeight() : SPECTROGRAM_HEIGHT;
    return Math.max(10, (int) (((float) Window.getClientHeight()) / 1200f * heightForType));
  }

  protected float getWaveformHeight() {
    return WAVEFORM_HEIGHT;
  }

  private void showResult(ImageResponse result, final ImageAndCheck imageAndCheck) {
    // logger.info("\tgetImageURLForAudio : using new url " + result.imageURL);
    imageAndCheck.getImage().setUrl(result.getImageURL());
    imageAndCheck.getImage().setVisible(true);
    audioPositionPopup.reinitialize(result.getDurationInSeconds());
  }

  private void logMessage(ImageResponse result, long roundtrip) {
    String message = "getImageURLForAudio : (success) " + result + " took " + roundtrip + " millis, audio dur " +
        (result.getDurationInSeconds() * 1000f) + " millis, " +
        " " + ((float) roundtrip / (float) (result.getDurationInSeconds() * 1000f)) + " roundtrip/audio duration ratio.";
    controller.getService().logMessage(message, false, new AsyncCallback<Void>() {
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
