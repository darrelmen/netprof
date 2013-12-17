package mitll.langtest.client.scoring;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.List;

/**
 * Asks server to score the audio.  Gets back transcript image URLs, phonem scores and end times.
 * Supports clicking on a phoneme or word and playing that audio.
 * User: GO22670
 * Date: 10/9/12
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ScoringAudioPanel extends AudioPanel {
  private static final int ANNOTATION_HEIGHT = 20;

  private String refSentence;
  private String refAudio;
  private long resultID = -1;
  private ScoreListener scoreListener;
  private PretestScore result;
  private boolean showOnlyOneExercise = false; // true for when called from the headstart website

  /**
   * @see ASRScoringAudioPanel#ASRScoringAudioPanel(mitll.langtest.client.LangTestDatabaseAsync, int, boolean, mitll.langtest.client.exercise.ExerciseController, ScoreListener)
   * @param service
   * @param numRepeats
   * @param useKeyboard
   * @param gaugePanel
   */
  public ScoringAudioPanel(LangTestDatabaseAsync service,
                           int numRepeats, boolean useKeyboard, ExerciseController controller, ScoreListener gaugePanel) {
    this(null, null, service, numRepeats, useKeyboard, controller, true, gaugePanel);
  }

  /**
   * @see ASRScoringAudioPanel#ASRScoringAudioPanel(String, String, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, boolean, boolean, ScoreListener)
   * @param path
   * @param refSentence
   * @param service
   * @param numRepeats
   * @param useKeyboard
   * @param showSpectrogram
   * @param gaugePanel
   */
  public ScoringAudioPanel(String path, String refSentence, LangTestDatabaseAsync service,
                           int numRepeats, boolean useKeyboard, ExerciseController controller,
                           boolean showSpectrogram, ScoreListener gaugePanel) {
    super(path, service, useKeyboard, controller, showSpectrogram, gaugePanel);
    this.refSentence = refSentence;
    showOnlyOneExercise = controller.showOnlyOneExercise();
    addClickHandlers(numRepeats);
  }

  private void addClickHandlers(int numRepeats) {
    this.phones.image.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    this.phones.image.addClickHandler(new TranscriptEventClickHandler(NetPronImageType.PHONE_TRANSCRIPT, numRepeats));
    this.words.image.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    this.words.image.addClickHandler(new TranscriptEventClickHandler(NetPronImageType.WORD_TRANSCRIPT, numRepeats));
  }

  /**
   * @see GoodwaveExercisePanel#getAnswerWidget(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @param l
   */
  public void addScoreListener(ScoreListener l) { this.scoreListener = l;}

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getScoringAudioPanel(mitll.langtest.shared.Exercise, String)
   * @param path
   * @param refSentence
   */
  public void setRefAudio(String path, String refSentence) {
    setRefAudio(path);
    this.refSentence = refSentence;
  }

  public void setRefAudio(String path) {
    this.refAudio = path;
  }

  public void setResultID(long resultID) { this.resultID = resultID;}

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getImages()
   * @param width
   */
  @Override
  protected void getEachImage(int width) {
    super.getEachImage(width);
    if (refAudio != null) {
      getTranscriptImageURLForAudio(audioPath, refAudio, refSentence, width,words,phones);
    }
    else {
      System.out.println("AudioPanel.getImages : no ref audio");
    }
  }

  /**
   *
   * @see #getEachImage(int)
   * @param path
   * @param refSentence
   * @param width
   * @param wordTranscript
   * @param phoneTranscript
   */
  private void getTranscriptImageURLForAudio(final String path, final String refAudio, String refSentence, int width,
                                             final ImageAndCheck wordTranscript,
                                             final ImageAndCheck phoneTranscript) {
    int widthToUse = Math.max(MIN_WIDTH, width);
    int reqid = getReqID("score");
    scoreAudio(path, resultID, refAudio, refSentence, wordTranscript, phoneTranscript, widthToUse, ANNOTATION_HEIGHT, reqid);
  }

  protected abstract void scoreAudio(final String path, long resultID, String refAudio, String refSentence,
                                     final ImageAndCheck wordTranscript, final ImageAndCheck phoneTranscript,
                                     int toUse, int height, int reqid);

  private static final String IMAGES_REDX_PNG  = LangTest.LANGTEST_IMAGES +"redx.png";

  /**
   * Record the image URLs in the Image widgets and enable the check boxes
   * @see ScoringAudioPanel#scoreAudio(String, long, String, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param result
   * @param wordTranscript
   * @param phoneTranscript
   * @param scoredBefore
   */
  protected void useResult(PretestScore result, ImageAndCheck wordTranscript, ImageAndCheck phoneTranscript,
                           boolean scoredBefore) {
    //System.out.println("useResult got " + result);
    if (result.getsTypeToImage().get(NetPronImageType.WORD_TRANSCRIPT) != null) {
      showImageAndCheck(result.getsTypeToImage().get(NetPronImageType.WORD_TRANSCRIPT), wordTranscript);
    }
    else {
      wordTranscript.image.setUrl(IMAGES_REDX_PNG);
    }
    if (result.getsTypeToImage().get(NetPronImageType.PHONE_TRANSCRIPT) != null) {
      showImageAndCheck(result.getsTypeToImage().get(NetPronImageType.PHONE_TRANSCRIPT), phoneTranscript);
    }
    else {
      phoneTranscript.image.setUrl(IMAGES_REDX_PNG);
    }
    if (!scoredBefore && scoreListener != null) {
      System.out.println("new score returned " + result);
      scoreListener.gotScore(result, showOnlyOneExercise);
    }
    this.result = result;
  }

  private void showImageAndCheck(String imageURL, ImageAndCheck wordTranscript) {
    wordTranscript.image.setUrl(imageURL);
    wordTranscript.image.setVisible(true);
    wordTranscript.check.setVisible(true);
  }

  /**
   * Find the start-end time period corresponding to the click on either the phone or the word image and then
   * play the segment. <br></br>
   * NOTE : the duration (or length) of the wav file is usually about 0.1 sec shorter than the
   * the mp3 file (silence padding at the end).<br></br>
   * This means we have to scale the values returned from alignment so the audio times
   * line up with those in the mp3 file.
   * @see #addClickHandlers
   */
  private class TranscriptEventClickHandler implements ClickHandler {
    private final boolean debug = false;
    private final NetPronImageType type;
    private final int numRepeats;

    /**
     * @see mitll.langtest.client.scoring.ScoringAudioPanel#addClickHandlers
     * @param type
     * @param numRepeats
     */
    public TranscriptEventClickHandler(NetPronImageType type, int numRepeats) {
      this.type = type;
      this.numRepeats = numRepeats;
    }

    /**
     * The last transcript event end time is guaranteed to be = the length of the wav audio file.<br></br>
     * First normalize the click location, then scale it to the audio file length, then find which segment
     * it's in by looking for a click that falls between the end time of a candidate segment and the
     * end time of the next segment.  <br></br>
     * Then plays, the segment - note we have to adjust between the duration of a wav file and an mp3 file, which
     * will likely be different. (A little surprising to me, initially.)
     * @see AudioPanel#playSegment
     * @param event
     */
    public void onClick(ClickEvent event) {
      float i = (float) event.getX() / (float) phones.image.getWidth();
      if (result != null) {
        int index = 0;
        List<Float> endTimes = result.getsTypeToEndTimes().get(type);
        float wavFileLengthInSeconds = result.getWavFileLengthInSeconds();//endTimes.get(endTimes.size() - 1);
        float mouseClickTime = wavFileLengthInSeconds * i;
        if (debug) System.out.println("got client at " + event.getX() + " or " + i + " or time " + mouseClickTime +
            " duration " + wavFileLengthInSeconds + " secs or " + wavFileLengthInSeconds * 1000 + " millis");
        for (Float endTime : endTimes) {
          float next = endTimes.get(Math.min(endTimes.size() - 1, index + 1));
          if (mouseClickTime > endTime && mouseClickTime <= next) {
            if (debug) System.out.println("\t playing from " + endTime + " to " + next);

            playSegment(endTime, next, numRepeats);
          }
          index++;
        }
      } else {
        System.err.println("no result for to click against?");
      }
    }
  }
}
