package mitll.langtest.client.sound;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 8/31/12
 * Time: 12:17 AM
 * To change this template use File | Settings | File Templates.
 */
public interface AudioControl {
  void reinitialize();
  void songFirstLoaded(double durationEstimate);
  void songLoaded(double duration);
  void songFinished();
  void update(double position);
}
