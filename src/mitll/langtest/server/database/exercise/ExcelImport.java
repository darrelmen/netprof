/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import org.apache.log4j.Logger;
import org.apache.poi.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.internal.PackagePropertiesPart;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlException;

import java.io.*;
import java.util.*;

/**
 * Reads an excel spreadsheet from DLI.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/6/13
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExcelImport extends BaseExerciseDAO implements ExerciseDAO<CommonExercise> {
  private static final Logger logger = Logger.getLogger(ExcelImport.class);
  private static final String CONTEXT_TRANSLATION = "context translation";
  private static final String TRANSLATION_OF_CONTEXT = "Translation of Context";
  private static final String CONTEXT = "context";
  private static final String MEANING = "meaning";
  private static final String ID = "id";
  private static final String WORD = "word";

  private final List<String> errors = new ArrayList<String>();
  private final String file;

  private boolean shouldHaveRefAudio = false;
  private final boolean usePredefinedTypeOrder;
  private final boolean skipSemicolons;
  private final int maxExercises;

  private int unitIndex;
  private int chapterIndex;
  private int weekIndex;

  private final boolean DEBUG = false;
  private long lastModified;
  private boolean first = false;

  /**
   * @param file
   * @param userListManager
   * @param addDefects
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public ExcelImport(String file, ServerProperties serverProps,
                     UserListManager userListManager,
                     boolean addDefects) {
    super(serverProps, userListManager, addDefects);
    this.file = file;

    //  logger.info("Reading from " + file);
    maxExercises = serverProps.getMaxNumExercises();
    // turn off missing fast/slow for classroom
    shouldHaveRefAudio = false;
    this.usePredefinedTypeOrder = serverProps.usePredefinedTypeOrder();
    this.skipSemicolons = serverProps.shouldSkipSemicolonEntries();
    // from the tier index property
    this.unitIndex = serverProps.getUnitChapterWeek()[0];
    this.chapterIndex = serverProps.getUnitChapterWeek()[1];
    this.weekIndex = serverProps.getUnitChapterWeek()[2];
    if (DEBUG) logger.debug("unit " + unitIndex + " chapter " + chapterIndex + " week " + weekIndex);
  }

  protected List<CommonExercise> readExercises() {
    File file = new File(this.file);
    lastModified = file.lastModified();
    long excelLastModified = getExcelLastModified(file);
    lastModified = excelLastModified == 0 ? lastModified : excelLastModified;

    return readExercises(file);
  }

  /**
   * Ask the excel file for when it was modified
   * @param file
   * @return
   * @see #readExercises
   */
  private long getExcelLastModified(File file) {
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
      logger.info("readExercises Reading from " + file.getAbsolutePath() + " modified " +modified);

      return modified.getTime();
    } catch (IOException | OpenXML4JException | XmlException e) {
      logger.error("got " + e, e);
    }
    return 0;
  }

  /**
   * @param file
   * @return
   * @see #getRawExercises()
   */
  private List<CommonExercise> readExercises(File file) {
    try {
      return readExercises(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      logger.error(language + " : looking for " + file.getAbsolutePath() + " got " + e, e);
    }
    return new ArrayList<>();
  }

  private void log() {
    int MB = (1024 * 1024);
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    logger.debug(serverProps.getLanguage() + " current thread group " + threadGroup.getName() + " = " + threadGroup.activeCount() +
        " : # cores = " + Runtime.getRuntime().availableProcessors() + " heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
  }

  /**
   * @param inp
   * @return
   * @seex mitll.langtest.server.SiteDeployer#readExercises(mitll.langtest.shared.Site, org.apache.commons.fileupload.FileItem)
   */
  private List<CommonExercise> readExercises(InputStream inp) {
    log();
    List<CommonExercise> exercises = new ArrayList<CommonExercise>();
    String language = serverProps.getLanguage();
    try {
      long then = System.currentTimeMillis();
      // logger.debug("starting to read spreadsheet for " + language + " on " + Thread.currentThread() + " at " + System.currentTimeMillis());

      XSSFWorkbook wb = new XSSFWorkbook(inp);
//      logger.debug("finished reading spreadsheet for " + language + " on " + Thread.currentThread() + " at " + System.currentTimeMillis());

      long now = System.currentTimeMillis();
      if (now - then > 1000) {
        logger.info("readExercises took " + (now - then) + " millis to open spreadsheet for " + language + " on " + Thread.currentThread());
      }
      then = now;

      for (int i = 0; i < wb.getNumberOfSheets(); i++) {
        Sheet sheet = wb.getSheetAt(i);
        if (sheet.getPhysicalNumberOfRows() > 0) {
          Collection<CommonExercise> exercises1 = readFromSheet(sheet);
          exercises.addAll(exercises1);
          logger.info("readExercises sheet " + sheet.getSheetName() + " had " + exercises1.size() + " items.");
          if (DEBUG) {
            if (!exercises1.isEmpty()) {
              CommonExercise first = exercises1.iterator().next();
              logger.debug("e.g. " + first);// + " content  " + first.getContent());
            }
          }
        }
      }

      if (!errors.isEmpty()) {
        logger.warn("readExercises there were " + errors.size() + " errors");
        int count = 0;
        for (String error : errors) {
          if (count++ < 10) logger.warn(error);
        }
      }
      if (DEBUG) getSectionHelper().report();
      getSectionHelper().allKeysValid();
      inp.close();
      now = System.currentTimeMillis();

      if (now - then > 20) {
        logger.info("readExercises took " + (now - then) + " millis to make " + exercises.size() + " exercises for " + language);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return exercises;
  }

  /**
   * @param sheet
   * @return
   * @see #readExercises(java.io.InputStream)
   */
  private Collection<CommonExercise> readFromSheet(Sheet sheet) {
    if (DEBUG) logger.info("read from sheet " + sheet.getSheetName());

    List<CommonExercise> exercises = new ArrayList<CommonExercise>();
    int id = 0;
    boolean gotHeader = false;

    int colIndexOffset = -1;

    int transliterationIndex = -1;
    int meaningIndex = -1;
    int idIndex = -1;
    int contextIndex = -1;
    int contextTranslationIndex = -1;
    int audioIndex = -1;
    boolean hasAudioIndex = false;

    List<String> lastRowValues = new ArrayList<String>();
    Set<String> knownIds = new HashSet<String>();

    int semis = 0;
    int logging = 0;
    int skipped = 0;
    int deleted = 0;
    int englishSkipped = 0;

    String unitName = null, chapterName = null, weekName = null;
    try {
      Iterator<Row> iter = sheet.rowIterator();
      Map<Integer, CellRangeAddress> rowToRange = getRowToRange(sheet);
      boolean gotUCW = unitIndex != -1;
      List<String> columns;

      for (; iter.hasNext(); ) {
        Row next = iter.next();
        if (id > maxExercises) break;
        boolean inMergedRow = rowToRange.keySet().contains(next.getRowNum());

        if (!gotHeader) {
          columns = getHeader(next); // could be several junk rows at the top of the spreadsheet

          List<String> predefinedTypeOrder = new ArrayList<String>();
          for (String col : columns) {
            String colNormalized = col.toLowerCase();
            //if (DEBUG) logger.info("col " + colNormalized + "/" + col);

            if (colNormalized.startsWith(WORD)) {
              gotHeader = true;
              colIndexOffset = columns.indexOf(col);
            } else if (colNormalized.contains("transliteration")) {
              transliterationIndex = columns.indexOf(col);
            } else if (colNormalized.contains(MEANING)) {
              meaningIndex = columns.indexOf(col);
            } else if (colNormalized.contains(ID)) {
              idIndex = columns.indexOf(col);
            } else if (contextTransMatch(colNormalized)) { //be careful of ordering wrt this and the next item
              contextTranslationIndex = columns.indexOf(col);
            } else if (contextColMatch(colNormalized)) {
              contextIndex = columns.indexOf(col);
            } else if (colNormalized.contains("audio_index")) {
              audioIndex = columns.indexOf(col);
              hasAudioIndex = true;
            } else if (gotUCW) {
              if (DEBUG) logger.debug("using predef unit/chapter/week ");
              if (columns.indexOf(col) == unitIndex) {
                predefinedTypeOrder.add(col);
                if (DEBUG) logger.debug("predefinedTypeOrder unit " + predefinedTypeOrder);

                unitName = col;
              } else if (columns.indexOf(col) == chapterIndex) {
                predefinedTypeOrder.add(col);
                if (DEBUG) logger.debug("predefinedTypeOrder chapter " + predefinedTypeOrder);

                chapterName = col;
              } else if (columns.indexOf(col) == weekIndex) {
                predefinedTypeOrder.add(col);
                if (DEBUG) logger.debug("predefinedTypeOrder week " + predefinedTypeOrder);

                weekName = col;
              }
            } else if (colNormalized.contains("unit") || colNormalized.contains("book")) {
              unitIndex = columns.indexOf(col);
              if (DEBUG) logger.debug("predefinedTypeOrder unit " + predefinedTypeOrder);

              predefinedTypeOrder.add(col);
              unitName = col;
            } else if (colNormalized.contains("chapter") || colNormalized.contains("lesson")) {
              chapterIndex = columns.indexOf(col);
              predefinedTypeOrder.add(col);
              if (DEBUG) logger.debug("predefinedTypeOrder chapter " + predefinedTypeOrder);

              chapterName = col;
            } else if (colNormalized.contains("week")) {
              weekIndex = columns.indexOf(col);
              predefinedTypeOrder.add(col);
              if (DEBUG) logger.debug("predefinedTypeOrder week " + predefinedTypeOrder);
              weekName = col;
            }
          }
          if (usePredefinedTypeOrder) {
            getSectionHelper().setPredefinedTypeOrder(predefinedTypeOrder);
          }

          if (DEBUG) logger.debug("columns word index " + colIndexOffset +
              " week " + weekIndex + " unit " + unitIndex + " chapter " + chapterIndex +
              " meaning " + meaningIndex +
              " transliterationIndex " + transliterationIndex +
              " contextIndex " + contextIndex +
              " id " + idIndex + " audio " + audioIndex + " predef " + predefinedTypeOrder
          );
        } else {
          int colIndex = colIndexOffset;
          boolean isDelete = isDeletedRow(sheet, next, colIndex);

          String english = getCell(next, colIndex++).trim();
          // remove starting or ending tics
          String foreignLanguagePhrase = cleanTics(getCell(next, colIndex).trim());
          String translit = getCell(next, transliterationIndex);

          //logger.info("for row " + next.getRowNum() + " english = " + english + " in merged " + inMergedRow + " last row " + lastRowValues.size());

          if (inMergedRow && !lastRowValues.isEmpty()) {
            if (english.length() == 0) {
              english = lastRowValues.get(0);
            }
          }
          if (gotHeader && english.length() > 0) {
            if (inMergedRow) logger.info("got merged row ------------ ");

            if (inMergedRow && !lastRowValues.isEmpty()) {
              if (foreignLanguagePhrase.length() == 0) {
                foreignLanguagePhrase = lastRowValues.get(1);
                //logger.info("for row " + next.getRowNum() + " for foreign lang using " + foreignLanguagePhrase);
              }
            }
            if (foreignLanguagePhrase.length() == 0) {
              logger.info("Got empty foreign language phrase row #" + next.getRowNum() + " for " + english);
              errors.add(sheet.getSheetName() + "/" + "row #" + (next.getRowNum() + 1) + " phrase was blank.");
              id++;    // TODO is this the right thing for Dari and Farsi???
            } else if (skipSemicolons && (foreignLanguagePhrase.contains(";") || translit.contains(";"))) {
              semis++;
              id++;     // TODO is this the right thing for Dari and Farsi???
            } else {
              if (inMergedRow && !lastRowValues.isEmpty()) {
                if (translit.length() == 0) {
                  translit = lastRowValues.get(2);
                }
              }

              String meaning = getCell(next, meaningIndex);
              String givenIndex = getCell(next, idIndex);
              String context = getCell(next, contextIndex);
              String contextTranslation = getCell(next, contextTranslationIndex);

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;

              CommonExercise imported = isDelete ? null : getExercise(idToUse, english, foreignLanguagePhrase, translit,
                  meaning, context, contextTranslation, hasAudioIndex ? getCell(next, audioIndex) : "");

              String id1 = imported == null ? idToUse : imported.getID();

              if (!isDelete &&
                  (imported.hasRefAudio() || !shouldHaveRefAudio)) {  // skip items without ref audio, for now.
                recordUnitChapterWeek(unitIndex, chapterIndex, weekIndex, next, imported, unitName, chapterName, weekName);

                if (knownIds.contains(id1)) {
                  if (!id1.isEmpty()) {
                    logger.warn("readFromSheet : found duplicate entry under '" + id1 + "' " + imported);
                  }
                } else {
                  knownIds.add(id1);
                  exercises.add(imported);
                }
              } else {
                if (isDelete) {
                  deleted++;
                } else {
                  if (logging++ < 3) {
                    logger.info("skipping exercise " + id1 + " : '" + imported.getEnglish() + "' since no audio.");
                  }
                  skipped++;
                }
              }
              if (inMergedRow) {
                //logger.debug("found merged row...");
                lastRowValues.add(english);
                lastRowValues.add(foreignLanguagePhrase);
                lastRowValues.add(translit);
              } else if (!lastRowValues.isEmpty()) {
                lastRowValues.clear();
              }
            }
          } else if (gotHeader && foreignLanguagePhrase.length() > 0) {
            errors.add(sheet.getSheetName() + "/" + "row #" + (next.getRowNum() + 1) + " Word/Expression was blank");
          }
        }
      }
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    logStatistics(id, semis, skipped, englishSkipped, deleted);

    // put the skips at the end
    Collection<CommonExercise> Ts = readFromSheetSkips(sheet, id);
    exercises.addAll(Ts);
    return exercises;
  }

  private String doTrim(String s) {
    String trim = s.trim();
    String s1 = trim.replaceAll("\\p{Z}+", " ");
    String trim1 = s1.trim();
/*    if (!trim1.equals(s)) {
      logger.info("trim  '" + trim +
          "'");
      logger.info("s1    '" + s1 +
          "'");
      logger.info("trim1 '" + trim1 +
          "'");
    }*/
    return trim1;
  }

  private List<String> getHeader(Row next) {
    List<String> columns = new ArrayList<String>();

    Iterator<Cell> cellIterator = next.cellIterator();
    while (cellIterator.hasNext()) {
      Cell next1 = cellIterator.next();
      columns.add(next1.toString().trim());
    }

    return columns;
  }

  private boolean contextTransMatch(String colNormalized) {
    return colNormalized.contains(CONTEXT_TRANSLATION.toLowerCase()) || colNormalized.contains(TRANSLATION_OF_CONTEXT.toLowerCase());
  }

  private Collection<CommonExercise> readFromSheetSkips(Sheet sheet, int id) {
    List<CommonExercise> exercises = new ArrayList<CommonExercise>();
    boolean gotHeader = false;

    int colIndexOffset = -1;

    int transliterationIndex = -1;
    // int weightIndex = -1;
    int meaningIndex = -1;
    int idIndex = -1;
    int contextIndex = -1;
    int contextTranslationIndex = -1;
    int audioIndex = -1;
    int semis = 0;
    int skipped = 0;
    int deleted = 0;
    int englishSkipped = 0;
    String unitName = null, chapterName = null, weekName = null;
    try {
      Iterator<Row> iter = sheet.rowIterator();
      boolean gotUCW = unitIndex != -1;

      List<String> columns;

      for (; iter.hasNext(); ) {
        Row next = iter.next();

        if (!gotHeader) {
          columns = getHeader(next); // could be several junk rows at the top of the spreadsheet

          for (String col : columns) {
            String colNormalized = col.toLowerCase();
            if (colNormalized.startsWith(WORD)) {
              gotHeader = true;
              colIndexOffset = columns.indexOf(col);
            } else if (colNormalized.contains("transliteration")) {
              transliterationIndex = columns.indexOf(col);
            } else if (colNormalized.contains(MEANING)) {
              meaningIndex = columns.indexOf(col);
            } else if (colNormalized.contains(ID)) {
              idIndex = columns.indexOf(col);
            } else if (contextColMatch(colNormalized)) {
              contextIndex = columns.indexOf(col);
            } else if (contextTransMatch(colNormalized)) {
              contextTranslationIndex = columns.indexOf(col);
            } else if (colNormalized.contains("audio_index")) {
              audioIndex = columns.indexOf(col);
            } else if (gotUCW) {
              if (columns.indexOf(col) == unitIndex) {
                unitName = col;
              } else if (columns.indexOf(col) == chapterIndex) {
                chapterName = col;
              } else if (columns.indexOf(col) == weekIndex) {
                weekName = col;
              }
            } else if (colNormalized.contains("unit") || colNormalized.contains("book")) {
              unitIndex = columns.indexOf(col);
              unitName = col;
            } else if (colNormalized.contains("chapter") || colNormalized.contains("lesson")) {
              chapterIndex = columns.indexOf(col);
              chapterName = col;
            } else if (colNormalized.contains("week")) {
              weekIndex = columns.indexOf(col);
              weekName = col;
            }
          }

          if (DEBUG && !first) {
            logger.debug("columns word index " + colIndexOffset +
                " week " + weekIndex + " unit " + unitIndex + " chapter " + chapterIndex +
                " meaning " + meaningIndex +
                " transliterationIndex " + transliterationIndex +
                " contextIndex " + contextIndex +
                " contextTranslationIndex " + contextTranslationIndex +
                " id " + idIndex + " audio " + audioIndex

            );
            first = true;
          }
        } else {
          int colIndex = colIndexOffset;
          boolean isDelete = isDeletedRow(sheet, next, colIndex);
          String english = getCell(next, colIndex++).trim();
          // remove starting or ending tics
          String foreignLanguagePhrase = cleanTics(getCell(next, colIndex).trim());
          String translit = getCell(next, transliterationIndex);

          if (gotHeader && english.length() > 0) {
            if (skipSemicolons && (foreignLanguagePhrase.contains(";") || translit.contains(";"))) {
              String meaning = getCell(next, meaningIndex);
              String givenIndex = getCell(next, idIndex);
              String context = getCell(next, contextIndex);
              String contextTranslation = getCell(next, contextTranslationIndex);

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;
              CommonExercise imported = getExercise(idToUse, english, foreignLanguagePhrase, translit,
                  meaning, context, contextTranslation, (audioIndex != -1) ? getCell(next, audioIndex) : "");
              if (isDelete) {
                deleted++;
              } else if (imported.hasRefAudio() || !shouldHaveRefAudio) {  // skip items without ref audio, for now.
                recordUnitChapterWeek(unitIndex, chapterIndex, weekIndex, next, imported, unitName, chapterName, weekName);

                rememberExercise(exercises, imported);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    logStatistics(id, semis, skipped, englishSkipped, deleted);
    return exercises;
  }

  private boolean contextColMatch(String colNormalized) {
    return colNormalized.contains(CONTEXT) && (colNormalized.contains("sentence") || !colNormalized.contains("translation"));
  }

  private boolean isDeletedRow(Sheet sheet, Row next, int colIndex) {
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
      logger.debug("got error reading delete strikeout at row " + next.getRowNum() + " for " + serverProps.getLanguage());
    }
    return isDelete;
  }

  private void logStatistics(int id, int semis, int skipped, int englishSkipped, int deleted) {
    if (skipped > 0) {
      logger.info("Skipped " + skipped + " entries with missing audio. " + getPercent(skipped, id));
    }
    if (englishSkipped > 0) {
      logger.info("Skipped " + englishSkipped + " entries with missing english. " + getPercent(englishSkipped, id));
    }
    if (semis > 0) {
      logger.info("Skipped " + semis + " entries with semicolons or " + getPercent(semis, id));
    }
    if (deleted > 0) {
      logger.info("Skipped " + deleted + " deleted entries with semicolons or " + getPercent(deleted, id));
    }
  }

  private String getPercent(float skipped, float total) {
    return total == 0 ? "0 %" : (100f * skipped / total) + "%";
  }

  private Map<Integer, CellRangeAddress> getRowToRange(Sheet sheet) {
    Map<Integer, CellRangeAddress> rowToRange = new HashMap<Integer, CellRangeAddress>();
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

  /**
   * @param exercises
   * @param imported
   * @see #readFromSheet(Sheet)
   */
  private void rememberExercise(Collection<CommonExercise> exercises,
                                CommonExercise imported) {
    exercises.add(imported);
  }

  private String cleanTics(String foreignLanguagePhrase) {
    if (foreignLanguagePhrase.startsWith("\'")) {
      foreignLanguagePhrase = foreignLanguagePhrase.substring(1);
    }
    if (foreignLanguagePhrase.endsWith("\'"))
      foreignLanguagePhrase = foreignLanguagePhrase.substring(0, foreignLanguagePhrase.length() - 1);
    return doTrim(foreignLanguagePhrase);
  }

  /**
   * Don't do an overlay if it's older than the file creation date.
   *
   * @param id
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @param meaning
   * @param context
   * @return
   * @paramx weightIndex
   * @see #readFromSheet(org.apache.poi.ss.usermodel.Sheet)
   */
  private CommonExercise getExercise(String id,
                                     String english, String foreignLanguagePhrase, String translit, String meaning,
                                     String context, String contextTranslation,
                                     String audioIndex) {
    Exercise imported = new Exercise(id, context, contextTranslation, meaning, audioIndex);

    imported.setEnglishSentence(english);
    if (translit.length() > 0) {
      imported.setTransliteration(translit);
    }

    List<String> translations = new ArrayList<String>();
    if (foreignLanguagePhrase.length() > 0) {
      translations.add(foreignLanguagePhrase);
    }
    imported.setRefSentences(translations);
    imported.setForeignLanguage(foreignLanguagePhrase);

    imported.setUpdateTime(lastModified);
    return imported;
  }

  private void recordUnitChapterWeek(int unitIndex, int chapterIndex, int weekIndex,
                                     Row next,
                                     CommonExercise imported, String unitName, String chapterName, String weekName) {
    String unit = getCell(next, unitIndex);
    String chapter = getCell(next, chapterIndex);
    String week = getCell(next, weekIndex);

    if (unit.isEmpty() && chapter.isEmpty() && week.isEmpty()) {
      unit = "Other";
      chapter = "Other";
    }

    // hack to trim off leading tics
    if (unit.startsWith("'")) unit = unit.substring(1);
    if (unit.equals("intro")) unit = "Intro"; // hack
    if (chapter.startsWith("'")) chapter = chapter.substring(1);
    if (week.startsWith("'")) week = week.substring(1);

//   if (debug && false)
/*      logger.debug("unit(" +unitName+
        ")" + unitIndex + "/" + unit + " chapter " + chapterIndex + "/(" +chapterName+
        ")" + chapter + " week (" + weekName+ ") : " + week);*/

    List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();
    SectionHelper<CommonExercise> sectionHelper = getSectionHelper();
    if (unit.length() > 0) {
      pairs.add(sectionHelper.addExerciseToLesson(imported, unitName, unit));
    } else if (unitName != null) {
      unit = chapter;
      pairs.add(sectionHelper.addExerciseToLesson(imported, unitName, unit));
    }
    if (chapter.length() > 0) {
      if (language.equalsIgnoreCase("English")) {
        chapter = (unitIndex == -1 ? "" : unit + "-") + chapter; // hack for now to get unique chapters...
      }
      pairs.add(sectionHelper.addExerciseToLesson(imported, chapterName, chapter));
    } else if (chapterName != null) {
      chapter = unit;
      pairs.add(sectionHelper.addExerciseToLesson(imported, chapterName, chapter));
//      logger.info("skip unit '" +unit + "' and chapter '" +chapter+ "'");
    }
    if (week.length() > 0) {
      pairs.add(sectionHelper.addExerciseToLesson(imported, weekName, week));
    }
    sectionHelper.addAssociations(pairs);
  }

  private String getCell(Row next, int col) {
    if (col == -1) return "";
    Cell cell = next.getCell(col);
    if (cell == null) return "";
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      double numericCellValue = cell.getNumericCellValue();
      if ((new Double(numericCellValue).intValue()) < numericCellValue)
        return "" + numericCellValue;
      else
        return "" + new Double(numericCellValue).intValue();
    } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
      return cell.getStringCellValue().trim();
    } else {
      return cell.toString().trim();
    }
  }
}
