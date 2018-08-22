package mitll.langtest.client.scoring;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

public class HttpStatus {
  private int code;
  private String statusText;

  HttpStatus(JSONObject jsonObject) {
    JSONNumber code = jsonObject.get("code").isNumber();
    this.code = (int) (code == null ? -1 : code.doubleValue());
    JSONString statusText = jsonObject.get("statusText").isString();
    this.statusText = statusText == null ? "Unknown" : statusText.stringValue();
  }

  boolean isWellFormed() {
    return code != -1;
  }

  public int getCode() {
    return code;
  }

  public String getStatusText() {
    return statusText;
  }

  public boolean isError() {
    return code != 200;
  }

  public String toString() {
    return code + " : " + statusText;
  }
}
