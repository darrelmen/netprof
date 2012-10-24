package mitll.langtest.client.scoring;

import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
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

  public ScoringAudioPanel(LangTestDatabaseAsync service, SoundManagerAPI soundManager, boolean useFullWidth) {
    super(null, service, soundManager, useFullWidth);
  }
  public ScoringAudioPanel(String path, String refSentence, LangTestDatabaseAsync service, SoundManagerAPI soundManager, boolean useFullWidth) {
    super(path, service, soundManager, useFullWidth);
    this.refSentence = refSentence;
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
    this.refAudio = path;
    this.refSentence = refSentence;
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
      wordTranscript.image.setUrl(result.getsTypeToImage().get(NetPronImageType.WORD_TRANSCRIPT));
      wordTranscript.image.setVisible(true);
      wordTranscript.check.setVisible(true);
    }
    if (result.getsTypeToImage().get(NetPronImageType.PHONE_TRANSCRIPT) != null) {
      phoneTranscript.image.setUrl(result.getsTypeToImage().get(NetPronImageType.PHONE_TRANSCRIPT));
      phoneTranscript.image.setVisible(true);
      phoneTranscript.check.setVisible(true);
    }
    if (result.getsTypeToImage().get(NetPronImageType.SPEECH_TRANSCRIPT) != null) {
      speechTranscript.image.setUrl(result.getsTypeToImage().get(NetPronImageType.SPEECH_TRANSCRIPT));
      speechTranscript.image.setVisible(true);
      speechTranscript.check.setVisible(true);
    }
    if (!scoredBefore && scoreListener != null) {
      // System.out.println("new score returned " + result);
      scoreListener.gotScore(result);
    }
  }
}
