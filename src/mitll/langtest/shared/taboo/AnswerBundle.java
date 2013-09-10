package mitll.langtest.shared.taboo;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 9/4/13
* Time: 12:55 PM
* To change this template use File | Settings | File Templates.
*/
public class AnswerBundle implements IsSerializable {
  private int reqID;
  private String answer;
  private boolean correct;
  private long timestamp;
  private boolean receiverReplied;

  public AnswerBundle() {}

  /**
   * @see mitll.langtest.server.database.taboo.OnlineUsers#registerAnswer(long, String, boolean)
   * @param answer
   * @param correct
   */
  public AnswerBundle(String answer, boolean correct) {
    receiverReplied = true;
   // this.reqID = reqID;
    this.answer = answer;
    this.correct = correct;
    this.timestamp = System.currentTimeMillis();
  }

  public String getAnswer() {
    return answer;
  }

  public boolean isCorrect() {
    return correct;
  }

  public boolean didReceiverReply() {
    return receiverReplied;
  }

  public int getReqID() {
    return reqID;
  }

  public String toString() { return " answer '" + getAnswer() + "' is " +(isCorrect() ? "correct" : "incorrect") + " at " + new Date(timestamp);
  }
}
