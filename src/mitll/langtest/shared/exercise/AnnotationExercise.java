package mitll.langtest.shared.exercise;

import mitll.langtest.shared.ExerciseAnnotation;

import java.util.Collection;
import java.util.Map;

/**
 * Created by go22670 on 1/5/16.
 */
public interface AnnotationExercise {
  Map<String, ExerciseAnnotation> getFieldToAnnotation();

  ExerciseAnnotation getAnnotation(String field);

  void addAnnotation(String field, String status, String comment);

  Collection<String> getFields();
}
