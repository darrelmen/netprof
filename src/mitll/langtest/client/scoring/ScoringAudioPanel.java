package mitll.langtest.client.scoring;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  protected final Set<String> tested = new HashSet<String>();
  private ScoreListener scoreListener;
  private PretestScore result;

  /**
   * @see ASRScoringAudioPanel#ASRScoringAudioPanel(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.sound.SoundManagerAPI, boolean)
   * @param service
   * @param soundManager
   * @param useFullWidth
   */
  public ScoringAudioPanel(LangTestDatabaseAsync service, SoundManagerAPI soundManager, boolean useFullWidth) {
    super(null, service, soundManager, useFullWidth);
    addClickHandlers();
  }

  /**
   * @see ASRScoringAudioPanel
   * @param path
   * @param refSentence
   * @param service
   * @param soundManager
   * @param useFullWidth
   */
  public ScoringAudioPanel(String path, String refSentence, LangTestDatabaseAsync service, SoundManagerAPI soundManager, boolean useFullWidth) {
    super(path, service, soundManager, useFullWidth);
    this.refSentence = refSentence;
    addClickHandlers();
  }

  private void addClickHandlers() {
    this.phones.image.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    this.phones.image.addClickHandler(new TranscriptEventClickHandler(NetPronImageType.PHONE_TRANSCRIPT));
    this.words.image.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    this.words.image.addClickHandler(new TranscriptEventClickHandler(NetPronImageType.WORD_TRANSCRIPT));
  }

  /**
   * @see GoodwaveExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @param l
   */
  public void addScoreListener(ScoreListener l) { this.scoreListener = l;}

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.PostAudioRecordButton#stopRecording()
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

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getImages()
   * @param width
   */
  @Override
  protected void getEachImage(int width) {
    super.getEachImage(width);
    if (refAudio != null) {
      getTranscriptImageURLForAudio(audioPath, refAudio, refSentence, width,words,phones,speech);
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
   * @param speechTranscript
   */
  private void getTranscriptImageURLForAudio(final String path, final String refAudio, String refSentence, int width,
                                             final ImageAndCheck wordTranscript,
                                             final ImageAndCheck phoneTranscript,
                                             final ImageAndCheck speechTranscript) {
    int widthToUse = Math.max(MIN_WIDTH, width);
    int height = ANNOTATION_HEIGHT;

    int reqid = getReqID("score");
    scoreAudio(path, refAudio, refSentence, wordTranscript, phoneTranscript, speechTranscript, widthToUse, height, reqid);
  }

  protected abstract void scoreAudio(final String path, String refAudio, String refSentence,
                                     final ImageAndCheck wordTranscript, final ImageAndCheck phoneTranscript,
                                     final ImageAndCheck speechTranscript, int toUse, int height, int reqid);

  /**
   * Record the image URLs in the Image widgets and enable the check boxes
   * @see ASRScoringAudioPanel#scoreAudio(String, String, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param result
   * @param wordTranscript
   * @param phoneTranscript
   * @param speechTranscript
   * @param scoredBefore
   */
  protected void useResult(PretestScore result, ImageAndCheck wordTranscript, ImageAndCheck phoneTranscript,
                           ImageAndCheck speechTranscript, boolean scoredBefore) {
    //System.out.println("useResult got " + result);
    if (result.getsTypeToImage().get(NetPronImageType.WORD_TRANSCRIPT) != null) {
      showImageAndCheck(result.getsTypeToImage().get(NetPronImageType.WORD_TRANSCRIPT), wordTranscript);
    }
    if (result.getsTypeToImage().get(NetPronImageType.PHONE_TRANSCRIPT) != null) {
      showImageAndCheck(result.getsTypeToImage().get(NetPronImageType.PHONE_TRANSCRIPT), phoneTranscript);
    }
    if (result.getsTypeToImage().get(NetPronImageType.SPEECH_TRANSCRIPT) != null) {
      showImageAndCheck(result.getsTypeToImage().get(NetPronImageType.SPEECH_TRANSCRIPT), speechTranscript);
    }
    if (!scoredBefore && scoreListener != null) {
      System.out.println("new score returned " + result);
      scoreListener.gotScore(result);
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
    boolean debug = false;
    private NetPronImageType type;

    public TranscriptEventClickHandler(NetPronImageType type) {
      this.type = type;
    }

    /**
     * The last transcript event end time is guaranteed to be = the length of the wav audio file.<br></br>
     * First normalize the click location, then scale it to the audio file length, then find which segment
     * it's in by looking for a click that falls between the end time of a candidate segment and the
     * end time of the next segment.  <br></br>
     * Then plays, the segment - note we have to adjust between the duration of a wav file and an mp3 file, which
     * will likely be different. (A little surprising to me, initially.)
     * @see ScoringAudioPanel#playSegment(float, float, float)
     * @param event
     */
    public void onClick(ClickEvent event) {
      float i = (float) event.getX() / (float) phones.image.getWidth();
      if (result != null) {
        int index = 0;
        List<Float> endTimes = result.getsTypeToEndTimes().get(type);
        float wavFileLengthInSeconds = endTimes.get(endTimes.size() - 1);
        float mouseClickTime = wavFileLengthInSeconds * i;
        if (debug) System.out.println("got client at " + event.getX() + " or " + i + " or time " + mouseClickTime +
            " duration " + wavFileLengthInSeconds + " secs or " + wavFileLengthInSeconds * 1000 + " millis");
        for (Float endTime : endTimes) {
          float next = endTimes.get(Math.min(endTimes.size() - 1, index + 1));
          if (mouseClickTime > endTime && mouseClickTime <= next) {
            if (debug) System.out.println("\t playing from " + endTime + " to " + next);

            playSegment(endTime, next, wavFileLengthInSeconds);
          }
          index++;
        }
      } else {
        System.err.println("no result for to click against?");
      }
    }
  }
}
