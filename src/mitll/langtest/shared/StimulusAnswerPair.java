package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 8/13/13
* Time: 2:37 PM
* To change this template use File | Settings | File Templates.
*/
public class StimulusAnswerPair implements IsSerializable {
  public String stimulus;
  public String answer;

  public StimulusAnswerPair() {}

  public StimulusAnswerPair(String stimulus, String answer) {
    this.stimulus = stimulus;
    this.answer = answer;
  }

  public String toString() { return "Stim : '" + stimulus + "' answer '" + answer + "'"; }
}
