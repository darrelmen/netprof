package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 6/5/17.
 */
public class VocabToken implements IsSerializable {
  private boolean isMarkup;
  private String token;
  private String norm;

  public VocabToken() {
  }

  public VocabToken(String token) {
    this.isMarkup = false;
    this.token = token;
  }

  public VocabToken(boolean isMarkup, String token) {
    this.isMarkup = isMarkup;
    this.token = token;
  }

  public String getNorm() {
    return norm;
  }

  public void setNorm(String norm) {
    this.norm = norm;
  }

  public String toString() {
    return token + (isMarkup ? " (HTML) " : "");
  }

  public boolean isMarkup() {
    return isMarkup;
  }

  public String getToken() {
    return token;
  }
}
