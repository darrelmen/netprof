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
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.Pair;
import mitll.npdata.dao.SlickExercisePhone;
import mitll.npdata.dao.SlickUpdateDominoPair;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.internal.PackagePropertiesPart;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
  private static final Logger logger = LogManager.getLogger(ExcelImport.class);
  private static final String CONTEXT_TRANSLATION = "context translation";
  private static final String TRANSLATION_OF_CONTEXT = "Translation of Context";
  private static final String CONTEXT = "context";
  private static final String MEANING = "meaning";
  private static final String ID = "id";
  private static final String WORD = "word";
  private static final int REASONABLE_PROPERTY_SPACE_LIMIT = 50;

  private static final String UNIT = "unit";

  @Override
  public int updateDominoBulk(List<SlickUpdateDominoPair> pairs) {
    return 0;
  }

  @Override
  public int getExIDForDominoID(int projID, int dominoID) {
    return 0;
  }

  @Override
  public int getParentFor(int exid) {
    return 0;
  }

  private static final String BOOK = "book";

  private static final String CHAPTER = "chapter";
  private static final String LESSON = "lesson";
  private static final String MODULE = "Module";

  private static final String OTHER = "Other";
  private static final String EN_TRANSLATION = "EN Translation";
  private static final String ENGLISH = "English";

  /**
   * @see #readFromSheet(Sheet)
   */
  private static final String WEEK = "week";
  private static final String ALT = "alt";

  private final List<String> errors = new ArrayList<>();
  private final String file;

  private boolean shouldHaveRefAudio;
  private final boolean usePredefinedTypeOrder;
  private final boolean skipSemicolons;
  private final int maxExercises;

  private int unitIndex;
  private int chapterIndex;
  private int weekIndex;

  private final boolean DEBUG = false;
  private final boolean DEBUG_DETAIL = false;
  private long lastModified;
  private boolean first = false;

  /**
   * @param file
   * @param userListManager
   * @param addDefects
   * @see mitll.langtest.server.database.DatabaseImpl#makeExerciseDAO
   */
  public ExcelImport(String file,
                     ServerProperties serverProps,
                     IUserListManager userListManager,
                     boolean addDefects) {
    super(serverProps, userListManager, addDefects, serverProps.getLanguage());
    this.file = file;

    logger.info("Reading from " + file);
    maxExercises = serverProps.getMaxNumExercises();
    // turn off missing fast/slow for classroom
    shouldHaveRefAudio = false;
    this.usePredefinedTypeOrder = serverProps.usePredefinedTypeOrder();
    this.skipSemicolons = serverProps.shouldSkipSemicolonEntries();
    // from the tier index property
    this.unitIndex = serverProps.getUnitChapterWeek()[0];
    this.chapterIndex = serverProps.getUnitChapterWeek()[1];
    this.weekIndex = serverProps.getUnitChapterWeek()[2];
    if (DEBUG || unitIndex > 0)
      logger.debug("ExcelImport unit " + unitIndex + " chapter " + chapterIndex + " week " + weekIndex);
  }

  @Override
  public void setDependencies(IUserExerciseDAO userExerciseDAO,
                              AddRemoveDAO addRemoveDAO,
                              IAudioDAO audioDAO,
                              int projid,
                              Database database,
                              boolean isMyProject) {
    this.userExerciseDAO = new UserExerciseDAO(database);
    this.userExerciseDAO.setExerciseDAO(this);
    this.addRemoveDAO = addRemoveDAO;
    setAudioDAO(audioDAO, projid, isMyProject);
  }

  /**
   * @return
   * @see #getRawExercises
   */
  protected List<CommonExercise> readExercises() {
    File file = new File(this.file);
    logger.info("readExercises from " + file.getAbsolutePath());

    lastModified = file.lastModified();
    long excelLastModified = getExcelLastModified(file);
    lastModified = excelLastModified == 0 ? lastModified : excelLastModified;

    List<CommonExercise> exercises = readExercises(file);

    for (CommonExercise exercise : exercises) {
      if (exercise.getOldID().equalsIgnoreCase("3473")) {
        logger.info("readExercises got " + exercise);
      } else if (exercise.getEnglish().equalsIgnoreCase("blackboard")) {
        logger.info("readExercises got blackboard " + exercise);
      }
    }

    getSectionHelper().report();

    return exercises;
  }

  /**
   * Ask the excel file for when it was modified
   *
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
      logger.info("readExercises Reading from " + file.getAbsolutePath() + " modified " + modified);

      return modified == null ? System.currentTimeMillis() : modified.getTime();
    } catch (IOException | OpenXML4JException | XmlException e) {
      logger.error("got " + e, e);
    }
    return 0;
  }

  /**
   * @param file
   * @return
   * @see #readExercises()
   */
  private List<CommonExercise> readExercises(File file) {
    try {
      FileInputStream inp = new FileInputStream(file);
      List<CommonExercise> commonExercises = readExercises(inp);
      inp.close();
      return commonExercises;
    } catch (Exception e) {
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
  public List<CommonExercise> readExercises(InputStream inp) {
    log();
    List<CommonExercise> exercises = new ArrayList<>();
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
        int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();

        if (DEBUG)
          logger.info("readExercises sheet " + sheet.getSheetName() + " had " + physicalNumberOfRows + " rows.");

        if (physicalNumberOfRows > 0) {
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
    List<CommonExercise> exercises = new ArrayList<>();
    int id = 0;
    boolean gotHeader = false;

    int colIndexOffset = -1;

    int transliterationIndex = -1;
    int meaningIndex = -1;
    int idIndex = -1;
    int altIndex = -1;
    int contextIndex = -1;
    int altcontextIndex = -1;
    int contextTranslationIndex = -1;
    int audioIndex = -1;
//    boolean hasAudioIndex = false;

    List<String> lastRowValues = new ArrayList<>();
    Set<String> knownIds = new HashSet<>();

    int semis = 0;
    int logging = 0;
    int skipped = 0;
    int deleted = 0;
    int englishSkipped = 0;
    int rows = 0;
    String unitName = null, chapterName = null, weekName = null;

    List<String> typeOrder = getTypeOrder();
    String first = typeOrder.size() > 0 ? typeOrder.get(0) : "";
    String second = typeOrder.size() > 1 ? typeOrder.get(1) : "";

    logger.info("readFromSheet initial type order First '" + first + "' second '" + second + "'");
    try {
      Iterator<Row> iter = sheet.rowIterator();
      Map<Integer, CellRangeAddress> rowToRange = getRowToRange(sheet);
      boolean gotUCW = unitIndex != -1;
      List<String> columns;

      Map<Integer, String> colToHeader = new HashMap<>();
      Map<String, ExerciseAttribute> pairToAttr = new HashMap<>();
      Map<String, List<ExerciseAttribute>> attrToItself = new HashMap<>();

      for (; iter.hasNext(); ) {
        Row next = iter.next();
        rows++;
        if (id > maxExercises) break;
        boolean inMergedRow = rowToRange.keySet().contains(next.getRowNum());

        if (!gotHeader) {
          columns = getHeader(next); // could be several junk rows at the top of the spreadsheet

          List<String> predefinedTypeOrder = new ArrayList<>();
          for (String col : columns) {
            String colNormalized = col.toLowerCase();
            int i = columns.indexOf(col);

//            logger.info("readFromSheet col " + i + " '" + colNormalized + "'");
            if (isMatchForEnglish(colNormalized)) {
              gotHeader = true;
              colIndexOffset = i;
            } else if (colNormalized.contains("transliteration")) {
              transliterationIndex = i;
            } else if (colNormalized.contains(MEANING)) {
              meaningIndex = i;
            } else if (colNormalized.contains(ID)) {
              idIndex = i;
            } else if (contextColMatch(colNormalized) && colNormalized.contains(ALT)) {
              altcontextIndex = i;
            } else if (colNormalized.contains(ALT)) {
              altIndex = i;
            } else if (contextTransMatch(colNormalized)) { //be careful of ordering wrt this and the next item
              contextTranslationIndex = i;
            } else if (contextColMatch(colNormalized)) {
              contextIndex = i;
            } else if (colNormalized.contains("audio_index")) {
              audioIndex = i;
//              hasAudioIndex = true;
            } else if (gotUCW) {
              if (DEBUG || true)
                logger.debug("readFromSheet using predef unit/chapter/week " + unitIndex + "," + chapterIndex + "," + weekIndex);
              if (i == unitIndex) {
                predefinedTypeOrder.add(col);
                unitName = col;
              } else if (i == chapterIndex) {
                predefinedTypeOrder.add(col);
                chapterName = col;
              } else if (i == weekIndex) {
                predefinedTypeOrder.add(col);
                weekName = col;
              } else {
                addColToHeaderForProperty(colToHeader, col, i);
              }
            } else if (isFirstTypeMatch(colNormalized, first) && unitIndex == -1) {
              logger.info("readFromSheet first type match " + colNormalized + " " + first + " unit " + i);
              unitIndex = i;
              predefinedTypeOrder.add(col);
              unitName = col;
            } else if (isSecondTypeMatch(colNormalized, second)) {
              logger.info("readFromSheet second type match " + colNormalized + " " + second + " unit " + i);
              chapterIndex = i;
              predefinedTypeOrder.add(col);
              chapterName = col;
            } else if (colNormalized.contains(WEEK)) {
              weekIndex = i;
              predefinedTypeOrder.add(col);
              weekName = col;
            } else {
              addColToHeaderForProperty(colToHeader, col, i);
            }
          }
          if (usePredefinedTypeOrder) {
            getSectionHelper().setPredefinedTypeOrder(predefinedTypeOrder);
          }

          if (DEBUG || true) logger.info("readFromSheet columns" +
              " word index " + colIndexOffset +
              " altfl " + altIndex +
              " week " + weekIndex +
              " unit " + unitIndex +
              " chapter " + chapterIndex +
              " meaning " + meaningIndex +
              " transliterationIndex " + transliterationIndex +
              " contextIndex " + contextIndex +
              " altcontext " + altcontextIndex +
              " id " + idIndex +
              " audio " + audioIndex +
              " other " + colToHeader +
              " predef " + usePredefinedTypeOrder
          );
        } else {
          int colIndex = colIndexOffset;
          boolean isDelete = isDeletedRow(sheet, next, colIndex);

          String english = getCell(next, colIndex++).trim();
          // remove starting or ending tics
          String foreignLanguagePhrase = cleanTics(getCell(next, colIndex).trim());
          String translit = getCell(next, transliterationIndex);
          String altfl = getCell(next, altIndex);

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
              String altcontext = getCell(next, altcontextIndex);
              String contextTranslation = getCell(next, contextTranslationIndex);

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;

              List<ExerciseAttribute> exerciseAttributes = getExerciseAttributes(colToHeader, pairToAttr, attrToItself, next);

              CommonExercise imported = isDelete ? null :
                  getExercise(idToUse, english, foreignLanguagePhrase, altfl, translit,
                      meaning,
                      context, altcontext, contextTranslation
                  );

              if (imported != null) imported.setAttributes(exerciseAttributes);

//              logger.info("attr for " + imported.getOldID() + " are " + imported.getAttributes());
              String id1 = imported == null ? idToUse : imported.getOldID();

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
                    logger.info("readFromSheet skipping exercise " + imported.getOldID() + " : '" + imported.getEnglish() + "' since no audio.");
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

    if (DEBUG) logger.debug("read " + rows + " rows");

    logStatistics(id, semis, skipped, englishSkipped, deleted);

    // put the skips at the end
    Collection<CommonExercise> Ts = readFromSheetSkips(sheet, id);
    exercises.addAll(Ts);
    return exercises;
  }

  private boolean isMatchForEnglish(String colNormalized) {
    return colNormalized.startsWith(WORD) || colNormalized.equalsIgnoreCase(ENGLISH);
  }

  private final Set<String> toSkip =
      new HashSet<>(Arrays.asList(
          "Translation".toLowerCase(),
          "Transliteration".toLowerCase(),
          "Correction".toLowerCase(),
          "Corrections".toLowerCase(),
          "Comment".toLowerCase(),
          "Comments".toLowerCase()
      ));

  /**
   * @param colToHeader
   * @param col
   * @param i
   * @see #readFromSheet
   */
  private void addColToHeaderForProperty(Map<Integer, String> colToHeader, String col, int i) {
    String s = language.toLowerCase();
    String lcCol = col.toLowerCase();
    if (lcCol.contains(s) || toSkip.contains(lcCol)) {
      logger.debug("addColToHeaderForProperty skipping col " + col);
    } else if (!col.isEmpty()) {
      if (DEBUG) logger.debug("addColToHeaderForProperty adding col '" + col + "' at  " + i + " vs '" + language + "'");
      colToHeader.put(i, col);
    }
  }

  /**
   * @param colToHeader
   * @param pairToAttr
   * @param attrToItself
   * @param next
   * @return
   * @see #readFromSheet
   */
  @NotNull
  private List<ExerciseAttribute> getExerciseAttributes(Map<Integer, String> colToHeader,
                                                        Map<String, ExerciseAttribute> pairToAttr,
                                                        Map<String, List<ExerciseAttribute>> attrToItself,
                                                        Row next) {
    List<ExerciseAttribute> toAdd = new ArrayList<>();
    for (Map.Entry<Integer, String> pair : colToHeader.entrySet()) {
      Integer colIndex = pair.getKey();
      String value = getCell(next, colIndex);
      if (!value.isEmpty()) {
        String property = pair.getValue();

        String propertyValuePair = property + "-" + value;
        ExerciseAttribute exerciseAttribute1 = pairToAttr.get(propertyValuePair);
        if (exerciseAttribute1 == null) {
          exerciseAttribute1 = new ExerciseAttribute(property, value);
          //     logger.info("Remember attr " + exerciseAttribute1);
          pairToAttr.put(propertyValuePair, exerciseAttribute1);
          if (pairToAttr.size() > REASONABLE_PROPERTY_SPACE_LIMIT) {
            logger.warn("getExerciseAttributes more than " + pairToAttr.size() + " distinct values for property " + property +
                " e.g. " + propertyValuePair + " = " + exerciseAttribute1);
          }
        }
        toAdd.add(exerciseAttribute1);
      }
    }

    String attributeSet = toAdd.toString();
    List<ExerciseAttribute> exerciseAttributes = attrToItself.get(attributeSet);
    if (exerciseAttributes == null) {
      attrToItself.put(attributeSet, exerciseAttributes = toAdd);
      if (DEBUG) logger.info("getExerciseAttributes Remember attr list " + exerciseAttributes);
    }
    return exerciseAttributes;
  }

  private List<String> getHeader(Row next) {
    List<String> columns = new ArrayList<>();

    Iterator<Cell> cellIterator = next.cellIterator();
    while (cellIterator.hasNext()) {
      columns.add(cellIterator.next().toString().trim());
    }

    return columns;
  }

  private boolean contextTransMatch(String colNormalized) {
    return
        colNormalized.contains(CONTEXT_TRANSLATION.toLowerCase()) ||
            colNormalized.contains(TRANSLATION_OF_CONTEXT.toLowerCase()) ||
            colNormalized.contains(EN_TRANSLATION.toLowerCase());
  }

  private Collection<CommonExercise> readFromSheetSkips(Sheet sheet, int id) {
    List<CommonExercise> exercises = new ArrayList<>();
    boolean gotHeader = false;

    int colIndexOffset = -1;

    int transliterationIndex = -1;
    // int weightIndex = -1;
    int meaningIndex = -1;
    int idIndex = -1;
    int altIndex = -1;
    int contextIndex = -1;
    int altcontextIndex = -1;
    int contextTranslationIndex = -1;
    int audioIndex = -1;
    int semis = 0;
    int skipped = 0;
    int deleted = 0;
    int englishSkipped = 0;
    String unitName = null, chapterName = null, weekName = null;

    List<String> typeOrder = getTypeOrder();
    String firstType = typeOrder.size() > 0 ? typeOrder.get(0) : "";
    String second = typeOrder.size() > 1 ? typeOrder.get(1) : "";

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
            if (isMatchForEnglish(colNormalized)) {
              gotHeader = true;
              colIndexOffset = columns.indexOf(col);
            } else if (colNormalized.contains("transliteration")) {
              transliterationIndex = columns.indexOf(col);
            } else if (colNormalized.contains(MEANING)) {
              meaningIndex = columns.indexOf(col);
            } else if (colNormalized.contains(ID)) {
              idIndex = columns.indexOf(col);
            } else if (contextColMatch(colNormalized) && colNormalized.contains(ALT)) {
              altcontextIndex = columns.indexOf(col);
            } else if (colNormalized.contains(ALT)) {
              altIndex = columns.indexOf(col);
            } else if (contextColMatch(colNormalized)) {
              contextIndex = columns.indexOf(col);
            } else if (contextTransMatch(colNormalized)) { //be careful of ordering wrt this and the next item
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
            } else if (isFirstTypeMatch(colNormalized, firstType) && unitIndex == -1) {
              unitIndex = columns.indexOf(col);
              unitName = col;
            } else if (isSecondTypeMatch(colNormalized, second)) {
              chapterIndex = columns.indexOf(col);
              chapterName = col;
            } else if (colNormalized.contains(WEEK)) {
              weekIndex = columns.indexOf(col);
              weekName = col;
            }
          }

          if (DEBUG && !first) {
            logger.debug("readFromSheetSkips columns word index " + colIndexOffset +
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
          String altfl = getCell(next, altIndex);
          String translit = getCell(next, transliterationIndex);

          if (gotHeader && english.length() > 0) {
            if (skipSemicolons && (foreignLanguagePhrase.contains(";") || translit.contains(";"))) {
              String meaning = getCell(next, meaningIndex);
              String givenIndex = getCell(next, idIndex);
              String context = getCell(next, contextIndex);
              String altcontext = getCell(next, altcontextIndex);
              String contextTranslation = getCell(next, contextTranslationIndex);

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;
              CommonExercise imported = getExercise(idToUse, english, foreignLanguagePhrase, altfl, translit,
                  meaning, context, altcontext, contextTranslation);
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

  private boolean isFirstTypeMatch(String colNormalized, String typeSpecified) {
    return !colNormalized.isEmpty() &&
        (colNormalized.equalsIgnoreCase(typeSpecified) ||
            colNormalized.contains(UNIT) ||
            colNormalized.contains(BOOK));
  }

  private boolean isSecondTypeMatch(String colNormalized, String typeSpecified) {
    boolean b = !colNormalized.isEmpty() &&
        (colNormalized.equalsIgnoreCase(typeSpecified) ||
            colNormalized.contains(CHAPTER) ||
            colNormalized.contains(LESSON) ||
            colNormalized.contains(MODULE)
        );
    logger.info("isSecondTypeMatch check '" + colNormalized + "' vs '" + typeSpecified + "' = " + b);
    return b;
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
    return foreignLanguagePhrase;
  }

  /**
   * Don't do an overlay if it's older than the file creation date.
   *
   * @param id
   * @param english
   * @param foreignLanguagePhrase
   * @param altfl
   * @param translit
   * @param meaning
   * @param context
   * @return
   * @see #readFromSheet(org.apache.poi.ss.usermodel.Sheet)
   */
  private CommonExercise getExercise(String id,
                                     String english,
                                     String foreignLanguagePhrase,
                                     String altfl,
                                     String translit,
                                     String meaning,
                                     String context,
                                     String altcontext,
                                     String contextTranslation) {
    Exercise imported = new Exercise(id, context, altcontext, contextTranslation, meaning, -1, lastModified, StringUtils.stripAccents(context));

    imported.setEnglishSentence(english);
    if (translit.length() > 0) {
      imported.setTransliteration(translit);
    }

    List<String> translations = new ArrayList<>();
    if (foreignLanguagePhrase.length() > 0) {
      translations.add(foreignLanguagePhrase);
    }
    imported.setRefSentences(translations);
    imported.setForeignLanguage(foreignLanguagePhrase);
    imported.setAltFL(altfl);

    if (DEBUG_DETAIL) logger.info("getExercise got " + imported);
    return imported;
  }

  private void recordUnitChapterWeek(int unitIndex, int chapterIndex, int weekIndex,
                                     Row next,
                                     CommonExercise imported, String unitName, String chapterName, String weekName) {
    String unit = getCell(next, unitIndex);
    String chapter = getCell(next, chapterIndex);
    String week = getCell(next, weekIndex);

    if (unit.isEmpty() && chapter.isEmpty() && week.isEmpty()) {
      unit = OTHER;
      chapter = OTHER;
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

    List<Pair> pairs = new ArrayList<>();
    ISection<CommonExercise> sectionHelper = getSectionHelper();
    if (unit.length() > 0) {
      pairs.add(sectionHelper.addExerciseToLesson(imported, unitName, unit));
//      logger.info("recordUnitChapterWeek Adding " + unitName + "=" + unit + " to " + imported.getID() + " " + imported.getEnglish());
    } else if (unitName != null) {
      unit = chapter;
      pairs.add(sectionHelper.addExerciseToLesson(imported, unitName, unit));
    }
    if (chapter.length() > 0) {
      if (language.equalsIgnoreCase("English")) {
        chapter = (unitIndex == -1 ? "" : unit + "-") + chapter; // hack for now to get unique chapters...
      }
      pairs.add(sectionHelper.addExerciseToLesson(imported, chapterName, chapter));
//      logger.info("recordUnitChapterWeek Adding " + chapterName + "=" + chapter + " to " + imported.getID() + " " + imported.getEnglish());
    } else if (chapterName != null) {
      chapter = unit;
      pairs.add(sectionHelper.addExerciseToLesson(imported, chapterName, chapter));
//      logger.info("skip unit '" +unit + "' and chapter '" +chapter+ "'");
    }
    if (week.length() > 0) {
      pairs.add(sectionHelper.addExerciseToLesson(imported, weekName, week));
    }
//    logger.info("recordUnitChapterWeek now  " + imported.getID() + " = " + imported.getUnitToValue());
  }

  private String getCell(Row next, int col) {
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

/*
  @Override
  public Map<Integer, String> getIDToFL(int projid) {
    return null;
  }
*/

  @Override
  public void markSafeUnsafe(Set<Integer> safe, Set<Integer> unsafe, long dictTimestamp) {
  }

  @Override
  public void updatePhonesBulk(List<SlickExercisePhone> pairs) {

  }
}
