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

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.UserAndTime;

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
  private static final String FILE_MISSING = "FILE_MISSING";

  private static final String SPEED = "speed";
  public static final String SLOW = "slow";
  public static final String REGULAR = "regular";
  public static final String REGULAR_AND_SLOW = "regular and slow";
  private static final String CONTEXT = "context";
  private static final String UNKNOWN = "unknown";

  /**
   * TODO : if every have slow recordings of context audio we'll need to add another type or an enum
   */
  public static final String CONTEXT_AUDIO_TYPE = "context=" + Result.AUDIO_TYPE_REGULAR;

  private MiniUser user;

  private int uniqueID;
  private String audioRef;
  private String exid;
  private long userid;
  private long timestamp;
  private long durationInMillis;
  private transient float dnr;

  /**
   * Don't send to client - just server side
   */
  private transient String transcript = "";

  // 9/24/14 : setting it here may stop intermittent gwt rpc exceptions
  private Map<String, String> attributes = new HashMap<String, String>();
  private boolean hasBeenPlayed;

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
   * @see mitll.langtest.server.database.AudioDAO#getAudioAttribute
   * @see mitll.langtest.server.database.AudioDAO#getResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   */
  public AudioAttribute(int uniqueID, long userid,
                        String exid,
                        String audioRef,
                        long timestamp, long durationInMillis, String type, MiniUser user, String transcript,
                        float dnr) {
    this.uniqueID = uniqueID;
    this.userid = userid;
    this.exid = exid;
    this.audioRef = audioRef;
    this.timestamp = timestamp;
    this.durationInMillis = durationInMillis;
    this.transcript = transcript;
    this.dnr = dnr;

    this.setUser(user);
    if (type.equals(Result.AUDIO_TYPE_REGULAR)) markRegular();
    else if (type.equals(Result.AUDIO_TYPE_SLOW)) markSlow();
    else if (type.equals(Result.AUDIO_TYPE_FAST_AND_SLOW)) {
      addAttribute(SPEED, REGULAR_AND_SLOW);
    } else if (type.contains("=")) { // e.g. context=regular or context=slow - or any key-value pair
      String[] split = type.split("=");
      addAttribute(split[0], split[1]);
    }
  }

  /**
   * @param audioRef
   * @see mitll.langtest.shared.exercise.AudioExercise#setRefAudio(String)
   */
  protected AudioAttribute(String audioRef) {
    this.audioRef = audioRef;
    if (audioRef == null) throw new IllegalArgumentException("huh audio ref is null?");
    markRegular();
  }

  public AudioAttribute(String audioRef, MiniUser miniUser) {
    this(audioRef);
    this.setUser(miniUser);
    this.userid = miniUser.getId();
  }

  public boolean isValid() {
    return audioRef != null && !audioRef.contains(FILE_MISSING);
  }

  public String getExid() {
    return exid;
  }

  @Override
  public String getID() {
    return exid + "/1";
  }

  public void setAudioRef(String audioRef) {
    this.audioRef = audioRef;
  }

  public AudioAttribute markSlow() {
    addAttribute(SPEED, SLOW);
    return this;
  }

  public AudioAttribute markRegular() {
    addAttribute(SPEED, REGULAR);
    return this;
  }

  public boolean isSlow() {
    return matches(SPEED, SLOW);
  }

  public String getAudioType() {
    String speed = getSpeed();
    if (speed == null && !attributes.isEmpty()) {
      String s = attributes.toString();
      return s.substring(1, s.length() - 1);
    } else {
      return speed == null ? UNKNOWN : speed;
    }
  }

  public boolean isMale() {
    return user != null && user.isMale();
  }

  public boolean isFemale() {
    return !isMale();
  }

  public boolean isRegularSpeed() {
    String speed = getSpeed();
    return speed != null && speed.equalsIgnoreCase(REGULAR);
  }

  public String getSpeed() {
    return getAttributes().get(SPEED);
  }

  public boolean hasOnlySpeed() {
    return attributes.size() == 1 && attributes.containsKey(SPEED);
  }

  public boolean matches(String name, String value) {
    return attributes.containsKey(name) && attributes.get(name).equals(value);
  }

  public boolean isContextAudio() {
    String audioType = getAudioType();
    return audioType != null && audioType.startsWith(CONTEXT);
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#filterByUnrecorded
   */
  public boolean isExampleSentence() {
    return attributes.containsKey(CONTEXT);
  }

  public void addAttribute(String name, String value) {
    if (attributes.containsKey(name)) {
      String s = attributes.get(name);
      if (!s.equals(REGULAR)) System.out.println("replacing value at " + name + " was " + s + " now " + value);
    }
    attributes.put(name, value);
  }

  public String getAudioRef() {
    return audioRef;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public Set<String> getAttributeKeys() {
    return attributes.keySet();
  }

  public String getKey() {
    return "user=" + userid + ", " + getAttributes().toString();
  }

  public String getDisplay() {
    if (hasOnlySpeed()) {
      String speed = attributes.values().iterator().next();
      return speed.substring(0, 1).toUpperCase() + speed.substring(1);
    } else {

      StringBuilder stringBuilder = new StringBuilder();
      for (Map.Entry<String, String> pair : attributes.entrySet()) {
        String key = pair.getKey();
        stringBuilder.append(key.substring(0, 1).toUpperCase() + key.substring(1));

        String value = pair.getValue();
        stringBuilder.append(" : " + value.substring(0, 1).toUpperCase() + value.substring(1));
        stringBuilder.append(", ");
      }
      String s = stringBuilder.toString();
      if (s.endsWith(", ")) s = s.substring(0, s.length() - 2);
      return s;
    }
  }

  public MiniUser getUser() {
    return user;
  }

  /**
   * @param user
   * @see mitll.langtest.client.qc.QCNPFExercise#getGenderGroup(mitll.langtest.client.custom.tabs.RememberTabAndContent, AudioAttribute, com.github.gwtbootstrap.client.ui.Button, java.util.List)
   */
  public void setUser(MiniUser user) {
    this.user = user;
  }

  public long getUserid() {
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#addPlayedMarkings(long, CommonExercise)
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

  public int getUniqueID() {
    return uniqueID;
  }

  public void setExid(String exid) {
    this.exid = exid;
  }

  /**
   * Check to see if the audio transcript matches the vocabulary item.
   * Don't worry about punctuation or case.
   *
   * @param foreignLanguage
   * @return
   */
/*  public boolean hasMatchingTranscript(String foreignLanguage) {
    try {

//      String before = foreignLanguage;
//      String fixedAgainst = StringUtils.stripAccents(before);

//      if (!before.equals(fixedAgainst)) {
//        logger.info("attachAudio before '" + before +
//            "' after '" + fixedAgainst +
//            "'");
//      }

      return matchTranscript(foreignLanguage);
    } catch (Exception e) {
      return true;
    }
  }*/

/*  private boolean matchTranscript(String foreignLanguage) {
    String transcript = this.transcript;
    return matchTranscript(foreignLanguage, transcript);
  }*/

  /**
   * @param foreignLanguage
   * @param transcript
   * @return
   * @see
   */
  public boolean matchTranscript(String foreignLanguage, String transcript) {
    return transcript == null ||
        foreignLanguage.isEmpty() ||
        transcript.isEmpty() ||
        removePunct(transcript).toLowerCase().equals(removePunct(foreignLanguage).toLowerCase());
  }

  private String removePunct(String t) {
    return t.replaceAll("\\p{P}", "").replaceAll("\\s++", "");
  }

  public String getTranscript() {
    return transcript == null ? "" : transcript;
  }

  public void setTranscript(String transcript) {
    this.transcript = transcript;
  }

  @Override
  public String toString() {
    return "Audio id " + uniqueID +
        " : " + audioRef +
        " attrs " + attributes +
        " by " + userid + "/" + user +
        " transcript '" + transcript +
        "'\n\tdnr\t" + dnr;
  }

  public float getDnr() {
    return dnr;
  }
}
