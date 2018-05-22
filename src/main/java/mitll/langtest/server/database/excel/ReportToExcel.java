package mitll.langtest.server.database.excel;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.database.Report;
import mitll.langtest.server.database.ReportStats;
import mitll.langtest.server.database.ReportStats.INFO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static mitll.langtest.server.database.Report.EARLIEST_YEAR;

/**
 * Created by go22670 on 3/25/16.
 */
public class ReportToExcel {
  private static final Logger logger = LogManager.getLogger(ReportToExcel.class);

  private static final String NET_PRO_F_HISTORICAL = "NetProF Historical";
  /**
   * @see #writeYTDAndHistorical
   */
  private static final String NET_PROF_VRT = "NetProF-Vrt";
  private static final String INCREASE = "INCREASE";
  private static final String LANGUAGE = "Language";
  private static final String PROJECT = "Project";
  private static final String LANGUAGES = LANGUAGE + "s";
  private static final String PROJECTS = PROJECT + "s";

  public static final boolean DEBUG = false;

  protected final LogAndNotify logAndNotify;

  public static final String ID = "ID";

  public ReportToExcel(LogAndNotify logAndNotify) {
    this.logAndNotify = logAndNotify;
  }

  /**
   * @param out
   * @see Report#getSummaryReport
   */
  public void toXLSX(List<ReportStats> stats, OutputStream out) {
    XSSFWorkbook wb = new XSSFWorkbook();

    if (DEBUG) {
      logger.info("toXLSX Got " + stats.size() + " stats");
      stats.forEach(stat -> logger.info("\t" + stat));
    }

    writeSheets(stats, wb, false);
    writeSheets(stats, wb, true);
    writeTheFile(out, wb);
  }

  /**
   * Write out both a per language and a per project set of sheets - ytd and yearly historical for each
   *
   * @param stats
   * @param wb
   */
  private void writeSheets(List<ReportStats> stats, XSSFWorkbook wb, boolean deviceRecordings) {
/*    if (!deviceRecordings) {
      stats.forEach(reportStats -> {
        Map<String, Integer> weekToCountForLang = reportStats.getKeyToValue(INFO.ALL_RECORDINGS_WEEKLY);
        weekToCountForLang.forEach((k, v) -> logger.info("\t" + reportStats.getName() + " " + k + " = " + v));
      });
    }*/

    List<ReportStats> copy = new ArrayList<>();
    stats.forEach(reportStats -> copy.add(new ReportStats(true, reportStats)));

    {
      boolean byLanguage = true;
      Map<String, Map<Integer, ReportStats>> langToYearToStats = getLangToYearToStats(stats, byLanguage);
      writeYTDAndHistorical(stats, wb, langToYearToStats, !byLanguage, deviceRecordings);
    }

    {
      boolean byLanguage = false;
      Map<String, Map<Integer, ReportStats>> nameToYearToStats = getLangToYearToStats(copy, byLanguage);
      writeYTDAndHistorical(copy, wb, nameToYearToStats, !byLanguage, deviceRecordings);
    }

  }

