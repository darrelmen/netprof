/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * What the client wants to know about a reference audio cut.
 * <p/>
 * Includes info about who recorded it and when, for which exercise, and the path to the audio on the server.
 * <p/>
 * Also indicates any attributes REGULAR or SLOW and whether it's been played by a reviewer.
 * User: GO22670
 * Date: 12/6/13
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioAttribute implements IsSerializable, UserAndTime {
  private static final String FILE_MISSING = "FILE_MISSING";

  private static final String SPEED = "speed";
  public static final String SLOW = "slow";
  public static final String REGULAR = "regular";
  public static final String REGULAR_AND_SLOW = "regular and slow";
  public static final String CONTEXT = "context";

  private MiniUser user;

  private int uniqueID;
  private String audioRef;
  private String exid;
  private long userid;
  private long timestamp;
  private long durationInMillis;

  // 9/24/14 : setting it here may stop intermittent gwt rpc exceptions
  private Map<String, String> attributes = new HashMap<String, String>();
  private boolean hasBeenPlayed;

  public AudioAttribute() {}

  /**
   * @see mitll.langtest.server.database.AudioDAO#getAudioAttribute
   * @see mitll.langtest.server.database.AudioDAO#getResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   * @param uniqueID
   * @param userid
   * @param exid
   * @param audioRef
   * @param timestamp
   * @param durationInMillis
   * @param type
   * @param user
   */
  public AudioAttribute(int uniqueID, long userid,
                        String exid,
                        String audioRef,
                        long timestamp, long durationInMillis, String type, MiniUser user) {
    this.uniqueID = uniqueID;
    this.userid = userid;
    this.exid = exid;
    this.audioRef = audioRef;
    this.timestamp = timestamp;
    this.durationInMillis = durationInMillis;
    this.setUser(user);
    if (type.equals(Result.AUDIO_TYPE_REGULAR)) markRegular();
    else if (type.equals(Result.AUDIO_TYPE_SLOW)) markSlow();
    else if (type.equals(Result.AUDIO_TYPE_FAST_AND_SLOW)) {
      addAttribute(SPEED, REGULAR_AND_SLOW);
    }
    else if (type.contains("=")) { // e.g. context=regular or context=slow - or any key-value pair
      String[] split = type.split("=");
      addAttribute(split[0], split[1]);
    }
  }

  /**
   * @see mitll.langtest.shared.AudioExercise#setRefAudio(String)
   * @param audioRef
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
      return s.substring(1,s.length()-1);
    }
    else {
      return speed;
    }
  }

  public boolean isMale() { return user != null && user.isMale();  }
  public boolean isFemale() {
    return !isMale();
  }

  public boolean isRegularSpeed() {
    String speed = getSpeed();
    return speed != null && speed.equalsIgnoreCase(REGULAR);
  }

  public String getSpeed() { return getAttributes().get(SPEED); }

  public boolean hasOnlySpeed() {
    return attributes.size() == 1 && attributes.containsKey(SPEED);
  }

  public boolean matches(String name, String value) {
    return attributes.containsKey(name) && attributes.get(name).equals(value);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#filterByUnrecorded
   * @return
   */
  public boolean isExampleSentence() { return attributes.containsKey(CONTEXT);  }

  public void addAttribute(String name, String value) {
    if (attributes.containsKey(name)) {
      String s = attributes.get(name);
      if (!s.equals(REGULAR)) System.out.println("replacing value at " + name + " was " + s + " now " + value);
    }
    attributes.put(name, value);
  }

  public String getAudioRef() { return audioRef;  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public Set<String> getAttributeKeys() { return attributes.keySet(); }

  public String getKey() { return "user="+userid+", "+getAttributes().toString(); }

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

  public MiniUser getUser() { return user; }

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#getGenderGroup(mitll.langtest.client.custom.tabs.RememberTabAndContent, AudioAttribute, com.github.gwtbootstrap.client.ui.Button, java.util.List)
   * @param user
   */
  public void setUser(MiniUser user) { this.user = user;  }

  public long getUserid() { return userid; }

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#addTabsForUsers(CommonExercise, com.github.gwtbootstrap.client.ui.TabPanel, java.util.Map, java.util.List)
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#addTabsForUsers(CommonExercise, com.github.gwtbootstrap.client.ui.TabPanel, java.util.Map, java.util.List)
   * @return
   */
  public boolean isHasBeenPlayed() {
    return hasBeenPlayed;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addPlayedMarkings(long, CommonExercise)
   * @param hasBeenPlayed
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
  public int getUniqueID() { return uniqueID;  }
  public void setExid(String exid) {  this.exid = exid;  }

  @Override
  public String toString() {
    return "Audio id " +uniqueID + " : " + audioRef + " attrs " + attributes + " by " + userid +"/"+user;
  }
}
