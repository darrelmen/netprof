/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.sound;

/**
 * Created by go22670 on 8/1/14.
 */
public interface SimpleAudioListener {
  void songLoaded(double duration);

  void songFinished();
}