  private void writeYTDAndHistorical(List<ReportStats> stats,
                                     XSSFWorkbook wb,
                                     Map<String, Map<Integer, ReportStats>> langToYearToStats,
                                     boolean perProject,
                                     boolean deviceRecordings) {
    // write the vert sheet
    long then = System.currentTimeMillis();
    String suffix = perProject ? "_project" : "";
    suffix += deviceRecordings ? "_ios" : "";

    {
      INFO allRecordingsWeekly = deviceRecordings ? INFO.DEVICE_RECORDINGS_WEEKLY : INFO.ALL_RECORDINGS_WEEKLY;

      int thisYear = getThisYear();
      if (suffix.isEmpty()) {
        for (int year = thisYear; year >= EARLIEST_YEAR; year--) {
          String suffixToUse = (year == thisYear) ? "" : "_" + year;
          int rownum = langToYearToStats.isEmpty() ? 1 :
              writeWeeklySheetForYear(
                  wb,
                  wb.createSheet(NET_PROF_VRT + suffixToUse),
                  langToYearToStats,
                  perProject,
                  allRecordingsWeekly,
                  year);
          long now = System.currentTimeMillis();
          if (now - then > 100) logger.warn("toXLSX : took " + (now - then) + " millis to write " + rownum +
              " rows to sheet, or " + (now - then) / rownum + " millis/row");
        }
      } else {
        int rownum = langToYearToStats.isEmpty() ? 1 :
            writeWeeklySheet(
                wb,
                wb.createSheet(NET_PROF_VRT + suffix),
                langToYearToStats,
                perProject,
                allRecordingsWeekly);
        long now = System.currentTimeMillis();
        if (now - then > 100) logger.warn("toXLSX : took " + (now - then) + " millis to write " + rownum +
            " rows to sheet, or " + (now - then) / rownum + " millis/row");
      }
    }

    // write the historical sheet
    {
      writeHistoricalSheet(
          wb,
          stats,
          wb.createSheet(NET_PRO_F_HISTORICAL + suffix),
          langToYearToStats,
          perProject,
          deviceRecordings ? INFO.DEVICE_RECORDINGS : INFO.ALL_RECORDINGS);
    }
  }

  /**
   * @param workbook
   * @param stats
   * @param sheet
   * @param langToYearToStats
   * @param perProject
   * @return
   * @see #toXLSX(List, OutputStream)
   */
  private int writeHistoricalSheet(XSSFWorkbook workbook,
                                   List<ReportStats> stats,
                                   Sheet sheet,
                                   Map<String, Map<Integer, ReportStats>> langToYearToStats,
                                   boolean perProject,
                                   INFO recordingType) {
    //INFO recordingType = INFO.ALL_RECORDINGS;

    logger.info("writeHistoricalSheet sheet for " + langToYearToStats.size() + " projects, for " + recordingType);
    int rownum = 0;

    short yellow = IndexedColors.YELLOW.getIndex();
    Set<Integer> yearsFromStats = getYearsFromStats(stats);

    rownum = writeHeaderRow(workbook, sheet, rownum, yearsFromStats, perProject);

    Map<Integer, Integer> yearToTotal = new HashMap<>();
    Set<String> sortedLang = new TreeSet<>(langToYearToStats.keySet());
    XSSFCellStyle lightGreenStyle = getLightGreenStyle(workbook);
    XSSFCellStyle cellStyle = getCellStyle(workbook);
    int maxCol = 0;
    for (String lang : sortedLang) {
      Row row = sheet.createRow(rownum++);

      int col = 0;

      {
        Cell langCell = row.createCell(col++);
        langCell.setCellStyle(lightGreenStyle);
        langCell.setCellValue(lang);
      }

      for (Integer year : yearsFromStats) {
        Map<Integer, ReportStats> yearToStats = langToYearToStats.get(lang);
        ReportStats reportStats = yearToStats.get(year);
//        logger.info("For " + lang + " " + year + " got " + reportStats);
        Integer orDefault = reportStats == null ? 0 : reportStats.getIntKeyToValue().getOrDefault(recordingType, 0);
        {
          col = addCellAtCol(cellStyle, row, col, orDefault);
        }
        yearToTotal.put(year, yearToTotal.getOrDefault(year, 0) + orDefault);
      }
      {
        Cell cell = row.createCell(col++);
        cell.setCellStyle(lightGreenStyle);
        cell.setCellValue(lang);
      }
    }

    {
      XSSFCellStyle brightGreenStyle = getBrightGreenStyle(workbook);
      Row row = sheet.createRow(rownum++);
      int col = 0;
      {
        Cell cell1 = row.createCell(col++);
        cell1.setCellValue("TOTALS");
        cell1.setCellStyle(brightGreenStyle);
      }

      for (Integer year : yearsFromStats) {
        col = addCellAtCol(brightGreenStyle, row, col, yearToTotal.getOrDefault(year, 0));
      }

      {
        Cell cell = row.createCell(col++);
        cell.setCellStyle(brightGreenStyle);
        cell.setCellValue("TOTAL");
      }

      if (maxCol < col) maxCol = col;
    }
    autoSizeColumns(sheet, maxCol);

    return rownum;
  }

