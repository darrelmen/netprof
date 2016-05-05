/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.AudioExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.MutableAudioExercise;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Created by go22670 on 11/10/15.
 */
public class AttachAudio {
	private static final Logger logger = Logger.getLogger(AttachAudio.class);
	private static final String FAST_WAV = "Fast" + ".wav";
	private static final String SLOW_WAV = "Slow" + ".wav";

	private int missingExerciseCount = 0;
	private int c = 0;
	private final int audioOffset;
	private final String mediaDir, mediaDir1;
	private final File installPath;
	private Map<String, List<AudioAttribute>> exToAudio;

	/**
	 * @param mediaDir
	 * @param mediaDir1
	 * @param installPath
	 * @param audioOffset
	 * @param exToAudio
	 * @see BaseExerciseDAO#setAudioDAO
	 */
	public AttachAudio(String mediaDir,
										 String mediaDir1,
										 File installPath,
										 int audioOffset,
										 Map<String, List<AudioAttribute>> exToAudio) {
		this.mediaDir = mediaDir;
		this.mediaDir1 = mediaDir1;
		this.installPath = installPath;
		this.setExToAudio(exToAudio);
		this.audioOffset = audioOffset;
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
	 * @see ExcelImport#getExercise
	 */
	public <T extends AudioExercise> void addOldSchoolAudio(String refAudioIndex, T imported) {
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

	/**
	 * Make sure every audio file we attach is a valid audio file -- it's really where it says it's supposed to be.
	 * <p>
	 * TODOx : rationalize media path -- don't force hack on bestAudio replacement
	 * Why does it sometimes have the config dir on the front?
	 *
	 * @param imported
	 * @paramx id
	 * @see ExcelImport#attachAudio
	 * @see ExcelImport#getRawExercises()
	 */
	public <T extends CommonExercise> int attachAudio(T imported, Collection<String> transcriptChanged) {
		String id = imported.getID();
		int missing = 0;
		if (exToAudio.containsKey(id) || exToAudio.containsKey(id + "/1") || exToAudio.containsKey(id + "/2")) {
			List<AudioAttribute> audioAttributes = exToAudio.get(id);
			//   if (audioAttributes.isEmpty()) logger.info("huh? audio attr empty for " + id);
			missing = attachAudio(imported, missing, audioAttributes, transcriptChanged);
		}
		return missing;
	}

	/**
	 * @param imported
	 * @param missing
	 * @param audioAttributes
	 * @param <T>
	 * @return
	 * @see #attachAudio(CommonExercise, Collection)
	 */
	private <T extends CommonExercise> int attachAudio(T imported, int missing,
																										 Collection<AudioAttribute> audioAttributes,
																										 Collection<String> transcriptChanged) {
		MutableAudioExercise mutableAudio = imported.getMutableAudio();

		if (audioAttributes == null) {
			missingExerciseCount++;
			if (missingExerciseCount < 10) {
				String id = imported.getID();
				logger.error("attachAudio can't find " + id);
			}
		} else if (!audioAttributes.isEmpty()) {
			Set<String> previouslyAttachedAudio = new HashSet<>();
			for (AudioAttribute audioAttribute : imported.getAudioAttributes()) {
				previouslyAttachedAudio.add(audioAttribute.getAudioRef());
			}

		//	int m = 0;

			for (AudioAttribute audio : audioAttributes) {
				String child = mediaDir1 + File.separator + audio.getAudioRef();
				File test = new File(installPath, child);

				boolean exists = test.exists();
				if (!exists) {
					test = new File(installPath, audio.getAudioRef());
					exists = test.exists();
					child = audio.getAudioRef();
				}

				if (exists) {
					if (!previouslyAttachedAudio.contains(child)) {

						if (audio.isContextAudio()) {
							Collection<CommonExercise> directlyRelated = imported.getDirectlyRelated();
							if (directlyRelated.isEmpty()) logger.warn("huh? no context exercise on " +imported.getID());
							else {
								if (directlyRelated.size() == 1) {
									audio.setAudioRef(child);   // remember to prefix the path
									directlyRelated.iterator().next().getMutableAudio().addAudio(audio);
								}
							}
						}
						else if (audio.hasMatchingTranscript(imported.getForeignLanguage())) {
							audio.setAudioRef(child);   // remember to prefix the path

							mutableAudio.addAudio(audio);
						} else {
							transcriptChanged.add(audio.getExid());
/*							if (m++ < 10) {
								logger.warn("for " + imported + " audio transcript " + audio.getTranscript() +
										" doesn't match : '" + removePunct(audio.getTranscript()) + "' vs '" + removePunct(imported.getForeignLanguage()) + "'");
							}*/
						}
						previouslyAttachedAudio.add(child);
//            logger.debug("imported " +imported.getID()+ " now " + imported.getAudioAttributes());
					} else {
						logger.debug("skipping " + child);
					}
				} else {
					missing++;
					c++;
					if (c < 5) {
						logger.warn("attachAudio file " + test.getAbsolutePath() + " does not exist - \t" + audio.getAudioRef());
//            if (c < 2) {
//              logger.warn("installPath " + installPath + "mediaDir " + mediaDir + " mediaDir1 " + mediaDir1);
//            }
					}
				}
			}
		}
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

	public void setExToAudio(Map<String, List<AudioAttribute>> exToAudio) {
		this.exToAudio = exToAudio;
	}
}
