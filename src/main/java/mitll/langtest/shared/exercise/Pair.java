package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 3/9/17.
 */
public class Pair implements IsSerializable {
  protected String property;
  protected String value;

  public Pair() {
  }

  public Pair(String property, String value) {
    this.property = property;
    this.value = value;
  }

  public String getProperty() {
    return property;
  }

  public String getValue() {
    return value;
  }

  public void swap() {
    String tmp = property;
    property = value;
    value = tmp;
  }

  public String toString() {
    return property + "=" + value;
  }
}
