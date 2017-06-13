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

package mitll.langtest.client.flashcard;

import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.services.LangTestDatabaseAsync;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/11/14.
 */
public class ControlState {
  private static final String SHOW_STATE = "showState";
  private static final String AUDIO_ON = "audioOn";
  private static final String AUDIO_FEEDBACK_ON = "audioFeedbackOn";
  private static final String SHUFFLE_ON = "shuffleOn";
  private static final String TRUE_VALUE = Boolean.TRUE.toString();
  private static int count = 0;

  private boolean audioOn = true;
  private boolean audioFeedbackOn = true;
  private boolean shuffleOn = false;
  private boolean autoPlay = false;

  public static final String ENGLISH = "english";
  public static final String FOREIGN = "foreign";
  public static final String BOTH = "both";
  public static final String AUTO_PLAY = "autoPlay";
  private String showState = BOTH; // english/foreign/both - default

  private KeyStorage storage = null;
  private final int id;

  public ControlState() {
    id = count++;
  }

  boolean showEnglish() {
    return showState.equals(ENGLISH) || showState.equals(BOTH);
  }

  boolean showForeign() {
    return showState.equals(FOREIGN) || showState.equals(BOTH);
  }

  boolean showBoth() {
    return showState.equals(BOTH);
  }

  public boolean isEnglish() {
    return showState.equals(ENGLISH);
  }

  public boolean isForeign() {
    return showState.equals(FOREIGN);
  }

  /**
   * @param storage
   * @see StatsFlashcardFactory#StatsFlashcardFactory(LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.list.ListInterface, String, mitll.langtest.shared.custom.UserList)
   */
  public void setStorage(KeyStorage storage) {
    this.storage = storage;

    String showState1 = storage.getValue(SHOW_STATE);
    if (showState1.equals(FOREIGN)) showState = FOREIGN;
    else if (showState1.equals(BOTH)) showState = BOTH;
    else if (showState1.equals(ENGLISH)) showState = ENGLISH;

    audioOn = getValue(AUDIO_ON);
    audioFeedbackOn = getValue(AUDIO_FEEDBACK_ON);
    shuffleOn = getValue(SHUFFLE_ON);
    autoPlay = getValue(AUTO_PLAY);
  }

  private boolean getValue(String audioOn) {
    return storage.getValue(audioOn).equalsIgnoreCase(TRUE_VALUE);
  }

  public void setAudioOn(boolean audioOn) {
    this.audioOn = audioOn;
    storeValue(AUDIO_ON, audioOn);
  }

  public void setAudioFeedbackOn(boolean audioFeedbackOn) {
    this.audioFeedbackOn = audioFeedbackOn;
    storeValue(AUDIO_FEEDBACK_ON, audioFeedbackOn);
  }

  void setSuffleOn(boolean shuffleOn) {
    this.shuffleOn = shuffleOn;
    storeValue(SHUFFLE_ON, shuffleOn);
  }

  void setShowState(String showState) {
    this.showState = showState;
    if (storage != null) storage.storeValue(SHOW_STATE, showState);
  }

  /**
   * @param autoPlay
   * @see FlashcardPanel#getAutoPlayButton
   */
  void setAutoPlayOn(boolean autoPlay) {
    this.autoPlay = autoPlay;
    storeValue(AUTO_PLAY, autoPlay);
  }

  private void storeValue(String slot, boolean shuffleOn) {
    if (storage != null) {
      storage.storeValue(slot, Boolean.toString(shuffleOn));
    }
  }

  boolean isAudioOn() {
    return audioOn;
  }

  boolean isAudioFeedbackOn() {
    return audioFeedbackOn;
  }

  /**
   * @return
   * @see FlashcardPanel#getRightColumn(ControlState)
   */
  boolean isShuffle() {
    return shuffleOn;
  }

  /**
   * @return
   * @see BootstrapExercisePanel#checkThenLoadNextOnTimer
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#
   */
  boolean isAutoPlay() {
    return autoPlay;
  }

  public String toString() {
    return "ControlState :" +
        " id " + id +
        " audio " + audioOn +
        " show " + showState +
        " shuffle " + shuffleOn +
        " auto play " + autoPlay;
  }
}
