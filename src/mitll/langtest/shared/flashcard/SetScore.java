/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.flashcard;

/**
 * Created by GO22670 on 3/4/14.
 */
public interface SetScore {
  int getCorrect();

  float getAvgScore();
  float getCorrectPercent();

  long getUserid();
}
