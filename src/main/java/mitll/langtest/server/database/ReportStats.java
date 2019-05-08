/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
    otherMap.forEach((k, v) -> intKeyToValue.merge(k, v, Integer::sum));
  }

  private void mergeKeyToValue2(Map<String, Integer> intKeyToValue, Map<String, Integer> otherMap) {
    otherMap.forEach((k, v) -> intKeyToValue.merge(k, v, Integer::sum));
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

  public ReportStats(SlickProject project, int year) {
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
   *
   * @return
   */
  public String getLanguage() {
    String language = this.language;
    if (language.equalsIgnoreCase("Mandarin")) language = "Chinese";
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
