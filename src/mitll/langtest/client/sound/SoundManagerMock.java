package mitll.langtest.client.sound;

import com.google.gwt.core.client.GWT;

/**
 * Created by IntelliJ IDEA.
 * User: GO22670
 * Date: 1/12/12
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoundManagerMock implements SoundManagerAPI {
  public SoundManagerMock() {
    GWT.log("making SoundManagerMock");
  }
  public void initialize() {
    GWT.log("SoundManagerMock initialize");
  }

  public void createSound(Sound sound, String title, String file) {
    GWT.log("SoundManagerMock createSound");
  }

  public void playInterval(Sound sound, double start, double end) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void destroySound(Sound sound) {
    GWT.log("SoundManagerMock destroySound");
  }

  public void pause(Sound sound) {
    GWT.log("SoundManagerMock pause");
  }

  public void play(Sound sound) {
    GWT.log("SoundManagerMock play");
  }

  public void setPosition(Sound sound, double position) {
    GWT.log("SoundManagerMock setPosition " + position);
  }

  public void setPositionAndPlay(Sound sound, double position) {
    GWT.log("SoundManagerMock setPositionAndPlay " + position);
  }

  public void exportStaticMethods() {
    GWT.log("SoundManagerMock exportStaticMethods");
  }

  public void loaded() {
    GWT.log("SoundManagerMock loaded");
  }

  public void songFinished(Sound sound) {
    GWT.log("SoundManagerMock songFinished");
  }

  public void songFirstLoaded(Sound sound, double durationEstimate) {
    GWT.log("SoundManagerMock songFirstLoaded " + durationEstimate);
  }

  public void songLoaded(Sound sound, double duration) {
    GWT.log("SoundManagerMock songLoaded " + duration);
  }

  public void update(Sound sound, double position) {
    GWT.log("SoundManagerMock update " + position);
  }
}
