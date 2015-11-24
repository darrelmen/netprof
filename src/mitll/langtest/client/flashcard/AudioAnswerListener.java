/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.flashcard;

import mitll.langtest.shared.AudioAnswer;

/**
 * Created by go22670 on 2/11/14.
 */
interface AudioAnswerListener {
  void receivedAudioAnswer(AudioAnswer result);
}
