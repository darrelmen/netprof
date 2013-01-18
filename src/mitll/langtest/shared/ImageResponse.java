package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 9/17/12
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageResponse implements IsSerializable {
  public int req;
  public String imageURL;
  public double durationInSeconds;
  public boolean successful = false;
  public ImageResponse() {}

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getImageForAudioFile(int, String, String, int, int)
   * @param req
   * @param imageURL
   * @param durationInSeconds
   */
  public ImageResponse(int req, String imageURL, double durationInSeconds) {
    this.req = req;
    this.imageURL = imageURL;
    this.successful = true;
    this.durationInSeconds = durationInSeconds;
  }

  public String toString() {
    return successful ? " req " + req + " : " + imageURL + " dur " + durationInSeconds : " error for req " + req;
  }
}
