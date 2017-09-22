package mitll.langtest.server.database;

import mitll.npdata.dao.SlickProject;
import net.sf.json.JSONObject;

import java.util.*;

public class ReportStats {
  private int projid;
  private String language;
  private String name;
  private int year;
  private int recordings;
  private JSONObject jsonObject;
  private String html;

  public void merge(ReportStats reportStats) {
    Map<INFO, Integer> intKeyToValue = getIntKeyToValue();
    Map<INFO, Integer> otherMap = reportStats.getIntKeyToValue();
    mergeKeyToValue(intKeyToValue, otherMap);

    reportStats.intMultiKeyToValue.forEach((k, v) -> {
      intMultiKeyToValue.merge(k, v, (a, b) -> {
        mergeKeyToValue2(a, b);
        return a;
      });
    });
  }

  private void mergeKeyToValue(Map<INFO, Integer> intKeyToValue, Map<INFO, Integer> otherMap) {
    otherMap.forEach((k, v) -> intKeyToValue.merge(k, v, (a, b) -> a + b));
  }

  private void mergeKeyToValue2(Map<String, Integer> intKeyToValue, Map<String, Integer> otherMap) {
    otherMap.forEach((k, v) -> intKeyToValue.merge(k, v, (a, b) -> a + b));
  }

  public void setYear(int year) {
    this.year = year;
  }

  public enum INFO {ALL_RECORDINGS, DEVICE_RECORDINGS, ALL_RECORDINGS_WEEKLY}

  ;

  Map<INFO, String> keyToValue = new HashMap<>();
  Map<INFO, Integer> intKeyToValue = new HashMap<>();
  Map<INFO, Map<String, Integer>> intMultiKeyToValue = new HashMap<>();


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

  void putInt(INFO key, Integer value) {
    intKeyToValue.put(key, value);
  }

  void putIntMulti(INFO key, String key2, Integer value) {
    Map<String, Integer> weekToValue = intMultiKeyToValue.computeIfAbsent(key, k -> new TreeMap<>());
    weekToValue.put(key2, value);
  }

  /**
   * @see Report#getResultsForSet
   * @param key
   * @param weekToCount
   */
  void putIntMulti(INFO key, Map<String, Integer> weekToCount) {
    intMultiKeyToValue.put(key, weekToCount);
  }

  public Map<String, Integer> getKeyToValue(INFO key) {
    return intMultiKeyToValue.get(key);
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
