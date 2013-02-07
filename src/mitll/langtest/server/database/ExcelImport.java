package mitll.langtest.server.database;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 2/6/13
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExcelImport {
  public void importSheet() {
    try {
      InputStream inp = new FileInputStream("Farsi_Curriculum_Glossary_vowelized_2013_02_04.xlsx");
      //InputStream inp = new FileInputStream("workbook.xlsx");

      Workbook wb = WorkbookFactory.create(inp);
      Sheet sheet = wb.getSheetAt(0);
      System.out.println("sheet " +sheet.getSheetName());
      Iterator<Row> iter = sheet.rowIterator();
      int c= 0;
      for (; iter.hasNext(); ) {
        Row next = iter.next();
        System.out.println("Row #" + next.getRowNum() + " : ");
        if (c++ > 4) break;
        Iterator<Cell> cellIterator = next.cellIterator();

        while (cellIterator.hasNext()) {
          Cell next1 = cellIterator.next();

          System.out.println("got " + next1);

        }
      }

/*
      Row row = sheet.getRow(2);
      Cell cell = row.getCell(3);
      if (cell == null)
        cell = row.createCell(3);
      cell.setCellType(Cell.CELL_TYPE_STRING);
      cell.setCellValue("a test");*/

      inp.close();
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (InvalidFormatException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public static void main(String [] arg) {
    new ExcelImport().importSheet();
  }
}
