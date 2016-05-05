/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/29/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 * <p>
 * TODO : UserList should not extend this
 */
public class ExerciseShell extends BaseExercise implements CommonShell, MutableShell {
  protected String english;
  protected String meaning;
  protected String foreignLanguage;
  protected String transliteration;
  String displayID;

  public ExerciseShell() {
  }

  /**
   * @param id
   * @see AudioExercise#AudioExercise(String)
   * @see UserList#UserList()
   */
  public ExerciseShell(String id) {
    this(id, "", "", "", "", id);
  }

  /**
   * @param id
   * @param english
   * @param meaning
   * @param foreignLanguage
   * @param transliteration
   * @param displayID
   * @paramx context
   * @paramx contextTranslation
   * @see #getShell()
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseShells
   */
  private ExerciseShell(String id, String english, String meaning, String foreignLanguage,
                        String transliteration, String displayID) {
    super(id);
    this.english = english;
    this.meaning = meaning;
    this.foreignLanguage = foreignLanguage;
    this.transliteration = transliteration;
    this.displayID = displayID;
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseShells(java.util.Collection)
   */
  public CommonShell getShell() {
    return new ExerciseShell(getID(), english, meaning, foreignLanguage, transliteration, displayID);
  }

  public String getEnglish() {
    return english;
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
  public boolean equals(Object other) {
    return other instanceof ExerciseShell && getID().equals(((ExerciseShell) other).getID());
  }

  @Override
  public String getDisplayID() {
    return displayID;
  }

  @Override
  public MutableShell getMutableShell() {
    return this;
  }

  @Override
  public Collection<String> getRefSentences() {
    return Collections.singleton(getForeignLanguage());
  }

  @Override
  public String getTransliteration() {
    return transliteration;
  }

  public void setForeignLanguage(String foreignLanguage) {
    this.foreignLanguage = foreignLanguage;
  }

  @Override
  public void setEnglish(String english) {
    this.english = english;
  }

  public String toString() {
    return "Exercise id = " + getID() + "/" + getEnglish() + " states " + getState() + "/" + getSecondState();
  }
}
