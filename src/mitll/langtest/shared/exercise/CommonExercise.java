/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import java.util.Collection;
import java.util.List;

/**
 * Created by GO22670 on 3/20/2014.
 */
public interface CommonExercise extends CommonAudioExercise, ScoredExercise {
  CommonShell getShell();

  List<String> getFirstPron();

  String getRefAudioIndex();

  boolean isPredefined();

  boolean hasContext();

  /**
   * Get the first context sentence.
   * @return
   */
  String getContext();

  Collection<CommonExercise> getDirectlyRelated();

  Collection<CommonExercise> getMentions();

  boolean isSafeToDecode();

  /**
   * @see mitll.langtest.client.custom.dialog.EditItem#didICreateThisItem(CommonExercise)
   * @return
   */
  int getCreator();

  long getUpdateTime();

  MutableExercise getMutable();
  MutableAudioExercise getMutableAudio();
  MutableAnnotationExercise getMutableAnnotation();
  CombinedMutableUserExercise getCombinedMutableUserExercise();
  CommonAnnotatable getCommonAnnotatable();
}
