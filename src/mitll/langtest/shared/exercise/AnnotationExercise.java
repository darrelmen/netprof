package mitll.langtest.shared.exercise;

import mitll.langtest.shared.ExerciseAnnotation;

import java.util.Collection;
import java.util.Map;

/**
 * Created by go22670 on 1/5/16.
 */
public interface AnnotationExercise {
  /**
   * @see mitll.langtest.server.database.custom.UserListManager#duplicate(CommonExercise)
   * @return
   */
  Map<String, ExerciseAnnotation> getFieldToAnnotation();

  /**
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#setFields(CommonShell)
   * @param field
   * @return
   */
  ExerciseAnnotation getAnnotation(String field);

  Collection<String> getFields();
}
