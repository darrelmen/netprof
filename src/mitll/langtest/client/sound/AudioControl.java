/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.sound;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 8/31/12
 * Time: 12:17 AM
 * To change this template use File | Settings | File Templates.
 */
public interface AudioControl extends SimpleAudioListener {
  void reinitialize();
  void songFirstLoaded(double durationEstimate);

  void update(double position);
}
