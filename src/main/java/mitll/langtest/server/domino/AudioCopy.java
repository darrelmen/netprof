package mitll.langtest.server.domino;

import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.dialog.IDialogDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.Language;
import mitll.npdata.dao.SlickAudio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class AudioCopy {
  private static final Logger logger = LogManager.getLogger(AudioCopy.class);
  private static final String UNKNOWN = "unknown";
  private final DAOContainer daoContainer;

  private final ProjectServices projectServices;
  private final IProjectManagement projectManagement;

  /**
   * @param projectServices
   * @param projectManagement
   * @param daoContainer
   */
  public AudioCopy(ProjectServices projectServices, IProjectManagement projectManagement, DAOContainer daoContainer) {
    this.projectServices = projectServices;
    this.projectManagement = projectManagement;
    this.daoContainer = daoContainer;
  }

  /**
   * Find source project(s) to copy audio from. Could be yourself.
   *
   * @param projectid
   * @param newEx
   * @param dominoToExID
   * @see ProjectSync#addPending
   * @see mitll.langtest.server.services.ListServiceImpl#reallyCreateNewItems
   * @see mitll.langtest.server.database.project.DialogPopulate#addDialogs(Project, Project, IDialogDAO, Map, int, DialogType, Map, boolean)
   */
  public <T extends ClientExercise> void copyAudio(int projectid,
                                                   Collection<T> newEx,
                                                   Map<Integer, Integer> dominoToExID) {
    try {
      List<Project> sourceProjects = getProjectsForSameLanguage(projectid);

      int nSourceProjects = sourceProjects.size();

      logger.info("copyAudio found " + nSourceProjects + " source projects for project " + projectid +
          "\n\tdomino->ex " + dominoToExID.size());

      /**
       * Collect all the SlickAudio that needs to be copied for exercises and context exercises.
       */

      Collection<AudioCopy.AudioMatches> copyAudioForEx = new ArrayList<>();
      Collection<AudioCopy.AudioMatches> copyAudioForContext = new ArrayList<>();

      for (Project source : sourceProjects) {
        Map<String, List<SlickAudio>> transcriptToAudio = getTranscriptToAudio(source.getID());
        logger.info("copyAudio for " +
            "\n\tnew ex  " + newEx.size()+
            "\n\tproject " + source.getID() + "/" + source.getProject().name() +
            "\n\tgot     " + transcriptToAudio.size() + " source candidates");
        getSlickAudios(projectid,
            newEx,
            dominoToExID,
            transcriptToAudio,

            copyAudioForEx,
            copyAudioForContext);
      }

      if (!sourceProjects.isEmpty()) {
        addCopiesToDatabase(newEx, sourceProjects, nSourceProjects, copyAudioForEx, copyAudioForContext);
      }
      else {
        logger.warn("copyAudio can't find source projects for " + projectid);
      }
    } catch (Exception e) {
      logger.info("Got " + e, e);
    }
  }

  private <T extends ClientExercise> void addCopiesToDatabase(Collection<T> newEx,
                                                              List<Project> sourceProjects,
                                                              int nSourceProjects,
                                                              Collection<AudioMatches> copyAudioForEx,
                                                              Collection<AudioMatches> copyAudioForContext) {
    List<SlickAudio> copies = getSlickAudios(copyAudioForEx, copyAudioForContext);
    if (copies.isEmpty()) {
      logger.info("copyAudio - no audio copies for " + newEx.size() + " exercises...");
    } else {
      logger.info("copyAudio :" +
          "\n\tcopying      " + copyAudioForEx + "/" + copyAudioForContext +
          "\n\taudio        " + copies.size() +
          "\n\tfrom         " + newEx.size() +
          "\n\tfrom sources " + nSourceProjects +
          " projects, e.g.  " + sourceProjects.iterator().next().getProject().name());

      daoContainer.getAudioDAO().addBulk(copies);
    }
  }

  /**
   * transcript to lower case.
   *
   * @param maxID
   * @return map of LC transcript to audio with that transcript
   * @see #copyAudio
   */
  @NotNull
  private Map<String, List<SlickAudio>> getTranscriptToAudio(int maxID) {
    Map<String, List<SlickAudio>> transcriptToAudio = new HashMap<>();

    Collection<SlickAudio> audioAttributesByProjectThatHaveBeenChecked
        = maxID == -1 ? Collections.emptyList() : daoContainer.getAudioDAO().getAllNoExistsCheck(maxID);

    logger.info("getTranscriptToAudio found " + audioAttributesByProjectThatHaveBeenChecked.size() + " audio entries for " + maxID);
    for (SlickAudio audioAttribute : audioAttributesByProjectThatHaveBeenChecked) {
      List<SlickAudio> audioAttributes = transcriptToAudio.computeIfAbsent(audioAttribute.transcript().toLowerCase(), k -> new ArrayList<>());
      audioAttributes.add(audioAttribute);
    }
    return transcriptToAudio;
  }

  /**
   * OK to look for source audio from your own project if your own project is in the state of PRODUCTION.
   *
   * TODO : Revisit this assumption???
   *
   * Could be a copied entry from something already in the same project.
   *
   * @param projectid
   * @return list of production projects of that language
   * @see #copyAudio
   */
  private List<Project> getProjectsForSameLanguage(int projectid) {
    Language language = projectServices.getProject(projectid).getLanguageEnum();

    // logger.info("getProjectsForSameLanguage look for " + language + " for " + projectid);

    return projectManagement
        .getProductionProjects()
        .stream()
        .filter(project -> project.getLanguageEnum() == language)
        .collect(Collectors.toList());
  }

  /**
   * Get all the audio that needs to be copied
   *
   * @param copyAudioForEx
   * @param copyAudioForContext
   * @return
   * @see #copyAudio
   */
  @NotNull
  private List<SlickAudio> getSlickAudios(Collection<AudioMatches> copyAudioForEx,
                                          Collection<AudioMatches> copyAudioForContext) {
    List<SlickAudio> copies = new ArrayList<>();
    copyAudioForEx.forEach(audioMatches -> audioMatches.deposit(copies));
    copyAudioForContext.forEach(audioMatches -> audioMatches.deposit(copies));
    return copies;
  }

  /**
   * @param projectid
   * @param newEx
   * @param dominoToExID               so we can figure out the id for a new exercise from its domino id
   * @param transcriptToAudio
   * @param transcriptToMatches        - output
   * @param transcriptToContextMatches
   * @see #copyAudio
   */
  @NotNull
  private <T extends ClientExercise> void getSlickAudios(int projectid,
                                                         Collection<T> newEx,
                                                         Map<Integer, Integer> dominoToExID,
                                                         Map<String, List<SlickAudio>> transcriptToAudio,

                                                         Collection<AudioMatches> transcriptToMatches,
                                                         Collection<AudioMatches> transcriptToContextMatches) {
    logger.info("getSlickAudios" +
        "\n\tnewEx                      " + newEx.size() +
        "\n\ttranscriptToAudio          " + transcriptToAudio.size()
    );

    MatchInfo vocab = new MatchInfo(0, 0);
    MatchInfo contextCounts = new MatchInfo(0, 0);

    boolean hasDominoMap = !dominoToExID.isEmpty();

    for (T ex : newEx) {
      Integer exid = ex.getID();

      if (exid == -1 && hasDominoMap) {
        exid = getExerciseForDominoID(dominoToExID, ex);
      }

      if (exid == null || exid == -1) {
        logger.info("getSlickAudios : no exercise id found for domino ID " + ex.getDominoID() + " or old ID " + ex.getOldID() + " : " + ex.getEnglish() + " " + ex.getForeignLanguage());
      } else {
        boolean hasAudioAlready = daoContainer.getAudioDAO().hasAudio(exid);
        if (hasAudioAlready) {
          logger.info("getSlickAudios skipping " + exid + " since it already has audio");
        } else {
          MatchInfo matchInfo = addAudioForVocab(projectid, transcriptToAudio, transcriptToMatches, ex, exid);
          if (matchInfo.isEmpty()) logger.warn("getSlickAudios can't find match for " + ex.getForeignLanguage() + " in " + transcriptToAudio.size() + " existing items.");
          vocab.add(matchInfo);
        }

        contextCounts.add(addAudioForContext(projectid, transcriptToAudio, transcriptToContextMatches, ex.getDirectlyRelated()));
      }
    }

    logger.info("getSlickAudio  : vocab " + vocab + " contextCounts " + contextCounts);
  }

  @Nullable
  private <T extends ClientExercise> Integer getExerciseForDominoID(Map<Integer, Integer> dominoToExID, T ex) {
    Integer exid;
    int dominoID = ex.getDominoID();
    exid = dominoToExID.get(dominoID);

    String oldID = ex.getOldID();
    if (exid == null && !oldID.equalsIgnoreCase(UNKNOWN)) {
      logger.info("getSlickAudios exercise old " + oldID + " -> " + exid +
          "\n\teng " + ex.getEnglish() +
          "\n\tfl  " + ex.getForeignLanguage());
    } else {
      logger.info("getSlickAudios exercise dominoID " + dominoID + " -> ex id " + exid +
          "\n\teng " + ex.getEnglish() +
          "\n\tfl  " + ex.getForeignLanguage());
    }

    if (exid == null) {
      String message = "getSlickAudios : huh? can't find domino ID " + dominoID + " in " + dominoToExID.size();
      if (dominoID == -1) {
        logger.warn(message);
      } else {
        logger.error(message);
      }
      // logger.error("getSlickAudios : huh? can't find old    ID " + oldID + " in " + oldIDToExID.size());
    }
    return exid;
  }


  /**
   * What about delete - we need to remove audio who transcripts no longer match.
   *
   * @param projectid
   * @param transcriptToAudio
   * @param transcriptToContextMatches
   * @param contextExercises
   * @return
   * @paramx exToInt
   * @see #getSlickAudios
   */
  private MatchInfo addAudioForContext(int projectid,
                                       //   Map<String, Integer> exToInt,
                                       Map<String, List<SlickAudio>> transcriptToAudio,
                                       Collection<AudioMatches> transcriptToContextMatches,
                                       Collection<ClientExercise> contextExercises) {
    int match = 0;
    int nomatch = 0;
    for (ClientExercise context : contextExercises) {
      int cexid = context.getID();
      String prefix = cexid + "/" + context.getDominoID();
      if (context.hasRefAudio()) {
        String cfl = context.getForeignLanguage().toLowerCase();
        List<SlickAudio> audioAttributes = transcriptToAudio.get(cfl);

        //   String coldID = context.getOldID();
        //  Integer cexid = exToInt.get(coldID);

        logger.info("getSlickAudios context " + prefix +
            //" old '" + coldID + "'" +
            " -> '" + cexid + "' matches = " + audioAttributes);
        if (audioAttributes != null && cexid != -1) {
          transcriptToContextMatches.add(copyMatchingAudio(projectid, cexid, audioAttributes, true));
          match++;
        } else {
          logger.info("getSlickAudios context " + prefix +
              "\n\tno match '" + context.getEnglish() + "'" +
              "\n\tfl       '" + cfl + "'" +
              "\n\tin        " + transcriptToAudio.size() + " possibilities");
          nomatch++;
        }
      } else {
        logger.info("getSlickAudios context " + prefix + " has audio already, so not adding audio to it.");
      }
    }
    return new MatchInfo(match, nomatch);
  }


  /**
   * Only does match on fl, not on pair of fl/english... might be better.
   *
   * Matches case insensitive.
   *
   * @param projectid
   * @param transcriptToAudio
   * @param transcriptMatches
   * @param ex
   * @param exid
   * @return
   * @see #getSlickAudios
   */
  private MatchInfo addAudioForVocab(int projectid,
                                     Map<String, List<SlickAudio>> transcriptToAudio,
                                     Collection<AudioMatches> transcriptMatches,
                                     CommonShell ex,
                                     Integer exid) {
    int match = 0;
    int nomatch = 0;

    String fl = ex.getForeignLanguage().toLowerCase();
    List<SlickAudio> audioAttributes = transcriptToAudio.get(fl);

    String english = ex.getEnglish();

    logger.info("addAudioForVocab (" + projectid +
        ") looking for match to ex " + exid + "/" + ex.getID() + " '" + english + "' = '" + fl + "'");

    if (audioAttributes != null) {
      transcriptMatches.add(copyMatchingAudio(projectid, exid, audioAttributes, false));
      match++;
    } else {
      logger.info("\taddAudioForVocab vocab no match english '" + english + "' = '" + fl + "'");
      nomatch++;
    }

    if (match == 0) {
      logger.info("\taddAudioForVocab vocab no match english '" + english + "' = '" + fl + "' in " +
          transcriptToAudio.size() + " transcripts");
    }

    return new MatchInfo(match, nomatch);
  }

  /**
   * Add to matches from input audio attributes
   *
   * @param projectid
   * @param exid
   * @param slickAudios
   * @param isContext
   * @return SlickAudio collection of new SlickAudios to add to database.
   * @see #addAudioForVocab
   * @see #addAudioForContext
   */
  private AudioMatches copyMatchingAudio(int projectid,
                                         int exid,
                                         List<SlickAudio> slickAudios,
                                         boolean isContext) {
    AudioMatches audioMatches = new AudioMatches();

    if (exid == -1) {
      logger.error("copyMatchingAudio huh? exid -1 for project " + projectid + " " + slickAudios.size());
    }

    Timestamp modified = new Timestamp(System.currentTimeMillis());
    List<SlickAudio> audioToUse = slickAudios;

    // if the audio match is from a vocab item, don't take slow speed audio, and then convert the type to context.
    if (isContext) {
      audioToUse = audioToUse
          .stream()
          .filter(slickAudio ->
              {
                String audiotype = slickAudio.audiotype();
                return audiotype.equalsIgnoreCase(AudioType.CONTEXT_REGULAR.toString()) || isRegular(audiotype);
              }
          )
          .collect(Collectors.toList());
    }

    audioToUse.forEach(audio ->
        audioMatches.add(getCopiedAudio(projectid, exid, isContext, modified, audio)));

    return audioMatches;
  }

  /**
   * @param exid
   * @param isContext
   * @param audio
   * @return
   * @see mitll.langtest.server.database.audio.SlickAudioDAO#copyOne
   */
  public SlickAudio getCopiedAudio(int exid, boolean isContext, SlickAudio audio) {
    return getCopiedAudio(audio.projid(), exid, isContext, new Timestamp(System.currentTimeMillis()), audio);
  }

  @NotNull
  private SlickAudio getCopiedAudio(int projectid, int exid, boolean isContext, Timestamp modified, SlickAudio audio) {
    return new SlickAudio(
        -1,
        audio.userid(),
        exid,
        modified,
        audio.audioref(),
        getAudioType(isContext, audio),
        audio.duration(),
        audio.defect(),
        audio.transcript(),
        projectid,
        audio.exists(),
        audio.lastcheck(),
        audio.actualpath(),
        audio.dnr(),
        audio.resultid(),
        audio.gender()
    );
  }

  private String getAudioType(boolean isContext, SlickAudio audio) {
    String audiotype = audio.audiotype();
    if (isContext && isRegular(audiotype)) {
      audiotype = AudioType.CONTEXT_REGULAR.toString().toLowerCase();
    }
    return audiotype;
  }

  private boolean isRegular(String audiotype) {
    return audiotype.equalsIgnoreCase(AudioType.REGULAR.toString());
  }

  static class AudioMatches {
    private SlickAudio mr = null;
    private SlickAudio ms = null;

    private SlickAudio fr = null;
    private SlickAudio fs = null;

    /**
     * @param candidate
     * @see #copyMatchingAudio
     */
    void add(SlickAudio candidate) {
      boolean regularSpeed = getAudioType(candidate).isRegularSpeed();
      //   int gender = candidate.gender();
//      logger.info("AudioMatches Examine candidate " + candidate);
//      logger.info("AudioMatches Examine regularSpeed " + regularSpeed + " " + audioType);
//      logger.info("AudioMatches Examine gender " + gender );
//      try {
//        audioType = AudioType.valueOf(candidate.audiotype());
//      } catch (IllegalArgumentException e) {
//        logger.error("Got " + e, e);
//      }
      int before = getCount();
      if (candidate.gender() == 0) {
        if (regularSpeed) {
          mr = mr == null ? candidate : mr.dnr() < candidate.dnr() ? candidate : mr;
        } else {
          ms = ms == null ? candidate : ms.dnr() < candidate.dnr() ? candidate : ms;
        }
      } else {
        if (regularSpeed) {
          fr = fr == null ? candidate : fr.dnr() < candidate.dnr() ? candidate : fr;
        } else {
          fs = fs == null ? candidate : fs.dnr() < candidate.dnr() ? candidate : fs;
        }
      }
      int after = getCount();
      if (after > before) {
        logger.info("AudioMatches now " + after + " added " + candidate);
      } else {
//        logger.info("AudioMatches not adding " + after+ " added " + candidate);
      }
    }

    /**
     * @param candidate
     * @return
     */
    @NotNull
    private AudioType getAudioType(SlickAudio candidate) {
      AudioType audioType = AudioType.UNSET;
      String rawAudioType = candidate.audiotype();
      try {
        if (rawAudioType.equals(AudioType.CONTEXT_REGULAR.toString())) {
          audioType = AudioType.CONTEXT_REGULAR;
        } else if (rawAudioType.equals(AudioType.CONTEXT_SLOW.toString())) {
          audioType = AudioType.CONTEXT_SLOW;
        } else {
          audioType = AudioType.valueOf(rawAudioType.toUpperCase());
        }
      } catch (IllegalArgumentException e) {
        logger.error("getAudioType : got unknown audio " + rawAudioType);
      }
      return audioType;
    }

    int getCount() {
      int count = 0;
      if (mr != null) count++;
      if (fr != null) count++;
      if (ms != null) count++;
      if (fs != null) count++;
      return count;
    }

    public String toString() {
      return
          (mr == null ? "x" : "1") +
              (fr == null ? "x" : "2") +
              (ms == null ? "x" : "3") +
              (fs == null ? "x" : "4");
    }

    void deposit(List<SlickAudio> copies) {
      if (mr != null) copies.add(mr);
      if (fr != null) copies.add(fr);
      if (ms != null) copies.add(ms);
      if (fs != null) copies.add(fs);
    }
  }


  /**
   * For debug output...?
   */
  private static class MatchInfo {
    private int match;
    private int noMatch;

    MatchInfo(int match, int noMatch) {
      this.match = match;
      this.noMatch = noMatch;
    }

/*    public MatchInfo(MatchInfo matchInfo) {
      add(matchInfo);
    }*/

    void add(MatchInfo matchInfo) {
      this.match += matchInfo.match;
      this.noMatch += matchInfo.noMatch;
    }

    boolean isEmpty() { return match == 0;}

    public String toString() {
      return match + "/" + noMatch;
    }
  }
}
