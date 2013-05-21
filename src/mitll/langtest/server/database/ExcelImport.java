package mitll.langtest.server.database;

import corpus.ModernStandardArabicLTS;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Lesson;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private final boolean isFlashcard;
  private final boolean isRTL;

  private List<Exercise> exercises = null;
  private List<String> errors = new ArrayList<String>();
  private final String file;
  private SectionHelper sectionHelper = new SectionHelper();
  private boolean debug = false;
  private String mediaDir;
  private Set<Integer> missingSlowSet = new HashSet<Integer>();
  private Set<Integer> missingFastSet = new HashSet<Integer>();
  /**
   * @see mitll.langtest.server.SiteDeployer#readExercisesPopulateSite(mitll.langtest.shared.Site, String, java.io.InputStream)
   */
  public ExcelImport() {
    this.file = null;
    this.isFlashcard = false;
    this.isRTL = false;
  }

  /**
   * @see DatabaseImpl#makeDAO
   * @param file
   * @param isFlashcard
   * @param isRTL
   * @param relativeConfigDir
   */
  public ExcelImport(String file, boolean isFlashcard, String mediaDir, boolean isRTL, String relativeConfigDir) {
    this.file = file;
    this.isFlashcard = isFlashcard;
    this.mediaDir = mediaDir;
    this.isRTL = isRTL;
    getMissing(relativeConfigDir,"missingSlow.txt",missingSlowSet);
    getMissing(relativeConfigDir,"missingFast.txt",missingFastSet);
    logger.debug("config " + relativeConfigDir +
      " media dir " +mediaDir + " slow missing " +missingSlowSet.size() + " fast " + missingFastSet.size());
  }

  private void getMissing(String relativeConfigDir,String file, Set<Integer> missing) {
    File missingSlow = new File(relativeConfigDir, file);
    if (missingSlow.exists()) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(missingSlow));
        String line;
        while ((line = reader.readLine()) != null) {
          String trim = line.trim();
          if (trim.length() > 0) {
            missing.add(Integer.parseInt(trim));
          }
        }
      } catch (Exception e) {
        logger.error("Reading " + missingSlow.getAbsolutePath() + " Got  " + e, e);
      }

    }
    else {
      logger.debug("Can't find " + file + " under " + relativeConfigDir + " abs path " + missingSlow.getAbsolutePath());
    }
  }

  @Override
  public SectionHelper getSectionHelper() {
    return sectionHelper;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getExercises()
   * @return
   */
  public List<Exercise> getRawExercises() {
    synchronized (this) {
      if (exercises == null) {
        exercises = readExercises(new File(file));
      }
    }
    return exercises;
  }

  private List<Exercise> readExercises(File file) {
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
      Workbook wb = WorkbookFactory.create(inp);

      for (int i = 0; i < wb.getNumberOfSheets(); i++) {
        Sheet sheet = wb.getSheetAt(i);
        if (sheet.getPhysicalNumberOfRows() > 0) {
          logger.info("------------ reading sheet " + sheet.getSheetName() + " ------------------");
          Collection<Exercise> exercises1 = readFromSheet(sheet);
          exercises.addAll(exercises1);
          logger.info("sheet " + sheet.getSheetName() + " had " + exercises1.size() + " items.");
          if (!exercises1.isEmpty()) {
            Exercise first = exercises1.iterator().next();
            logger.debug("e.g. " + first + " content  " + first.getContent() + " weight " + first.getWeight());
          }
          //for (Exercise e: exercises1) logger.info("ex " +e.getID() + " " +e.getSlots());
        }
      }

      if (!errors.isEmpty()) {
        logger.warn("there were " + errors.size() + " errors");
        for (String error : errors) {
          logger.warn(error);
        }
      }
      sectionHelper.report();

/*      if (!errors.isEmpty()) {
        logger.warn("there were " + errors.size() + " errors : " + errors);
      }*/

      inp.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InvalidFormatException e) {
      e.printStackTrace();
    }
    return exercises;
  }

  /**
   * @see #readExercises(java.io.InputStream)
   * @param sheet
   * @return
   */
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
    FileExerciseDAO dao = new FileExerciseDAO("","", false, isFlashcard);

    int colIndexOffset = 0;

    int transliterationIndex = 0;
    int unitIndex = 0;
    int chapterIndex = 0;
    int weekIndex = 0;
    int weightIndex = -1;
    List<String> lastRowValues = new ArrayList<String>();
    Map<String,List<Exercise>> englishToExercises = new HashMap<String, List<Exercise>>();
    int semis = 0;

    try {
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
            } else if (colNormalized.contains("weight")) {
              weightIndex = columns.indexOf(col);
            }
          }
        }
        else {
          int colIndex = colIndexOffset;
          String english = getCell(next, colIndex++).trim();
          String foreignLanguagePhrase = getCell(next, colIndex).trim();

          // remove starting or ending tics
          foreignLanguagePhrase = cleanTics(foreignLanguagePhrase);

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
              errors.add(sheet.getSheetName()+"/"+"row #" +(next.getRowNum()+1) + " phrase was blank.");
            } else if (foreignLanguagePhrase.contains(";")) {
              semis++;
            } else {
              String translit = getCell(next, transliterationIndex);

              if (inMergedRow && !lastRowValues.isEmpty()) {
                if (translit.length() == 0) {
                  translit = lastRowValues.get(2);
                  //logger.info("for row " + next.getRowNum() + " for translit using " + translit);
                }
              }

              Exercise imported = getExercise(id++, dao, weightIndex, next, english, foreignLanguagePhrase, translit);
              recordUnitChapterWeek(unitIndex, chapterIndex, weekIndex, next, imported);

              // keep track of synonyms (or better term)
              String englishSentence = imported.getEnglishSentence();
              List<Exercise> exercisesForSentence = englishToExercises.get(englishSentence);
              if (exercisesForSentence == null) englishToExercises.put(englishSentence, exercisesForSentence = new ArrayList<Exercise>());
              exercisesForSentence.add(imported);

              exercises.add(imported);
  /*            if (false)
                logger.debug("read '" + english + "' '" + foreignLanguagePhrase +
                    "' '" + translit + "' '" + unit + "' '" + chapter + "' '" + week + "'");*/
              if (inMergedRow) {
                lastRowValues.add(english);
                lastRowValues.add(foreignLanguagePhrase);
                lastRowValues.add(translit);
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

      addSynonyms(englishToExercises);
    } catch (Exception e) {
      logger.error("got " + e,e);
    }
    if (semis > 0) {
      logger.info("Skipped " + semis + " entries with semicolons or " + (100f*((float)semis)/(float)exercises.size())+ "%");
    }
    return exercises;
  }

  private void addSynonyms(Map<String, List<Exercise>> englishToExercises) {
    for (List<Exercise> exercises2 : englishToExercises.values()) {
      if (exercises2.size() > 1) {
        List<String> translations = new ArrayList<String>();
        List<String> transliterations = new ArrayList<String>();
        for (Exercise e : exercises2) {
          for (int i = 0; i < e.getRefSentences().size(); i++) {
            try {
              String ref = e.getRefSentences().get(i);
              String translit = e.getTranslitSentences().get(i);
              if (!translations.contains(ref)) {
                translations.add(ref);
                transliterations.add(translit);
              }
            } catch (Exception e1) {
              logger.error("got " + e1 + " on " + e);
            }
          }
        }

        for (Exercise e : exercises2) {
          e.setSynonymSentences(translations);
          e.setSynonymTransliterations(transliterations);
        }
      }
    }
  }

  private String cleanTics(String foreignLanguagePhrase) {
    if (foreignLanguagePhrase.startsWith("\'")) {
      foreignLanguagePhrase = foreignLanguagePhrase.substring(1);
    }
    if (foreignLanguagePhrase.endsWith("\'"))   foreignLanguagePhrase = foreignLanguagePhrase.substring(0,foreignLanguagePhrase.length()-1);
    return foreignLanguagePhrase;
  }

  private void checkLTS(int id, BufferedWriter writer, SmallVocabDecoder svd, ModernStandardArabicLTS lts, String english, String foreignLanguagePhrase) {
    List<String> tokens = svd.getTokens(foreignLanguagePhrase);
    try {

      for (String token : tokens) {
        String[][] process = lts.process(token);
        if (process == null) {
          String message = "couldn't do lts on exercise #" + (id - 1) + " token '" + token +
            "' length " + token.length() + " trim '" + token.trim() +
            "' " +
            " '" + foreignLanguagePhrase + "' english = '" + english + "'";
          logger.error(message);
          //logger.error("\t tokens " + tokens + " num =  " + tokens.size());

          writer.write(message);
          writer.write("\n");
        }
      }
    } catch (Exception e) {
      logger.error("couldn't do lts on " + (id - 1) + " " + foreignLanguagePhrase + " " + english);
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  /**
   * @see #readFromSheet(org.apache.poi.ss.usermodel.Sheet)
   * @param id
   * @param dao
   * @param weightIndex
   * @param next
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @return
   */
  private Exercise getExercise(int id, FileExerciseDAO dao, int weightIndex, Row next,
                               String english, String foreignLanguagePhrase, String translit) {
    Exercise imported;
    List<String> translations = new ArrayList<String>();
    if (foreignLanguagePhrase.length() > 0) {
      translations.addAll(Arrays.asList(foreignLanguagePhrase.split(";")));
      //logger.debug(english + "->" + translations);
    }
    if (isFlashcard) {
      imported = new Exercise("flashcardStimulus", "" + (id), english, translations, english);
      if (imported.getEnglishSentence() == null && imported.getContent() == null) {
        logger.warn("both english sentence and content null for exercise " + id);
      }
   //   logger.debug("Read " + imported);
    } else {
      //    Exercise imported = new Exercise("import", "" + id, content, false, true, english);
      imported = getExercise(id, dao, english, foreignLanguagePhrase, translit);
    }
    imported.setEnglishSentence(english);
    imported.setTranslitSentence(translit);
    List<String> inOrderTranslations = new ArrayList<String>(translations);
    if (isRTL) {
 /*     if (id < 5) {
        logger.debug("is RTL! ------- " + inOrderTranslations);
      }
      Collections.reverse(inOrderTranslations);
      if (id < 5) {
        logger.debug("after ------- " + inOrderTranslations);
      }*/
    }
    imported.setRefSentences(inOrderTranslations);

    if (weightIndex != -1) {
      imported.setWeight(getNumericCell(next, weightIndex));
    }
    return imported;
  }

  private boolean recordUnitChapterWeek(int unitIndex, int chapterIndex, int weekIndex,
                                        Row next,
                                        Exercise imported) {
    String unit = getCell(next, unitIndex);
    String chapter = getCell(next, chapterIndex);
    String week = getCell(next, weekIndex);
    List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();

    if (unit.length() == 0 &&
      chapter.length() == 0 &&
      week.length() == 0
      ) {
      unit = "Blank";
    }

    // hack to trim off leading tics
    if (unit.startsWith("'")) unit = unit.substring(1);
    if (unit.equals("intro")) unit = "Intro"; // hack
    if (chapter.startsWith("'")) chapter = chapter.substring(1);
    if (week.startsWith("'")) week = week.substring(1);

    if (debug) logger.debug("unit " + unit + " chapter " + chapter + " week " + week);

    if (unit.length() > 0) pairs.add(sectionHelper.addUnitToLesson(imported,unit));
    if (chapter.length() > 0) pairs.add(sectionHelper.addChapterToLesson(imported,chapter));
    if (week.length() > 0) pairs.add(sectionHelper.addWeekToLesson(imported,week));
    sectionHelper.addAssociations(pairs);

    return false;
  }

  /**
   * @see #getExercise(int, FileExerciseDAO, int, org.apache.poi.ss.usermodel.Row, String, String, String)
   * @param id
   * @param dao
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @return
   */
  private Exercise getExercise(int id, FileExerciseDAO dao, String english, String foreignLanguagePhrase, String translit) {
    String content = dao.getContent(foreignLanguagePhrase, translit, english);
    Exercise imported = new Exercise("import", "" + id, content, false, true, english);
/*    imported.addSlot(english);
    imported.addSlot(foreignLanguagePhrase);
    imported.addSlot(translit);*/

    imported.addQuestion();

    String name = ""+id;
    String fastAudioRef = mediaDir+File.separator+name+File.separator+ "Fast" + ".wav";
    String slowAudioRef = mediaDir+File.separator+name+File.separator+ "Slow" + ".wav";

  //  logger.debug("path is " + fastAudioRef);
    imported.setType(Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW);

    if (!missingFastSet.contains(id)) {
      imported.setRefAudio(ensureForwardSlashes(fastAudioRef));
    }
    //else logger.debug("no fast audio for " + id);
    if (!missingSlowSet.contains(id)) {
      imported.setSlowRefAudio(ensureForwardSlashes(slowAudioRef));
    }
   // else logger.debug("no slow audio for " + id);

    return imported;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
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
      return cell.getStringCellValue().trim();
    }
    else {
      return cell.toString().trim();
    }
  }

  private double getNumericCell(Row next, int col) {
    Cell cell = next.getCell(col);
    if (cell == null) return -1;
/*    if (cell.getArrayFormulaRange().getFirstRow() != cell.getArrayFormulaRange().getFirstRow()) {
      logger.warn("got multi row cell " + cell);
    }*/
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      return cell.getNumericCellValue();
    }
    else {
      return -1;
    }
  }

  public List<Exercise> getExercises() {
    return exercises;
  }

  public Set<String> getSections() { return sectionHelper.getSections(); }
  public Map<String, Lesson> getSection(String type) {
    return sectionHelper.getSection(type);
  }

  public List<String> getErrors() {
    return errors;
  }
}
