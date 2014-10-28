package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.*;

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
  public static final String EN = "en";
  public static final String FL = "fl";
  private static final int MAX_TOOLTIP_LENGTH = 15;
  private String plan;
  private String content;
  private boolean promptInEnglish = true;

  // don't serialize
  private transient Map<String,List<QAPair>> langToQuestion = null;

  private String englishSentence;
  private String meaning, context;
  private List<String> refSentences = new ArrayList<String>();
  private List<String> translitSentences = new ArrayList<String>();
  private STATE state;
  private List<CorrectAndScore> scores;
  private float avgScore;

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
   * @see mitll.langtest.server.database.exercise.SQLExerciseDAO#getExercise
   * @param id
   * @param content
   * @param promptInEnglish
   * @param recordAudio
   * @param tooltip
   * @param context
   */
  public Exercise(String plan, String id, String content, boolean promptInEnglish, boolean recordAudio, String tooltip,
                  String context) {
    super(id, tooltip);
    this.plan = plan;
    this.setContent(content);
    this.setPromptInEnglish(promptInEnglish);
    this.context = context;
  }

  /**
   * @see mitll.langtest.server.database.exercise.FileExerciseDAO#getSimpleExerciseForLine(String, int)
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
  }

  /**
   * @see mitll.langtest.server.database.exercise.FileExerciseDAO#getFlashcardExercise(int, String, String, String, String)
   * @param plan
   * @param id
   * @param content
   * @param sentenceRefs
   * @param tooltip
   */
  public Exercise(String plan, String id, String content, List<String> sentenceRefs, String tooltip) {
    super(id,tooltip);

    this.plan = plan;
    this.setContent(content);
    this.refSentences = sentenceRefs;
  }

  /**
   * @see mitll.langtest.server.database.exercise.FileExerciseDAO#getExerciseForLine(String)
   * @param plan
   * @param id
   * @param content
   * @param sentenceRef
   * @param tooltip
   */
  public Exercise(String plan, String id, String content, String sentenceRef, String tooltip) {
    this(plan, id, content, null, sentenceRef, tooltip);
  }

  public void setTooltip() {
    setTooltip(getCombinedTooltip());
  }

  public CommonShell getShellCombinedTooltip() {
    String combined = getCombinedTooltip();

    return new ExerciseShell(getID(), combined);
  }

  /**
   * Hack - if we have "N/A" for english, don't show it in the list.
   * @return
   */
  public String getCombinedTooltip() {
    String refSentence = getRefSentence();
    if (refSentence.length() > MAX_TOOLTIP_LENGTH) {
      refSentence = refSentence.substring(0, MAX_TOOLTIP_LENGTH);
    }
    boolean refSentenceEqualsTooltip = getTooltip().trim().equals(getRefSentence().trim());
    String combined = refSentenceEqualsTooltip ? getTooltip() : getTooltip() + (refSentence.isEmpty() ? "": " / " + refSentence);
    if (getTooltip().isEmpty() || getTooltip().equals("N/A")) combined = refSentence;
    return combined;
  }

/*
  public void addQuestion() {
    addQuestion(FL, "Please record the sentence above.", "", EMPTY_LIST);
  }
*/

  /**
   * @param lang
   * @param question
   * @param answer
   * @param alternateAnswers
   * @see mitll.langtest.server.database.exercise.SQLExerciseDAO#getExercise(String, String, net.sf.json.JSONObject)
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

  public String getRefSentence() {
    StringBuilder builder = new StringBuilder();
    for (String s : refSentences) {
      builder.append(s).append(" ");
    }
    return builder.toString();
  }

  /**
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise(String, int, org.apache.poi.ss.usermodel.Row, String, String, String, String, String, boolean, String, boolean)
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
   * @see mitll.langtest.server.database.Export#populateIdToExportMap(Exercise)
   */
  public List<QAPair> getQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(isPromptInEnglish() ? EN : FL);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

  /**
   * @see mitll.langtest.server.database.Export#addPredefinedAnswers(Exercise, boolean, java.util.Map)
   * @return
   */
  public List<QAPair> getEnglishQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(EN);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

  /**
   *
   * @return
   * @see mitll.langtest.server.database.Export#addPredefinedAnswers(Exercise, boolean, java.util.Map)
   */
  public List<QAPair> getForeignLanguageQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(FL);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

  public String getEnglish() {  return englishSentence;  }

  public void setContent(String content) { this.content = content;  }
  /**
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
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

  /**
   * @see mitll.langtest.shared.custom.UserExercise#copyFields(Exercise)  - only
   * @param context
   */
  public void setContext(String context) {
    this.context = context;
  }

  public Exercise toExercise() {
    return this;
  }

  @Override
  public long getModifiedDateTimestamp() {
    return 0;
  }

  @Override
  public STATE getState() {
    return state;
  }

  @Override
  public void setState(STATE state) {
    this.state = state;
  }

  public List<CorrectAndScore> getScores() {
    return scores;
  }

  @Override
  public void setScores(List<CorrectAndScore> scores) { this.scores = scores; }

  public float getAvgScore() {
    return avgScore;
  }
  public void setAvgScore(float avgScore) {
    this.avgScore = avgScore;
  }

  public String toString() {
    //  String moreAboutQuestions = DEBUG ? " : " +  getQuestionToString() : "";
    //  String questionInfo = langToQuestion == null ? " no questions" : " num questions " + langToQuestion.size() + moreAboutQuestions;

    Collection<AudioAttribute> audioAttributes1 = getAudioAttributes();

    // warn about attr that have no user
    StringBuilder builder = new StringBuilder();
    for (AudioAttribute attr:audioAttributes1) {
      if (attr.getUser() == null) {
        builder.append("\t").append(attr.toString()).append("\n");
      }
    }

    return "Exercise " + id +  " content bytes = " + content.length() + " english '" + getEnglish() +
        "' ref sentence '" + getRefSentence() +"' audio count = " + audioAttributes1.size()+
        (builder.toString().isEmpty() ? "":" \n\tmissing user audio " + builder.toString()) +
        //    " : " + questionInfo +
        " unit->lesson " + getUnitToValue();
  }

/*  private String getQuestionToString() {
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
  }*/
}
