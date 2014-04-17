package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.custom.UserExercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
public class Exercise extends AudioExercise implements CommonExercise {
  private static final ArrayList<String> EMPTY_LIST = new ArrayList<String>();

  public enum EXERCISE_TYPE implements IsSerializable { RECORD, TEXT_RESPONSE, REPEAT, REPEAT_FAST_SLOW, MULTI_REF }
  public static final String EN = "en";
  public static final String FL = "fl";
  private static final boolean DEBUG = false;
  private String plan;
  private String content;
  private EXERCISE_TYPE type = EXERCISE_TYPE.RECORD;
  private boolean promptInEnglish = true;
  private Map<String,List<QAPair>> langToQuestion = null;
  private String englishSentence;
  private String meaning, context;
  private List<String> refSentences = new ArrayList<String>();
  private List<String> translitSentences = new ArrayList<String>();
  private STATE state;

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
     * @return
     */
    private String getQuestion() { return question; }

    /**
     * @return
     */
    public String getAnswer() { return answer; }

    public List<String> getAlternateAnswers() {
      return alternateAnswers;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof QAPair)) return false;
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
   * @param context
   */
  public Exercise(String plan, String id, String content, boolean promptInEnglish, boolean recordAudio, String tooltip,
                  String context) {
    super(id,tooltip);
    this.plan = plan;
    this.setContent(content);
    this.setType(recordAudio ? EXERCISE_TYPE.RECORD : EXERCISE_TYPE.TEXT_RESPONSE);
    this.setPromptInEnglish(promptInEnglish);
    this.context = context;
  }

  /**
   * @see mitll.langtest.server.database.FileExerciseDAO#getSimpleExerciseForLine(String, int)
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
    this.setContent(content);
    setRefAudio(audioRef);
    this.refSentences.add(sentenceRef);
    this.setType(EXERCISE_TYPE.REPEAT);
  }

  public Exercise(String plan, String id, String content, List<String> sentenceRefs, String tooltip) {
    super(id,tooltip);

    this.plan = plan;
    this.setContent(content);
    this.refSentences = sentenceRefs;
    this.setType(EXERCISE_TYPE.RECORD);
  }

  /**
   * @see mitll.langtest.server.database.FileExerciseDAO#getExerciseForLine(String)
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
    setSlowRefAudio(slowAudioRef);
    this.setType(EXERCISE_TYPE.REPEAT_FAST_SLOW);
  }

  public CommonShell getShellCombinedTooltip() {
    String refSentence = getRefSentence();
    if (refSentence.length() > 15) {
      refSentence = refSentence.substring(0, 15);
    }
    boolean refSentenceEqualsTooltip = getTooltip().trim().equals(getRefSentence().trim());
    String combined = refSentenceEqualsTooltip ? getTooltip() : getTooltip() + (refSentence.isEmpty() ? "": " / " + refSentence);
    if (getTooltip().isEmpty()) combined = refSentence;

    return new ExerciseShell(getID(), combined);
  }

  public void addQuestion() {
    addQuestion(FL, "Please record the sentence above.", "", EMPTY_LIST);
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
  private EXERCISE_TYPE getType() { return type; }
  private boolean isRepeat() { return type == EXERCISE_TYPE.REPEAT || type == EXERCISE_TYPE.REPEAT_FAST_SLOW; }

  public String getRefSentence() {
    StringBuilder builder = new StringBuilder();
    for (String s : refSentences) {
      builder.append(s).append(" ");
    }
    return builder.toString();
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(String, int, org.apache.poi.ss.usermodel.Row, String, String, String, String, String, boolean, String)
   * @param sentenceRefs
   */
  public void setRefSentences(List<String> sentenceRefs) {
    this.refSentences = sentenceRefs;
  }
  public String getTransliteration() { return translitSentences.isEmpty() ? "" : translitSentences.get(0); }

  public void setRefSentence(String ref) {
    refSentences.clear();
    refSentences.add(ref);
  }

  public void setTranslitSentence(String translitSentence) {
    translitSentences.clear();
    translitSentences.add(translitSentence);
  }

  /**
   * @return
   */
  public List<QAPair> getQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(isPromptInEnglish() ? EN : FL);
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

/*
  public int getNumQuestions() {
    List<QAPair> en = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get("en");
    if (en == null) return 0; // should never happen
    return en.size();
  }
*/

  public String getEnglish() {  return englishSentence;  }
  public void setType(EXERCISE_TYPE type) { this.type = type;  }

  public void setContent(String content) { this.content = content;  }
  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise
   * @param englishSentence
   */
  public void setEnglishSentence(String englishSentence) {
    this.englishSentence = englishSentence;
  }

  /**
   * @param b
   */
  private void setPromptInEnglish(boolean b) { this.promptInEnglish = b;  }
  private boolean isPromptInEnglish() { return promptInEnglish;  }

  @Override
  public String getMeaning() {
    return meaning;
  }

  public void setMeaning(String meaning) {
    this.meaning = meaning;
  }

  @Override
  public String getForeignLanguage() {
    return getRefSentence();
  }

  @Override
  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public Exercise toExercise() {
    return this;
  }

  @Override
  public CommonUserExercise toCommonUserExercise() {
    return new UserExercise(this);
  }

  @Override
  public Date getModifiedDate() {
    return null;
  }

  @Override
  public STATE getState() {
    return state;
  }

  @Override
  public void setState(STATE state) {
    this.state = state;
  }

  public String toString() {
    String moreAboutQuestions = DEBUG ? " : " +  getQuestionToString() : "";
    String questionInfo = langToQuestion == null ? " no questions" : " num questions " + langToQuestion.size() + moreAboutQuestions;

    if (isRepeat() || getType() == EXERCISE_TYPE.MULTI_REF) {
      Collection<AudioAttribute> audioAttributes1 = getAudioAttributes();
      StringBuilder builder = new StringBuilder();
      for (AudioAttribute attr:audioAttributes1) builder.append("\t").append(attr.toString()).append("\n");
      return "Exercise " + type + " " +id +  " content bytes = " + content.length() + " english '" + getEnglish() +
          "' ref sentence '" + getRefSentence() +"'\n\taudio " + builder.toString() + " : " + questionInfo +
        " unit->lesson " + getUnitToValue();
    }
    else {
      return "Exercise " + getType() + " " + id + " " + (isPromptInEnglish() ?"english":"foreign")+
          " : content bytes = " + content.length() + (DEBUG ? " content : " +content : "")+
          " ref '" + getRefSentence() + "' translit '" + getTransliteration()+ "'"+
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
