package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Exercise implements IsSerializable  {
  private String content;
  private String id;
  private Map<String,List<QAPair>> langToQuestion = new HashMap<String, List<QAPair>>();
  public static class QAPair implements IsSerializable {
    private String question;
    private String answer;
    public QAPair() {}   // required for serialization
    public QAPair(String q, String a) { question = q; answer = a;}
    public String toString() { return "'"+ getQuestion() + "' : '" + getAnswer() +"'"; }

    public String getQuestion() {
      return question;
    }

    public String getAnswer() {
      return answer;
    }
  }

  public Exercise() {}     // required for serialization
  public Exercise(String id, String content) { this.id = id; this.content = content; }
  public void addQuestion(String lang, String question, String answer) {
    List<QAPair> qaPairs = langToQuestion.get(lang);
    if (qaPairs == null) {
      langToQuestion.put(lang, qaPairs = new ArrayList<QAPair>());
    }
    qaPairs.add(new QAPair(question, answer));
  }

  public String getID() { return id; }
  public String getContent() { return content; }
  public List<QAPair> getQuestions() {
    List<QAPair> pairs = langToQuestion.get("en");
    return pairs;
  }
  public String toString() {
    return "Exercise " + id + " : " + content + "\n\tquestions " + langToQuestion;
  }
}
