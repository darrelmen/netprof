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

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.MyCustomIconType;
import mitll.langtest.client.sound.SoundFeedback;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/1/13
 * Time: 6:02 PM
 * To change this template use File | Settings | File Templates.
 */
class ScoreFeedback {
  private IconAnchor feedbackImage;
  private Panel scoreFeedbackColumn;
  private final ProgressBar scoreFeedback = new ProgressBar();

  private SimplePanel feedbackDummyPanel;
  private final boolean useWhite;
  private ExerciseController controller;

  public ScoreFeedback(boolean useWhite) {
    this.useWhite = useWhite;
  }

  /**
   * Holds the pron score feedback.
   * Initially made with a placeholder.
   *
   * @return
   * @see mitll.langtest.client.flashcard.TextResponse#addWidgets
   */
  public FluidRow getScoreFeedbackRow(int height, ExerciseController controller) {
    this.controller = controller;

    FluidRow feedbackRow = new FluidRow();
    feedbackDummyPanel = new SimplePanel();
    feedbackDummyPanel.setHeight(height + "px");
    scoreFeedbackColumn = new SimplePanel(feedbackDummyPanel);

    feedbackRow.add(scoreFeedbackColumn);
    feedbackRow.getElement().setId("ScoreFeedback_feedbackRow");
    return feedbackRow;
  }

  /**
   * @param left
   * @param height
   * @return
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#addScoreFeedback
   */
  public Panel getSimpleRow(Widget left, int height) {
    Panel feedbackRow = new FlowPanel();
    feedbackRow.add(left);
    feedbackDummyPanel = new SimplePanel();
    feedbackDummyPanel.setHeight(height + "px");
    feedbackDummyPanel.addStyleName("floatLeft");

    scoreFeedbackColumn = new SimplePanel(feedbackDummyPanel);
    return feedbackRow;
  }

  public void setWaiting() {
    feedbackImage.setVisible(true);
    feedbackImage.setBaseIcon(MyCustomIconType.waiting);
  }

  public void hideWaiting() {
    feedbackImage.setBaseIcon(useWhite ? MyCustomIconType.white : MyCustomIconType.gray);
  }

  /**
   * @paramx result
   * @paramx controller
   * @paramx width
   * @see mitll.langtest.client.flashcard.TextResponse#getScoreForGuess
   * @see mitll.langtest.client.flashcard.PressAndHoldExercisePanel#getAnswerWidget
   */
  public void showCRTFeedback() {
    playCorrect();
    hideWaiting();
  }

  /**
   * @see mitll.langtest.client.flashcard.TextResponse#getScoreForGuess
   * @see mitll.langtest.client.flashcard.PressAndHoldExercisePanel#getAnswerWidget
   * @param controller
   * @paramx width
   */
  public void showCRTFeedback(/*, int width*/ExerciseController controller) {
    this.controller = controller;
  }

  private MySoundFeedback soundFeedback;

  private void lazyMakeSoundFeedback() {
    if (soundFeedback == null) soundFeedback = new MySoundFeedback();
  }

  private void playCorrect() {
    lazyMakeSoundFeedback();
    soundFeedback.queueSong(SoundFeedback.CORRECT);
   }

  public class MySoundFeedback extends SoundFeedback {
    public MySoundFeedback() {
      super(controller.getSoundManager());
    }

    public synchronized void queueSong(String song) {
      destroySound(); // if there's something playing, stop it!
      createSound(song, null);
    }
  }


  public void clearFeedback() {
    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(feedbackDummyPanel);
  }

  /**
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#addScoreFeedback(ScoreFeedback)
   * @return
   */
  public IconAnchor getFeedbackImage() {
    IconAnchor image;
    image = new IconAnchor();
    image.addStyleName("leftFiveMargin");
    image.setBaseIcon(useWhite ? MyCustomIconType.white : MyCustomIconType.gray);
    image.setHeight("48px");
    feedbackImage = image;
    return image;
  }

  public ProgressBar getScoreFeedback() {
    return scoreFeedback;
  }
}
