package mitll.langtest.server.database.exercise;

import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.AudioDAO;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.*;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

/**
 * Reads an excel spreadsheet from DLI.
 * <p>
 * User: GO22670
 * Date: 2/6/13
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExcelImport implements ExerciseDAO {
  private static final Logger logger = Logger.getLogger(ExcelImport.class);
  private static final String FAST_WAV = "Fast" + ".wav";
  private static final String SLOW_WAV = "Slow" + ".wav";
  private static final String CONTEXT_TRANSLATION = "context translation";
  private static final String TRANSLATION_OF_CONTEXT = "Translation of Context";
  private static final String CONTEXT = "context";
  private static final String MEANING = "meaning";
  private static final String ID = "id";

  private List<CommonExercise> exercises = null;
  private final Map<String, CommonExercise> idToExercise = new HashMap<String, CommonExercise>();
  private final List<String> errors = new ArrayList<String>();
  private final String file;
  private final SectionHelper sectionHelper = new SectionHelper();
  private final String mediaDir;
  private final String mediaDir1;
  private boolean shouldHaveRefAudio = false;
  private final boolean usePredefinedTypeOrder;
  private final String language;
  private final boolean skipSemicolons;
  private int audioOffset = 0;
  private final int maxExercises;
  private final ServerProperties serverProps;
  private final UserListManager userListManager;

  private AddRemoveDAO addRemoveDAO;
  private UserExerciseDAO userExerciseDAO;

  private final File installPath;
  private final boolean addDefects;
  private int unitIndex;
  private int chapterIndex;
  private int weekIndex;
  private final Map<String, Map<String, String>> idToDefectMap = new HashMap<String, Map<String, String>>();

  private final boolean DEBUG = false;

  /**
   * @param file
   * @param userListManager
   * @param addDefects
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public ExcelImport(String file, String mediaDir, ServerProperties serverProps,
                     UserListManager userListManager,
                     String installPath, boolean addDefects) {
    this.file = file;
    this.serverProps = serverProps;
    maxExercises = serverProps.getMaxNumExercises();
    this.mediaDir = mediaDir;
    mediaDir1 = mediaDir.replaceAll("bestAudio", "");
    this.addDefects = addDefects;
    this.installPath = new File(installPath);
    if (!this.installPath.exists()) {
      logger.warn("\n\n\nhuh? install path " + this.installPath.getAbsolutePath() + " doesn't exist???");
    }
    // turn off missing fast/slow for classroom
    shouldHaveRefAudio = false;
    this.usePredefinedTypeOrder = serverProps.usePredefinedTypeOrder();
    this.language = serverProps.getLanguage();
    this.skipSemicolons = serverProps.shouldSkipSemicolonEntries();
    this.audioOffset = serverProps.getAudioOffset();
    this.userListManager = userListManager;
    this.unitIndex = serverProps.getUnitChapterWeek()[0];
    this.chapterIndex = serverProps.getUnitChapterWeek()[1];
    this.weekIndex = serverProps.getUnitChapterWeek()[2];
    if (DEBUG) logger.debug("unit " + unitIndex + " chapter " + chapterIndex + " week " + weekIndex);
  }

  @Override
  public SectionHelper getSectionHelper() {
    return sectionHelper;
  }

  private Map<String, List<AudioAttribute>> exToAudio;

  /**
   * @param audioDAO
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  @Override
  public void setAudioDAO(AudioDAO audioDAO) {
    exToAudio = audioDAO.getExToAudio();
  }

  /**
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getExercises()
   */
  public List<CommonExercise> getRawExercises() {
    synchronized (this) {
      if (exercises == null) {
        exercises = readExercises(new File(file));

        addAlternates();

        addDefects(idToDefectMap);

        // remove exercises to remove
        Set<String> removes = addRemoveDAO.getRemoves();

        if (!removes.isEmpty()) logger.debug("Removing " + removes.size());
        for (String id : removes) {
          CommonExercise remove = idToExercise.remove(id);
          if (remove != null) {
//            logger.debug("\tremove " + id);
            exercises.remove(remove);
            getSectionHelper().removeExercise(remove);
          }
        }

        // mask over old items that have been overridden

        Collection<CommonUserExercise> overrides = userExerciseDAO.getOverrides();

        if (overrides.size() > 0) {
          logger.debug("found " + overrides.size() + " overrides...");
        }

        int override = 0;
        for (CommonUserExercise userExercise : overrides) {
          if (!removes.contains(userExercise.getID())) {
            CommonExercise exercise = getExercise(userExercise.getID());
            if (exercise != null) {
              //logger.debug("refresh exercise for " + userExercise.getID());
              sectionHelper.removeExercise(exercise);
              sectionHelper.addExercise(userExercise);
              addOverlay(userExercise);
              override++;
            }
            //else {
            //logger.debug("not adding as overlay " + userExercise.getID());
            //}
          }
        }
        if (override > 0) {
          logger.debug("overlay count was " + override);
        }

        // add new items
        for (String id : addRemoveDAO.getAdds()) {
          CommonUserExercise where = userExerciseDAO.getWhere(id);
          if (where == null) {
            logger.error("getRawExercises huh? couldn't find user exercise from add exercise table in user exercise table : " + id);
          } else {
            // logger.debug("adding new user exercise " + where.getID());
            add(where);
            sectionHelper.addExercise(where);
          }
        }

      }
    }
    return exercises;
  }

  /**
   * Keep track of possible alternatives for each english word - e.g. Good Bye = Ciao OR Adios
   */
  private void addAlternates() {
    Map<String, Set<String>> englishToFL = new HashMap<>();
    for (CommonExercise e : exercises) {
      idToExercise.put(e.getID(), e);

      Set<String> refs = englishToFL.getOrDefault(e.getEnglish(), new HashSet<>());
      if (refs.isEmpty()) englishToFL.put(e.getEnglish(), refs);
      refs.add(e.getForeignLanguage());
    }

    for (CommonExercise e : exercises) {
      Set<String> orDefault = englishToFL.getOrDefault(e.getEnglish(), new HashSet<>());
      if (orDefault.isEmpty()) {
        logger.error("huh? no fl for " + e);
      } else {
        e.setRefSentences(orDefault);
        //   if (orDefault.size() > 1) logger.info("For " + e.getID() + " found " + orDefault.size());
      }
    }
  }

  /**
   * @param userExercise
   * @return old exercises
   * @see mitll.langtest.server.database.DatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see #getRawExercises()
   */
  @Override
  public CommonExercise addOverlay(CommonUserExercise userExercise) {
    CommonExercise exercise = getExercise(userExercise.getID());

    if (exercise == null) {
      logger.error("addOverlay : huh? can't find " + userExercise);
    } else {
      //logger.debug("addOverlay at " +userExercise.getID() + " found " +exercise);
      synchronized (this) {
        int i = exercises.indexOf(exercise);
        if (i == -1) {
          logger.error("addOverlay : huh? couldn't find " + exercise);
        } else {
          exercises.set(i, userExercise);
        }
        idToExercise.put(userExercise.getID(), userExercise);

        //  logger.debug("addOverlay : after " + getExercise(userExercise.getID()));
      }
    }
    return exercise;
  }

  public void add(CommonUserExercise ue) {
    synchronized (this) {
      exercises.add(ue);
      idToExercise.put(ue.getID(), ue);
    }
  }

  @Override
  /**
   * @return true if exercise with this id was removed
   */
  public boolean remove(String id) {
    synchronized (this) {
      if (!idToExercise.containsKey(id)) return false;
      CommonExercise remove = idToExercise.remove(id);
      exercises.remove(remove);
      return true;
    }
  }

  @Override
  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
  }

  @Override
  public void setAddRemoveDAO(AddRemoveDAO addRemoveDAO) {
    this.addRemoveDAO = addRemoveDAO;
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#getPredefExercise(String)
   * @see mitll.langtest.server.database.DatabaseImpl#getExercise(String)
   */
  public CommonExercise getExercise(String id) {
    if (idToExercise.isEmpty()) {
      logger.error("huh? couldn't find any exercises..? " + id);
    }

    synchronized (this) {
      CommonExercise exercise = idToExercise.get(id);
      //if (exercise == null) logger.warn("no '" +id+"'  in " + idToExercise.keySet().size()+" keys");
      return exercise;
    }
  }

  /**
   * @param file
   * @return
   * @see #getRawExercises()
   */
  private List<CommonExercise> readExercises(File file) {
    try {
      logger.debug("Excel reading " + language + " from " + file.getAbsolutePath());
      return readExercises(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      logger.error("looking for " + file.getAbsolutePath() + " got " + e, e);
    }
    return new ArrayList<CommonExercise>();
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
    String language1 = serverProps.getLanguage();
    try {
      long then = System.currentTimeMillis();
      // logger.debug("starting to read spreadsheet for " + language1 + " on " + Thread.currentThread() + " at " + System.currentTimeMillis());

      XSSFWorkbook wb = new XSSFWorkbook(inp);
//      logger.debug("finished reading spreadsheet for " + language1 + " on " + Thread.currentThread() + " at " + System.currentTimeMillis());

      long now = System.currentTimeMillis();
      if (now - then > 1000) {
        logger.info("took " + (now - then) + " millis to open spreadsheet for " + language1 + " on " + Thread.currentThread());
      }
      then = now;

      for (int i = 0; i < wb.getNumberOfSheets(); i++) {
        Sheet sheet = wb.getSheetAt(i);
        if (sheet.getPhysicalNumberOfRows() > 0) {
          Collection<CommonExercise> exercises1 = readFromSheet(sheet);
          exercises.addAll(exercises1);
          logger.info("sheet " + sheet.getSheetName() + " had " + exercises1.size() + " items.");
          if (!exercises1.isEmpty()) {
            CommonExercise first = exercises1.iterator().next();
            logger.debug("e.g. " + first + " content  " + first.getContent());
          }
        }
      }

      if (!errors.isEmpty()) {
        logger.warn("there were " + errors.size() + " errors");
        int count = 0;
        for (String error : errors) {
          if (count++ < 10) logger.warn(error);
        }
      }
      if (DEBUG) sectionHelper.report();
      sectionHelper.allKeysValid();
      inp.close();
      now = System.currentTimeMillis();
      if (now - then > 1000) {
        logger.info("took " + (now - then) + " millis to make " + exercises.size() + " exercises for " + language1);
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
    List<CommonExercise> exercises = new ArrayList<CommonExercise>();
    // logger.debug("for " + sheet.getSheetName() + " regions " +sheet.getNumMergedRegions());
    int id = 0;
    boolean gotHeader = false;

    int colIndexOffset = -1;

    int transliterationIndex = -1;
    int weightIndex = -1;
    int meaningIndex = -1;
    int idIndex = -1;
    int contextIndex = -1;
    int contextTranslationIndex = -1;
    int audioIndex = -1;
    boolean hasAudioIndex = false;

    List<String> lastRowValues = new ArrayList<String>();
    Map<String, List<CommonExercise>> englishToExercises = new HashMap<String, List<CommonExercise>>();
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
      for (; iter.hasNext(); ) {
        Row next = iter.next();
        if (id > maxExercises) break;
        boolean inMergedRow = rowToRange.keySet().contains(next.getRowNum());
        Map<String, String> fieldToDefect = new HashMap<String, String>();
        List<String> columns = new ArrayList<String>();
        if (!gotHeader) {
          Iterator<Cell> cellIterator = next.cellIterator();
          while (cellIterator.hasNext()) {
            Cell next1 = cellIterator.next();
            columns.add(next1.toString().trim());
          }
        }

        if (!gotHeader) {
          List<String> predefinedTypeOrder = new ArrayList<String>();
          for (String col : columns) {
            String colNormalized = col.toLowerCase();
            if (colNormalized.startsWith("word")) {
              gotHeader = true;
              colIndexOffset = columns.indexOf(col);
            } else if (colNormalized.contains("transliteration")) {
              transliterationIndex = columns.indexOf(col);
//            } else if (colNormalized.contains("weight")) {
              //             weightIndex = columns.indexOf(col);
            } else if (colNormalized.contains(MEANING)) {
              meaningIndex = columns.indexOf(col);
            } else if (colNormalized.contains(ID)) {
              idIndex = columns.indexOf(col);
            } else if (contextTransMatch(colNormalized)) { //be careful of ordering wrt this and the next item
              contextTranslationIndex = columns.indexOf(col);
            } else if (colNormalized.contains(CONTEXT)) {
              contextIndex = columns.indexOf(col);
            } else if (colNormalized.contains("audio_index")) {
              audioIndex = columns.indexOf(col);
              hasAudioIndex = true;
            } else if (gotUCW) {
//              logger.debug("using predef unit/chapter/week ");
              if (columns.indexOf(col) == unitIndex) {
                predefinedTypeOrder.add(col);
                unitName = col;
              } else if (columns.indexOf(col) == chapterIndex) {
                predefinedTypeOrder.add(col);
                chapterName = col;
              } else if (columns.indexOf(col) == weekIndex) {
                predefinedTypeOrder.add(col);
                weekName = col;
              }
            } else if (colNormalized.contains("unit") || colNormalized.contains("book")) {
              unitIndex = columns.indexOf(col);
              predefinedTypeOrder.add(col);
              unitName = col;
            } else if (colNormalized.contains("chapter") || colNormalized.contains("lesson")) {
              chapterIndex = columns.indexOf(col);
              predefinedTypeOrder.add(col);
              chapterName = col;
            } else if (colNormalized.contains("week")) {
              weekIndex = columns.indexOf(col);
              predefinedTypeOrder.add(col);
              weekName = col;
            }
          }
          if (usePredefinedTypeOrder) {
            sectionHelper.setPredefinedTypeOrder(predefinedTypeOrder);
          }

          if (DEBUG) logger.debug("columns word index " + colIndexOffset +
                  " week " + weekIndex + " unit " + unitIndex + " chapter " + chapterIndex +
                  " meaning " + meaningIndex +
                  " transliterationIndex " + transliterationIndex +
                  " contextIndex " + contextIndex +
                  " id " + idIndex + " audio " + audioIndex
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
              //logger.info("\n\n\n-------- > for row " + next.getRowNum() + " english using " + english);
            }
          }
          if (english.length() == 0) {
            //if (serverProps.isClassroomMode()) {
            //english = "NO ENGLISH";    // DON'T DO THIS - it messes up the indexing.
            fieldToDefect.put("english", "missing english");
            // }
            //logger.info("-------- > for row " + next.getRowNum() + " english is blank ");
            // else {
            //   englishSkipped++;
            // }
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
              checkForSemicolons(fieldToDefect, foreignLanguagePhrase, translit);

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;

              CommonExercise imported = isDelete ? null : getExercise(idToUse, english, foreignLanguagePhrase, translit,
                  meaning, context, contextTranslation, false, hasAudioIndex ? getCell(next, audioIndex) : "", true);

              if (!isDelete &&
                  (imported.hasRefAudio() || !shouldHaveRefAudio)) {  // skip items without ref audio, for now.
                recordUnitChapterWeek(unitIndex, chapterIndex, weekIndex, next, imported, unitName, chapterName, weekName);

                if (knownIds.contains(imported.getID())) {
                  logger.warn("found duplicate entry under " + imported.getID() + " " + imported);
                } else {
                  knownIds.add(imported.getID());
                  rememberExercise(exercises, englishToExercises, imported);
                  if (!fieldToDefect.isEmpty()) {
                    idToDefectMap.put(imported.getID(), fieldToDefect);
                  }
                }
              } else {
                if (isDelete) {
                  deleted++;
                } else {
                  if (logging++ < 3) {
                    logger.info("skipping exercise " + imported.getID() + " : '" + imported.getEnglish() + "' since no audio.");
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
    Collection<CommonExercise> commonExercises = readFromSheetSkips(sheet, id);
    exercises.addAll(commonExercises);
    return exercises;
  }

  private boolean contextTransMatch(String colNormalized) {
    return colNormalized.contains(CONTEXT_TRANSLATION) || colNormalized.contains(TRANSLATION_OF_CONTEXT.toLowerCase());
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
    Map<String, List<CommonExercise>> englishToExercises = new HashMap<String, List<CommonExercise>>();
    int semis = 0;
    int skipped = 0;
    int deleted = 0;
    int englishSkipped = 0;
    String unitName = null, chapterName = null, weekName = null;
    try {
      Iterator<Row> iter = sheet.rowIterator();
      boolean gotUCW = unitIndex != -1;
      for (; iter.hasNext(); ) {
        Row next = iter.next();
        Map<String, String> fieldToDefect = new HashMap<String, String>();
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
            if (colNormalized.startsWith("word")) {
              gotHeader = true;
              colIndexOffset = columns.indexOf(col);
            } else if (colNormalized.contains("transliteration")) {
              transliterationIndex = columns.indexOf(col);
            } else if (colNormalized.contains(MEANING)) {
              meaningIndex = columns.indexOf(col);
            } else if (colNormalized.contains(ID)) {
              idIndex = columns.indexOf(col);
            } else if (colNormalized.contains(CONTEXT)) {
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
//            } else if (colNormalized.contains("weight")) {
//              weightIndex = columns.indexOf(col);
            }
          }

          if (DEBUG) logger.debug("columns word index " + colIndexOffset +
                  " week " + weekIndex + " unit " + unitIndex + " chapter " + chapterIndex +
                  " meaning " + meaningIndex +
                  " transliterationIndex " + transliterationIndex +
                  " contextIndex " + contextIndex +
                  " contextTranslationIndex " + contextTranslationIndex +
                  " id " + idIndex + " audio " + audioIndex
          );
        } else {
          int colIndex = colIndexOffset;
          boolean isDelete = isDeletedRow(sheet, next, colIndex);
          String english = getCell(next, colIndex++).trim();
          // remove starting or ending tics
          String foreignLanguagePhrase = cleanTics(getCell(next, colIndex).trim());
          String translit = getCell(next, transliterationIndex);

          //logger.info("for row " + next.getRowNum() + " english = " + english + " in merged " + inMergedRow + " last row " + lastRowValues.size());

          if (english.length() == 0) {
            /// if (serverProps.isClassroomMode()) {
            //english = "NO ENGLISH";    // DON'T DO THIS - it messes up the indexing.
            fieldToDefect.put("english", "missing english");
            // }
            //logger.info("-------- > for row " + next.getRowNum() + " english is blank ");
            // else {
            //   englishSkipped++;
            // }
          }
          if (gotHeader && english.length() > 0) {
            if (skipSemicolons && (foreignLanguagePhrase.contains(";") || translit.contains(";"))) {
              String meaning = getCell(next, meaningIndex);
              String givenIndex = getCell(next, idIndex);
              String context = getCell(next, contextIndex);
              String contextTranslation = getCell(next, contextTranslationIndex);
              checkForSemicolons(fieldToDefect, foreignLanguagePhrase, translit);

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;
              CommonExercise imported = getExercise(idToUse, english, foreignLanguagePhrase, translit,
                  meaning, context, contextTranslation, false, (audioIndex != -1) ? getCell(next, audioIndex) : "", false);
              if (isDelete) {
                deleted++;
              } else if (imported.hasRefAudio() || !shouldHaveRefAudio) {  // skip items without ref audio, for now.
                recordUnitChapterWeek(unitIndex, chapterIndex, weekIndex, next, imported, unitName, chapterName, weekName);

                rememberExercise(exercises, englishToExercises, imported);
                if (!fieldToDefect.isEmpty()) {
                  idToDefectMap.put(imported.getID(), fieldToDefect);
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    logStatistics(id, semis, skipped, englishSkipped, deleted);
    if (missingExerciseCount > 0) logger.debug("missing ex count " + missingExerciseCount);
    return exercises;
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
    logger.info("max exercise id = " + id);
    // logger.info("missing audio files = " +c);
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
    return (100f * skipped / total) + "%";
  }

  private void addDefects(Map<String, Map<String, String>> exTofieldToDefect) {
    if (addDefects) {
      int count = 0;
      for (Map.Entry<String, Map<String, String>> pair : exTofieldToDefect.entrySet()) {
        for (Map.Entry<String, String> fieldToDefect : pair.getValue().entrySet()) {
          if (userListManager.addDefect(pair.getKey(), fieldToDefect.getKey(), fieldToDefect.getValue())) {
            count++;
          }
        }
      }
      if (count > 0) {
        logger.info("Automatically added " + exTofieldToDefect.size() + "/" + count + " defects");
      }
    } else {
      logger.info("NOT Automatically adding " + exTofieldToDefect.size() + " defects");
    }
  }

  private void checkForSemicolons(Map<String, String> fieldToDefect, String foreignLanguagePhrase, String translit) {
    if (foreignLanguagePhrase.contains(";")) {
      fieldToDefect.put(QCNPFExercise.FOREIGN_LANGUAGE, "contains semicolon - should this item be split?");
    }
    if (translit.contains(";")) {
      fieldToDefect.put(QCNPFExercise.TRANSLITERATION, "contains semicolon - should this item be split?");
    }
/*    if (INCLUDE_ENGLISH_SEMI_AS_DEFECT && english.contains(";")) {
      fieldToDefect.put(QCNPFExercise.ENGLISH, "contains semicolon - should this item be split?");
    }*/
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

  private void rememberExercise(List<CommonExercise> exercises, Map<String, List<CommonExercise>> englishToExercises, CommonExercise imported) {
    String englishSentence = imported.getEnglish();
    List<CommonExercise> exercisesForSentence = englishToExercises.get(englishSentence);
    if (exercisesForSentence == null)
      englishToExercises.put(englishSentence, exercisesForSentence = new ArrayList<CommonExercise>());
    exercisesForSentence.add(imported);

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
   * @param weightIndex
   * @param id
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @param meaning
   * @param context
   * @param lookForOldAudio
   * @return
   * @see #readFromSheet(org.apache.poi.ss.usermodel.Sheet)
   */
  private CommonExercise getExercise(String id,
                                     String english, String foreignLanguagePhrase, String translit, String meaning,
                                     String context, String contextTranslation, boolean promptInEnglish,
                                     String audioIndex, boolean lookForOldAudio) {
    Exercise imported;

//    logger.debug("id " + id + " context " + context);
    imported = getExercise(id, english, foreignLanguagePhrase, translit, meaning, context, contextTranslation,
        audioIndex, lookForOldAudio);
    //   logger.debug("id " + id + " context " + imported.getContext());

    imported.setEnglishSentence(english);
    if (translit.length() > 0) {
      imported.setTranslitSentence(translit);
    }

    List<String> translations = new ArrayList<String>();
    if (foreignLanguagePhrase.length() > 0) {
      translations.add(foreignLanguagePhrase);
    }
    imported.setRefSentences(translations);
    imported.setForeignLanguage(foreignLanguagePhrase);

    return imported;
  }

  private void recordUnitChapterWeek(int unitIndex, int chapterIndex, int weekIndex,
                                     Row next,
                                     CommonExercise imported, String unitName, String chapterName, String weekName) {
    String unit = getCell(next, unitIndex);
    String chapter = getCell(next, chapterIndex);
    String week = getCell(next, weekIndex);
    List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();

    if (unit.length() == 0 && chapter.length() == 0 && week.length() == 0) {
      unit = "Blank";
    }

    // hack to trim off leading tics
    if (unit.startsWith("'")) unit = unit.substring(1);
    if (unit.equals("intro")) unit = "Intro"; // hack
    if (chapter.startsWith("'")) chapter = chapter.substring(1);
    if (week.startsWith("'")) week = week.substring(1);

/*    if (debug && false)
      logger.debug("unit(" +unitName+
        ")" + unitIndex + "/" + unit + " chapter " + chapterIndex + "/(" +chapterName+
        ")" + chapter + " week (" + weekName+ ") : " + week);*/

    if (unit.length() > 0) {
      pairs.add(sectionHelper.addExerciseToLesson(imported, unitName, unit));
    }
    if (chapter.length() > 0) {
      if (language.equalsIgnoreCase("English")) {
        chapter = (unitIndex == -1 ? "" : unit + "-") + chapter; // hack for now to get unique chapters...
      }
      pairs.add(sectionHelper.addExerciseToLesson(imported, chapterName, chapter));
    }
    if (week.length() > 0) {
      pairs.add(sectionHelper.addExerciseToLesson(imported, weekName, week));
    }
    sectionHelper.addAssociations(pairs);

  }

  /**
   * @param id
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @param meaning
   * @param context
   * @param contextTranslation
   * @param refAudioIndex
   * @param lookForOldAudio
   * @return
   * @see #getExercise(String, String, String, String, String, String, String, boolean, String, boolean)
   */
  private Exercise getExercise(String id,
                               String english, String foreignLanguagePhrase, String translit, String meaning,
                               String context, String contextTranslation, String refAudioIndex, boolean lookForOldAudio) {
    String content = ExerciseFormatter.getContent(foreignLanguagePhrase, translit, english, meaning, context,
        contextTranslation, language);
    Exercise imported = new Exercise(id, content, english, context, contextTranslation);

    //logger.debug("id  " + id+  " context " + imported.getContext());
    imported.setMeaning(meaning);

    if (lookForOldAudio) {
      addOldSchoolAudio(refAudioIndex, imported);
    }

    int i = attachAudio(id, imported);
    return imported;
  }

  /**
   * Go looking for audio in the media directory ("bestAudio") and if there's a file there
   * under a matching exercise id, attach Fast and/or slow versions to this exercise.
   * <p>
   * Can override audio file directory with a non-empty refAudioIndex.
   * <p>
   * Also uses audioOffset - audio index is an integer.
   *
   * @param refAudioIndex override place to look for audio
   * @param imported      to attach audio to
   * @see #getExercise(String, String, String, String, String, String, String, String, boolean)
   */
  private void addOldSchoolAudio(String refAudioIndex, Exercise imported) {
    String id = imported.getID();
    String audioDir = refAudioIndex.length() > 0 ? findBest(refAudioIndex) : id;
    if (audioOffset != 0) {
      audioDir = "" + (Integer.parseInt(audioDir.trim()) + audioOffset);
    }

    String parentPath = mediaDir + File.separator + audioDir + File.separator;
    String fastAudioRef = parentPath + FAST_WAV;
    String slowAudioRef = parentPath + SLOW_WAV;

    File test = new File(fastAudioRef);
    boolean exists = test.exists();
    if (!exists) {
      test = new File(installPath, fastAudioRef);
      exists = test.exists();
    }
    if (exists) {
      imported.addAudioForUser(ensureForwardSlashes(fastAudioRef), UserDAO.DEFAULT_USER);
    }

    test = new File(slowAudioRef);
    exists = test.exists();
    if (!exists) {
      test = new File(installPath, slowAudioRef);
      exists = test.exists();
    }
    if (exists) {
      imported.addAudio(new AudioAttribute(ensureForwardSlashes(slowAudioRef), UserDAO.DEFAULT_USER).markSlow());
    }
  }

  private int missingExerciseCount = 0;
  private int c = 0;

  /**
   * Make sure every audio file we attach is a valid audio file -- it's really where it says it's supposed to be.
   * <p>
   * TODOx : rationalize media path -- don't force hack on bestAudio replacement
   * Why does it sometimes have the config dir on the front?
   *
   * @param id
   * @param imported
   * @see #getExercise(String, String, String, String, String, String, String, String, boolean)
   */
  private int attachAudio(String id, Exercise imported) {
    //String mediaDir1 = mediaDir.replaceAll("bestAudio","");
    //logger.debug("media dir " + mediaDir1);
    int missing = 0;
    if (exToAudio.containsKey(id) || exToAudio.containsKey(id + "/1") || exToAudio.containsKey(id + "/2")) {
      List<AudioAttribute> audioAttributes = exToAudio.get(id);

      if (audioAttributes == null) {
        missingExerciseCount++;
        if (missingExerciseCount < 10) logger.error("attachAudio can't find " + id);
      } else if (!audioAttributes.isEmpty()) {
        Set<String> audioPaths = new HashSet<String>();
        for (AudioAttribute audioAttribute : imported.getAudioAttributes()) {
          audioPaths.add(audioAttribute.getAudioRef());
        }
        for (AudioAttribute audio : audioAttributes) {
          String child = mediaDir1 + File.separator + audio.getAudioRef();
          /*  if (child.contains("bestAudio\bestAudio")) {
              child = child.replaceAll("bestAudio\\bestAudio","bestAudio");
            }*/
          File test = new File(installPath, child);

          boolean exists = test.exists();
          if (!exists) {
            //   logger.debug("child " + test.getAbsolutePath() + " doesn't exist");
            test = new File(installPath, audio.getAudioRef());
            exists = test.exists();
            if (!exists) {
              //     logger.debug("child " + test.getAbsolutePath() + " doesn't exist");
            }
            child = audio.getAudioRef();
          }
          if (exists) {
            if (!audioPaths.contains(child)) {
              audio.setAudioRef(child);   // remember to prefix the path
              imported.addAudio(audio);
              audioPaths.add(child);
            }
          } else {
            missing++;
            c++;
            if (c < 5) {
              logger.warn("file " + test.getAbsolutePath() + " does not exist - \t" + audio.getAudioRef());
              if (c < 2) {
                logger.warn("installPath " + installPath + "mediaDir " + mediaDir + " mediaDir1 " + mediaDir1);
              }
            }
          }
        }
      }
      //logger.debug("added " + c + " to " + id);
    }
    //else {
    // logger.debug("can't find '" + id + "' in " + exToAudio.keySet().size() + " keys, e.g. '" + exToAudio.keySet().iterator().next() +"'");
    //}
    return missing;
  }

  /**
   * Assumes audio index field looks like : 11109 8723 8722 8721
   *
   * @param refAudioIndex
   * @return
   */
  private String findBest(String refAudioIndex) {
    String[] split = refAudioIndex.split("\\s+");
    return (split.length == 0) ? "" : split[0];
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
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
