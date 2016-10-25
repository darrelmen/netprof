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

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.List;
import java.util.Map;

/**
 * Asks server to score the audio.  Gets back transcript image URLs, phonem scores and end times.
 * Supports clicking on a phoneme or word and playing that audio.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/9/12
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ScoringAudioPanel<T extends Shell> extends AudioPanel<T> {
  // private Logger logger = Logger.getLogger("ScoringAudioPanel");

  private static final int ANNOTATION_HEIGHT = 20;
  private static final boolean SHOW_SPECTROGRAM = false;

  private final String refSentence;
  private long resultID = -1;
  private ScoreListener scoreListener;
  private PretestScore result;
  private boolean showOnlyOneExercise = false; // true for when called from the headstart website
  private static final boolean debug = false;

  /**
   * @param refSentence
   * @param service
   * @param gaugePanel
   * @param playButtonSuffix
   * @param exerciseID
   * @param exercise
   * @param instance
   * @see ASRScoringAudioPanel#ASRScoringAudioPanel(String, LangTestDatabaseAsync, ExerciseController, ScoreListener, String, String, T, String)
   */
  ScoringAudioPanel(String refSentence, LangTestDatabaseAsync service, ExerciseController controller,
                    ScoreListener gaugePanel, String playButtonSuffix, String exerciseID, T exercise, String instance) {
    this(null, refSentence, service, controller, SHOW_SPECTROGRAM, gaugePanel, 23, playButtonSuffix, exerciseID, exercise, instance);
  }

  /**
   * @param path
   * @param refSentence
   * @param service
   * @param showSpectrogram
   * @param gaugePanel
   * @param rightMargin
   * @param playButtonSuffix
   * @param exerciseID
   * @param exercise
   * @param instance
   * @see ASRScoringAudioPanel#ASRScoringAudioPanel(String, String, LangTestDatabaseAsync, ExerciseController, boolean, ScoreListener, int, String, String, T, String)
   */
  ScoringAudioPanel(String path, String refSentence, LangTestDatabaseAsync service,
                    ExerciseController controller,
                    boolean showSpectrogram, ScoreListener gaugePanel, int rightMargin, String playButtonSuffix,
                    String exerciseID, T exercise, String instance) {
    super(path, service, controller, showSpectrogram, gaugePanel, rightMargin, playButtonSuffix,
        controller.getAudioType(), exerciseID, exercise, instance);
    this.refSentence = refSentence;
    showOnlyOneExercise = controller.showOnlyOneExercise();
    addClickHandlers();
  }

  //PopupPanel popupPanel;
  private void addClickHandlers() {
    this.phones.getImage().getElement().getStyle().setCursor(Style.Cursor.POINTER);
    this.phones.getImage().addClickHandler(new TranscriptEventClickHandler(this.phones.getImage(), NetPronImageType.PHONE_TRANSCRIPT));
    final Image image = this.words.getImage();
    image.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    image.addClickHandler(new TranscriptEventClickHandler(image, NetPronImageType.WORD_TRANSCRIPT));

    // TODO come back to this
   /* final PopupPanel popupPanel = new PopupPanel(true);
    popupPanel.add(new Icon(IconType.BULLHORN));

    image.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(final MouseOverEvent event) {
        final int eventXPos = event.getX();


        popupPanel.setPopupPosition(event.getClientX()-10,event.getClientY()-10);
        popupPanel.show();

    *//*    getClickedOnSegment(eventXPos,NetPronImageType.WORD_TRANSCRIPT,new EventSegment() {
          @Override
          public void onSegmentClick(float start, float end) {

            popupPanel.setPopupPosition(event.getScreenX(),event.getScreenY());
            popupPanel.show();
          }
        });*//*
      }
    });
    image.addMouseOutHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        popupPanel.hide();
      }
    });*/
  }

  /**
   * @param l
   * @see GoodwaveExercisePanel#getAnswerWidget
   */
  public void addScoreListener(ScoreListener l) {
    this.scoreListener = l;
  }

  /**
   * @param resultID
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton#useResult(PretestScore, ImageAndCheck, ImageAndCheck, boolean, String)
   */
  public void setResultID(long resultID) {
    this.resultID = resultID;
  }

  /**
   * @param width
   * @see mitll.langtest.client.scoring.AudioPanel#getImages()
   */
  @Override
  protected void getEachImage(int width) {
    super.getEachImage(width);
    if (!controller.getProps().isNoModel()) {
      getTranscriptImageURLForAudio(audioPath, refSentence, width, words, phones);
    }
  }

  /**
   * @param path
   * @param refSentence
   * @param width
   * @param wordTranscript
   * @param phoneTranscript
   * @see #getEachImage(int)
   */
  private void getTranscriptImageURLForAudio(final String path, String refSentence, int width,
                                             final ImageAndCheck wordTranscript,
                                             final ImageAndCheck phoneTranscript) {
    int widthToUse = Math.max(MIN_WIDTH, width);
    scoreAudio(path, resultID, refSentence, wordTranscript, phoneTranscript, widthToUse, ANNOTATION_HEIGHT, getReqID("score"));
  }

  protected abstract void scoreAudio(final String path, long resultID, String refSentence,
                                     final ImageAndCheck wordTranscript, final ImageAndCheck phoneTranscript,
                                     int toUse, int height, int reqid);

  private static final String IMAGES_REDX_PNG = LangTest.LANGTEST_IMAGES + "redx.png";

  /**
   * Record the image URLs in the Image widgets and enable the check boxes
   *
   * @param result
   * @param wordTranscript
   * @param phoneTranscript
   * @param scoredBefore
   * @param path
   * @see ScoringAudioPanel#scoreAudio(String, long, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   */
  void useResult(PretestScore result, ImageAndCheck wordTranscript, ImageAndCheck phoneTranscript,
                 boolean scoredBefore, String path) {
    Map<NetPronImageType, String> netPronImageTypeStringMap = result.getsTypeToImage();
    String words = netPronImageTypeStringMap.get(NetPronImageType.WORD_TRANSCRIPT);
    if (words != null) {
      showImageAndCheck(words, wordTranscript);
    } else {
      wordTranscript.getImage().setUrl(IMAGES_REDX_PNG);
    }
    String phones = netPronImageTypeStringMap.get(NetPronImageType.PHONE_TRANSCRIPT);
    if (phones != null) {
      showImageAndCheck(phones, phoneTranscript);
    } else {
      phoneTranscript.getImage().setUrl(IMAGES_REDX_PNG);
    }
    if (!scoredBefore && scoreListener != null) {
      scoreListener.gotScore(result, showOnlyOneExercise, path);
    }
    this.result = result;
  }

  private void showImageAndCheck(String imageURL, ImageAndCheck wordTranscript) {
    wordTranscript.getImage().setUrl(imageURL);
    wordTranscript.getImage().setVisible(true);
    //if (ADD_CHECKBOX) wordTranscript.getCheck().setVisible(true);
  }

  private void getClickedOnSegment(int eventXPos, NetPronImageType type, EventSegment onClick) {
    //int index = 0;
    List<TranscriptSegment> transcriptSegments = result.getsTypeToEndTimes().get(type);
    float wavFileLengthInSeconds = result.getWavFileLengthInSeconds();//transcriptSegments.get(transcriptSegments.size() - 1);
    float horizOffset = (float) eventXPos / (float) phones.getImage().getWidth();
    float mouseClickTime = wavFileLengthInSeconds * horizOffset;
//    if (debug) System.out.println("got client at " + eventXPos + " or " + horizOffset + " or time " + mouseClickTime +
//      " duration " + wavFileLengthInSeconds + " secs or " + wavFileLengthInSeconds * 1000 + " millis");

    for (TranscriptSegment segment : transcriptSegments) {
      // TranscriptSegment next = transcriptSegments.get(Math.min(transcriptSegments.size() - 1, index + 1));
      if (mouseClickTime > segment.getStart() && mouseClickTime <= segment.getEnd()) {
//        if (debug) System.out.println("\t playing " + segment);
        //   result.getsTypeToEndTimes();
        onClick.onSegmentClick(segment);
        break;
      }
      //index++;
    }
  }

  /**
   * @param score
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addUserRecorder(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, float, mitll.langtest.shared.exercise.T)
   */
  public void addScore(CorrectAndScore score) {
    scoreListener.addScore(score);
  }

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addUserRecorder(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, float, mitll.langtest.shared.exercise.T)
   */
  public void showChart() {
    scoreListener.showChart(showOnlyOneExercise);
  }

  /**
   * @param avgScore
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addUserRecorder(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, float, mitll.langtest.shared.exercise.T)
   */
  public void setClassAvg(float avgScore) {
    scoreListener.setClassAvg(avgScore);
  }

  public void setRefAudio(String refAudio) {
    scoreListener.setRefAudio(refAudio);
  }

  private abstract class MyClickHandler implements ClickHandler, EventSegment {
    final NetPronImageType type;

    private MyClickHandler(NetPronImageType type) {
      this.type = type;
    }

    /**
     * The last transcript event end time is guaranteed to be = the length of the wav audio file.<br></br>
     * First normalize the click location, then scale it to the audio file length, then find which segment
     * it's in by looking for a click that falls between the end time of a candidate segment and the
     * end time of the next segment.  <br></br>
     * Then plays, the segment - note we have to adjust between the duration of a wav file and an mp3 file, which
     * will likely be different. (A little surprising to me, initially.)
     *
     * @param event
     * @see mitll.langtest.client.scoring.AudioPanel#playSegment
     */
    public void onClick(ClickEvent event) {
      if (result != null) {
        int eventXPos = event.getX();

        getClickedOnSegment(eventXPos, type, this);
      } else {
        System.err.println("no result for to click against?");
      }
    }

/*    protected void getClickedOnSegment(MouseEvent<ClickHandler> event) {
      int index = 0;
      List<Float> endTimes = result.getsTypeToEndTimes().get(type);
      float wavFileLengthInSeconds = result.getWavFileLengthInSeconds();//endTimes.get(endTimes.size() - 1);
      float horizOffset = (float) event.getX() / (float) phones.image.getWidth();
      float mouseClickTime = wavFileLengthInSeconds * horizOffset;
      if (debug) System.out.println("got client at " + event.getX() + " or " + horizOffset + " or time " + mouseClickTime +
          " duration " + wavFileLengthInSeconds + " secs or " + wavFileLengthInSeconds * 1000 + " millis");
      for (Float endTime : endTimes) {
        float next = endTimes.get(Math.min(endTimes.size() - 1, index + 1));
        if (mouseClickTime > endTime && mouseClickTime <= next) {
          if (debug) System.out.println("\t playing from " + endTime + " to " + next);

          onSegmentClick(endTime, next);
          break;
        }
        index++;
      }
    }*/

    public abstract void onSegmentClick(TranscriptSegment segment);
  }

  /**
   * Find the start-end time period corresponding to the click on either the phone or the word image and then
   * play the segment. <br></br>
   * NOTE : the duration (or length) of the wav file is usually about 0.1 sec shorter than the
   * the mp3 file (silence padding at the end).<br></br>
   * This means we have to scale the values returned from alignment so the audio times
   * line up with those in the mp3 file.
   *
   * @see #addClickHandlers
   */
  private class TranscriptEventClickHandler extends MyClickHandler {
    final UIObject widget;

    /**
     * @param type
     * @see mitll.langtest.client.scoring.ScoringAudioPanel#addClickHandlers
     */
    public TranscriptEventClickHandler(UIObject widget, NetPronImageType type) {
      super(type);
      this.widget = widget;
    }

    @Override
    public void onSegmentClick(TranscriptSegment segment) {
      //   playSegment(MP3_HEADER_OFFSET+segment.getStart(), MP3_HEADER_OFFSET+segment.getEnd());
      playSegment(segment.getStart(), segment.getEnd());
      long user = (long) controller.getUser();
      controller.getButtonFactory().logEvent(widget, type.toString(), exerciseID, "Clicked on " + segment.getEvent(), user);
    }
  }

  private static interface EventSegment {
    void onSegmentClick(TranscriptSegment segment);
  }
}
