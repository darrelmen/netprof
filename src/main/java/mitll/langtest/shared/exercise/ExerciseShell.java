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
 */
class ExerciseShell extends BaseExercise implements CommonShell, MutableShell {
  protected String english = "";
  protected String meaning = "";
  protected String foreignLanguage = "";
  protected int numPhones;
  private float score = -1.0f;

  public ExerciseShell() {
  }

  /**
   * @see AudioExercise#AudioExercise(int, int)
   */
  ExerciseShell(int realID) {
    this("", "", "", realID, 0);
  }

  /**
   * @param english
   * @param meaning
   * @param foreignLanguage
   * @param realID
   * @paramx context
   * @paramx contextTranslation
   * @see #getShell()
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseShells
   */
  private ExerciseShell(String english,
                        String meaning,
                        String foreignLanguage,
                        int realID, int numPhones) {
    super(realID);
    this.english = english;
    this.meaning = meaning;
    this.foreignLanguage = foreignLanguage;
    this.numPhones = numPhones;
  }

  /**
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseShells
   */
  public CommonShell getShell() {
    return new ExerciseShell(english, meaning, foreignLanguage, getID(), numPhones);
  }

  public String getEnglish() {
    return english;
  }

  @Override
  public String getMeaning() {
    return meaning;
  }

  public void setMeaning(String meaning) {
    this.meaning = meaning;
  }

  @Override
  public void setScore(float score) {
    this.score = score;
  }

  @Override
  public String getForeignLanguage() {
    return foreignLanguage;
  }

  /**
   * TODO: don't put this here
   *
   * @return
   */
/*  public String getAltFL() {
    return "";
  }*/
  @Override
  public boolean equals(Object other) {
    //boolean checkOld = !getOldID().isEmpty();
    return other instanceof ExerciseShell &&
        //  (checkOld && getOldID().equals(((ExerciseShell) other).getOldID()) ||
        (getID() == ((ExerciseShell) other).getID()//)
        );
  }


  @Override
  public MutableShell getMutableShell() {
    return this;
  }

  @Override
  public Collection<String> getRefSentences() {
    return Collections.singleton(getForeignLanguage());
  }


  public void setForeignLanguage(String foreignLanguage) {
    this.foreignLanguage = foreignLanguage;
  }

  @Override
  public void setEnglish(String english) {
    this.english = english;
  }

  @Override
  public int getNumPhones() {
    return numPhones;
  }

  public float getScore() {
    return score;
  }

  public String toString() {
    return "ExerciseShell " +
        "id = " +
        //getOldID() + "/" +
        getID() +
        " : '" + getEnglish() + "'" +
        " states " + getState() + "/" + getSecondState();
  }
}
