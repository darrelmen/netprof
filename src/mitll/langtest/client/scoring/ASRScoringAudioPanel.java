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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Does ASR scoring -- adds phone and word transcript images below waveform and spectrum
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/9/12
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRScoringAudioPanel<T extends Shell> extends ScoringAudioPanel<T> {
  private Logger logger = Logger.getLogger("ASRScoringAudioPanel");
  private static final String SCORE = "score";
  private final Set<String> tested = new HashSet<String>();
  private boolean useScoreToColorBkg = true;

  /**
   * @see mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#FastAndSlowASRScoringAudioPanel
   * @param refSentence
   * @param gaugePanel
   * @param playButtonSuffix
   * @param exercise
   * @param instance
   */
  ASRScoringAudioPanel(String refSentence,
                              ExerciseController controller,
                              ScoreListener gaugePanel,
                              String playButtonSuffix,
                              T exercise,
                              String instance) {
    super(refSentence, controller, gaugePanel, playButtonSuffix, exercise, instance);
  }

  /**
   * @see mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#FastAndSlowASRScoringAudioPanel
   * @param path
   * @param refSentence
   * @param controller
   * @param showSpectrogram
   * @param gaugePanel
   * @param rightMargin
   * @param playButtonSuffix
   * @param exercise
   * @param instance
   */
  public ASRScoringAudioPanel(String path, String refSentence,
                              ExerciseController controller, boolean showSpectrogram, ScoreListener gaugePanel,
                              int rightMargin, String playButtonSuffix, T exercise, String instance) {
    super(path, refSentence, controller, showSpectrogram, gaugePanel, rightMargin, playButtonSuffix, exercise, instance);
    this.useScoreToColorBkg = controller.useBkgColorForRef();
  }

  public void setShowColor(boolean v) { this.useScoreToColorBkg = v;}

  /**
   * Shows spinning beachball (ish) gif while we wait...
   * @see ScoringAudioPanel#getTranscriptImageURLForAudio
   * @param path to audio file on server
   * @param resultID
   * @param refSentence what should be aligned
   * @param wordTranscript image panel that needs a URL pointing to an image generated on the server
   * @param phoneTranscript image panel that needs a URL pointing to an image generated on the server
   * @param toUse width of images made on serer
   * @param height of images returned
   * @param reqid so if many requests are made quickly and the returns are out of order, we can ignore older requests
   */
  protected void scoreAudio(final String path, int resultID, String refSentence,
                            final ImageAndCheck wordTranscript, final ImageAndCheck phoneTranscript,
                            int toUse, int height, final int reqid) {
    if (path == null) return;
    //System.out.println("scoring audio " + path +" with ref sentence " + refSentence + " reqid " + reqid);
    boolean wasVisible = wordTranscript.isVisible();

    // only show the spinning icon if it's going to take awhile
    final Timer t = new Timer() {
      @Override
      public void run() {
        wordTranscript.setUrl(LangTest.LANGTEST_IMAGES + "animated_progress44.gif");
       // wordTranscript.getImage().setVisible(true);
        phoneTranscript.setVisible(false);
      }
    };

    // Schedule the timer to run once in 1 seconds.
    t.schedule(wasVisible ? 1000 : 1);

    //logger.info("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " type " + "score" + " width " + toUse);

    AsyncCallback<PretestScore> async = new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {
        //if (!caught.getMessage().trim().equals("0")) {
        //  Window.alert("Server error -- couldn't contact server.");
        //}
        //  logger.info("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " failure? "+ caught.getMessage());
        wordTranscript.setVisible(false);
        phoneTranscript.setVisible(false);
      }

      public void onSuccess(PretestScore result) {
        t.cancel();

        if (isMostRecentRequest(SCORE, result.getReqid())) {
          //  logger.info("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " success " + result);
          useResult(result, wordTranscript, phoneTranscript, tested.contains(path), path);
          tested.add(path);
        } else {
          //logger.info("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " success " + result + " DISCARDING : " + reqs);
        }
      }
    };

    int id = exercise.getID();
    LangTestDatabaseAsync service = controller.getService();
    if (controller.getProps().shouldUsePhoneToDisplay()) {
      service.getASRScoreForAudioPhonemes(
          reqid, resultID, path, refSentence, toUse, height, useScoreToColorBkg, id, async);
    } else {
      service.getASRScoreForAudio(
          reqid, resultID, path, refSentence, toUse, height, useScoreToColorBkg, id, async);
    }
  }
}
