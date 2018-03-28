package mitll.langtest.server.database;

import mitll.npdata.dao.SlickProject;
import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ReportStats {
  private final int projid;
  private final String language;
  private final String name;
  private int year;
  private final JSONObject jsonObject;
  private String html;

  public enum INFO {ALL_RECORDINGS, DEVICE_RECORDINGS, ALL_RECORDINGS_WEEKLY}

  private final Map<INFO, Integer> intKeyToValue = new HashMap<>();
  private final Map<INFO, Map<String, Integer>> intMultiKeyToValue = new HashMap<>();

  public void merge(ReportStats reportStats) {
    Map<INFO, Integer> intKeyToValue = getIntKeyToValue();
    Map<INFO, Integer> otherMap = reportStats.getIntKeyToValue();
    mergeKeyToValue(intKeyToValue, otherMap);

    reportStats.intMultiKeyToValue.forEach((k, v) -> intMultiKeyToValue.merge(k, v, (a, b) -> {
      mergeKeyToValue2(a, b);
      return a;
    }));
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

  private ReportStats(int projid, String language, String name, int year, JSONObject jsonObject) {
    this.projid = projid;
    this.language = language;
    this.name = name;
    this.year = year;
    //s this.recordings = recordings;
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

/*
  public int getRecordings() {
    return recordings;
  }

  public void setRecordings(int recordings) {
    this.recordings = recordings;
  }
*/

  public JSONObject getJsonObject() {
    return jsonObject;
  }

/*
  public void setJsonObject(JSONObject jsonObject) {
    this.jsonObject = jsonObject;
  }
*/

  /*
    public void put(INFO key, String value) {
      keyToValue.put(key, value);
    }

  */
  void putInt(INFO key, Integer value) {
    intKeyToValue.put(key, value);
  }

/*  void putIntMulti(INFO key, String key2, Integer value) {
    Map<String, Integer> weekToValue = intMultiKeyToValue.computeIfAbsent(key, k -> new TreeMap<>());
    weekToValue.put(key2, value);
  }*/

  /**
   * @param key
   * @param weekToCount
   * @see Report#getResultsForSet
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

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public String toString() {
    return "stats for " + name + " : " + year +// " = " + keyToValue;
        intKeyToValue;
  }
}
