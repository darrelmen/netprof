package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;

public interface IProject {
  Language getLanguageEnum();

  CommonExercise getExerciseByID(int id);
}