  @NotNull
  private Map<String, Map<Integer, ReportStats>> getLangToYearToStats(List<ReportStats> stats, boolean byLanguage) {
    Map<String, List<ReportStats>> langToStats = byLanguage ? getLangToReports(stats) : getProjectToReports(stats);

    Map<String, Map<Integer, ReportStats>> langToYearToStats = new HashMap<>();
    langToStats.forEach((k, statsForLang) -> {
      Map<Integer, ReportStats> yearToStats = langToYearToStats.computeIfAbsent(k, k1 -> new HashMap<>());

      for (ReportStats reportStats : statsForLang) {
        int year = getYearSafe(reportStats);

        ReportStats current = yearToStats.get(year);
        if (current == null) {
          yearToStats.put(year, reportStats);
//          logger.info("getLangToYearToStats : not merging " + reportStats + " byLanguage " + byLanguage + " " + year);
        } else {
          //        logger.info("getLangToYearToStats : merging " + reportStats + " into " + current + " byLanguage " + byLanguage + " " + year);
          ReportStats merged = current.getMerged(reportStats);
          yearToStats.put(year, merged);
        }

      }
    });
    return langToYearToStats;
  }

  @NotNull
  private Map<String, List<ReportStats>> getLangToReports(List<ReportStats> stats) {
    Map<String, List<ReportStats>> langToStats = new HashMap<>(stats.size());

    stats.forEach(reportStats -> {
      List<ReportStats> reportStats1 = langToStats.computeIfAbsent(reportStats.getLanguage(), k -> new ArrayList<>());
      reportStats1.add(reportStats);
    });
    return langToStats;
  }

  @NotNull
  private Map<String, List<ReportStats>> getProjectToReports(List<ReportStats> stats) {
    Map<String, List<ReportStats>> langToStats = new HashMap<>(stats.size());

    stats.forEach(reportStats -> {
      String language1 = reportStats.getLanguage();
      String name = reportStats.getName();
      String label = language1.equalsIgnoreCase(name) ?
          language1 :
          language1 + "/" + name;
      List<ReportStats> reportStats1 = langToStats.computeIfAbsent(label, k -> new ArrayList<>());
      reportStats1.add(reportStats);
    });
    return langToStats;
  }

  private int writeHeaderRow(XSSFWorkbook workbook, Sheet sheet, int rownum, Set<Integer> years, boolean perProject) {
    Row headerRow = sheet.createRow(rownum++);
    // headerRow.getRowStyle().setFillBackgroundColor(IndexedColors.YELLOW.getIndex());
    int col = 0;
    Cell headerCell = headerRow.createCell(col++);
    makeYellow(workbook, headerCell);
    String headerLabel = getHeaderLabel(perProject);
    headerCell.setCellValue(headerLabel);

    for (Integer year : years) {
      Cell cell = headerRow.createCell(col++);
      cell.setCellValue(year);
      makeYellow(workbook, cell);
    }
    Cell cell = headerRow.createCell(col++);
    cell.setCellValue(headerLabel);
    makeYellow(workbook, cell);
    return rownum;
  }

  @NotNull
  private String getPluralLabel(boolean perProject) {
    return perProject ? PROJECTS : LANGUAGES;
  }

  @NotNull
  private String getHeaderLabel(boolean perProject) {
    return perProject ? PROJECT : LANGUAGE;
  }

  private int writeHeaderRow2(Sheet sheet, int rownum, Set<String> years, XSSFCellStyle lightGreenStyle) {
    Row headerRow = sheet.createRow(rownum++);
    // headerRow.getRowStyle().setFillBackgroundColor(IndexedColors.YELLOW.getIndex());
    int col = 0;
    Cell headerCell = headerRow.createCell(col++);
    //makeYellow(yellow, headerCell);
    headerCell.setCellValue("");
    headerCell.setCellStyle(lightGreenStyle);
    for (String year : years) {
      Cell cell = headerRow.createCell(col++);
      cell.setCellValue(year);
      cell.setCellStyle(lightGreenStyle);
    }
    Cell cell = headerRow.createCell(col++);
    cell.setCellValue("TOTAL");
    cell.setCellStyle(lightGreenStyle);
    //makeYellow(yellow, cell);
    return rownum;
  }

