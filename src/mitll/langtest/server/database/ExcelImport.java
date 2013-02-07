package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Lesson;
import mitll.langtest.shared.TeacherClass;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Reads an excel spreadsheet from DLI.
 *
 * User: GO22670
 * Date: 2/6/13
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExcelImport {
  private static Logger logger = Logger.getLogger(ExcelImport.class);

  private List<Exercise> exercises = new ArrayList<Exercise>();
  private List<Lesson> lessons = new ArrayList<Lesson>();
  private TeacherClass teacherClass;

  public List<Exercise> readExercises(File file) {
    try {
      InputStream inp = new FileInputStream(file);//"Farsi_Curriculum_Glossary_vowelized_2013_02_04.xlsx");

      Workbook wb = WorkbookFactory.create(inp);
      Sheet sheet = wb.getSheetAt(0);
     // System.out.println("sheet " +sheet.getSheetName());
      Iterator<Row> iter = sheet.rowIterator();
      int c= 0;
      int id = 0;
      boolean gotHeader = false;
      FileExerciseDAO dao = new FileExerciseDAO(null,false);

      teacherClass = new TeacherClass(-1);
      Lesson lesson = null;

      int numColumns = 0;
      for (; iter.hasNext(); ) {
        Row next = iter.next();
        //System.out.println("Row #" + next.getRowNum() + " : ");
      //  if (c++ > 4) break;
        List<String> columns = new ArrayList<String>();
        if (!gotHeader) {
          Iterator<Cell> cellIterator = next.cellIterator();
          while (cellIterator.hasNext()) {
            Cell next1 = cellIterator.next();
            //next1.getCell
            columns.add(next1.toString().trim());
          }
        }

        if (!gotHeader && columns.get(0).toLowerCase().startsWith("Word".toLowerCase())) {
          gotHeader = true;
      //    System.out.println("got header line " + columns);
          numColumns = columns.size();
        }
        else {
          String english = getCell(next, 0);
          if (gotHeader && english.trim().length() > 0) {
           // System.out.println("got entry line " +columns);
            String arabic = getCell(next, 1);
            String translit = getCell(next, 2);
            String unit = getCell(next, 3);
            String chapter = getCell(next, 4);
            String week = getCell(next, 5);
            if (lesson == null || !lesson.chapter.equals(chapter)) {
              lesson = new Lesson(unit,chapter,week);
              lessons.add(lesson);
            }
            String content = dao.getContent(arabic, translit, english);
            Exercise imported = new Exercise("import", "" + id++, content, false, true, english);
            exercises.add(imported);
            lesson.addExercise(imported);
          }
        }
      }

      inp.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InvalidFormatException e) {
      e.printStackTrace();
    }
    for (Lesson l : lessons) {
      logger.debug("lesson " + l);
    }
    return exercises;
  }

  private String getCell(Row next, int col) {
    Cell cell = next.getCell(col);
    if (cell == null) return "";
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      double numericCellValue = cell.getNumericCellValue();
      return "" + new Double(numericCellValue).intValue();
    }
    return cell.toString();
  }

  public List<Exercise> getExercises() {
    return exercises;
  }

  public static void main(String [] arg) {
    new ExcelImport().readExercises(new File("Farsi_Curriculum_Glossary_vowelized_2013_02_04.xlsx"));
  }
}
