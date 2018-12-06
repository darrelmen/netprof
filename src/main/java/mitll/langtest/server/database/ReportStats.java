package mitll.langtest.server.database;

import com.google.gson.JsonObject;
import mitll.npdata.dao.SlickProject;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportStats {
  private final int projid;
  private final String language;
  private final String name;
  private int year;
  private final JsonObject jsonObject;
  private String html;

  public enum INFO {
    /**
     * @see Report#getResultsForSet
     * @see mitll.langtest.server.database.excel.ReportToExcel#writeHistoricalSheet
     */
    ALL_RECORDINGS,
    /**
     * @see Report#getResultsForSet
     */
    DEVICE_RECORDINGS,
    /**
     * @see mitll.langtest.server.database.excel.ReportToExcel#writeWeeklySheet
     * @see Report#getResultsForSet
     **/
    ALL_RECORDINGS_WEEKLY,
    DEVICE_RECORDINGS_WEEKLY
  }

  private final Map<INFO, Integer> intKeyToValue = new HashMap<>();
  private final Map<INFO, Map<String, Integer>> intMultiKeyToValue = new LinkedHashMap<>();


  public ReportStats getMerged(ReportStats toMerge) {
    ReportStats copy = new ReportStats(true, this);
    copy.merge(toMerge);
    return copy;
  }

  private void merge(ReportStats reportStats) {
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

  /**
   * @param year
   * @see Report#getReportForProject
   */
  public void setYear(int year) {
    this.year = year;
  }

  /**
   * @param reportStats
   */
  ReportStats(ReportStats reportStats) {
    this(reportStats.projid,
        reportStats.getLanguage(),
        reportStats.name,
        reportStats.year,
        reportStats.jsonObject);
  }

  public ReportStats(boolean full, ReportStats toCopy) {
    this(toCopy);

    intKeyToValue.putAll(toCopy.intKeyToValue);


    Map<INFO, Map<String, Integer>> intMultiKeyToValue = toCopy.intMultiKeyToValue;

    intMultiKeyToValue.forEach((k, v) -> {
      this.intMultiKeyToValue.put(k, new LinkedHashMap<>(v));
    });
    // intMultiKeyToValue.putAll(toCopy.intMultiKeyToValue);
    this.html = toCopy.html;
  }

  ReportStats(SlickProject project, int year) {
    this(project.id(), project.language(), project.name(), year, new JsonObject());
  }

  ReportStats(SlickProject project, int year, JsonObject jsonObject) {
    this(project.id(), project.language(), project.name(), year, jsonObject);
  }

  ReportStats(int projid, String language, String name, int year) {
    this(projid, language, name, year, new JsonObject());
  }

  private ReportStats(int projid, String language, String name, int year, JsonObject jsonObject) {
    this.projid = projid;
    this.language = language;
    this.name = name;
    this.year = year;
    this.jsonObject = jsonObject;
  }

  public int getProjid() {
    return projid;
  }

  /**
   * Little hack to deal with mandarin renaming...
   * @return
   */
  public String getLanguage() {
    String language = this.language;
    if (language.equalsIgnoreCase("Mandarin")) language="Chinese";
    return language;
  }

  public String getName() {
    return name;
  }

  public int getYear() {
    return year;
  }

  public JsonObject getJsonObject() {
    return jsonObject;
  }

  void putInt(INFO key, Integer value) {
    intKeyToValue.put(key, value);
  }

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
    return "stats for " + language + "/" + name + " : " + year +// " = " + keyToValue;
        intKeyToValue;
  }
}
