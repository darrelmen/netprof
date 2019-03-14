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

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.instrumentation.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by go22670 on 3/25/16.
 */
public class EventDAOToExcel {
  private static final Logger logger = LogManager.getLogger(EventDAOToExcel.class);

  private static final String EVENTS = "Events";
  private static final List<String> COLUMNS2 = Arrays.asList("id", "type", "exercise", "context", "userid", "timestamp", "time_millis", "hitID", "device");

  protected final LogAndNotify logAndNotify;

  public static final String ID = "ID";

  public EventDAOToExcel(Database database) {
    this.logAndNotify = database.getLogAndNotify();
  }

  /**
   * @param out
   * @see mitll.langtest.server.DownloadServlet#returnSpreadsheet(HttpServletResponse, DatabaseImpl, String, int, String)
   */
  public void toXLSX(List<Event> all, OutputStream out) {
    long then = System.currentTimeMillis();

    long now = System.currentTimeMillis();
    if (now - then > 100) logger.info("toXLSX : took " + (now - then) + " millis to read " + all.size() +
        " events from database");
    then = now;

    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk

    Sheet sheet = wb.createSheet(EVENTS);
    int rownum = 0;

    {
      Row headerRow = sheet.createRow(rownum++);
      for (int i = 0; i < COLUMNS2.size(); i++) {
        Cell headerCell = headerRow.createCell(i);
        headerCell.setCellValue(COLUMNS2.get(i));
      }
    }

    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss"));

    for (Event event : all) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      Cell cell = row.createCell(j++);
      cell.setCellValue(event.getWidgetID());

      cell = row.createCell(j++);
      cell.setCellValue(event.getWidgetType());

      cell = row.createCell(j++);
      cell.setCellValue(event.getExerciseID());

      cell = row.createCell(j++);
      cell.setCellValue(event.getContext());

      cell = row.createCell(j++);
      cell.setCellValue(event.getUserID());

      cell = row.createCell(j++);
      cell.setCellValue(new Date(event.getTimestamp()));
      cell.setCellStyle(cellStyle);

      cell = row.createCell(j++);
      cell.setCellValue(event.getTimestamp());

      cell = row.createCell(j++);
      cell.setCellValue(event.getDevice());

    }
    now = System.currentTimeMillis();
    if (now - then > 100) logger.warn("toXLSX : took " + (now - then) + " millis to write " + rownum +
        " rows to sheet, or " + (now - then) / rownum + " millis/row");
    then = now;
    try {
      wb.write(out);
      now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.warn("toXLSX : took " + (now - then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
      logAndNotify.logAndNotifyServerException(e);
    }
  }
}
