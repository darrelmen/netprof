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
  public ImageResponse() {}
  public ImageResponse(int req, String imageURL) {this.req = req; this.imageURL = imageURL; }

  public String toString() { return " req " + req + " : " + imageURL; }
}
