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

import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/20/2014.
 */
public interface CommonExercise extends CommonAudioExercise, ScoredExercise, HasUnitChapter {
  String getOldID();

  int getDominoID();

  /**
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#addAltFL
   * @return
   */
  String getAltFL();

  /**
   * @deprecated - can't guarantee we'll have this on the znetprof instance
   * @return
   */
  List<String> getFirstPron();

  boolean isPredefined();

  boolean hasContext();

  String getNoAccentFL();

  /**
   * Get the first context sentence.
   * @return
   */
  String getContext();
  String getContextTranslation();

  List<CommonExercise> getDirectlyRelated();

  boolean isSafeToDecode();

  /**
   * @return
   * @see mitll.langtest.client.custom.dialog.EditItem#didICreateThisItem
   */
  int getCreator();

  long getUpdateTime();

  int getProjectID();

  String getTransliteration();

  long getLastChecked();

  MutableExercise getMutable();

  MutableAudioExercise getMutableAudio();

  MutableAnnotationExercise getMutableAnnotation();

  CommonAnnotatable getCommonAnnotatable();

  /**
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#addAttributeToExercise
   * @param exerciseAttributes
   */
  void setAttributes(List<ExerciseAttribute> exerciseAttributes);

  List<ExerciseAttribute> getAttributes();

  boolean isContext();

  int getParentExerciseID();

  int getParentDominoID();
}
