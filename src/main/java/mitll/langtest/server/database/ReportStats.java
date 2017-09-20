package mitll.langtest.server.database;

import mitll.npdata.dao.SlickProject;
import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ReportStats {
  private int projid;
  private String language;
  private String name;
  private int year;
  private int recordings;
  private JSONObject jsonObject;
  private String html;

  public void merge(ReportStats reportStats) {
    reportStats.getIntKeyToValue().forEach((k, v) -> {
      Map<INFO, Integer> intKeyToValue = getIntKeyToValue();
      intKeyToValue.merge(k, v, (a, b) -> a + b);
    });
  }

  public void setYear(int year) {
    this.year = year;
  }

  public enum INFO {ALL_RECORDINGS, DEVICE_RECORDINGS}

  ;

  Map<INFO, String> keyToValue = new HashMap<>();
  Map<INFO, Integer> intKeyToValue = new HashMap<>();


  public ReportStats(ReportStats reportStats) {
    this(reportStats.projid,
        reportStats.getLanguage(),
        reportStats.name,
        reportStats.year,
        reportStats.jsonObject);
  }

  public ReportStats(SlickProject project, int year) {
    this(project.id(), project.language(), project.name(), year, new JSONObject());
  }

  public ReportStats(SlickProject project, int year, JSONObject jsonObject) {
    this(project.id(), project.language(), project.name(), year, jsonObject);
  }

  public ReportStats(int projid, String language, String name, int year) {
    this(projid, language, name, year, new JSONObject());
  }

  public ReportStats(int projid, String language, String name, int year, JSONObject jsonObject) {
    this.projid = projid;
    this.language = language;
    this.name = name;
    this.year = year;
    this.recordings = recordings;
    this.jsonObject = jsonObject;
  }

  public int getProjid() {
    return projid;
  }

  public String getLanguage() {
    return language;
  }

  public String getName() {
    return name;
  }

  public int getYear() {
    return year;
  }

  public int getRecordings() {
    return recordings;
  }

  public void setRecordings(int recordings) {
    this.recordings = recordings;
  }

  public JSONObject getJsonObject() {
    return jsonObject;
  }

  public void setJsonObject(JSONObject jsonObject) {
    this.jsonObject = jsonObject;
  }

  public void put(INFO key, String value) {
    keyToValue.put(key, value);
  }

  public void putInt(INFO key, Integer value) {
    intKeyToValue.put(key, value);
  }

  public Map<INFO, Integer> getIntKeyToValue() {
    return intKeyToValue;
  }

  public String toString() {
    return "stats for " + name + " : " + year + " = " + keyToValue + "/" + intKeyToValue;
  }

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }
}