  private Set<Integer> getYearsFromStats(List<ReportStats> stats) {
    Set<Integer> years = new TreeSet<>();
    stats.forEach(reportStats -> years.add(getYearSafe(reportStats)));
    return years;
  }

  /**
   * @param workbook
   * @param sheet
   * @param langToYearToStats
   * @param perProject
   * @param recordingType
   * @return
   * @see #toXLSX
   */
  private int writeWeeklySheet(XSSFWorkbook workbook,
                               Sheet sheet,
                               Map<String, Map<Integer, ReportStats>> langToYearToStats,
                               boolean perProject,
                               INFO recordingType) {
    return writeWeeklySheetForYear(workbook, sheet, langToYearToStats, perProject, recordingType, getThisYear());
  }

  private int writeWeeklySheetForYear(XSSFWorkbook workbook,
                                      Sheet sheet,
                                      Map<String, Map<Integer, ReportStats>> langToYearToStats,
                                      boolean perProject,
                                      INFO recordingType,
                                      int yearToReport) {
    logger.info("writeWeeklySheetForYear per project " + perProject + " type " + recordingType + " for year " + yearToReport);

    int rownum = 0;

    Set<String> sortedLang = new TreeSet<>(langToYearToStats.keySet());

    rownum = writeHeaderRow2(sheet, rownum, sortedLang, getLightGreenStyle(workbook));

    if (langToYearToStats.isEmpty()) {
      logger.error("huh? no data in " + langToYearToStats);
    }
    Map<String, Integer> weekToCountFirstLang = getWeektoCountFirstLang(langToYearToStats, yearToReport);

    Map<String, Integer> langToLastWeek = new HashMap<>();
    Map<String, Integer> langToCurrent = new HashMap<>();
    Map<String, Integer> langToPrev = new HashMap<>();
    int marginalDiff = 0;
    XSSFCellStyle cellStyle = getCellStyle(workbook);
    setBorder(cellStyle);
    XSSFCellStyle greenStyle = getDarkGreenStyle(workbook);
    XSSFCellStyle greenStyleWeeklyTotal = getDarkGreenStyle(workbook);
    setNumberFormat(workbook, greenStyleWeeklyTotal);

    int prevMarginal = 0;

    int maxCol = 0;

    for (String week : weekToCountFirstLang.keySet()) {
      if (DEBUG) logger.info("writeWeeklySheetForYear : week " + week + " = " + weekToCountFirstLang.get(week));
      Row row = sheet.createRow(rownum++);

      int col = 0;
      Cell cell1 = row.createCell(col++);
      cell1.setCellValue(getWeek(week));
      cell1.setCellStyle(greenStyle);
      int marginalTotal = 0;

      // for every lang per week
      for (String lang : sortedLang) {
        Map<Integer, ReportStats> yearToStats = langToYearToStats.get(lang);

        if (yearToStats == null) {
          logger.warn("writeWeeklySheetForYear : no stats for " + lang);
          col = addCellAtCol(cellStyle, row, col, 0);
        } else {
          ReportStats reportStats = yearToStats.get(yearToReport);

          if (reportStats == null) {
            logger.warn("writeWeeklySheetForYear : no stats for " + lang + " and year " + yearToReport);
            col = addCellAtCol(cellStyle, row, col, 0);
          } else {
            //  if (debug) logger.info("writeWeeklySheet lang " + lang + " week " + week + " stats " + reportStats);
            Map<String, Integer> weekToCountForLang = reportStats.getKeyToValue(recordingType);

            Integer count = weekToCountForLang.getOrDefault(week, 0);
            // if (debug) logger.info("writeWeeklySheet lang " + lang + " week " + week + " count " + count);

            int cumulative = langToCurrent.getOrDefault(lang, 0) + count;
            // if (debug) logger.info("writeWeeklySheet lang " + lang + " week " + week + " cumulative " + cumulative);

            marginalTotal += cumulative;
            langToCurrent.put(lang, cumulative);

            col = addCellAtCol(cellStyle, row, col, cumulative);
            int diffLastWeek = cumulative - langToPrev.getOrDefault(lang, 0);
            //   if (debug) logger.info("writeWeeklySheet lang " + lang + " week " + week + " diffLastWeek " + diffLastWeek);

//        logger.info("lang " + lang + " = " + diffLastWeek);
            langToLastWeek.put(lang, diffLastWeek);
            langToPrev.put(lang, cumulative);
          }
        }
      }

      {
        Cell cell = row.createCell(col++);
        cell.setCellValue(marginalTotal);
        cell.setCellStyle(greenStyleWeeklyTotal);
      }
      marginalDiff = marginalTotal - prevMarginal;
      prevMarginal = marginalTotal;

      if (maxCol < col) maxCol = col;
    }

    doIncreaseRow(workbook, sortedLang, langToLastWeek, marginalDiff, sheet.createRow(rownum++));

    rownum = addFooterRow(workbook, sheet, rownum, sortedLang, greenStyle, perProject);

    autoSizeColumns(sheet, maxCol);
    return rownum;
  }

