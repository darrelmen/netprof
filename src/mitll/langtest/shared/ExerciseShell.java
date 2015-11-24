/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/29/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseShell implements IsSerializable, CommonShell {
  private String tooltip;
  protected String id;

  protected STATE state = STATE.UNSET;
  protected STATE secondState = STATE.UNSET;

  protected String englishSentence;
  protected String meaning;
  protected String foreignLanguage;

  public ExerciseShell() {
  }

  /**
   * @param id
   * @see AudioExercise#AudioExercise(String)
   */
  public ExerciseShell(String id) {
    this.id = id;
    this.englishSentence = "";
    this.meaning = "";
    this.foreignLanguage = "";
  }

  public ExerciseShell(String id, String tooltip) {
    this.id = id;
    setTooltip(tooltip);
    this.englishSentence = "";
    this.meaning = "";
    this.foreignLanguage = "";
  }

  public ExerciseShell(String id, String tooltip, String englishSentence, String meaning, String foreignLanguage) {
    this.id = id;
    setTooltip(tooltip);
    this.englishSentence = englishSentence;
    this.meaning = meaning;
    this.foreignLanguage = foreignLanguage;
  }

  public String getID() {
    return id;
  }

  public void setID(String id) {
    this.id = id;
  }

  public String getTooltip() {
    return tooltip;
  }

  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
    //if (tooltip.isEmpty() && !id.equals("-1")) throw new IllegalArgumentException("tooltip is empty for " + id);
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

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseShellsShort(java.util.Collection)
   */
  public CommonShell getShell() {
    return new ExerciseShell(getID(), getTooltip(), englishSentence, meaning, foreignLanguage);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExerciseShell && getID().equals(((ExerciseShell) other).getID());
  }

  public String toString() {
    return "Exercise id = " + getID() + "/" + getTooltip() + " states " + getState() + "/" + getSecondState();
  }
}
