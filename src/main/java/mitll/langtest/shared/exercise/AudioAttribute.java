/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.user.MiniUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * What the client wants to know about a reference audio cut.
 * <p>
 * Includes info about who recorded it and when, for which exercise, and the path to the audio on the server.
 * <p>
 * Also indicates any attributes REGULAR or SLOW and whether it's been played by a reviewer.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/6/13
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioAttribute implements IsSerializable, UserAndTime {
  /**
   * @Deprecated - this shouldn't ever be used, since we only return audio that has been recently confirmed to be
   * on disk.
   */
  private static final String FILE_MISSING = "FILE_MISSING";

  private static final String SPEED = "speed";
  public static final String SLOW = AudioType.SLOW.toString();
  public static final String REGULAR = AudioType.REGULAR.toString();

  /**
   * TODO : if we ever have slow recordings of context audio we'll need to add another type or an enum
   */
  public static final AudioType CONTEXT_AUDIO_TYPE = AudioType.CONTEXT_REGULAR;

  private MiniUser user;
  private MiniUser.Gender realGender;

  private int uniqueID;
  private String audioRef;

  // DON'T send this to client
  private transient String actualPath; // where we found it on the hydra server - might be different from audioRef if it's been moved
  private int exid;
  private transient String oldexid;
  private int userid;
  private long timestamp;
  private long durationInMillis;
  private transient float dnr;
  private transient int resultid;

  /**
   * Don't send to client - just server side
   */
  private transient String transcript = "";

  // 9/24/14 : setting it here may stop intermittent gwt rpc exceptions
  private Map<String, String> attributes = new HashMap<String, String>();
  private boolean hasBeenPlayed;
  private AudioType audioType;
  private AlignmentOutput alignmentOutput = null;

  public AudioAttribute() {
  }

  /**
   * @param uniqueID
   * @param userid
   * @param exid
   * @param audioRef
   * @param timestamp
   * @param durationInMillis
   * @param type
   * @param user
   * @param transcript       what the speaker read at the time of recording
   * @param actualPath
   * @seex mitll.langtest.server.database.audio.BaseAudioDAO#getResultsForQuery
   * @seex mitll.langtest.server.database.audio.BaseAudioDAO#getAudioAttribute
   */
  public AudioAttribute(int uniqueID,
                        int userid,
                        int exid,
                        String audioRef,
                        long timestamp,
                        long durationInMillis,
                        AudioType type,
                        MiniUser user,
                        String transcript,
                        String actualPath,
                        float dnr,
                        int resultid,
                        MiniUser.Gender realGender) {
    this.uniqueID = uniqueID;
    this.userid = userid;
    this.exid = exid;
    this.audioRef = audioRef;
    this.timestamp = timestamp;
    this.durationInMillis = durationInMillis;
    this.transcript = transcript;
    this.dnr = dnr;
    this.resultid = resultid;
    this.audioType = type;

    this.setUser(user);
    this.actualPath = actualPath;

    this.realGender = realGender;

    if (type.equals(AudioType.REGULAR)) {
      markRegular();
    } else if (type.equals(AudioType.SLOW)) {
      markSlow();
    } else {
      addAttribute(type.getType(), type.getSpeed());
    }
  }

  public String getAudioRef() {
    return audioRef;
  }

  public AudioAttribute setAudioRef(String audioRef) {
    this.audioRef = audioRef;
    return this;
  }

  public boolean isValid() {
    return audioRef != null && !audioRef.contains(FILE_MISSING);
  }

  public int getExid() {
    return exid;
  }

  /**
   * @param exid
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyAudio
   */
  public void setExid(int exid) {
    this.exid = exid;
  }

  private AudioAttribute markRegular() {
    addAttribute(SPEED, REGULAR);
    return this;
  }

  private AudioAttribute markSlow() {
    addAttribute(SPEED, SLOW);
    return this;
  }

  public boolean isRegularSpeed() {
    return audioType == AudioType.REGULAR || matches(SPEED, REGULAR) || audioType.equals(AudioType.CONTEXT_REGULAR);
  }

  public boolean isSlow() {
    return audioType == AudioType.SLOW || matches(SPEED, SLOW) || audioType.equals(AudioType.CONTEXT_SLOW);
  }

  public AudioType getAudioType() {
    return audioType;
  }

  public void setAudioType(AudioType audioType) {
    this.audioType = audioType;
  }

  public boolean isMale() {
    return user != null && user.isMale();
  }

  public String getSpeed() {
    String s = getAttributes().get(SPEED);
    return s == null ? isRegularSpeed() ? REGULAR : SLOW : s;
  }

  public boolean matches(String name, String value) {
    return attributes.containsKey(name) && attributes.get(name).equals(value);
  }

  /**
   * Or potentially we'd want to look at an attribute - context = true?
   *
   * @return
   */
  public boolean isContextAudio() {
    return audioType.isContext();
  }

  private void addAttribute(String name, String value) {
    attributes.put(name, value);
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  Set<String> getAttributeKeys() {
    return attributes.keySet();
  }

  /**
   * Why use this as a key??
   *
   * @return
   */
  public String getKey() {
    return "user=" + userid + ", " + getAttributes().toString();
  }

  public MiniUser getUser() {
    return user;
  }

  /**
   * @param user
   * @see mitll.langtest.client.qc.QCNPFExercise#getGenderGroup
   */
  public void setUser(MiniUser user) {
    this.user = user;
  }

  public int getUserid() {
    return userid;
  }

  /**
   * @return
   * @see mitll.langtest.client.qc.QCNPFExercise#addTabsForUsers
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#addTabsForUsers
   */
  public boolean isHasBeenPlayed() {
    return hasBeenPlayed;
  }

  /**
   * @param hasBeenPlayed
   * @seex EventDAO#addPlayedMarkings
   */
  public void setHasBeenPlayed(boolean hasBeenPlayed) {
    this.hasBeenPlayed = hasBeenPlayed;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getDurationInMillis() {
    return durationInMillis;
  }

  public void setDurationInMillis(long durationInMillis) {
    this.durationInMillis = durationInMillis;
  }

  public int getUniqueID() {
    return uniqueID;
  }

  public String getTranscript() {
    return transcript == null ? "" : transcript;
  }

  /**
   * @param transcript
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyAudio
   */
  public void setTranscript(String transcript) {
    this.transcript = transcript;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyAudio
   */
  public String getOldexid() {
    return oldexid;
  }

  /**
   * @param oldexid
   * @see mitll.langtest.server.database.audio.AudioDAO#getResultsForQuery
   */
  public void setOldexid(String oldexid) {
    this.oldexid = oldexid;
  }

  /**
   * @return
   * @seex mitll.langtest.server.database.audio.BaseAudioDAO#addOrUpdateUser(int, int, AudioAttribute)
   * @deprecated - do we ever set this properly???
   */
  public String getActualPath() {
    return actualPath;
  }

  public float getDnr() {
    return dnr;
  }

  public int getResultid() {
    return resultid;
  }

  /**
   * @return
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#makeFirstRow
   */
  public AlignmentOutput getAlignmentOutput() {
    return alignmentOutput;
  }

  /**
   * @param alignmentOutput
   * @seex mitll.langtest.server.services.ExerciseServiceImpl#setAlignmentInfo
   */
  public void setAlignmentOutput(AlignmentOutput alignmentOutput) {
    this.alignmentOutput = alignmentOutput;
  }

  public MiniUser.Gender getRealGender() {
    return realGender;
  }

  @Override
  public String toString() {
    return "Audio" +
        "\n\tid         " + uniqueID +
        "\n\texid       " + exid +
        (getOldexid() == null ? "" : " (old ex " + getOldexid() + ") :") +
        "\n\tpath       " + audioRef +
        "\n\tattrs      " + attributes +
        "\n\tby         " + userid + "/" + user +
        "\n\ttranscript '" + transcript + "'" +
        "\n\tdnr        " + dnr;
  }
}
