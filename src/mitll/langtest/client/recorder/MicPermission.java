package mitll.langtest.client.recorder;

/**
 * Interface for getting events back from allow/deny Flash mic permission dialog.
 *
 * User: GO22670
 * Date: 7/5/12
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public interface MicPermission {
  void gotPermission();
  void gotDenial();
}
