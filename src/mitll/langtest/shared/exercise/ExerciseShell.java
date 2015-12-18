/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.custom.UserList;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/29/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseShell implements IsSerializable, CommonShell {
  protected String id;

  private STATE state = STATE.UNSET;
  private STATE secondState = STATE.UNSET;

  protected String englishSentence;
  protected String meaning;
  protected String foreignLanguage;

  public ExerciseShell() {
  }

  /**
   * @param id
   * @see AudioExercise#AudioExercise(String)
   * @see UserList#UserList()
   */
  public ExerciseShell(String id) {
    this(id, "", "", "");
  }

  /**
   * @param id
   * @param englishSentence
   * @param meaning
   * @param foreignLanguage
   * @see #getShell()
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseShells
   */
  private ExerciseShell(String id, String englishSentence, String meaning, String foreignLanguage) {
    this.id = id;
    this.englishSentence = englishSentence;
    this.meaning = meaning;
    this.foreignLanguage = foreignLanguage;
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseShells(java.util.Collection)
   */
  public CommonShell getShell() {
    return new ExerciseShell(getID(), englishSentence, meaning, foreignLanguage);
  }

  public String getID() {
    return id;
  }

  public void setID(String id) {
    this.id = id;
  }

  public String getEnglish() {
    return englishSentence;
  }

  @Override
  public String getMeaning() {
    return meaning;
  }

  @Override
  public String getForeignLanguage() {
    return foreignLanguage;
  }

  @Override
  public STATE getState() {
    return state;
  }

  @Override
  public void setState(STATE state) {
    this.state = state;
  }

  @Override
  public STATE getSecondState() {
    return secondState;
  }

  @Override
  public void setSecondState(STATE state) {
    this.secondState = state;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExerciseShell && getID().equals(((ExerciseShell) other).getID());
  }

  public String toString() {
    return "Exercise id = " + getID() + "/" + getEnglish() + " states " + getState() + "/" + getSecondState();
  }
}
