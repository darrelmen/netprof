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
  public enum EXERCISE_TYPE implements IsSerializable { RECORD, TEXT_RESPONSE, REPEAT }

  private String plan;
  private String content;
  private String id;
  private EXERCISE_TYPE type = EXERCISE_TYPE.RECORD;
  public boolean promptInEnglish = true;
  private Map<String,List<QAPair>> langToQuestion = new HashMap<String, List<QAPair>>();
  private String refAudio;
  private String refSentence;

  public static class QAPair implements IsSerializable {
    private String question;
    private String answer;
    public QAPair() {}   // required for serialization

    /**
     * @see Exercise#addQuestion(String, String, String)
     * @param q
     * @param a
     */
    public QAPair(String q, String a) { question = q; answer = a;}
    public String toString() { return "'"+ getQuestion() + "' : '" + getAnswer() +"'"; }

    /**
     * @see mitll.langtest.client.exercise.ExercisePanel#addQuestions
     * @return
     */
    public String getQuestion() {
      return question;
    }

    public String getAnswer() {
      return answer;
    }
  }

  public Exercise() {}     // required for serialization

  /**
   * @see mitll.langtest.server.database.SQLExerciseDAO#getExercise
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

  public Exercise(String plan, String id, String content, String audioRef, String sentenceRef) {
    this.plan = plan;
    this.id = id;
    this.content = content;
    this.refAudio = audioRef;
    this.refSentence = sentenceRef;
    this.type = EXERCISE_TYPE.REPEAT;
  }

  /**
   * @see mitll.langtest.server.database.SQLExerciseDAO#getExercise(String, String, net.sf.json.JSONObject)
   * @param lang
   * @param question
   * @param answer
   */
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
  public String getRefAudio() { return refAudio; }
  public String getRefSentence() { return refSentence; }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getExercises(long)
   * @param b
   */
  public void setPromptInEnglish(boolean b) {
    this.promptInEnglish = b;
  }

  public void setRecordAnswer(boolean spoken) {
    type = spoken ? EXERCISE_TYPE.RECORD : EXERCISE_TYPE.TEXT_RESPONSE;
  }

  /**
   * @see mitll.langtest.client.exercise.ExercisePanel#addQuestions(Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @return
   */
  public List<QAPair> getQuestions() {
    return langToQuestion.get(promptInEnglish ? "en" : "fl");
  }

  public List<QAPair> getEnglishQuestions() { return langToQuestion.get("en"); }
  //public List<QAPair> getForeignLanguageQuestions() { return langToQuestion.get("fl"); }

  public int getNumQuestions() {
    List<QAPair> en = langToQuestion.get("en");
    if (en == null) return 0; // should never happen
    return en.size();
  }

  public String toString() {
    if (getType() == EXERCISE_TYPE.REPEAT) {
      return "Exercise " + plan+"/"+ id + "/" + " content bytes = " + content.length() + " ref sentence " + refSentence +" audio " + refAudio;
    }
    else {
      return "Exercise " + plan+"/"+ id + "/" + (promptInEnglish?"english":"foreign")+"/" + getType()+
          " : content bytes = " + content.length() + " num questions " + langToQuestion.size();
    }
  }
}
