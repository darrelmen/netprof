package mitll.langtest.client.scoring;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

class HttpStatus {
  private int code;
  private String statusText;

  HttpStatus(JSONObject jsonObject) {
    JSONValue code1 = jsonObject.get("code");
    if (code1 != null) {
      JSONNumber code = code1.isNumber();
      this.code = (int) (code == null ? -1 : code.doubleValue());
      JSONString statusText = jsonObject.get("statusText").isString();
      this.statusText = statusText == null ? "Unknown" : statusText.stringValue();
    }
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

  public String toString() {
    return code + " : " + statusText;
  }
}
