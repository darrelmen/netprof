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

import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
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
public abstract class ScoringAudioPanel<T extends HasID> extends AudioPanel<T> {
  //  private Logger logger = Logger.getLogger("ScoringAudioPanel");
  private static final int ANNOTATION_HEIGHT = 20;
  private static final boolean SHOW_SPECTROGRAM = false;

  private final String refSentence;
  private final ClickableTranscript clickableTranscript;
  private int resultID = -1;
  private final String transliteration;
  private MiniScoreListener miniScoreListener;
//  private static final boolean debug = false;

  /**
   * @param refSentence
   * @param playButtonSuffix
   * @param exercise
   * @see ASRScoringAudioPanel#ASRScoringAudioPanel
   */
  ScoringAudioPanel(String refSentence, String transliteration, ExerciseController controller,
                    String playButtonSuffix, T exercise) {
    this(null, refSentence, transliteration, controller, SHOW_SPECTROGRAM, 23,
        playButtonSuffix, exercise, exercise.getID());
  }

  /**
   * @param path
   * @param refSentence
   * @param showSpectrogram
   * @param rightMargin
   * @param playButtonSuffix
   * @param exercise
   * @param exerciseID
   * @see ASRScoringAudioPanel#ASRScoringAudioPanel
   */
  ScoringAudioPanel(String path, String refSentence,
                    String transliteration,
                    ExerciseController controller,
                    boolean showSpectrogram,
                    int rightMargin,
                    String playButtonSuffix,
                    T exercise,
                    int exerciseID) {
    super(path, controller, showSpectrogram, rightMargin, playButtonSuffix, exercise, exerciseID);
    this.refSentence = refSentence;
    this.transliteration = transliteration;
    this.clickableTranscript = new ClickableTranscript(words, phones, controller.getButtonFactory(), exerciseID, playAudio);
  }


  public void addMinicoreListener(MiniScoreListener l) {
    this.miniScoreListener = l;
  }

  /**
   * @param resultID
   * @seex mitll.langtest.client.scoring.GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton#useResult(PretestScore, ImageAndCheck, ImageAndCheck, boolean, String)
   */
  public void setResultID(int resultID) {
    this.resultID = resultID;
  }

  /**
   * @param width
   * @see mitll.langtest.client.scoring.AudioPanel#getImages()
   */
  @Override
  protected void getEachImage(int width) {
    super.getEachImage(width);
    if (controller.hasModel()) {
      getTranscriptImageURLForAudio(audioPath, refSentence, transliteration, width, words, phones);
    }
  }

  /**
   * @param path
   * @param refSentence
   * @param width
   * @param wordTranscript
   * @param phoneTranscript
   * @see #getEachImage
   */
  private void getTranscriptImageURLForAudio(final String path,
                                             String refSentence,
                                             String transliteration,
                                             int width,
                                             final ImageAndCheck wordTranscript,
                                             final ImageAndCheck phoneTranscript) {
    int widthToUse = Math.max(MIN_WIDTH, width);
    // logger.info("getTranscriptImageURLForAudio width " + widthToUse);
    scoreAudio(path, resultID, refSentence, transliteration, wordTranscript, phoneTranscript, widthToUse,
        ANNOTATION_HEIGHT, getReqID("score"));
  }

  protected abstract void scoreAudio(final String path,
                                     int resultID,
                                     String refSentence,
                                     String transliteration,
                                     final ImageAndCheck wordTranscript,
                                     final ImageAndCheck phoneTranscript,
                                     int toUse,
                                     int height,
                                     int reqid);

  private static final String IMAGES_REDX_PNG = LangTest.LANGTEST_IMAGES + "redx.png";

  /**
   * Record the image URLs in the Image widgets and enable the check boxes
   *
   * @param result
   * @param wordTranscript
   * @param phoneTranscript
   * @param scoredBefore
   * @param path
   * @see #scoreAudio
   */
  protected void useResult(PretestScore result,
                           ImageAndCheck wordTranscript,
                           ImageAndCheck phoneTranscript,
                           boolean scoredBefore,
                           String path) {
    Map<NetPronImageType, String> netPronImageTypeStringMap = result.getsTypeToImage();
    {
      String words = netPronImageTypeStringMap.get(NetPronImageType.WORD_TRANSCRIPT);
      if (words != null) {
        showImageAndCheck(words, wordTranscript);
      } else {
        wordTranscript.getImage().setUrl(IMAGES_REDX_PNG);
      }
    }
    {
      String phones = netPronImageTypeStringMap.get(NetPronImageType.PHONE_TRANSCRIPT);
      if (phones != null) {
        showImageAndCheck(phones, phoneTranscript);
      } else {
        phoneTranscript.getImage().setUrl(IMAGES_REDX_PNG);
      }
    }
    if (!scoredBefore && miniScoreListener != null) {
      miniScoreListener.gotScore(result, path);
    }
    clickableTranscript.setScore(result);
  }

  private void showImageAndCheck(String imageURL, ImageAndCheck wordTranscript) {
    wordTranscript.getImage().setUrl(imageURL);
    wordTranscript.getImage().setVisible(true);
  }

  /**
   * @param scores
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addUserRecorder
   */
  void addScores(Collection<CorrectAndScore> scores) {
    for (CorrectAndScore score : scores) {
      miniScoreListener.addScore(score);
    }
  }

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addUserRecorder
   */
  public void showChart() {
    miniScoreListener.showChart(controller.getHost());
  }
}
