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

package mitll.langtest.server.database.excel;

import mitll.langtest.shared.analysis.BestScore;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.custom.UserList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class UserPerfToExcel {
  private static final Logger logger = LogManager.getLogger(UserPerfToExcel.class);

  /**
   * @param typeOrder
   * @param userListNoExercises
   * @param out
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void writeExcelToStream(Collection<UserInfo> userInfos, UserList userListNoExercises, OutputStream out) {
    writeToStream(out, writeExcel(userInfos, userListNoExercises));
  }

  private SXSSFWorkbook writeExcel(Collection<UserInfo> userInfos, UserList userListNoExercises) {
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk
    Sheet sheet = wb.createSheet("UserPerformance");
    int rownum = 0;
    CellStyle dateStyle = getDateStyle(wb);
    Row headerRow = sheet.createRow(rownum++);

    List<String> columns = new ArrayList<String>(Arrays.asList("Student", "Name", "Start", "lifetime #", "Lifetime Avg.",
        "# in session",
        "# Recorded", "Session Avg.",
        "Session #1 Date", "Session #1 # Recorded", "Session #1 Score",
        "Session #2 Date", "Session #2 # Recorded", "Session #2 Score..."
    ));

    for (int i = 0; i < columns.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(columns.get(i));
    }

    for (UserInfo result : userInfos) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      Cell cell = row.createCell(j++);
      cell.setCellValue(result.getUserID());
      cell = row.createCell(j++);
      cell.setCellValue(result.getName());
      cell = row.createCell(j++);
      cell.setCellValue(new Date(result.getTimestampMillis()));
      cell.setCellStyle(dateStyle);
      cell = row.createCell(j++);
      cell.setCellValue(result.getNum());

      cell = row.createCell(j++);
      cell.setCellValue(result.getCurrent());

      cell = row.createCell(j++);
      int lastSessionSize = userListNoExercises == null ? result.getLastSessionSize() : userListNoExercises.getNumItems();
      cell.setCellValue(lastSessionSize);

      cell = row.createCell(j++);
      cell.setCellValue(result.getLastSessionNum());

      cell = row.createCell(j++);
      cell.setCellValue(result.getAdjustedScore());

      Map<Long, List<BestScore>> sessions = result.getSessions();
      TreeSet<Long> sorted = new TreeSet<>(sessions.keySet());
      for (Long session : sorted) {
        List<BestScore> bestScores = sessions.get(session);
        cell = row.createCell(j++);
        cell.setCellValue(new Date(bestScores.get(0).getTimestamp()));
        cell.setCellStyle(dateStyle);
        cell = row.createCell(j++);
        cell.setCellValue(bestScores.size());
        cell = row.createCell(j++);
        float roundedHundred = result.getRoundedHundred(bestScores);
        int round = Math.round(roundedHundred * 100F);
        float fround = Integer.valueOf(round).floatValue() / 100F;
        cell.setCellValue(fround);
      }
    }

    long now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.warn("toXLSX : took " + (now - then) + " millis to add " + rownum + " rows to sheet, or " + (now - then) / rownum + " millis/row");
    }
    return wb;
  }

  @NotNull
  private CellStyle getDateStyle(SXSSFWorkbook wb) {
    CellStyle dateStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();
    dateStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss 'yy"));
    return dateStyle;
  }

  private void writeToStream(OutputStream out, SXSSFWorkbook wb) {
    long then = System.currentTimeMillis();
    try {
      wb.write(out);
      long now2 = System.currentTimeMillis();
      if (now2 - then > 100) {
        logger.warn("toXLSX : took " + (now2 - then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }
}
