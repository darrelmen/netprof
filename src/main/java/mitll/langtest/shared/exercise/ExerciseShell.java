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

import mitll.langtest.client.list.ExerciseList;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static mitll.langtest.shared.analysis.SimpleTimeAndScore.SCALE;

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
public class ExerciseShell extends BaseExercise implements CommonShell, MutableShell, ScoredExercise {
  protected String english = "";
  protected String meaning = "";
  protected String foreignLanguage = "";
  protected boolean shouldSwap;

  /**
   *
   */
  private boolean isContext;

  int numPhones;
  private int numContext;

  /**
   *
   */
  String altfl = "";
  private int score = -1;
  private List<CorrectAndScore> scores = new ArrayList<>();

  public ExerciseShell() {
  }

  /**
   * @see AudioExercise#AudioExercise(int, int, boolean)
   */
  ExerciseShell(int realID, boolean isContext) {
    this("", "", "", realID, 0, isContext, 0);
  }

  /**
   * @param english
   * @param meaning
   * @param foreignLanguage
   * @param realID
   * @param isContext
   * @see Exercise#getShell
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseShells
   */
  ExerciseShell(String english,
                String meaning,
                String foreignLanguage,
                int realID,
                int numPhones,
                boolean isContext,
                int numContext) {
    super(realID);
    this.english = english;
    this.meaning = meaning;
    this.foreignLanguage = foreignLanguage;
    this.numPhones = numPhones;
    this.isContext = isContext;
    this.numContext = numContext;
  }

  /**
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseShells
   */
//  public CommonShell getShell() {
//    return new ExerciseShell(english, meaning, foreignLanguage, getID(), numPhones, isContext);
//  }
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
  public String getForeignLanguage() {
    return foreignLanguage;
  }

  public void setForeignLanguage(String foreignLanguage) {
    this.foreignLanguage = foreignLanguage;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExerciseShell &&
        (getID() == ((ExerciseShell) other).getID());
  }

  @Override
  public MutableShell getMutableShell() {
    return this;
  }

  @Override
  public void setEnglish(String english) {
    this.english = english;
  }

  /**
   * @return
   * @see mitll.langtest.client.list.ListSorting#compPhones
   */
  @Override
  public int getNumPhones() {
    return numPhones;
  }

  /**
   * @see ExerciseList#setScore
   * @see mitll.langtest.client.list.FacetExerciseList#setScores
   * @param score
   */
  @Override
  public void setScore(float score) {
    this.score = toInt(score);
  }

  /**
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#setProgressBarScore
   */
  public float getScore() {
    return fromInt(score);
  }

  private int toInt(float value) {
    return (int) (value * SCALE);
  }

  private float fromInt(int value) {
    return ((float) value) / SCALE;
  }

  public int getRawScore() {
    return score;
  }

  public boolean hasScore() {
    return score > 0;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#toSlick(CommonExercise, boolean, int, int, boolean, Collection)
   */
  public String getAltFL() {
    return altfl;
  }

  /**
   * @return
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#getAltFL
   */
  public String getAltFLToShow() {
    return shouldSwap ? foreignLanguage : altfl;
  }

  /**
   * @return
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#getFL
   */
  public String getFLToShow() {
    return shouldSwap ? altfl : foreignLanguage;
  }

  public boolean shouldSwap() {
    return shouldSwap;
  }

  /**
   * @return
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#getFirstRow
   */
  public List<CorrectAndScore> getScores() {
    return scores;
  }

  /**
   * @param scores
   * @see mitll.langtest.server.database.result.BaseResultDAO#attachScoreHistory(int, CommonExercise, mitll.langtest.shared.project.Language)
   */
  @Override
  public void setScores(List<CorrectAndScore> scores) {
    this.scores = scores;
  }

  @Override
  public boolean isContext() {
    return isContext;
  }

  @Override
  public int getNumContext() {
    return numContext;
  }

  public String toString() {
    return "ExerciseShell " +
        "\n\tid        " + getID() +
        "\n\tisContext " + isContext() +
        "\n\tshouldSwap " + shouldSwap() +
        "\n\t: '" + getEnglish() + "'"
        //+
        //" states " + getState()
        //+ "/" + getSecondState()
        ;
  }
}
