/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.amas;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

/**
 * Created by go22670 on 12/21/15.
 */
public class QAPair implements IsSerializable {
  private String question;
  //  private String answer;
  private List<String> alternateAnswers;

  public QAPair() {
  }   // required for serialization

  /**
   * @param q
   * @param alternateAnswers
   * @paramx a
   * @see AmasExerciseImpl#addQuestion
   */
  QAPair(String q, List<String> alternateAnswers) {
    question = q;
    this.alternateAnswers = alternateAnswers;
  }

  /**
   * @return
   */
  public String getQuestion() {
    return question;
  }

  /**
   * @return
   */
//    public String getAnswer() {  return answer;   }
  public List<String> getAlternateAnswers() {
    return alternateAnswers;
  }

/*  public List<String> getAllAnswers() {
    List<String> alternateAnswers = getAlternateAnswers();
    alternateAnswers = new ArrayList<String>(alternateAnswers);

    if (!alternateAnswers.contains(getAnswer())) {
      alternateAnswers.add(getAnswer());
    }
    return alternateAnswers;
  }*/

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof QAPair)) return false;
    QAPair otherpair = (QAPair) obj;
    return question.equals(otherpair.question);// && answer.equals(otherpair.answer);
  }

  public String toString() {
    String alts = "";
    int i = 1;
    if (alternateAnswers != null && !alternateAnswers.isEmpty()) {
      alts += "alternates ";
      for (String answer : alternateAnswers) alts += "#" + (i++) + " : '" + answer + "' ";
    }
    return "Q: '" + getQuestion() + "' A: " + alts;
  }
}