  private int addCellAtCol(XSSFCellStyle cellStyle, Row row, int col, int cumulative) {
    Cell cell = row.createCell(col++);
    cell.setCellStyle(cellStyle);
    cell.setCellValue(cumulative);
    return col;
  }

  private void autoSizeColumns(Sheet sheet, int maxCol) {
    for (int i = 0; i < maxCol; i++) sheet.autoSizeColumn(i);
  }

  private Map<String, Integer> getWeektoCountFirstLang(Map<String, Map<Integer, ReportStats>> langToYearToStats, int thisYear) {
    Map<Integer, ReportStats> firstLanguage = langToYearToStats.values().iterator().next();
    ReportStats statsForFirstLang = firstLanguage.get(thisYear);
    return statsForFirstLang.getKeyToValue(INFO.ALL_RECORDINGS_WEEKLY);
  }

  private int addFooterRow(XSSFWorkbook workbook, Sheet sheet, int rownum, Set<String> sortedLang, XSSFCellStyle greenStyle, boolean perProject) {
    Row row;
    int col;

    row = sheet.createRow(rownum++);
    col = 0;
    Cell cell1 = row.createCell(col++);
    cell1.setCellValue(getPluralLabel(perProject));
    cell1.setCellStyle(greenStyle);


    XSSFCellStyle lightGreenStyle = getLightGreenStyle(workbook);
    for (String lang : sortedLang) {
      Cell cell2 = row.createCell(col++);
      cell2.setCellValue(lang);
      cell2.setCellStyle(lightGreenStyle);
    }

    Cell cell2 = row.createCell(col++);
    cell2.setCellValue("TOTAL");
    cell2.setCellStyle(greenStyle);
    return rownum;
  }

  private void doIncreaseRow(XSSFWorkbook workbook,
                             Set<String> sortedLang,
                             Map<String, Integer> langToLastWeek,
                             int marginalDiff,
                             Row row) {
    int col = 0;

    XSSFCellStyle brightGreenStyle = getBrightGreenStyle(workbook);
    XSSFCellStyle yellowStyle = getYellowStyle(workbook);

    //  logger.info("doIncreaseRow marginal " + marginalDiff);

    {
      Cell cell = row.createCell(col++);
      cell.setCellValue(INCREASE);
      cell.setCellStyle(brightGreenStyle);
    }

    for (String lang : sortedLang) {
      Integer value = langToLastWeek.get(lang);
      if (value == null) {
        logger.error("doIncreaseRow no value for " + lang);
        value = 0;
      } else {
        // logger.info("doIncreaseRow Got " + lang + " = " + value);
      }

      {
        Cell cell1 = row.createCell(col++);
        cell1.setCellValue(value);
        cell1.setCellStyle(value == 0 ? yellowStyle : brightGreenStyle);
      }
    }
    Cell cell = row.createCell(col++);
    cell.setCellValue(marginalDiff);
    cell.setCellStyle(getBlueStyle(workbook));
  }

