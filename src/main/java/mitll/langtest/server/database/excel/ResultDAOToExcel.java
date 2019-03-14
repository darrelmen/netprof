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

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.shared.result.MonitorResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class ResultDAOToExcel {
  private static final Logger logger = LogManager.getLogger(ResultDAOToExcel.class);
  private static final String YES = "Yes";
  private static final String NO = "No";

  /**
   * @param typeOrder
   * @param out
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void writeExcelToStream(Collection<MonitorResult> results, Collection<String> typeOrder, OutputStream out) {
    writeToStream(out, writeExcel(results, typeOrder));
  }

  private SXSSFWorkbook writeExcel(Collection<MonitorResult> results, Collection<String> typeOrder) {
    long now;
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(10000); // keep 100 rows in memory, exceeding rows will be flushed to disk
    Sheet sheet = wb.createSheet("Results");
    int rownum = 0;
    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss 'yy"));
    //DateTimeFormat format = DateTimeFormat.getFormat("MMM dd h:mm:ss a z ''yy");
    Row headerRow = sheet.createRow(rownum++);

    List<String> columns = new ArrayList<String>(Arrays.asList(ResultDAO.USERID, "Exercise", "Text"));

    columns.addAll(typeOrder);

    List<String> columns2 = Arrays.asList(
        "Recording",
        Database.TIME,
        ResultDAO.AUDIO_TYPE,
        ResultDAO.DURATION,
        "Valid",
        "Validity",
        "Dynamic Range",
        ResultDAO.CORRECT,
        ResultDAO.PRON_SCORE,
        "Device",
        "Process",
        "RoundTrip"
    );

    columns.addAll(columns2);

    for (int i = 0; i < columns.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(columns.get(i));
    }

    for (MonitorResult result : results) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      Cell cell = row.createCell(j++);
      cell.setCellValue(result.getUserid());
      cell = row.createCell(j++);
      cell.setCellValue(result.getExID());
      cell = row.createCell(j++);
      cell.setCellValue(result.getForeignText());

      for (String type : typeOrder) {
        cell = row.createCell(j++);
        cell.setCellValue(result.getUnitToValue().get(type));
      }

      cell = row.createCell(j++);
      cell.setCellValue(result.getAnswer());

      cell = row.createCell(j++);
      cell.setCellValue(new Date(result.getTimestamp()));
      cell.setCellStyle(cellStyle);

      cell = row.createCell(j++);
      String audioType = result.getAudioType().toString();
      cell.setCellValue(audioType);

      cell = row.createCell(j++);
      cell.setCellValue(result.getDurationInMillis());

      cell = row.createCell(j++);
      cell.setCellValue(result.isValid() ? YES : NO);

      cell = row.createCell(j++);
      cell.setCellValue(result.getValidity());

      cell = row.createCell(j++);
      cell.setCellValue(result.getSnr());

      cell = row.createCell(j++);
      cell.setCellValue(result.isCorrect() ? YES : NO);

      cell = row.createCell(j++);
      cell.setCellValue(result.getPronScore());

      cell = row.createCell(j++);
      cell.setCellValue(result.getDevice());

      cell = row.createCell(j++);
      cell.setCellValue(result.getProcessDur());

      cell = row.createCell(j++);
      cell.setCellValue(result.getRoundTripDur());
    }
    now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.warn("toXLSX : took " + (now - then) + " millis to add " + rownum + " rows to sheet, or " + (now - then) / rownum + " millis/row");
    }
    return wb;
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
