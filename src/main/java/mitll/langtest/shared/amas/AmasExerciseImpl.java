/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared.amas;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.io.InputStream;
import java.util.*;

/**
 * Representation of a individual item of work the user sees.  Could be a pronunciation exercise or a question(s)
 * based on a prompt.
 * <p>
 * TODO : consider subclass for pronunciation exercises?
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class AmasExerciseImpl implements CommonShell, HasUnitChapter {
  public static final String EN = "en";
  public static final String FL = "fl";

  private String content;
  //private String contentTrans;

  private String orient;
  //private String orientTrans;
  //private String ilr;
  private boolean isListening;
  private String audioURL;
  private Map<String, String> unitToValue = new HashMap<String, String>();

  private Map<String, List<QAPair>> langToQuestion = null;

  private String id;
  int realID;
  private STATE state = STATE.UNSET;
  private String altID;

  public AmasExerciseImpl() {
  }  // required for serialization


  public AmasExerciseImpl(String id, String content, String altID) {
    this.id = id;
    this.content = content;
    this.altID = altID;
  }

  /**
   * @param id
   * @see mitll.langtest.server.database.exercise.AMASJSONURLExerciseDAO#toAMASExercise
   */
  public AmasExerciseImpl(String id, String content, //String altID,
                          String contentTrans,
                          String orient,
                          String orientTrans,
                          boolean isListening,
                          String ilr,
                          String audioURL) {
    this.id = id;
    this.content = content;
    this.altID = id;  // TODO : how does this work? do we need to worry about it?
    //this.contentTrans = contentTrans;
    this.orient = orient;
    // this.orientTrans = orientTrans;
    this.isListening = isListening;
    // this.ilr = ilr;
    this.audioURL = audioURL;
  }

  public String getAltID() {
    return altID;
  }

  private void addQuestion(boolean isFL, String question, String answer) throws Exception {
    addQuestion(isFL ? FL : EN, question, answer);
  }

/*  public void addQuestion(boolean isFL, String question, String[] alternateAnswers) throws Exception {
    addQuestion(isFL ? FL : EN, question, alternateAnswers);
  }*/

  public void addQuestion(boolean isFL, String question, Collection<String> alternateAnswers) throws Exception {
    addQuestion(isFL ? FL : EN, question, alternateAnswers);
  }

  private void addQuestion(String lang, String question, String answer) {
    List<String> serializableCollection = new ArrayList<>();
    serializableCollection.add(answer);
    addQuestion(lang, new QAPair(question, serializableCollection));
  }

  private void addQuestion(String lang, String question, String[] alternateAnswers) {
    addQuestion(lang, new QAPair(question, Arrays.asList(alternateAnswers)));
  }

  /**
   * @param lang
   * @param question
   * @param alternateAnswers
   * @see mitll.langtest.server.amas.FileExerciseDAO#addQuestion
   */
  public void addQuestion(String lang, String question, Collection<String> alternateAnswers) {
    addQuestion(lang, new QAPair(question, alternateAnswers));
  }

  /**
   * @param lang
   * @param pairs
   * @see mitll.langtest.server.amas.FileExerciseDAO#readExercises(String, String, String, InputStream)
   */
  public void addQuestions(String lang, List<QAPair> pairs) {
    for (QAPair pair : pairs) {
      addQuestion(lang, pair);
    }
  }

  private void addQuestion(String lang, QAPair pair) {
    if (langToQuestion == null) langToQuestion = new HashMap<>();
    List<QAPair> qaPairs = langToQuestion.get(lang);
    if (qaPairs == null) {
      langToQuestion.put(lang, qaPairs = new ArrayList<QAPair>());
    }

    qaPairs.add(pair);
  }

  /**
   * @return
   * @see mitll.langtest.server.database.Export#populateIdToExportMap(AmasExerciseImpl)
   */
  public List<QAPair> getQuestions() {
    return getForeignLanguageQuestions();
  }

  /**
   * @return
   * @see mitll.langtest.server.database.Export#addPredefinedAnswers
   */
  public List<QAPair> getEnglishQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(EN);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.Export#addPredefinedAnswers
   */
  //@Override
  public List<QAPair> getForeignLanguageQuestions() {
    List<QAPair> qaPairs = langToQuestion == null ? new ArrayList<QAPair>() : langToQuestion.get(FL);
    return qaPairs == null ? new ArrayList<QAPair>() : qaPairs;
  }

  public String getContent() {
    return content;
  }

  public void setScores(List<CorrectAndScore> scores) {
  }

  public Map<String, String> getUnitToValue() {
    return unitToValue;
  }

  /**
   * @param unit
   * @param value
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson
   */

  public void addUnitToValue(String unit, String value) {
    if (value == null) return;
    this.getUnitToValue().put(unit, value);
  }

  @Override
  public void addPair(Pair pair) {

  }

  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }

  public String getOldID() {
    return id;
  }

  @Override
  public int getID() {
    return realID;
  }

  @Override
  public STATE getState() {
    return state;
  }

  @Override
  public void setState(STATE state) {
    this.state = state;
  }

  /**
   * TODO : refactor so this isn't needed.
   *
   * @return
   */
  @Override
  public STATE getSecondState() {
    return null;
  }

  @Override
  public void setSecondState(STATE state) {
  }

  public String toString() {
    return "Exercise " + getOldID() + (getAltID().isEmpty() ? "" : "/" + getAltID()) +
        " Questions " + getQuestions() + " unit->lesson " + getUnitToValue();
  }

  // TODO : workaround for the moment - hard to use the current exercise lists without extending from CommonShell...?
  @Override
  public String getEnglish() {
    return null;
  }

  @Override
  public String getMeaning() {
    return null;
  }

  @Override
  public String getForeignLanguage() {
    return "";
  }

  public String getAltFL() {
    return null;
  }

  public int getDominoID() {
    return realID;
  }

  @Override
  public MutableShell getMutableShell() {
    return null;
  }

  @Override
  public Collection<String> getRefSentences() {
    return Collections.singleton(getForeignLanguage());
  }

  @Override
  public CommonShell getShell() {
    return null;
  }

  @Override
  public int getNumPhones() {
    return 0;
  }

  @Override
  public int getRawScore() {
    return 0;
  }

  @Override
  public float getScore() {
    return 0;
  }

  @Override
  public boolean hasScore() {
    return false;
  }
/*

  @Override
  public String getCforeignLanguage() {
    return null;
  }

  @Override
  public String getCenglish() {
    return null;
  }
*/

  public String getOrient() {
    return orient;
  }

  public boolean isListening() {
    return isListening;
  }

  /**
   * @return
   * @see mitll.langtest.client.amas.AudioExerciseContent#addAudioRow(AmasExerciseImpl, String, boolean, Panel, int, int)
   */
  public String getAudioURL() {
    return audioURL;
  }

  @Override
  public int compareTo(HasID o) {
    return Integer.compare(getID(), o.getID());
  }

  public String getId() {
    return id;
  }
}
