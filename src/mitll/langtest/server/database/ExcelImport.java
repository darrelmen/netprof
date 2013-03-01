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
import org.apache.poi.ss.util.CellRangeAddress;
import scala.actors.threadpool.Arrays;
import scala.collection.parallel.mutable.ParArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reads an excel spreadsheet from DLI.
 *
 * User: GO22670
 * Date: 2/6/13
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExcelImport implements ExerciseDAO {
  private static Logger logger = Logger.getLogger(ExcelImport.class);

  private List<Exercise> exercises = null;
  private List<Lesson> lessons = new ArrayList<Lesson>();
  private Map<String,Map<String,Lesson>> typeToUnitToLesson = new HashMap<String,Map<String,Lesson>>();


  private List<String> errors = new ArrayList<String>();
  private TeacherClass teacherClass;
  private final String file;

  @Override
  public Map<String, Collection<String>> getTypeToSections() {
    Map<String,Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
    for (String key : typeToUnitToLesson.keySet()) {
      typeToSection.put(key,typeToUnitToLesson.keySet());
    }
    return typeToSection;
  }

  @Override
  public Map<String, List<String>> getTypeToSectionsForTypeAndSection(String type, String section) {
    return null;
  }

  @Override
  public Collection<Exercise> getExercisesForSection(String type, String section) {
    Map<String, Lesson> sectionToLesson = typeToUnitToLesson.get(type);
    if (sectionToLesson == null) {
      return Collections.emptyList();
    }
    else {
      return sectionToLesson.get(section).getExercises();
    }
  }

  public ExcelImport() { this.file = null;}
  public ExcelImport(String file) { this.file = file;}

  public List<Exercise> getRawExercises() {
    synchronized (this) {
      if (exercises == null) {
        exercises = readExercises(new File(file));
      }
    }
    return exercises;
  }

  public List<Exercise> readExercises(File file) {
    try {
      InputStream inp = new FileInputStream(file);
      return readExercises(inp);
    } catch (FileNotFoundException e) {
      logger.error("got "+e,e);
    }
    return new ArrayList<Exercise>();
  }

  /**
   * @see mitll.langtest.server.SiteDeployer#readExercises(mitll.langtest.shared.Site, org.apache.commons.fileupload.FileItem)
   * @param inp
   * @return
   */
  public List<Exercise> readExercises(InputStream inp) {
    List<Exercise> exercises = new ArrayList<Exercise>();
    try {
      //logger.info("reading from " +inp);
      teacherClass = new TeacherClass(-1);
      Workbook wb = WorkbookFactory.create(inp);

      for (int i = 0; i < wb.getNumberOfSheets(); i++) {
        Sheet sheet = wb.getSheetAt(i);
        if (sheet.getPhysicalNumberOfRows() > 0) {
          logger.info("------------ reading sheet " + sheet.getSheetName() + " ------------------");
          Collection<Exercise> exercises1 = readFromSheet(sheet);
          exercises.addAll(exercises1);
          logger.info("sheet " + sheet.getSheetName() + " had " + exercises1.size() + " items.");
          //for (Exercise e: exercises1) logger.info("ex " +e.getID() + " " +e.getSlots());
        }
      }

      if (!errors.isEmpty()) {
        logger.warn("there were " + errors.size() + " errors");
        for (String error : errors) {
          logger.warn(error);
        }
      }
      for (String key : typeToUnitToLesson.keySet()) {
        Map<String, Lesson> categoryToLesson = typeToUnitToLesson.get(key);
        lessons.addAll(categoryToLesson.values());
        Set<String> sections = categoryToLesson.keySet();
        if (!sections.isEmpty()) logger.debug(key+ " : " + sections);
      }

/*      if (!errors.isEmpty()) {
        logger.warn("there were " + errors.size() + " errors : " + errors);
      }*/

      inp.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InvalidFormatException e) {
      e.printStackTrace();
    }
    if (false && logger.isDebugEnabled()) {
      for (Lesson l : getLessons()) {
        logger.debug("lesson " + l);
      }
    }
    return exercises;
  }

  private Collection<Exercise> readFromSheet(Sheet sheet) {
    Iterator<Row> iter = sheet.rowIterator();
    List<Exercise> exercises = new ArrayList<Exercise>();
   // logger.debug("for " + sheet.getSheetName() + " regions " +sheet.getNumMergedRegions());
    Map<Integer,CellRangeAddress> rowToRange = new HashMap<Integer, CellRangeAddress>();
    for (int  r = 0; r < sheet.getNumMergedRegions();r++) {
      CellRangeAddress mergedRegion = sheet.getMergedRegion(r);
      for (int rr = mergedRegion.getFirstRow(); rr <= mergedRegion.getLastRow();rr++) {
        rowToRange.put(rr,mergedRegion);
      }
/*      logger.debug("for " + sheet.getSheetName() + " region  " + mergedRegion + " " +
          mergedRegion.getFirstRow() + " " + mergedRegion.getFirstColumn());*/
    }
    int id = 0;
    boolean gotHeader = false;
    FileExerciseDAO dao = new FileExerciseDAO(null,false,false);

    int colIndexOffset = 0;

    int transliterationIndex = 0;
    int unitIndex = 0;
    int chapterIndex = 0;
    int weekIndex = 0;
    List<String> lastRowValues = new ArrayList<String>();
    for (String type : new String[]{"unit", "chapter", "week"}) {
      typeToUnitToLesson.put(type, new TreeMap<String, Lesson>());
    }
    for (; iter.hasNext(); ) {
      Row next = iter.next();
  //    logger.warn("------------ Row # " + next.getRowNum() + " --------------- ");
      boolean inMergedRow = rowToRange.keySet().contains(next.getRowNum());

      List<String> columns = new ArrayList<String>();
      if (!gotHeader) {
        Iterator<Cell> cellIterator = next.cellIterator();
        while (cellIterator.hasNext()) {
          Cell next1 = cellIterator.next();
          columns.add(next1.toString().trim());
        }
      }

      if (!gotHeader) {
        for (String col : columns) {
          String colNormalized = col.toLowerCase();
          if (colNormalized.startsWith("Word".toLowerCase())) {
            gotHeader = true;
            colIndexOffset = columns.indexOf(col);
          } else if (colNormalized.contains("transliteration")) {
            transliterationIndex = columns.indexOf(col);
          } else if (colNormalized.contains("unit")) {
            unitIndex = columns.indexOf(col);
          } else if (colNormalized.contains("chapter")) {
            chapterIndex = columns.indexOf(col);
          } else if (colNormalized.contains("week")) {
            weekIndex = columns.indexOf(col);
          }
        }
      }
      else {
        int colIndex = colIndexOffset;
        String english = getCell(next, colIndex++).trim();
        String foreignLanguagePhrase = getCell(next, colIndex).trim();
        //logger.info("for row " + next.getRowNum() + " english = " + english + " in merged " + inMergedRow + " last row " + lastRowValues.size());

        if (inMergedRow && !lastRowValues.isEmpty()) {
          if (english.length() == 0) {
            english = lastRowValues.get(0);
            //logger.info("for row " + next.getRowNum() + " english using " + english);
          }
        }
        if (gotHeader && english.length() > 0) {
          // System.out.println("got entry line " +columns);
          if (inMergedRow && !lastRowValues.isEmpty()) {
            if (foreignLanguagePhrase.length() == 0) {
              foreignLanguagePhrase = lastRowValues.get(1);
              //logger.info("for row " + next.getRowNum() + " for foreign lang using " + foreignLanguagePhrase);
            }
          }
          if (foreignLanguagePhrase.length() == 0) {
            //logger.warn("Got empty foreign language phrase row #" + next.getRowNum() +" for " + english);
            errors.add(sheet.getSheetName()+"/"+
                "row #" +(next.getRowNum()+1) + " phrase was blank.");
          } else {
            String translit = getCell(next, transliterationIndex);

            if (inMergedRow && !lastRowValues.isEmpty()) {
              if (translit.length() == 0) {
                translit = lastRowValues.get(2);
                //logger.info("for row " + next.getRowNum() + " for translit using " + translit);
              }
            }

            Exercise imported = getExercise(id++, dao, english, foreignLanguagePhrase, translit);
            exercises.add(imported);
            recordUnitChapterWeek(unitIndex, chapterIndex, weekIndex, next, imported);
/*            if (false)
              logger.debug("read '" + english + "' '" + foreignLanguagePhrase +
                  "' '" + translit + "' '" + unit + "' '" + chapter + "' '" + week + "'");*/
            if (inMergedRow) {
              lastRowValues.add(english);
              lastRowValues.add(foreignLanguagePhrase);
              lastRowValues.add(translit);
          /*    lastRowValues.add(unit);
              lastRowValues.add(chapter);
              lastRowValues.add(week);*/
            }
            else if (!lastRowValues.isEmpty()) {
              lastRowValues.clear();
            }
          }
        } else if (gotHeader && foreignLanguagePhrase.length() > 0) {
          errors.add(sheet.getSheetName()+"/"+
              "row #" +(next.getRowNum()+1) + " Word/Expression was blank");// but phrase was " +foreignLanguagePhrase);
        }
      }
    }

    return exercises;
  }

  private boolean recordUnitChapterWeek(int unitIndex, int chapterIndex, int weekIndex,
                                        Row next,
                                        //String english, String foreignLanguagePhrase, String translit,
                                        Exercise imported) {
    String unit = getCell(next, unitIndex);
    String chapter = getCell(next, chapterIndex);
    String week = getCell(next, weekIndex);
    boolean val = rememberExercise(typeToUnitToLesson.get("unit"), unit.trim(), unit, chapter, week, imported);
    val |= rememberExercise(typeToUnitToLesson.get("chapter"), chapter.trim(), unit, chapter, week, imported);
    val |= rememberExercise(typeToUnitToLesson.get("week"), week.trim(), unit, chapter, week, imported);

    return val;
  }

  private Exercise getExercise(int id, FileExerciseDAO dao, String english, String foreignLanguagePhrase, String translit) {
    String content = dao.getContent(foreignLanguagePhrase, translit, english);
    Exercise imported = new Exercise("import", "" + id, content, false, true, english);
    imported.addSlot(english);
    imported.addSlot(foreignLanguagePhrase);
    imported.addSlot(translit);

    imported.addQuestion();

   // exercises.add(imported);
    return imported;
  }

  private boolean rememberExercise(Map<String, Lesson> unitToLesson, String key, String unit, String chapter, String week, Exercise imported) {
    if (key.length() > 0) {
      Lesson unitLesson = unitToLesson.get(key);
      if (unitLesson == null) unitToLesson.put(key, unitLesson = new Lesson(unit, chapter, week));
      unitLesson.addExercise(imported);
      return true;
    } else {
      return false;
    }
  }

  private String getCell(Row next, int col) {
    Cell cell = next.getCell(col);
    if (cell == null) return "";
/*    if (cell.getArrayFormulaRange().getFirstRow() != cell.getArrayFormulaRange().getFirstRow()) {
      logger.warn("got multi row cell " + cell);
    }*/
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      double numericCellValue = cell.getNumericCellValue();
      return "" + new Double(numericCellValue).intValue();
    }
    else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
      return cell.getStringCellValue();
    }
    else {
      return cell.toString();
    }
  }

  public List<Exercise> getExercises() {
    return exercises;
  }

  public List<Lesson> getLessons() {
    return lessons;
  }

  public Set<String> getSections() { return typeToUnitToLesson.keySet(); }
  public Map<String, Lesson> getSection(String type) {
    return typeToUnitToLesson.get(type);
  }

  public static void main(String[] arg) {
    //new ExcelImport().readExercises(new File("Farsi_Curriculum_Glossary_vowelized_2013_02_04.xlsx"));
    new ExcelImport().readExercises(new File("2013_02_13_Dari_List_ZR_Path.xlsx"));
  }

  public List<String> getErrors() {
    return errors;
  }
}
