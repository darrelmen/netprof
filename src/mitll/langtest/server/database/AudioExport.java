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

package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.TrackInfo;
import mitll.langtest.server.database.excel.ExcelExport;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.scoring.LTSFactory;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Take a selection and write out a zip with a spreadsheet and the audio as mp3 files.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class AudioExport {
  private static final Logger logger = Logger.getLogger(AudioExport.class);

  private static final String MALE = "Male";
  private static final String FEMALE = "Female";

  private Collection<String> typeOrder;
  private final ServerProperties props;

  AudioExport(ServerProperties props) {
    this.props = props;
  }

  /**
   * @param out
   * @param typeToSection
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param language1
   * @param audioDAO
   * @param installPath
   * @param relPath
   * @param isDefectList
   * @throws Exception
   * @see DatabaseImpl#writeUserListAudio
   */
  void writeZip(OutputStream out,
                Map<String, Collection<String>> typeToSection,
                SectionHelper<?> sectionHelper,
                Collection<CommonExercise> exercisesForSelectionState,
                String language1,
                AudioDAO audioDAO,
                String installPath,
                String relPath,
                boolean isDefectList,
                AudioExportOptions options) throws Exception {
    List<CommonExercise> copy = getSortedExercises(sectionHelper, exercisesForSelectionState);
    boolean skipAudio = typeToSection.isEmpty() && !options.isAllContext();
    logger.info("skip audio = " + skipAudio);
    writeToStream(copy, audioDAO, installPath, relPath, getPrefix(typeToSection, typeOrder), typeOrder, language1, out,
        skipAudio, isDefectList, options);
  }

  /**
   * @param out
   * @param prefix
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param language1
   * @param audioDAO
   * @param installPath
   * @param relPath
   * @param isDefectList
   * @throws Exception
   * @see mitll.langtest.server.database.DatabaseImpl#writeUserListAudio
   */
  void writeUserListAudio(OutputStream out,
                          String prefix,
                          SectionHelper<?> sectionHelper,
                          Collection<? extends CommonExercise> exercisesForSelectionState,
                          String language1,
                          AudioDAO audioDAO,
                          String installPath,
                          String relPath,
                          boolean isDefectList,
                          AudioExportOptions options) throws Exception {
    List<CommonExercise> copy = getSortableExercises(sectionHelper, exercisesForSelectionState);
    new ExerciseSorter(typeOrder).sortByTooltip(copy);
    writeToStream(copy, audioDAO, installPath, relPath, prefix, typeOrder, language1, out, false, isDefectList, options);
  }

  private List<CommonExercise> getSortedExercises(SectionHelper<?> sectionHelper, Collection<CommonExercise> exercisesForSelectionState) {
    List<CommonExercise> copy = getSortableExercises(sectionHelper, exercisesForSelectionState);
    new ExerciseSorter(typeOrder).getSortedByUnitThenAlpha(copy, false);
    return copy;
  }

  private List<CommonExercise> getSortableExercises(SectionHelper<?> sectionHelper, Collection<? extends CommonExercise> exercisesForSelectionState) {
    this.typeOrder = sectionHelper.getTypeOrder();
    return new ArrayList<>(exercisesForSelectionState);
  }

  /**
   * @param out
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param installPath
   * @throws Exception
   * @see mitll.langtest.server.database.DatabaseImpl#writeUserListAudio(java.io.OutputStream)
   */
  public void writeZipJustOneAudio(OutputStream out,
                                   SectionHelper<?> sectionHelper,
                                   Collection<? extends CommonExercise> exercisesForSelectionState,
                                   String installPath) throws Exception {
    List<CommonExercise> copy = new ArrayList<>(exercisesForSelectionState);
    new ExerciseSorter(sectionHelper.getTypeOrder()).sortByTooltip(copy);
    writeToStreamJustOneAudio(copy, installPath, out);
  }

  /**
   * @param sectionHelper
   * @param typeToSection
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getPrefix(java.util.Map)
   */
  String getPrefix(SectionHelper<?> sectionHelper, Map<String, Collection<String>> typeToSection) {
    return getPrefix(typeToSection, sectionHelper.getTypeOrder());
  }

  /**
   * @param typeToSection
   * @param typeOrder
   * @return
   * @see #getPrefix(SectionHelper, Map)
   * @see #writeZip(OutputStream, Map, SectionHelper, Collection, String, AudioDAO, String, String, boolean, AudioExportOptions)
   */
  private String getPrefix(Map<String, Collection<String>> typeToSection, Collection<String> typeOrder) {
    String prefix = "";
    for (String type : typeOrder) {
      Collection<String> selections = typeToSection.get(type);
      if (selections != null) {
        prefix += type + "_";
        for (String sel : selections) {
          prefix += sel + ",";
        }
        if (!selections.isEmpty()) prefix = prefix.substring(0, prefix.length() - 1);
        prefix += "_";
      }
    }
    if (prefix.isEmpty()) {
      prefix = "All";
    } else {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    return prefix;
  }

  /**
   * @param toWrite
   * @param audioDAO
   * @param installPath
   * @param relativeConfigDir1
   * @param name
   * @param typeOrder
   * @param language1
   * @param out
   * @param skipAudio
   * @param isDefectList
   * @throws Exception
   * @see #writeUserListAudio
   */
  private void writeToStream(Collection<CommonExercise> toWrite, AudioDAO audioDAO, String installPath,
                             String relativeConfigDir1, String name, Collection<String> typeOrder,
                             String language1, OutputStream out, boolean skipAudio, boolean isDefectList,
                             AudioExportOptions options) throws Exception {
    ZipOutputStream zOut = new ZipOutputStream(out);

    String baseName = baseName(name, language1).replaceAll("\\,", "_");
    String overallName = baseName + options.getInfo();
    //overallName = overallName.replaceAll("\\,", "_");

    logger.info("writeToStream overall name " + overallName);
    if (!skipAudio) {
      writeFolderContents(zOut, toWrite, audioDAO, installPath, relativeConfigDir1,
          overallName,
          isEnglish(language1), getCountryCode(language1), options);
    } else {
      logger.info("skip audio export.");
    }

    addSpreadsheetToZip(toWrite, typeOrder, language1, zOut, baseName, isDefectList);
  }

  private String baseName(String name, String language1) {
    return language1 + "_" + name;
  }

  public static class AudioExportOptions {
    private boolean justMale = false;
    private boolean justRegularSpeed = true;
    private boolean justContext = false;
    private boolean allContext = false;
    private boolean isUserList = false;
    boolean skip = false;

    public AudioExportOptions() {
    }

/*
    public AudioExportOptions(boolean justMale, boolean justRegularSpeed, boolean justContext, boolean isUserList) {
      this.justContext = justContext;
      this.justRegularSpeed = justRegularSpeed;
      this.justMale = justMale;
      this.isUserList = isUserList;
    }
*/

/*
    public boolean isJustMale() {
      return justMale;
    }
*/

    public void setJustMale(boolean justMale) {
      this.justMale = justMale;
    }

/*
    public boolean isJustRegularSpeed() {
      return justRegularSpeed;
    }
*/

    public void setJustRegularSpeed(boolean justRegularSpeed) {
      this.justRegularSpeed = justRegularSpeed;
    }

    public boolean isJustContext() {
      return justContext;
    }

    public void setSkip(boolean skip) {
      this.skip = skip;
    }

    public void setJustContext(boolean justContext) {
      this.justContext = justContext;
    }

    public void setAllContext(boolean justContext) {
      this.allContext = justContext;
    }

    boolean isUserList() {
      return isUserList;
    }

    public void setUserList(boolean userList) {
      this.isUserList = userList;
    }

    public String toString() {
      return "options " +
          getInfo() + " " +
          (isUserList ? "user list" : "predef");
    }

    public String getInfo() {
      if (isAllContext()) {
        return "";
      }
      else {
        return skip || isUserList ?
            "" :
            "_" + (justMale ? "male" : "female") + "_" +
                (justRegularSpeed ? "regular" : "slow") + "_" +
                (justContext ? "context" : "vocab");
      }
    }

    public boolean isAllContext() {
      return allContext;
    }
  }

  private String getCountryCode(String language1) {
    return LTSFactory.getID(LTSFactory.Language.valueOf(language1.toUpperCase()));
  }

  private void addSpreadsheetToZip(Collection<CommonExercise> toWrite, Collection<String> typeOrder,
                                   String language1, ZipOutputStream zOut, String overallName,
                                   boolean isDefectList) throws IOException {
    zOut.putNextEntry(new ZipEntry(overallName + File.separator + overallName + ".xlsx"));
    new ExcelExport(props).writeExcelToStream(toWrite, zOut, typeOrder, language1, isDefectList);
  }

  private void writeToStreamJustOneAudio(Collection<CommonExercise> toWrite, String installPath,
                                         OutputStream out) throws Exception {
    writeFolderContentsSimple(new ZipOutputStream(out), toWrite, installPath);
  }

  private boolean isEnglish(String language1) {
    return language1.equalsIgnoreCase("English");
  }

  /**
   * @param zOut
   * @param toWrite
   * @param audioDAO
   * @param installPath
   * @param relativeConfigDir1
   * @param overallName
   * @param isEnglish
   * @param options
   * @throws Exception
   * @see #writeToStream
   */
  private void writeFolderContents(ZipOutputStream zOut,
                                   Collection<CommonExercise> toWrite,
                                   AudioDAO audioDAO,
                                   String installPath,
                                   String relativeConfigDir1,
                                   String overallName,
                                   boolean isEnglish,
                                   String countryCode,
                                   AudioExportOptions options) throws Exception {
    //int c = 0;
    long then = System.currentTimeMillis();

    logger.info("overall name " + overallName);
    if (options.isAllContext()) {
      overallName += "_allContextAudio";
    }
    //logger.debug("found audio for " + exToAudio.size() + " items and writing " + toWrite.size() + " items ");
    // logger.debug("realContextPath " + realContextPath + " installPath " + installPath + " relativeConfigDir1 " +relativeConfigDir1);

    // get male and female majority users - map of user->count of recordings for this exercise
    Map<MiniUser, Integer> maleToCount = new HashMap<>();
    Map<MiniUser, Integer> femaleToCount = new HashMap<>();

    // attach audio
    int numAttach = attachAudio(toWrite, audioDAO, installPath, relativeConfigDir1);

    boolean justContext = options.justContext || options.isAllContext();
    if (!justContext) {
      populateGenderToCount(toWrite, maleToCount, femaleToCount);
    }
    // find the male and female with most recordings for this exercise
    MiniUser male = justContext ? null : getMaxUser(maleToCount);
    MiniUser female = justContext ? null : getMaxUser(femaleToCount);

    AudioConversion audioConversion = new AudioConversion(props);

    int numMissing = 0;
    Set<String> names = new HashSet<>();

    for (CommonExercise ex : toWrite) {
      boolean someAudio = false;

      // write male/female fast/slow
      if (justContext) {
        someAudio = someAudio || copyContextAudioBothGenders(zOut, installPath, overallName, isEnglish, countryCode,
            audioConversion, names, ex, options.justMale);
        if (options.isAllContext()) {
          someAudio = someAudio || copyContextAudioBothGenders(zOut, installPath, overallName, isEnglish, countryCode,
              audioConversion, names, ex, !options.justMale);
        }
        // if (someAudio) logger.info("found context for " + ex.getID());

      } else {
        MiniUser majorityUser = options.justMale ? male : female;
        String speed = options.justRegularSpeed ? AudioAttribute.REGULAR : AudioAttribute.SLOW;

        if (options.isUserList()) {
          AudioAttribute audioAttribute = getLatest(ex, true);
          if (audioAttribute != null) {
            copyAudio(zOut, installPath, overallName, isEnglish, audioConversion, names, ex, speed, audioAttribute);
            someAudio = true;
          } else {
            logger.info("no   male audio for " + ex.getID());
          }
          audioAttribute = getLatest(ex, false);
          if (audioAttribute != null) {
            copyAudio(zOut, installPath, overallName, isEnglish, audioConversion, names, ex, speed, audioAttribute);
            someAudio = true;
          } else {
            logger.info("no female audio for " + ex.getID());
          }
        } else {
          AudioAttribute recording = getAudioAttribute(majorityUser, ex, options.justMale, speed);
          if (recording != null) {
            // logger.debug("found " + recording + " by " + recording.getUser());
            copyAudio(zOut, installPath, overallName, isEnglish, audioConversion, names, ex, speed, recording);
            someAudio = true;
          }
        }
      }

      if (!someAudio) {
        if (numMissing < 10) {
          logger.debug("writeFolderContents : no audio for exercise " + ex.getID());
        }
        numMissing++;
      }
    }
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 1000) {
      logger.debug("writeFolderContents : " +
          "took " + diff + " millis " +
          "to export " + toWrite.size() + " items, " +
          "num attached " + numAttach +
          " missing audio " + numMissing);
    }
  }

  private AudioAttribute getLatest(CommonExercise ex, boolean isMale) {
    long latestTime = 0;
    AudioAttribute latest = null;
    for (AudioAttribute audioAttribute : ex.getAudioAttributes()) {
      if (audioAttribute.isValid() &&
          (isMale && audioAttribute.isMale()) ||
          (!isMale && !audioAttribute.isMale())
          ) {

        if (audioAttribute.getTimestamp() >= latestTime) {
          latest = audioAttribute;
          latestTime = audioAttribute.getTimestamp();
/*          logger.info("getLatest for " + ex.getID() + " male = " + isMale +
              " found new latest " + audioAttribute.getUniqueID() + " : " + audioAttribute);*/
        }
      }
    }

    return latest;
  }

  private void copyAudio(ZipOutputStream zOut, String installPath, String overallName,
                         boolean isEnglish,
                         AudioConversion audioConversion,
                         Set<String> names, CommonExercise ex, String speed, AudioAttribute audioAttribute) throws IOException {
    String name = overallName + File.separator + getUniqueName(ex, !isEnglish);
    copyAudio(zOut, names, name, speed, installPath, audioConversion, audioAttribute, ex.getID(), getTrackInfo(ex, audioAttribute));
  }

  private boolean copyContextAudioBothGenders(ZipOutputStream zOut,
                                              String installPath,
                                              String overallName,
                                              boolean isEnglish,
                                              String countryCode,
                                              AudioConversion audioConversion,
                                              Set<String> names,
                                              CommonExercise ex,
                                              boolean justMale) throws IOException {
    boolean someAudio = false;

    AudioAttribute latestContext = ex.getLatestContext(justMale);
    if (latestContext != null) {
      copyContextAudio(zOut, installPath, overallName, isEnglish, audioConversion, names, ex, latestContext, countryCode);
      someAudio = true;
    }
    return someAudio;
  }

  private int attachAudio(Collection<CommonExercise> toWrite, AudioDAO audioDAO, String installPath, String relativeConfigDir1) {
    int numAttach = 0;
    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    for (CommonExercise ex : toWrite) {
      Collection<AudioAttribute> audioAttributes = exToAudio.get(ex.getID());
      if (audioAttributes != null) {
        audioDAO.attachAudio(ex, installPath, relativeConfigDir1, audioAttributes);
        numAttach++;
      }
    }
    return numAttach;
  }

  private void populateGenderToCount(Collection<CommonExercise> toWrite,
                                     Map<MiniUser, Integer> maleToCount,
                                     Map<MiniUser, Integer> femaleToCount) {
    for (CommonExercise ex : toWrite) {
      for (AudioAttribute attr : ex.getAudioAttributes()) {
        MiniUser user = attr.getUser();

        Map<MiniUser, Integer> userToCount = (user.isMale()) ? maleToCount : femaleToCount;
        Integer count = userToCount.get(user);
        userToCount.put(user, (count == null) ? 1 : count + 1);
      }
    }
  }

  /**
   * ae_cntxt_c01_u01_e0003. Mp3
   * <p>
   * ae = Egyptian Arabic
   * contxt = context
   * c01 = Chapter 01
   * U01 = Unit 01
   * E0001 = entry one
   *
   * @param zOut
   * @param installPath
   * @param overallName
   * @param isEnglish
   * @param audioConversion
   * @param namesSoFar
   * @param ex
   * @param latestContext
   * @throws IOException
   */
  private void copyContextAudio(ZipOutputStream zOut,
                                String installPath,
                                String overallName,
                                boolean isEnglish,
                                AudioConversion audioConversion,
                                Set<String> namesSoFar,
                                CommonExercise ex,
                                AudioAttribute latestContext,
                                String countryCode) throws IOException {
    String speed = latestContext.getSpeed();
    String folder = overallName + File.separator;
    Map<String, String> unitToValue = ex.getUnitToValue();
    String name;

    String id = ex.getID();
    if (countryCode.isEmpty()) {
      name = folder + getUniqueName(ex, !isEnglish) + "_context";
    } else {
      // make a name that looks like: ae_cntxt_c01_u01_e0003. Mp3
      StringBuilder builder = new StringBuilder();
      builder.append(countryCode);
      builder.append("_cntxt_");
      for (String type : typeOrder) {
        builder.append(type.toLowerCase().substring(0, 1));
        builder.append(unitToValue.get(type));
        builder.append("_");
      }
      builder.append("e");
      builder.append(id);
      name = folder + builder.toString();
    }
    copyAudio(zOut, namesSoFar, name, speed == null ? "" : speed, installPath, audioConversion, latestContext, id,
        getTrackInfo(ex, latestContext));
  }

  private TrackInfo getTrackInfo(CommonExercise ex, AudioAttribute latestContext) {
    return new TrackInfo(ex.getForeignLanguage(), latestContext.getUser().getUserID(), ex.getEnglish());
  }

  private MiniUser getMaxUser(Map<MiniUser, Integer> maleToCount) {
    MiniUser male = null;
    int mCount = 0;
    for (Map.Entry<MiniUser, Integer> pair : maleToCount.entrySet()) {
      if (male == null || pair.getValue() > mCount) {
        male = pair.getKey();
        mCount = pair.getValue();
      }
    }
    return male;
  }

  private void writeFolderContentsSimple(ZipOutputStream zOut,
                                         Collection<CommonExercise> toWrite,
                                         String installPath) throws Exception {
    long then = System.currentTimeMillis();
    AudioConversion audioConversion = new AudioConversion(props);
    logger.debug("writing " + toWrite.size());
    int c = 0;
    int d = 0;
    for (CommonExercise ex : toWrite) {
      if (ex.getRefAudio() != null) {
        try {
          AudioAttribute audio = ex.getRegularSpeed();
          String artist = audio.getUser().getUserID();

          copyAudioSimple(zOut, installPath, audioConversion, ex.getRefAudio(),
              new TrackInfo(ex.getForeignLanguage(), artist, ex.getEnglish()));
        } catch (IOException e) {
          //logger.debug("skipping duplicate " +e);
          d++;
        }
        c++;
      }
    }
    zOut.close();
    long now = System.currentTimeMillis();
    long diff = now - then;
    logger.debug("took " + diff + " millis to export " + toWrite.size() + " items " + c + " duplicates " + d);

    // if (diff > 1000) {
    //    logger.debug("took " + diff + " millis to export " + toWrite.size() + " items");
    // }
  }

/*  private AudioAttribute getAudioAttribute(MiniUser majorityUser, CommonExercise ex, boolean isMale, boolean regularSpeed) {
    AudioAttribute recordingAtSpeed = majorityUser != null ? ex.getRecordingsBy(majorityUser.getExID(), regularSpeed) : null;

    if (recordingAtSpeed == null) {  // can't find majority user or nothing by this person at that speed
      Collection<AudioAttribute> byGender = ex.getByGender(isMale);
      // choose the first one if not the majority majorityUser
      for (AudioAttribute attribute : byGender) {
        if ((attribute.isRegularSpeed() && regularSpeed) || (!attribute.isRegularSpeed() && !regularSpeed)) {
          recordingAtSpeed = attribute;
          break;
        }
      }
    }
    return recordingAtSpeed;
  }*/

  /**
   * Prefer recordings by the speaker with the most recordings.
   * If that person didn't do this cut, find someone with the matching gender and speed.
   *
   * @param majorityUser
   * @param ex
   * @param isMale
   * @param speed
   * @return
   * @see #writeFolderContents
   */
  private AudioAttribute getAudioAttribute(MiniUser majorityUser, CommonExercise ex, boolean isMale, String speed) {
    AudioAttribute recordingAtSpeed = majorityUser != null ? ex.getRecordingsBy(majorityUser.getId(), speed) : null;

    if (recordingAtSpeed == null) {  // can't find majority user or nothing by this person at that speed
      Collection<AudioAttribute> byGender = ex.getByGender(isMale);
      // choose the first one if not the majority majorityUser
      for (AudioAttribute attribute : byGender) {
        String speed1 = attribute.getSpeed();
        if (speed1 != null &&
            speed1.equalsIgnoreCase(speed)) {
          recordingAtSpeed = attribute;
          break;
        }
      }
    }
    return recordingAtSpeed;
  }

  /**
   * Worry about name collisions for entries composed of english-fl pairs
   *
   * @param includeFL true if not english
   * @param ex
   * @return
   * @see #writeFolderContents
   */
  private String getUniqueName(CommonExercise ex, boolean includeFL) {
    String trim = ex.getEnglish().trim();
    String foreignLanguage = ex.getForeignLanguage();

    String name = trim.equals("N/A") ? foreignLanguage : trim + (includeFL ? "_" + foreignLanguage : "");
    name = name.trim();
    name = name.replaceAll("\"", "\\'").replaceAll("\\?", "").replaceAll("\\:", "").replaceAll("/", " or ").replaceAll("\\\\", " or ");
    name = name.replaceAll("\\,", "_");
    return name;
  }

  /**
   * @param zOut
   * @param names           guarantees entries have unique names (sometimes duplicates in a chapter)
   * @param parent
   * @param speed
   * @param realContextPath
   * @param audioConversion
   * @param attribute
   * @param exid
   * @param trackInfo
   * @throws IOException
   * @see #writeFolderContents
   * @see #copyContextAudio
   */
  private void copyAudio(ZipOutputStream zOut, Set<String> names, String parent, String speed,
                         String realContextPath, AudioConversion audioConversion, AudioAttribute attribute,
                         String exid, TrackInfo trackInfo) throws IOException {
    String audioRef = attribute.getAudioRef();
/*    logger.debug("\tfor ex id " +exid +
      " writing audio under context path " + realContextPath + " at " + audioRef);*/
    // String author = attribute.getUser().getUserID();
    String s = audioConversion.ensureWriteMP3(audioRef, realContextPath, false, trackInfo);
    File mp3 = new File(s);
    if (mp3.exists()) {
      //  logger.debug("---> Did write " + mp3.getAbsolutePath());
      String name = getName(parent, speed, attribute);
      if (names.contains(name)) {
        name += "_" + exid;
      }

      boolean add = names.add(name);
      name = name.replaceAll(" ", "_");
      name += ".mp3";

      //logger.debug("copyContextAudio : mp3 name is " + name);
      if (add) {
        addZipEntry(zOut, mp3, name);
      } else {
        logger.info("skip duplicate " + name);
      }
    } else {

      String absolutePath = mp3.getAbsolutePath();
      if (!absolutePath.endsWith(AudioConversion.FILE_MISSING)) {
        logger.warn("\tDidn't write " + absolutePath);
      }
    }
  }

  /**
   * @param zOut
   * @param realContextPath
   * @param audioConversion
   * @param audioRef
   * @param trackInfo
   * @return
   * @throws IOException
   * @see #writeFolderContentsSimple
   */
  private void copyAudioSimple(ZipOutputStream zOut,
                               String realContextPath,
                               AudioConversion audioConversion,
                               String audioRef,
                               TrackInfo trackInfo) throws IOException {
    String filePath = audioConversion.ensureWriteMP3(audioRef, realContextPath, false, trackInfo);
    File mp3 = new File(filePath);
    if (mp3.exists()) {
      String name = audioRef.replaceAll(".wav", ".mp3");
      //logger.debug("copyAudioSimple : mp3 name is " + name);
      try {
        addZipEntry(zOut, mp3, name);
      } catch (Exception e) {
        logger.debug("skip entry for audio " + name);
      }
    } else {
      logger.warn("\tDidn't write " + mp3.getAbsolutePath());
    }
  }

  private void addZipEntry(ZipOutputStream zOut, File mp3, String name) throws IOException {
    zOut.putNextEntry(new ZipEntry(name));
    FileUtils.copyFile(mp3, zOut);
    zOut.flush();
    zOut.closeEntry();
  }

  /**
   * @param parent
   * @param speed
   * @param attribute
   * @return
   * @see #copyContextAudio
   */
  private String getName(String parent, String speed, AudioAttribute attribute) {
    MiniUser user = attribute.getUser();
    String userInfo = user.isDefault() ?
        "" :
        (attribute.isMale() ? MALE : FEMALE);
    String speedSuffix = speed.equals(AudioAttribute.REGULAR) || speed.isEmpty() ? "" : "_" + speed;
    return parent + (user.isDefault() ? "" : "_" + userInfo) + speedSuffix;
  }
}
