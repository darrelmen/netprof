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

package mitll.langtest.server.database.exercise;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.internal.PackagePropertiesPart;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.xmlbeans.XmlException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExcelUtil {
  private static final Logger logger = LogManager.getLogger(ExcelUtil.class);

  /**
   * Ask the excel file for when it was modified
   *
   * @param file
   * @return
   * @see #readExercises
   */
  long getExcelLastModified(File file) {
    if (!file.exists()) return 0;
/*    try {
      BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
      logger.info("creationTime:     " + attr.creationTime());
//      logger.info("lastAccessTime:   " + attr.lastAccessTime());
      logger.info("lastModifiedTime: " + attr.lastModifiedTime());
    } catch (IOException e) {
      logger.error("got " + e, e);
    }*/

    try {
      OPCPackage pkg = OPCPackage.open(file);
      POIXMLProperties props = new POIXMLProperties(pkg);
      PackagePropertiesPart ppropsPart = props.getCoreProperties().getUnderlyingProperties();

      Date created = ppropsPart.getCreatedProperty().getValue();
      logger.info("creationTime:     " + created);
      Date modified = ppropsPart.getModifiedProperty().getValue();
      logger.info("lastModifiedTime: " + modified);
      String lastModifiedBy = ppropsPart.getLastModifiedByProperty().getValue();
      logger.info("lastModifiedBy:   " + lastModifiedBy);
      logger.info("readExercises Reading from " + file.getAbsolutePath() + " modified " + modified);

      return modified == null ? System.currentTimeMillis() : modified.getTime();
    } catch (IOException | OpenXML4JException | XmlException e) {
      logger.error("got " + e, e);
    }
    return 0;
  }


  public boolean isDeletedRow(Sheet sheet, Row next, int colIndex) {
    boolean isDelete = false;
    try {
      Cell cell = next.getCell(colIndex);
      if (cell != null) {
        CellStyle cellStyle = cell.getCellStyle();
        if (cellStyle != null) {
          isDelete = sheet.getWorkbook().getFontAt(cellStyle.getFontIndex()).getStrikeout();
        }
      }
    } catch (Exception e) {
      logger.debug("got error reading delete strikeout at row " + next.getRowNum());
    }
    return isDelete;
  }

  Map<Integer, CellRangeAddress> getRowToRange(Sheet sheet) {
    Map<Integer, CellRangeAddress> rowToRange = new HashMap<>();
    for (int r = 0; r < sheet.getNumMergedRegions(); r++) {
      CellRangeAddress mergedRegion = sheet.getMergedRegion(r);
      for (int rr = mergedRegion.getFirstRow(); rr <= mergedRegion.getLastRow(); rr++) {
        rowToRange.put(rr, mergedRegion);
      }
/*      logger.debug("for " + sheet.getSheetName() + " region  " + mergedRegion + " " +
          mergedRegion.getFirstRow() + " " + mergedRegion.getFirstColumn());*/
    }
    return rowToRange;
  }

  public List<String> getHeader(Row next) {
    List<String> columns = new ArrayList<>();

    Iterator<Cell> cellIterator = next.cellIterator();
    while (cellIterator.hasNext()) {
      columns.add(cellIterator.next().toString().trim());
    }

    return columns;
  }

  public String getCell(Row next, int col) {
    if (col == -1) return "";
    Cell cell = next.getCell(col);
    if (cell == null) return "";
    if (cell.getCellTypeEnum() == CellType.NUMERIC) {
      double numericCellValue = cell.getNumericCellValue();
      if ((new Double(numericCellValue).intValue()) < numericCellValue)
        return "" + numericCellValue;
      else
        return "" + new Double(numericCellValue).intValue();
    } else if (cell.getCellTypeEnum() == CellType.STRING) {
      return cell.getStringCellValue().trim();
    } else {
      return cell.toString().trim();
    }
  }

  String cleanTics(String foreignLanguagePhrase) {
    if (foreignLanguagePhrase.startsWith("\'")) {
      foreignLanguagePhrase = foreignLanguagePhrase.substring(1);
    }
    if (foreignLanguagePhrase.endsWith("\'"))
      foreignLanguagePhrase = foreignLanguagePhrase.substring(0, foreignLanguagePhrase.length() - 1);
    return foreignLanguagePhrase;
  }

  public void log(String lang) {
    int MB = (1024 * 1024);
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    if (logger.isDebugEnabled()) {
      logger.debug(lang + " current thread group " + threadGroup.getName() + " = " + threadGroup.activeCount() +
          " : # cores = " + Runtime.getRuntime().availableProcessors() + " heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
    }
  }
}
