/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import java.util.Collection;

/**
 * Created by GO22670 on 3/21/2014.
 */
public interface CommonShell extends Shell {
  String getEnglish();
  String getMeaning();
  String getForeignLanguage();
  String getTransliteration();

  String getContext();
  String getContextTranslation();
  String getDisplayID();

  MutableShell getMutableShell();

  /**
   * @see mitll.langtest.server.autocrt.DecodeCorrectnessChecker#getRefSentences(CommonExercise, String, boolean)
   * @return
   */
  Collection<String> getRefSentences();
}
