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

import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/1/16.
 */
public interface MutableExercise extends CommonShell, MutableShell {
  void setFirstPron(List<String> phones);

  void setTransliteration(String transliteration);

  /**
   * @see BaseResultDAO#attachScoreHistory
   * @param scoreTotal
   */
  void setScores(List<CorrectAndScore> scoreTotal);

  void setRefSentences(Collection<String> orDefault);

  void setSafeToDecode(boolean isSafeToDecode);

  void addContextExercise(CommonExercise contextExercise);

  Collection<CommonExercise> getDirectlyRelated();

  void setOldID(String id);

  void setID(int id);

  void setCreator(int id);

  void setPredef(boolean isPredef);
  /**
   * @see mitll.langtest.client.custom.exercise.CommentNPFExercise#addAltFL
   * @return
   */
  String getAltFL();

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson
   * @param unit
   * @param value
   */
  void addUnitToValue(String unit, String value);

  void setUnitToValue(Map<String,String> unitToValue);
}
