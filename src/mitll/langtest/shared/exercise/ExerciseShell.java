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

package mitll.langtest.shared.exercise;

import mitll.langtest.shared.custom.UserList;

import java.util.Collection;
import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/29/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 * <p>
 * TODO : UserList should not extend this
 */
public class ExerciseShell extends BaseExercise implements CommonShell, MutableShell {
  protected String englishSentence;
  protected String meaning;
  protected String foreignLanguage;
  protected String context;
  protected String transliteration;
  protected String contextTranslation;
  protected String displayID;

  public ExerciseShell() {
  }

  /**
   * @param id
   * @see AudioExercise#AudioExercise(String)
   * @see UserList#UserList()
   */
  public ExerciseShell(String id) {
    this(id, "", "", "", "", "", "", id);
  }

  /**
   * @param id
   * @param englishSentence
   * @param meaning
   * @param foreignLanguage
   * @param transliteration
   * @param context
   * @param contextTranslation
   * @param displayID
   * @see #getShell()
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseShells
   */
  private ExerciseShell(String id, String englishSentence, String meaning, String foreignLanguage,
                        String transliteration, String context, String contextTranslation, String displayID) {
    super(id);
    this.englishSentence = englishSentence;
    this.meaning = meaning;
    this.foreignLanguage = foreignLanguage;
    this.transliteration = transliteration;
    this.context = context;
    this.contextTranslation = contextTranslation;
    this.displayID = displayID;
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseShells(java.util.Collection)
   */
  public CommonShell getShell() {
    return new ExerciseShell(getID(), englishSentence, meaning, foreignLanguage, transliteration, context, contextTranslation, displayID);
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
  public boolean equals(Object other) {
    return other instanceof ExerciseShell && getID().equals(((ExerciseShell) other).getID());
  }

  @Override
  public String getContext() {
    return context;
  }

  @Override
  public String getContextTranslation() {
    return contextTranslation;
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
    this.englishSentence = english;
  }

  public String toString() {
    return "Exercise id = " + getID() + "/" + getEnglish() + " states " + getState() + "/" + getSecondState();
  }
}
