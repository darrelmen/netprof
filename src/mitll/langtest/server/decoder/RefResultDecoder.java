package mitll.langtest.server.decoder;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Created by go22670 on 7/22/15.
 */
public class RefResultDecoder {
    private static final Logger logger = Logger.getLogger(RefResultDecoder.class);
    private final DatabaseImpl db;
    private final ServerProperties serverProps;
    public static final boolean DO_REF_DECODE = true;
    private final AudioFileHelper audioFileHelper;
    private boolean stopDecode = false;
    private PathHelper pathHelper;

    public RefResultDecoder(DatabaseImpl db, ServerProperties serverProperties, PathHelper pathHelper,
                            AudioFileHelper audioFileHelper) {
        this.db = db;
        this.serverProps = serverProperties;
        this.pathHelper = pathHelper;
        this.audioFileHelper = audioFileHelper;
    }

    public void doRefDecode(final List<CommonExercise> exercises, final String relativeConfigDir) {
        if (serverProps.shouldDoDecode()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000); // ???
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    writeRefDecode(exercises, relativeConfigDir);
                }
            }).start();
        } else {
            logger.debug(getLanguage() + " not doing decode all");
        }
    }

    private String getLanguage() {
        return serverProps.getLanguage();
    }

    /**
     * Do alignment and decoding on all the reference audio and store the results in the RefResult table.
     *
     * @see #doDecode(Set, CommonExercise, Collection)
     */
    private void writeRefDecode(List<CommonExercise> exercises, String relativeConfigDir) {
        if (DO_REF_DECODE) {
            Map<String, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio();
            String installPath = pathHelper.getInstallPath();

            int numResults = db.getRefResultDAO().getNumResults();
            logger.debug(getLanguage() + "writeRefDecode : found " +
                    numResults + " in ref results table vs " + exToAudio.size() + " exercises with audio");

            Set<String> decodedFiles = getDecodedFiles();
            //       List<CommonExercise> exercises = getExercises();
            logger.debug(getLanguage() + " found " + decodedFiles.size() + " previous ref results, checking " +
                    exercises.size() + " exercises ");

            if (stopDecode) logger.debug("Stop decode true");

            int count = 0;
            int attrc = 0;
            int maleAudio = 0;
            int femaleAudio = 0;
            int defaultAudio = 0;
            for (CommonExercise exercise : exercises) {
                if (stopDecode) return;

                List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
                if (audioAttributes != null) {
//					logger.warn("hmm - audio recorded for " + )
                    db.getAudioDAO().attachAudio(exercise, installPath, relativeConfigDir, audioAttributes);
                    attrc += audioAttributes.size();
                }

                Set<Long> preferredVoices = serverProps.getPreferredVoices();
                Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices);
                Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices);
                Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();

                List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
                boolean maleEmpty = maleUsers.isEmpty();

                List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);
                boolean femaleEmpty = femaleUsers.isEmpty();

                if (!maleEmpty) {
                    List<AudioAttribute> audioAttributes1 = malesMap.get(maleUsers.get(0));
                    maleAudio += audioAttributes1.size();
                    count += doDecode(decodedFiles, exercise, audioAttributes1);
                }
                if (!femaleEmpty) {
                    List<AudioAttribute> audioAttributes1 = femalesMap.get(femaleUsers.get(0));
                    femaleAudio += audioAttributes1.size();

                    count += doDecode(decodedFiles, exercise, audioAttributes1);
                } else if (maleEmpty) {
                    defaultAudio += defaultUserAudio.size();

                    count += doDecode(decodedFiles, exercise, defaultUserAudio);
                }
            }
            logger.debug("Out of " + attrc + " best audio files, " + maleAudio + " male, " + femaleAudio + " female, " +
                    defaultAudio + " default " + "decoded " + count);
        }
    }

    /**
     * Get the set of files that have already been decoded and aligned so we don't do them a second time.
     *
     * @return
     * @see #writeRefDecode
     */
    private Set<String> getDecodedFiles() {
        List<Result> results = db.getRefResultDAO().getResults();
        logger.debug(getLanguage() + " found " + results.size() + " previous ref results");

        Set<String> decodedFiles = new HashSet<String>();
        int count = 0;
        for (Result res : results) {
            if (count++ < 20) {
                logger.debug("\t found " + res);
            }

            String[] bestAudios = res.getAnswer().split(File.separator);
            if (bestAudios.length > 1) {
                String bestAudio = bestAudios[bestAudios.length - 1];
                //		logger.debug("added " + bestAudio);
                decodedFiles.add(bestAudio);
                if (stopDecode) break;
                //	logger.debug("previously found " + res);
            }
        }
        return decodedFiles;
    }

    private int doDecode(Set<String> decodedFiles, CommonExercise exercise, Collection<AudioAttribute> audioAttributes) {
        int count = 0;
        List<AudioAttribute> toDecode = new ArrayList<AudioAttribute>();
        for (AudioAttribute attribute : audioAttributes) {
            if (!attribute.isExampleSentence()) {
                String bestAudio = getFile(attribute);
                if (!decodedFiles.contains(bestAudio)) {
                    toDecode.add(attribute);
                }
            }
        }
        for (AudioAttribute attribute : toDecode) {
            if (stopDecode) return 0;

            try {
                audioFileHelper.decodeOneAttribute(exercise, attribute);
                count++;
            } catch (Exception e) {
                logger.error("Got " + e, e);
            }
        }

        return count;
    }

    private String getFile(AudioAttribute attribute) {
        String[] bestAudios = attribute.getAudioRef().split(File.separator);
        return bestAudios[bestAudios.length - 1];
    }

    public void setStopDecode(boolean stopDecode) {
        this.stopDecode = stopDecode;
    }
}
