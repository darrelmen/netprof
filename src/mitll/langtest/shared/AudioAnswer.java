package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 7/6/12
* Time: 7:01 PM
* To change this template use File | Settings | File Templates.
*/
public class AudioAnswer implements IsSerializable {
  public String path;
  public boolean valid;

  public AudioAnswer() {}
  public AudioAnswer(String path, boolean valid) { this.path = path; this.valid = valid; }
  public String toString() { return "Path " + path; }
}
