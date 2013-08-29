package mitll.langtest.shared.taboo;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 8/13/13
* Time: 2:37 PM
* To change this template use File | Settings | File Templates.
*/
public class StimulusAnswerPair implements IsSerializable {
  private int numClues;
  private String stimulus;
  private String answer;
  private String exerciseID;
  public boolean isLastStimulus;
  public boolean didGiverSkip;
  public boolean noStimYet;
  private boolean gameOver;
  private boolean chapterComplete;

  public StimulusAnswerPair() {}

  /**
   * @see mitll.langtest.client.taboo.SinglePlayerRobot#checkForStimulus(com.google.gwt.user.client.rpc.AsyncCallback)
   * @param gameOver
   * @param chapterComplete
   */
  public StimulusAnswerPair(boolean gameOver, boolean chapterComplete) {
    this.gameOver = gameOver;
    this.chapterComplete = chapterComplete;
  }

  public StimulusAnswerPair(String exerciseID, String stimulus, String answer, boolean isLastStimulus,
                            boolean didGiverSkip, int totalExpected, boolean isGameOver) {
    this(isGameOver, false);
    this.setExerciseID(exerciseID);
    this.setStimulus(stimulus);
    this.setAnswer(answer);
    this.isLastStimulus = isLastStimulus;
    this.didGiverSkip = didGiverSkip;
    this.numClues = totalExpected;
  }

  public void setNoStimYet(boolean v) { this.noStimYet = v; }

  public String getStimulus() {
    return stimulus;
  }

  public void setStimulus(String stimulus) {
    this.stimulus = stimulus;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public String getExerciseID() {
    return exerciseID;
  }

  public void setExerciseID(String exerciseID) {
    this.exerciseID = exerciseID;
  }

  public int getNumClues() {
    return numClues;
  }

  public boolean isGameOver() {
    return gameOver;
  }

/*  public boolean isChapterComplete() {
    return chapterComplete;
  }*/

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof StimulusAnswerPair)) return false;
    else {
      StimulusAnswerPair ostim = (StimulusAnswerPair) other;
      return noStimYet == ostim.noStimYet && exerciseID.equals(ostim.exerciseID) && stimulus.equals(ostim.stimulus);
    }
  }

  public String toString() { return "Ex " + getExerciseID() +
    " Stim : '" + getStimulus() + "' answer '" + getAnswer() + "'"; }
}
