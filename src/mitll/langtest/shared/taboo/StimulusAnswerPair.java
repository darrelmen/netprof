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
  private String stimulus;
  private String answer;
  private String exerciseID;
  public boolean isLastStimulus;
  public boolean didGiverSkip;
  public boolean noStimYet;

  public StimulusAnswerPair() {}

  public StimulusAnswerPair(String exerciseID, String stimulus, String answer, boolean isLastStimulus, boolean didGiverSkip) {
    this.setExerciseID(exerciseID);
    this.setStimulus(stimulus);
    this.setAnswer(answer);
    this.isLastStimulus = isLastStimulus;
    this.didGiverSkip = didGiverSkip;
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

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof StimulusAnswerPair)) return false;
    else {
      StimulusAnswerPair ostim = (StimulusAnswerPair) other;
      if (noStimYet != ostim.noStimYet) {
        return false;
      } else if (!exerciseID.equals(ostim.exerciseID)) {
        return false;
      } else return stimulus.equals(ostim.stimulus);
    }
  }

  public String toString() { return "Ex " + getExerciseID() +
    " Stim : '" + getStimulus() + "' answer '" + getAnswer() + "'"; }
}
