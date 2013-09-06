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
  private boolean isLastStimulus;
 // private boolean didGiverSkip;
  private boolean noStimYet;
  private boolean gameOver;
  private boolean giverChosePoorly;

  public StimulusAnswerPair() {} // just for serialization
  public StimulusAnswerPair(String exerciseID, String stimulus, String answer, boolean isLastStimulus,
                            int totalExpected, boolean isGameOver, boolean giverChosePoorly) {
    this.gameOver = isGameOver;
    this.setExerciseID(exerciseID);
    this.setStimulus(stimulus);
    this.setAnswer(answer);
    this.isLastStimulus = isLastStimulus;
  //  this.didGiverSkip = didGiverSkip;
    this.numClues = totalExpected;
    this.giverChosePoorly = giverChosePoorly;
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

  public boolean isLastStimulus() {
    return isLastStimulus;
  }

/*
  public boolean isDidGiverSkip() {
    return didGiverSkip;
  }
*/

  public boolean isNoStimYet() {
    return noStimYet;
  }

  public boolean didGiverChosePoorly() {
    return giverChosePoorly;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof StimulusAnswerPair)) return false;
    else {
      StimulusAnswerPair ostim = (StimulusAnswerPair) other;
      return isNoStimYet() == ostim.isNoStimYet() && exerciseID.equals(ostim.exerciseID) && stimulus.equals(ostim.stimulus);
    }
  }

  public String toString() { return "Ex " + getExerciseID() + " Stim : '" + getStimulus() + "' answer '" + getAnswer() + "'"; }
}
