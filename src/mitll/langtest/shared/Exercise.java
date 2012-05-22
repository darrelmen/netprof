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
  public enum EXERCISE_TYPE implements IsSerializable { RECORD, TEXT_RESPONSE }

  private String plan;
  private String content;
  private String id;
  private EXERCISE_TYPE type = EXERCISE_TYPE.RECORD;
  public boolean promptInEnglish = true;
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

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getExercise
   * @param id
   * @param content
   * @param promptInEnglish
   * @param recordAudio
   */
  public Exercise(String plan, String id, String content, boolean promptInEnglish, boolean recordAudio) {
    this.plan = plan; this.id = id; this.content = content;
    this.type = recordAudio ? EXERCISE_TYPE.RECORD : EXERCISE_TYPE.TEXT_RESPONSE;
    this.promptInEnglish = promptInEnglish;
  }
  public void addQuestion(String lang, String question, String answer) {
    List<QAPair> qaPairs = langToQuestion.get(lang);
    if (qaPairs == null) {
      langToQuestion.put(lang, qaPairs = new ArrayList<QAPair>());
    }
    qaPairs.add(new QAPair(question, answer));
  }

  public String getPlan() { return plan; }
  public String getID() { return id; }
  public String getContent() { return content; }
  public EXERCISE_TYPE getType() { return type; }

  public void setPromptInEnglish(boolean b) {
    this.promptInEnglish = b;
  }

  public void setRecordAnswer(boolean spoken) {
    type = spoken ? EXERCISE_TYPE.RECORD : EXERCISE_TYPE.TEXT_RESPONSE;
  }

  /**
   * @see mitll.langtest.client.ExercisePanel#ExercisePanel(Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.UserFeedback, mitll.langtest.client.ExerciseController)
   * @return
   */
  public List<QAPair> getQuestions() {
    return langToQuestion.get(promptInEnglish ? "en" : "fl");
  }

  public String toString() {
    return "Exercise " + plan+"/"+ id + "/" + (promptInEnglish?"english":"foreign")+"/" + getType()+
      " : content bytes = " + content.length() + " num questions " + langToQuestion.size();
  }
}
