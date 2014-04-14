package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/6/13
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioAttribute implements IsSerializable {
  private static final String SPEED = "speed";
  private static final String SLOW = "slow";
  private static final String REGULAR = "regular";

/*  private static final String GENDER = "gender";
  private static final String MALE = "male";
  private static final String FEMALE = "female";*/
  private MiniUser user;

  private String audioRef;
  private String exid;
  private long userid;
  private long timestamp;
  private long duration;
  private Map<String, String> attributes;
 // private List<String> annotations;
  //private long userID; // who recorded it - later
  private boolean hasBeenPlayed;

  public AudioAttribute() {}

  public AudioAttribute(long userid,
                        String exid,
                        String audioRef,
                        long timestamp, long duration, String type, MiniUser user) {
    this.userid = userid;
    this.exid = exid;
    this.audioRef = audioRef;
    this.timestamp = timestamp;
    this.duration = duration;
    this.user = user;
    if (type.equals(Result.AUDIO_TYPE_REGULAR)) markRegular();
    else if (type.equals(Result.AUDIO_TYPE_SLOW)) markSlow();
  }

  public AudioAttribute(String audioRef) {
    this.audioRef = audioRef;
    if (audioRef == null) throw new IllegalArgumentException("huh audio ref is null?");
    markRegular();
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

/*
  public AudioAttribute markMale() {
    addAttribute(GENDER, MALE);
    return this;
  }

  public AudioAttribute markFemale() {
    addAttribute(GENDER, FEMALE);
    return this;
  }
*/

  public boolean isFast() {
    return matches(SPEED, REGULAR);
  }

  public boolean isSlow() {
    return matches(SPEED, SLOW);
  }

  public boolean isMale() {

    //return matches(GENDER, MALE);
   return user.isMale();
  }

  public boolean isFemale() {
    return !isMale();
  }

  public boolean isRegularSpeed() {
    String speed = getAttributes().get(SPEED);
    return speed != null && speed.equalsIgnoreCase(REGULAR);
  }

  public boolean hasOnlySpeed() {
    return attributes.size() == 1 && attributes.containsKey(SPEED);
  }

  public boolean matches(String name, String value) {
    return attributes.containsKey(name) && attributes.get(name).equals(value);
  }

  public void addAttribute(String name, String value) {
    if (attributes == null) attributes = new HashMap<String, String>();
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

  public MiniUser getUser() {
    return user;
  }

  public boolean isHasBeenPlayed() {
    return hasBeenPlayed;
  }

  public void setHasBeenPlayed(boolean hasBeenPlayed) {
    this.hasBeenPlayed = hasBeenPlayed;
  }

  @Override
  public String toString() {
    return "Audio " + audioRef + " attrs " + attributes;
  }
}
