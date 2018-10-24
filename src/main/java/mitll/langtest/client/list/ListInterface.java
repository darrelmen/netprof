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

package mitll.langtest.client.list;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.shared.exercise.HasID;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/27/12
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ListInterface<T extends HasID, U extends HasID> extends RequiresResize, Reloadable {
  /**
   * @seex Navigation#showLearnAndItem
   * @param id
   * @return
   */
  T byID(int id);

  void loadByID(int id);

  Map<Integer, T> getIdToExercise();

  void addExercise(T es);

  T simpleRemove(int id);

  /**
   * @param factory
   * @seex mitll.langtest.client.custom.content.AVPHelper#makeExerciseList(Panel, String)
   */
  void setFactory(ExercisePanelFactory<T,U> factory);

  /**
   * @return
   * @see mitll.langtest.client.custom.content.NPFHelper#doInternalLayout
   */
  Widget getExerciseListOnLeftSide();

  void loadFirst();

  boolean loadNext();

  /**
   * @param current
   * @return
   * @see ListInterface#loadNextExercise
   */
  boolean loadNextExercise(HasID current);

  boolean loadNextExercise(int id);

  boolean loadPrev();

  boolean loadPreviousExercise(HasID current);

  boolean onFirst(HasID current);

  boolean onLast();

  boolean onLast(HasID current);

  int getComplete();

  void flushWith(Comparator<T> comparator);

  int getSize();

  /**
   * @return
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#getAnswerWidget
   */
  boolean isPendingReq();

  void hide();

  void addListChangedListener(ListChangeListener<T> listener);

  void setShuffle(boolean doShuffle);

  void simpleSetShuffle(boolean doShuffle);

  boolean isShuffle();

  void reload(Map<String, Collection<String>> typeToSection);

  /**
   * @param text
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#makeClickableText(String, String, String, boolean)
   */
  void searchBoxEntry(String text);

  String getTypeAheadText();

  int getIndex(int currentID);

  void markCurrentExercise(int id);

  void gotClickOnItem(T newExercise);

  /**
   * @see mitll.langtest.client.scoring.SimpleRecordAudioPanel#useScoredResult
   * @param id
   * @param hydecScore
   */
  void setScore(int id, float hydecScore);

  boolean isCurrentReq(int req);
}
