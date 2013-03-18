package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of a individual item of work the user sees.  Could be a pronunciation exercise or a question(s)
 * based on a prompt.
 *
 * TODO : consider subclass for pronunciation exercises?
 *
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Exercise extends ExerciseShell  {
  private static final ArrayList<String> EMPTY_LIST = new ArrayList<String>();

  public enum EXERCISE_TYPE implements IsSerializable { RECORD, TEXT_RESPONSE, REPEAT, REPEAT_FAST_SLOW, MULTI_REF }
  private static final String EN = "en";
  public static final String FL = "fl";
  private static final boolean DEBUG = false;
  private String plan;
  private String content;
  private EXERCISE_TYPE type = EXERCISE_TYPE.RECORD;
  public boolean promptInEnglish = true;
  private Map<String,List<QAPair>> langToQuestion = null;
  private String refAudio;
  private String slowAudioRef;
  private String refSentence;
  private double weight;
  private transient List<String> slots = new ArrayList<String>();

  public static class QAPair implements IsSerializable {
    private String question;
    private String answer;
    private List<String> alternateAnswers;
    public QAPair() {}   // required for serialization

    /**
     * @param q
     * @param a
     * @param alternateAnswers
     * @see Exercise#addQuestion(String, String, String, java.util.List
     */
    public QAPair(String q, String a, List<String> alternateAnswers) { question = q; answer = a; this.alternateAnswers = alternateAnswers;}

    /**
     * @see mitll.langtest.client.exercise.ExercisePanel#addQuestions
     * @return
     */
    public String getQuestion() {
      return question;
    }

    /**
     * @see mitll.langtest.client.exercise.ExercisePanel#getQuestionHeader
     * @return
     */
    public String getAnswer() {
      return answer;
    }

    public List<String> getAlternateAnswers() {
      return alternateAnswers;
    }

    public String toString() {
      String alts = "";
      int i = 1;
      if (alternateAnswers != null && !alternateAnswers.isEmpty()) {
        alts += "alternates ";
        for (String answer : alternateAnswers) alts += "#" + (i++) + " : " +answer +"; ";
      }
      return "Q: '"+ getQuestion() + "' A: '" + getAnswer() +"' " + alts;
    }
  }

  public Exercise() {}     // required for serialization

  /**
   * @see mitll.langtest.server.database.SQLExerciseDAO#getExercise
   * @param id
   * @param content
   * @param promptInEnglish
   * @param recordAudio
   * @param tooltip
   */
  public Exercise(String plan, String id, String content, boolean promptInEnglish, boolean recordAudio, String tooltip) {
    super(id,tooltip);
    this.plan = plan;
    this.content = content;
    this.type = recordAudio ? EXERCISE_TYPE.RECORD : EXERCISE_TYPE.TEXT_RESPONSE;
    this.promptInEnglish = promptInEnglish;
  }

  /**
   * @see mitll.langtest.server.database.FileExerciseDAO#readExercises(String)
   * @param plan
   * @param id
   * @param content
   * @param audioRef
   * @param sentenceRef
   * @param tooltip
   */
  public Exercise(String plan, String id, String content, String audioRef, String sentenceRef, String tooltip) {
    super(id,tooltip);

    this.plan = plan;
    this.content = content;
    this.refAudio = audioRef;
    this.refSentence = sentenceRef;
    this.type = EXERCISE_TYPE.REPEAT;
  }

  /**
   * @see mitll.langtest.server.database.FileExerciseDAO#readFastAndSlowExercises(String, String)
   * @param plan
   * @param id
   * @param content
   * @param fastAudioRef
   * @param slowAudioRef
   * @param sentenceRef
   * @param tooltip
   */
  public Exercise(String plan, String id, String content, String fastAudioRef, String slowAudioRef, String sentenceRef, String tooltip) {
    this(plan,id,content,fastAudioRef,sentenceRef, tooltip);
    this.slowAudioRef = slowAudioRef;
    this.type = EXERCISE_TYPE.REPEAT_FAST_SLOW;
  }

  public ExerciseShell getShell() { return new ExerciseShell(getID(), getTooltip()); }

  public void addQuestion() {
    addQuestion(FL, "Please record the sentence above.", "", EMPTY_LIST);
  }

  /**
   * @see mitll.langtest.server.database.SQLExerciseDAO#getExercise(String, String, net.sf.json.JSONObject)
   * @param lang
   * @param question
   * @param answer
   * @param alternateAnswers
   */
  public void addQuestion(String lang, String question, String answer, List<String> alternateAnswers) {
    if (langToQuestion == null) langToQuestion = new HashMap<String, List<QAPair>>();
    List<QAPair> qaPairs = langToQuestion.get(lang);
    if (qaPairs == null) {
      langToQuestion.put(lang, qaPairs = new ArrayList<QAPair>());
    }

    qaPairs.add(new QAPair(question, answer, alternateAnswers));
  }

  public String getPlan() { return plan; }

  public String getContent() { return content; }
  public EXERCISE_TYPE getType() { return type; }
  public boolean isRepeat() { return type == EXERCISE_TYPE.REPEAT || type == EXERCISE_TYPE.REPEAT_FAST_SLOW; }
  public String getRefAudio() { return refAudio; }
  public String getSlowAudioRef() { return slowAudioRef; }
  public void setRefAudio(String s) { this.refAudio = s; }
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
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(promptInEnglish ? EN : FL);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

  public List<QAPair> getEnglishQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(EN);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }
  public List<QAPair> getForeignLanguageQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(FL);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

  public int getNumQuestions() {
    List<QAPair> en = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get("en");
    if (en == null) return 0; // should never happen
    return en.size();
  }

  public double getWeight() {
    return weight;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public void addSlot(String s) {
    if (slots == null) slots = new ArrayList<String>();
    slots.add(s);
  }
  //public List<String> getSlots() { return slots; }

  public String toString() {
    if (isRepeat() || getType() == EXERCISE_TYPE.MULTI_REF) {
      return "Exercise " + type + " " +plan+"/"+ id + "/" + " content bytes = " + content.length() +
          " ref sentence " + refSentence +" audio " + refAudio;
    }
    else {
      String questions = "";
      if (langToQuestion != null) {
        for (Map.Entry<String, List<QAPair>> pair : langToQuestion.entrySet()) {
          questions += pair.getKey() + " -> ";
          int i =1;
          for (QAPair qa : pair.getValue()) {
            questions += "#"+ (i++) +" : "+qa.toString() + ", ";
          }
        }
      }
      String moreAboutQuestions = DEBUG ? " : " + questions : "";
      return "Exercise " + getType() + " " +plan+"/"+ id + "/" + (promptInEnglish?"english":"foreign")+
          " : content bytes = " + content.length() + (DEBUG ? " content : " +content : "")+
          (langToQuestion == null ? " no questions" : " num questions " + langToQuestion.size() + moreAboutQuestions);
    }
  }
}
