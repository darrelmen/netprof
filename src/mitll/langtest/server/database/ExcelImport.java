package mitll.langtest.server.database;

import mitll.langtest.client.custom.QCNPFExercise;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseFormatter;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads an excel spreadsheet from DLI.
 * <p/>
 * User: GO22670
 * Date: 2/6/13
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExcelImport implements ExerciseDAO {
  private static final Logger logger = Logger.getLogger(ExcelImport.class);
  public static final String FAST_WAV = "Fast" + ".wav";
  public static final String SLOW_WAV = "Slow" + ".wav";
  //private static final boolean INCLUDE_ENGLISH_SEMI_AS_DEFECT = false;

  private List<CommonExercise> exercises = null;
  private final Map<String, CommonExercise> idToExercise = new HashMap<String, CommonExercise>();
  private final List<String> errors = new ArrayList<String>();
  private final String file;
  private final SectionHelper sectionHelper = new SectionHelper();
  private final boolean debug = false;
  private String mediaDir;
  private final Set<String> missingSlowSet = new HashSet<String>();
  private final Set<String> missingFastSet = new HashSet<String>();
  private boolean shouldHaveRefAudio = false;
  private boolean usePredefinedTypeOrder;
  private final String language;
  private boolean skipSemicolons;
  private int audioOffset = 0;
  private final int maxExercises;
  private ServerProperties serverProps;
  private UserListManager userListManager;
  private AddRemoveDAO addRemoveDAO;
  private File installPath;
  private boolean addDefects;
  private int unitIndex;
  private int chapterIndex;
  private int weekIndex;

  /**
   *
   * @param file
   * @param relativeConfigDir
   * @param userListManager
   * @param addDefects
   * @see DatabaseImpl#makeDAO
   */
  public ExcelImport(String file, String mediaDir, String relativeConfigDir, ServerProperties serverProps,
                     UserListManager userListManager,
                     String installPath, boolean addDefects) {
    this.file = file;
    this.serverProps = serverProps;
    maxExercises = serverProps.getMaxNumExercises();
    this.mediaDir = mediaDir;
    this.addDefects = addDefects;
    this.installPath = new File(installPath);
    if (!this.installPath.exists()) {
      logger.warn("\n\n\nhuh? install path " + this.installPath.getAbsolutePath() + " doesn't exist???");
    }
    // turn off missing fast/slow for classroom
    boolean missingExists = serverProps.isClassroomMode() || getMissing(relativeConfigDir, "missingSlow.txt", missingSlowSet);
    missingExists &= serverProps.isClassroomMode() || getMissing(relativeConfigDir, "missingFast.txt", missingFastSet);
    shouldHaveRefAudio = missingExists && !serverProps.isClassroomMode();
    this.usePredefinedTypeOrder = serverProps.usePredefinedTypeOrder();
    this.language = serverProps.getLanguage();
    this.skipSemicolons = serverProps.shouldSkipSemicolonEntries();
    this.audioOffset = serverProps.getAudioOffset();
    this.userListManager = userListManager;
    this.unitIndex = serverProps.getUnitChapterWeek()[0];
    this.chapterIndex = serverProps.getUnitChapterWeek()[1];
    this.weekIndex = serverProps.getUnitChapterWeek()[2];
  }

  public boolean getMissing(String relativeConfigDir, String file, Set<String> missing) {
    File missingSlow = new File(relativeConfigDir, file);
    if (missingSlow.exists()) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(missingSlow));
        String line;
        while ((line = reader.readLine()) != null) {
          String trim = line.trim();
          if (trim.length() > 0) {
            missing.add(trim);
          }
        }
        reader.close();

        logger.debug("Read from " + missingSlow.getAbsolutePath() + " and found " + missing.size());

      } catch (Exception e) {
        logger.error("Reading " + missingSlow.getAbsolutePath() + " Got  " + e, e);
      }

    } else {
      if (serverProps.isGoodwaveMode()) {
        logger.debug("Can't find " + file + " under " + relativeConfigDir + " abs path " + missingSlow.getAbsolutePath());
      }
    }
    return missingSlow.exists();
  }

  @Override
  public SectionHelper getSectionHelper() {
    return sectionHelper;
  }

  private Map<String, List<AudioAttribute>> exToAudio;
  @Override
  public void setAudioDAO(AudioDAO audioDAO) {  exToAudio = audioDAO.getExToAudio();  }

  /**
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getExercises()
   */
  public List<CommonExercise> getRawExercises() {
    synchronized (this) {
      if (exercises == null) {
        exercises = readExercises(new File(file));
        for (CommonExercise e : exercises) idToExercise.put(e.getID(), e);
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

        Collection<CommonUserExercise> overrides = userExerciseDAO.getOverrides(false);

        for (CommonUserExercise userExercise : overrides) {
          if (!removes.contains(userExercise.getID())) {
            CommonExercise exercise = getExercise(userExercise.getID());
            if (exercise != null) {
              //logger.debug("refresh exercise for " + userExercise.getID());
              sectionHelper.removeExercise(exercise);
              sectionHelper.addExercise(userExercise);
              addOverlay(userExercise);
            }
            //else {
              //logger.debug("not adding as overlay " + userExercise.getID());
            //}
          }
        }

        // add new items
        for (String id : addRemoveDAO.getAdds()) {
          CommonUserExercise where = userExerciseDAO.getWhere(id);
          if (where == null) {
            logger.error("huh? couldn't find user exercise dup " + id);
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
   * @see mitll.langtest.server.database.DatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see #getRawExercises()
   * @param userExercise
   * @return old exercises
   */
  @Override
  public CommonExercise addOverlay(CommonUserExercise userExercise) {
    CommonExercise exercise = getExercise(userExercise.getID());

    if (exercise == null) {
      logger.error("addOverlay : huh? can't find " + userExercise);
    }
    else {
      //logger.debug("addOverlay at " +userExercise.getID() + " found " +exercise);
      synchronized (this) {
        int i = exercises.indexOf(exercise);
        if (i == -1) {
          logger.error ("addOverlay : huh? couldn't find " + exercise);
        }
        else {
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

  private UserExerciseDAO userExerciseDAO;

  @Override
  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) { this.userExerciseDAO = userExerciseDAO; }
  @Override
  public void setAddRemoveDAO(AddRemoveDAO addRemoveDAO) { this.addRemoveDAO = addRemoveDAO; }

  /**
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#getPredefExercise(String)
   * @see mitll.langtest.server.database.DatabaseImpl#getExercise(String)
   * @param id
   * @return
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
     // logger.debug("reading from " + file.getAbsolutePath());
      return readExercises(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      logger.error("looking for " + file.getAbsolutePath() + " got " + e, e);
    }
    return new ArrayList<CommonExercise>();
  }

  /**
   * @param inp
   * @return
   * @seex mitll.langtest.server.SiteDeployer#readExercises(mitll.langtest.shared.Site, org.apache.commons.fileupload.FileItem)
   */
  private List<CommonExercise> readExercises(InputStream inp) {
    List<CommonExercise> exercises = new ArrayList<CommonExercise>();
    try {
      long then = System.currentTimeMillis();
      XSSFWorkbook wb = new XSSFWorkbook(inp);
      long now = System.currentTimeMillis();
      if (now-then > 1000) {
        logger.info("took " + (now - then) + " millis to read spreadsheet");
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
      if (debug) sectionHelper.report();
      inp.close();
      now = System.currentTimeMillis();
      if (now-then > 1000) {
        logger.info("took " + (now - then) + " millis to make " + exercises.size() + " exercises...");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return exercises;
  }

  public List<CommonExercise> getExercises() { return exercises; }

  private Map<String, Map<String, String>> idToDefectMap = new HashMap<String, Map<String, String>>();

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
    int audioIndex = -1;
    boolean hasAudioIndex = false;

    List<String> lastRowValues = new ArrayList<String>();
    Map<String, List<CommonExercise>> englishToExercises = new HashMap<String, List<CommonExercise>>();
    int semis = 0;
    int logging = 0;
    int skipped = 0;
    int englishSkipped = 0;
    String unitName = null, chapterName = null, weekName = null;
    try {
      Iterator<Row> iter = sheet.rowIterator();
      Map<Integer, CellRangeAddress> rowToRange = getRowToRange(sheet);
      boolean gotUCW = unitIndex != -1;
      for (; iter.hasNext(); ) {
        Row next = iter.next();
        if (id > maxExercises) break;
        //    logger.warn("------------ Row # " + next.getRowNum() + " --------------- ");
        boolean inMergedRow = rowToRange.keySet().contains(next.getRowNum());
        Map<String,String> fieldToDefect = new HashMap<String, String>();
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
            //  colNormalized = colNormalized.toLowerCase()
            if (colNormalized.startsWith("Word".toLowerCase())) {
              gotHeader = true;
              colIndexOffset = columns.indexOf(col);
            } else if (colNormalized.contains("transliteration")) {
              transliterationIndex = columns.indexOf(col);
            } else if (gotUCW) {
              if(columns.indexOf(col) == unitIndex){
                  predefinedTypeOrder.add(col);
                  unitName = col;
              } else if(columns.indexOf(col) == chapterIndex){
                  predefinedTypeOrder.add(col);
                  chapterName = col;
              } else if(columns.indexOf(col) == weekIndex){
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
            } else if (colNormalized.contains("weight")) {
              weightIndex = columns.indexOf(col);
            } else if (colNormalized.contains("meaning")) {
              meaningIndex = columns.indexOf(col);
            } else if (colNormalized.contains("id")) {
              idIndex = columns.indexOf(col);
            } else if (colNormalized.contains("context")) {
              contextIndex = columns.indexOf(col);
            } else if (colNormalized.contains("audio_index")) {
              audioIndex = columns.indexOf(col);
              hasAudioIndex = true;
            }
          }
          if (usePredefinedTypeOrder) {
            sectionHelper.setPredefinedTypeOrder(predefinedTypeOrder);
          }

          if (debug) logger.debug("columns word index " + colIndexOffset +
            " week " + weekIndex + " unit " + unitIndex + " chapter " + chapterIndex +
            " meaning " + meaningIndex +
            " transliterationIndex " + transliterationIndex +
            " contextIndex " + contextIndex +
            " id " + idIndex + " audio " + audioIndex
          );
        } else {
          int colIndex = colIndexOffset;
          String english = getCell(next, colIndex++).trim();
          String foreignLanguagePhrase = getCell(next, colIndex).trim();
          String translit = getCell(next, transliterationIndex);

          // remove starting or ending tics
          foreignLanguagePhrase = cleanTics(foreignLanguagePhrase);

          //logger.info("for row " + next.getRowNum() + " english = " + english + " in merged " + inMergedRow + " last row " + lastRowValues.size());

          if (inMergedRow && !lastRowValues.isEmpty()) {
            if (english.length() == 0) {
              english = lastRowValues.get(0);
              //logger.info("\n\n\n-------- > for row " + next.getRowNum() + " english using " + english);
            }
          }
          if (english.length() == 0) {
            if (serverProps.isClassroomMode()) {
              //english = "NO ENGLISH";    // DON'T DO THIS - it messes up the indexing.
              fieldToDefect.put("english", "missing english");
            }
            //logger.info("-------- > for row " + next.getRowNum() + " english is blank ");
            else {
              englishSkipped++;
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
              checkForSemicolons(fieldToDefect, english, foreignLanguagePhrase, translit);

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;
              CommonExercise imported = getExercise(idToUse, weightIndex, next, english, foreignLanguagePhrase, translit,
                meaning, context, false, hasAudioIndex ? getCell(next, audioIndex) : "", true);
              if (imported.hasRefAudio() || !shouldHaveRefAudio) {  // skip items without ref audio, for now.
                recordUnitChapterWeek(unitIndex, chapterIndex, weekIndex, next, imported, unitName, chapterName, weekName);

                // keep track of synonyms (or better term)
                rememberExercise(exercises, englishToExercises, imported);
                if (!fieldToDefect.isEmpty()) {
                  idToDefectMap.put(imported.getID(), fieldToDefect);
                }
              } else {
                if (logging++ < 3) {
                  logger.info("skipping exercise " + imported.getID() + " : '" + imported.getEnglish() + "' since no audio.");
                }
                skipped++;
              }
              if (inMergedRow) {
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

    logger.info("max exercise id = " + id);
    if (skipped > 0) {
      logger.info("Skipped " + skipped + " entries with missing audio. " + (100f * ((float) skipped) / (float) id) + "%");
    }
    if (englishSkipped > 0) {
      logger.info("Skipped " + englishSkipped + " entries with missing english. " + (100f * ((float) englishSkipped) / (float) id) + "%");
    }
    if (semis > 0) {
      logger.info("Skipped " + semis + " entries with semicolons or " + (100f * ((float) semis) / (float) id) + "%");
    }

    // put the skips at the end
    if (serverProps.isClassroomMode()) {
      Collection<CommonExercise> commonExercises = readFromSheetSkips(sheet, id);
      exercises.addAll(commonExercises);
    }
    return exercises;
  }

  private Collection<CommonExercise> readFromSheetSkips(Sheet sheet, int id) {
    List<CommonExercise> exercises = new ArrayList<CommonExercise>();
    boolean gotHeader = false;

    int colIndexOffset = -1;

    int transliterationIndex = -1;
    int weightIndex = -1;
    int meaningIndex = -1;
    int idIndex = -1;
    int contextIndex = -1;
    int audioIndex = -1;
    Map<String, List<CommonExercise>> englishToExercises = new HashMap<String, List<CommonExercise>>();
    int semis = 0;
    int skipped = 0;
    int englishSkipped = 0;
    String unitName = null, chapterName = null, weekName = null;
    try {
      Iterator<Row> iter = sheet.rowIterator();
      boolean gotUCW = unitIndex != -1;
      for (; iter.hasNext(); ) {
        Row next = iter.next();
        Map<String,String> fieldToDefect = new HashMap<String, String>();
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
            //  colNormalized = colNormalized.toLowerCase()
            if (colNormalized.startsWith("Word".toLowerCase())) {
              gotHeader = true;
              colIndexOffset = columns.indexOf(col);
            } else if (colNormalized.contains("transliteration")) {
              transliterationIndex = columns.indexOf(col);
            } else if (gotUCW) {
              if(columns.indexOf(col) == unitIndex){
                  predefinedTypeOrder.add(col);
                  unitName = col;
              } else if(columns.indexOf(col) == chapterIndex){
                  predefinedTypeOrder.add(col);
                  chapterName = col;
              } else if(columns.indexOf(col) == weekIndex){
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
            } else if (colNormalized.contains("weight")) {
              weightIndex = columns.indexOf(col);
            } else if (colNormalized.contains("meaning")) {
              meaningIndex = columns.indexOf(col);
            } else if (colNormalized.contains("id")) {
              idIndex = columns.indexOf(col);
            } else if (colNormalized.contains("context")) {
              contextIndex = columns.indexOf(col);
            } else if (colNormalized.contains("audio_index")) {
              audioIndex = columns.indexOf(col);
            }
          }

          if (debug) logger.debug("columns word index " + colIndexOffset +
              " week " + weekIndex + " unit " + unitIndex + " chapter " + chapterIndex +
              " meaning " + meaningIndex +
              " transliterationIndex " + transliterationIndex +
              " contextIndex " + contextIndex +
              " id " + idIndex + " audio " + audioIndex
          );
        } else {
          int colIndex = colIndexOffset;
          String english = getCell(next, colIndex++).trim();
          String foreignLanguagePhrase = getCell(next, colIndex).trim();
          String translit = getCell(next, transliterationIndex);

          // remove starting or ending tics
          foreignLanguagePhrase = cleanTics(foreignLanguagePhrase);

          //logger.info("for row " + next.getRowNum() + " english = " + english + " in merged " + inMergedRow + " last row " + lastRowValues.size());


          if (english.length() == 0) {
            if (serverProps.isClassroomMode()) {
              //english = "NO ENGLISH";    // DON'T DO THIS - it messes up the indexing.
              fieldToDefect.put("english", "missing english");
            }
            //logger.info("-------- > for row " + next.getRowNum() + " english is blank ");
            else {
              englishSkipped++;
            }
          }
          if (gotHeader && english.length() > 0) {
             if (skipSemicolons && (foreignLanguagePhrase.contains(";") || translit.contains(";"))) {
              String meaning = getCell(next, meaningIndex);
              String givenIndex = getCell(next, idIndex);
              String context = getCell(next, contextIndex);
              checkForSemicolons(fieldToDefect, english, foreignLanguagePhrase, translit);

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;
              CommonExercise imported = getExercise(idToUse, weightIndex, next, english, foreignLanguagePhrase, translit,
                meaning, context, false, (audioIndex != -1) ? getCell(next, audioIndex) : "", false);
              if (imported.hasRefAudio() || !shouldHaveRefAudio) {  // skip items without ref audio, for now.
                recordUnitChapterWeek(unitIndex, chapterIndex, weekIndex, next, imported, unitName, chapterName, weekName);

                // keep track of synonyms (or better term)
                rememberExercise(exercises, englishToExercises, imported);
/*                if (MARK_MISSING_AUDIO_AS_DEFECT && !imported.hasRefAudio()) {
                  fieldToDefect.put("refAudio", "missing reference audio");
                }*/
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

    logger.info("max exercise id = " + id);
    if (skipped > 0) {
      logger.info("Skipped " + skipped + " entries with missing audio. " + (100f * ((float) skipped) / (float) id) + "%");
    }
    if (englishSkipped > 0) {
      logger.info("Skipped " + englishSkipped + " entries with missing english. " + (100f * ((float) englishSkipped) / (float) id) + "%");
    }
    if (semis > 0) {
      logger.info("Skipped " + semis + " entries with semicolons or " + (100f * ((float) semis) / (float) id) + "%");
    }
    if (missingExerciseCount > 0) logger.debug("missing ex count " + missingExerciseCount);
    return exercises;
  }

  private void addDefects(Map<String,Map<String, String>> exTofieldToDefect) {
    if (addDefects) {
      int count = 0;
      for (Map.Entry<String, Map<String, String>> pair : exTofieldToDefect.entrySet()) {
        for (Map.Entry<String, String> fieldToDefect: pair.getValue().entrySet()) {
          if (userListManager.addDefect(pair.getKey(), fieldToDefect.getKey(), fieldToDefect.getValue())) {
            count++;
          }
        }
      }
      if (count > 0) {
        logger.info("Automatically added " + exTofieldToDefect.size() + "/" + count + " defects");
      }
    }
    else {
      logger.info("NOT Automatically adding " + exTofieldToDefect.size() + " defects");
    }
  }

  private void checkForSemicolons(Map<String, String> fieldToDefect, String english, String foreignLanguagePhrase, String translit) {
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

  /**
   * @see #readFromSheet(org.apache.poi.ss.usermodel.Sheet)
   * @paramx englishToExercises
   */
/*  private void addSynonyms(Map<String, List<CommonExercise>> englishToExercises) {
    for (List<CommonExercise> exercises2 : englishToExercises.values()) {
      if (exercises2.size() > 1) {
        Set<String> translationSet = new HashSet<String>();

        List<String> translations = new ArrayList<String>();
        List<String> transliterations = new ArrayList<String>();
        List<String> audioRefs = new ArrayList<String>();
        for (CommonExercise e : exercises2) {
          for (int i = 0; i < e.getRefSentences().size(); i++) {
            try {
              String ref = e.getRefSentences().get(i);
              String transLower = ref.toLowerCase().trim();

              List<String> translitSentences = e.getTranslitSentences();
              if (translitSentences.size() > i) {
                String translit = translitSentences.get(i);
                if (!translationSet.contains(transLower)) {
                  translations.add(ref);
                  transliterations.add(translit);
                  translationSet.add(transLower);
                  audioRefs.add(e.getRefAudio());
                }
              }
              else {
                //logger.warn("no translit sentence at " + i + " for " + e);
              }
            } catch (Exception e1) {
              logger.error("got " + e1 + " on " + e, e1);
            }
          }
        }

        if (translations.size() > 1) {
          for (CommonExercise e : exercises2) {
            e.setSynonymSentences(translations);
            e.setSynonymTransliterations(transliterations);
            e.setSynonymAudioRefs(audioRefs);
            //logger.debug("e " + e.getID() + " '" + e.getEnglish() + "' has " + e.getSynonymAudioRefs().size() + " audio refs or " + translations);
          }
        }
      }
    }
  }*/

  private String cleanTics(String foreignLanguagePhrase) {
    if (foreignLanguagePhrase.startsWith("\'")) {
      foreignLanguagePhrase = foreignLanguagePhrase.substring(1);
    }
    if (foreignLanguagePhrase.endsWith("\'"))
      foreignLanguagePhrase = foreignLanguagePhrase.substring(0, foreignLanguagePhrase.length() - 1);
    return foreignLanguagePhrase;
  }

/*  private void checkLTS(int id, BufferedWriter writer, SmallVocabDecoder svd, ModernStandardArabicLTS lts, String english, String foreignLanguagePhrase) {
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
  }*/

  /**
   * @param id
   * @param weightIndex
   * @param next
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @param meaning
   * @param context
   * @param lookForOldAudio
   * @return
   * @see #readFromSheet(org.apache.poi.ss.usermodel.Sheet)
   */
  private CommonExercise getExercise(String id, int weightIndex, Row next,
                                     String english, String foreignLanguagePhrase, String translit, String meaning,
                                     String context, boolean promptInEnglish, String audioIndex, boolean lookForOldAudio) {
    Exercise imported;
    List<String> translations = new ArrayList<String>();
    if (foreignLanguagePhrase.length() > 0) {
      translations.add(foreignLanguagePhrase);
    }
    imported = getExercise(id, english, foreignLanguagePhrase, translit, meaning, context, promptInEnglish, audioIndex, lookForOldAudio);

    imported.setEnglishSentence(english);
    if (translit.length() > 0) {
      imported.setTranslitSentence(translit);
    }
    List<String> inOrderTranslations = new ArrayList<String>(translations);
    imported.setRefSentences(inOrderTranslations);

    return imported;
  }

  private boolean recordUnitChapterWeek(int unitIndex, int chapterIndex, int weekIndex,
                                        Row next,
                                        CommonExercise imported, String unitName, String chapterName, String weekName) {
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

    return false;
  }

  /**
   * @param id
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @param meaning
   * @param context
   * @param refAudioIndex
   * @param lookForOldAudio
   * @return
   * @see #getExercise(String, int, org.apache.poi.ss.usermodel.Row, String, String, String, String, String, boolean, String, boolean)
   */
  private Exercise getExercise(String id,
                               String english, String foreignLanguagePhrase, String translit, String meaning,
                               String context, boolean promptInEnglish, String refAudioIndex, boolean lookForOldAudio) {
    String content = ExerciseFormatter.getContent(foreignLanguagePhrase, translit, english, meaning, context, language);
    Exercise imported = new Exercise("import", id, content, promptInEnglish, true, english, context);
    imported.setMeaning(meaning);
    imported.addQuestion();   // TODO : needed?
    imported.setType(Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW);

    if (lookForOldAudio) {
      addOldSchoolAudio(id, refAudioIndex, imported);
    }

    attachAudio(id, imported);

    return imported;
  }

  /**
   * Can override audio file directory with a non-empty refAudioIndex.
   *
   * @param id
   * @param refAudioIndex
   * @param imported
   */
  private void addOldSchoolAudio(String id, String refAudioIndex, Exercise imported) {
    String audioDir = refAudioIndex.length() > 0 ? findBest(refAudioIndex) : id;
    if (audioOffset != 0) {
      audioDir = "" + (Integer.parseInt(audioDir.trim()) + audioOffset);
    }

    String parentPath = mediaDir + File.separator + audioDir + File.separator;
    String fastAudioRef = parentPath + FAST_WAV;
    String slowAudioRef = parentPath + SLOW_WAV;

    if (!missingFastSet.contains(audioDir)) {
      File test = new File(fastAudioRef);
      boolean exists = test.exists();
      if (!exists) {
        test = new File(installPath, fastAudioRef);
        exists = test.exists();
      }
      if (exists) {
        imported.addAudioForUser(ensureForwardSlashes(fastAudioRef), UserDAO.DEFAULT_USER);
      } //else {
      //logger.debug("missing fast " + test.getAbsolutePath());
      //}
    }
    if (!missingSlowSet.contains(audioDir)) {
      File test = new File(slowAudioRef);
      boolean exists = test.exists();
      if (!exists) {
        test = new File(installPath, slowAudioRef);
        exists = test.exists();
      }
      if (exists) {
        imported.addAudio(new AudioAttribute(ensureForwardSlashes(slowAudioRef), UserDAO.DEFAULT_USER).markSlow());
      }
      // else {
      //logger.debug("missing slow " + test.getAbsolutePath());
      // }
    }
  }

  private int missingExerciseCount = 0;
  int c = 0;

  /**
   * TODO : rationalize media path -- don't force hack on bestAudio replacement
   * Why does it sometimes have the config dir on the front?
   * @param id
   * @param imported
   * @see #getExercise(String, String, String, String, String, String, boolean, String, boolean)
   */
  private void attachAudio(String id, Exercise imported) {
    String mediaDir1 = mediaDir.replaceAll("bestAudio","");
    //logger.debug("media dir " + mediaDir1);
    if (exToAudio.containsKey(id) || exToAudio.containsKey(id + "/1") || exToAudio.containsKey(id + "/2")) {
      List<AudioAttribute> audioAttributes = exToAudio.get(id);

      if (audioAttributes == null) {
        missingExerciseCount++;
        if (missingExerciseCount < 10) logger.error("attachAudio can't find " + id);
      } else if (!audioAttributes.isEmpty()) {
        for (AudioAttribute audio : audioAttributes) {
          String child = mediaDir1 + File.separator + audio.getAudioRef();
          /*  if (child.contains("bestAudio\bestAudio")) {
              child = child.replaceAll("bestAudio\\bestAudio","bestAudio");
            }*/
          File test = new File(installPath, child);

          boolean exists = test.exists();
          if (!exists) {
            test = new File(installPath, audio.getAudioRef());
            exists = test.exists();
            child = audio.getAudioRef();
          }
          if (exists) {
            audio.setAudioRef(child);   // remember to prefix the path
            imported.addAudio(audio);
          } else {
            c++;
            if (c < 15) {
              logger.warn("file " + test.getAbsolutePath() + " does not exist - " + audio.getAudioRef());
              logger.warn("installPath " + installPath + "mediaDir " + mediaDir +" mediaDir1 " + mediaDir1);
            }
          }
        }
      }
      //logger.debug("added " + c + " to " + id);
    } else {
     // logger.debug("can't find '" + id + "' in " + exToAudio.keySet().size() + " keys, e.g. " + exToAudio.keySet().iterator().next());
    }
  }

  /**
   * Assumes audio index field looks like : 11109 8723 8722 8721
   *
   * @param refAudioIndex
   * @return
   */
  private String findBest(String refAudioIndex) {
    String best = "";
    for (String recording : refAudioIndex.split("\\s+")) {
      if (!missingFastSet.contains(recording) && !missingSlowSet.contains(recording)) {
        best = recording;
        break;
      } else {
        best = recording;
      }
    }
    return best;
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
      if((new Double(numericCellValue).intValue()) < numericCellValue)
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