  @NotNull
  private XSSFCellStyle getCellStyle(XSSFWorkbook workbook) {
    XSSFCellStyle style = workbook.createCellStyle();
    setNumberFormat(workbook, style);
    style.setAlignment(HorizontalAlignment.CENTER);
    return style;
  }


  @NotNull
  private XSSFCellStyle getDarkGreenStyle(XSSFWorkbook workbook) {
    java.awt.Color darkGreen = new java.awt.Color(170, 207, 145);
    return getColorStyle(workbook, darkGreen);
  }

  @NotNull
  private XSSFCellStyle getLightGreenStyle(XSSFWorkbook workbook) {
    java.awt.Color darkGreen = new java.awt.Color(226, 239, 219);
    return getColorStyle(workbook, darkGreen);
  }

  @NotNull
  private XSSFCellStyle getBlueStyle(XSSFWorkbook workbook) {
    java.awt.Color darkGreen = new java.awt.Color(30, 177, 237);
    return getColorStyle(workbook, darkGreen);
  }

  @NotNull
  private XSSFCellStyle getBrightGreenStyle(XSSFWorkbook workbook) {
    java.awt.Color darkGreen = new java.awt.Color(109, 253, 110);

    XSSFCellStyle colorStyle = getColorStyle(workbook, darkGreen);
    setNumberFormat(workbook, colorStyle);
    return colorStyle;
  }

  private void setNumberFormat(XSSFWorkbook workbook, CellStyle style) {
    DataFormat format = workbook.createDataFormat();
    style.setDataFormat(format.getFormat("#,###"));
  }

  @NotNull
  private XSSFCellStyle getYellowStyle(XSSFWorkbook workbook) {
    java.awt.Color darkGreen = new java.awt.Color(255, 253, 56);
    XSSFCellStyle colorStyle = getColorStyle(workbook, darkGreen);
    setNumberFormat(workbook, colorStyle);
    return colorStyle;
  }

  @NotNull
  private XSSFCellStyle getYellowStyle2(XSSFWorkbook workbook) {
    java.awt.Color darkGreen = new java.awt.Color(255, 253, 56);
    XSSFCellStyle colorStyle = getColorStyle(workbook, darkGreen);
    setBold(workbook, colorStyle);
    return colorStyle;
  }

  private XSSFCellStyle getColorStyle(XSSFWorkbook workbook, java.awt.Color darkGreen) {
    XSSFCellStyle greenStyle = workbook.createCellStyle();
    setColorStyle(workbook, greenStyle, darkGreen);
    return greenStyle;
  }

  private void setColorStyle(XSSFWorkbook workbook, XSSFCellStyle greenStyle, java.awt.Color darkGreen) {
    greenStyle.setFillForegroundColor(new XSSFColor(darkGreen));
    greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    greenStyle.setAlignment(HorizontalAlignment.CENTER);
    setBold(workbook, greenStyle);
  }

  private void setBold(XSSFWorkbook workbook, XSSFCellStyle greenStyle) {
    XSSFFont font = workbook.createFont();
    font.setBold(true);
    setBorder(greenStyle);
    greenStyle.setFont(font);
  }

  private void setBorder(XSSFCellStyle style) {
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
  }

  private String getWeek(String week) {
    if (week.startsWith("0")) week = week.substring(1);
    return week.replace("-", "/");
  }

  private void makeYellow(XSSFWorkbook workbook, Cell headerCell) {
    headerCell.setCellStyle(getYellowStyle2(workbook));
  }

  private int getYearSafe(ReportStats reportStats) {
    return reportStats.getYear() == -1 ? getThisYear() : reportStats.getYear();
  }

  private void writeTheFile(OutputStream out, XSSFWorkbook wb) {
    long now;
    try {
      long then = System.currentTimeMillis();
      wb.write(out);
      now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.warn("toXLSX : took " + (now - then) + " millis to write excel to output stream ");
      }
      out.close();
      // wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
      logAndNotify.logAndNotifyServerException(e);
    }
  }


  private int getThisYear() {
    return Calendar.getInstance().get(Calendar.YEAR);
  }

}
