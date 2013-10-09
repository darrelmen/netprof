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
  public static final String EN = "en";
  public static final String FL = "fl";
  private static final boolean DEBUG = false;
  private String plan;
  private String content;
  private EXERCISE_TYPE type = EXERCISE_TYPE.RECORD;
  public boolean promptInEnglish = true;
  private Map<String,List<QAPair>> langToQuestion = null;
  private String refAudio;
  private String slowAudioRef;
  private String englishSentence;
  private String segmented;
  private List<String> refSentences = new ArrayList<String>();
  private List<String> synonymSentences = new ArrayList<String>();
  private List<String> synonymTransliterations = new ArrayList<String>();
  private List<String> synonymAudioRefs = new ArrayList<String>();
  private List<String> translitSentences = new ArrayList<String>();
  private double weight;

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
    private QAPair(String q, String a, List<String> alternateAnswers) { question = q; answer = a; this.alternateAnswers = alternateAnswers;}

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

    @Override
    public boolean equals(Object obj) {
      QAPair otherpair = (QAPair) obj;
      return question.equals(otherpair.question) && answer.equals(otherpair.answer);
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
    this.setType(recordAudio ? EXERCISE_TYPE.RECORD : EXERCISE_TYPE.TEXT_RESPONSE);
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
    this.refSentences.add(sentenceRef);
    this.setType(EXERCISE_TYPE.REPEAT);
  }

  public Exercise(String plan, String id, String content, List<String> sentenceRefs, String tooltip) {
    super(id,tooltip);

    this.plan = plan;
    this.content = content;
    this.refSentences = sentenceRefs;
    this.setType(EXERCISE_TYPE.RECORD);
  }

  /**
   * @see mitll.langtest.server.database.FileExerciseDAO#readFastAndSlowExercises
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
    this.setType(EXERCISE_TYPE.REPEAT_FAST_SLOW);
  }

  public ExerciseShell getShell() { return new ExerciseShell(getID(), getTooltip()); }

  public void addQuestion() {
    addQuestion(FL, "Please record the sentence above.", "", EMPTY_LIST);
  }

  /**
   * when not collecting audio, we only collect text, and
   * we only collect fl text (never english text only english audio)
   */
  public void setTextOnly() {
    setPromptInEnglish(false);
    setRecordAnswer(false);
  }

  /**
   * @param lang
   * @param question
   * @param answer
   * @param alternateAnswers
   * @see mitll.langtest.server.database.SQLExerciseDAO#getExercise(String, String, net.sf.json.JSONObject)
   */
  public void addQuestion(String lang, String question, String answer, List<String> alternateAnswers) {
    QAPair pair = new QAPair(question, answer, alternateAnswers);
    addQuestion(lang, pair);
  }

  public void addQuestions(String lang, List<QAPair> pairs) {
    for (QAPair pair : pairs) {
      addQuestion(lang, pair);
    }
  }

  private void addQuestion(String lang, QAPair pair) {
    if (langToQuestion == null) langToQuestion = new HashMap<String, List<QAPair>>();
    List<QAPair> qaPairs = langToQuestion.get(lang);
    if (qaPairs == null) {
      langToQuestion.put(lang, qaPairs = new ArrayList<QAPair>());
    }

    qaPairs.add(pair);
  }

  public String getPlan() { return plan; }

  public String getContent() { return content; }
  public EXERCISE_TYPE getType() { return type; }
  public boolean isRepeat() { return type == EXERCISE_TYPE.REPEAT || type == EXERCISE_TYPE.REPEAT_FAST_SLOW; }
  public String getRefAudio() { return refAudio; }
  public String getSlowAudioRef() { return slowAudioRef; }
  public void setRefAudio(String s) { this.refAudio = s; }
  public boolean hasRefAudio() { return refAudio != null || slowAudioRef != null; }

  public List<String> getSynonymSentences() {
    return synonymSentences;
  }

  public void setSynonymSentences(List<String> synonymSentences) {
    this.synonymSentences = synonymSentences;
  }

  public List<String> getSynonymTransliterations() {
    return synonymTransliterations;
  }

  public void setSynonymTransliterations(List<String> synonymTransliterations) {
    this.synonymTransliterations = synonymTransliterations;
  }

  public List<String> getSynonymAudioRefs() {
    return synonymAudioRefs;
  }

  public void setSynonymAudioRefs(List<String> synonymAudioRefs) {
    this.synonymAudioRefs = synonymAudioRefs;
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(int, mitll.langtest.server.database.FileExerciseDAO, String, String, String)
   * @param s
   */
  public void setSlowRefAudio(String s) { this.slowAudioRef = s; }

  public String getRefSentence() {
    StringBuilder builder = new StringBuilder();
    for (String s : refSentences) {
      builder.append(s).append(" ");
    }
    return builder.toString();
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(int, mitll.langtest.server.database.FileExerciseDAO, int, org.apache.poi.ss.usermodel.Row, String, String, String)
   * @param sentenceRefs
   */
  public void setRefSentences(List<String> sentenceRefs) {
    this.refSentences = sentenceRefs;
  }
  public String getTranslitSentence() { return translitSentences.isEmpty() ? "" : translitSentences.get(0); }
  public List<String> getRefSentences() { return refSentences; }
  public List<String> getTranslitSentences() { return translitSentences; }

  public void setRefSentence(String ref) {
    refSentences.clear();
    refSentences.add(ref);
  }

/*  public void addRefSentence(String ref) {
    if (!refSentences.contains(ref)) refSentences.add(ref);
  }*/

  public void setTranslitSentence(String translitSentence) {
    translitSentences.clear();
    translitSentences.add(translitSentence);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#setPromptAndRecordOnExercises(long, java.util.List)
   * @param b
   */
  public void setPromptInEnglish(boolean b) {
    this.promptInEnglish = b;
  }

  public void setRecordAnswer(boolean spoken) {
    setType(spoken ? EXERCISE_TYPE.RECORD : EXERCISE_TYPE.TEXT_RESPONSE);
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

  public String getEnglishSentence() {
    return englishSentence;
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(String, mitll.langtest.server.database.FileExerciseDAO, int, org.apache.poi.ss.usermodel.Row, String, String, String, String, String, String, boolean)
   * @param englishSentence
   */
  public void setEnglishSentence(String englishSentence) {
    this.englishSentence = englishSentence;
  }

  public void setType(EXERCISE_TYPE type) {
    this.type = type;
  }

  public void setSegmented(String segmented) {
    this.segmented = segmented;
  }

  public String toString() {
    String moreAboutQuestions = DEBUG ? " : " +  getQuestionToString() : "";
    String questionInfo = langToQuestion == null ? " no questions" : " num questions " + langToQuestion.size() + moreAboutQuestions;

    if (isRepeat() || getType() == EXERCISE_TYPE.MULTI_REF) {
      return "Exercise " + type + " " +plan+"/"+ id + "/" + " content bytes = " + content.length() +
          " ref sentence '" + getRefSentence() +"' audio " + refAudio + questionInfo;
    }
    else {
      return "Exercise " + getType() + " " +plan+"/"+ id + "/" + (promptInEnglish?"english":"foreign")+
          " : content bytes = " + content.length() + (DEBUG ? " content : " +content : "")+
          " ref '" + getRefSentence() + "' translit '" + getTranslitSentence()+ "'"+
        questionInfo;
    }
  }

  private String getQuestionToString() {
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
    return questions;
  }
}
