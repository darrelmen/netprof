/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.shared.custom.UserExercise;

/**
 * Created by GO22670 on 3/21/2014.
 * @see EditItem#getNewItem()
 */
public interface CommonUserExercise extends CommonExercise {
  long getCreator();

  long getUniqueID();

  UserExercise toUserExercise();
}
