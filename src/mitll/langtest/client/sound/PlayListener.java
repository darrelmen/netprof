package mitll.langtest.client.sound;

/**
 * Tell listeners that audio playback has started or stopped.
 * Helpful for enabling/disabling other widgets like a record button.
 *
 * User: GO22670
 * Date: 12/31/12
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PlayListener {
  void playStarted();
  void playStopped();
}
