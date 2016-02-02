/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import java.util.Collection;
import java.util.List;

/**
 * Created by GO22670 on 3/20/2014.
 */
public interface CommonExercise extends CommonShell, AudioAttributeExercise, AnnotationExercise, ScoredExercise {
  /**
   * @see mitll.langtest.server.autocrt.DecodeCorrectnessChecker#getRefSentences(CommonExercise, String, boolean)
   * @return
   */
  Collection<String> getRefSentences();

  CommonShell getShell();

  List<String> getFirstPron();

  String getRefAudioIndex();

  boolean isPredefined();

  MutableExercise getMutable();
  MutableAudioExercise getMutableAudio();
  MutableAnnotationExercise getMutableAnnotation();
  CombinedMutableUserExercise getCombinedMutableUserExercise();
}
