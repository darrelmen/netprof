package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/6/13
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioAttribute implements IsSerializable {
  private String audioRef;
  private Map<String, String> attributes;
  private List<String> annotations;
  //private long userID; // who recorded it - later

  public AudioAttribute() {
  }

  public AudioAttribute(String audioRef) {
    this.audioRef = audioRef;
    if (audioRef == null) throw new IllegalArgumentException("huh audio ref is null?");
    markFast();
  }

  public AudioAttribute markSlow() {
    addAttribute("speed", "slow");
    return this;
  }

  public AudioAttribute markFast() {
    addAttribute("speed", "regular");
    return this;
  }

  public AudioAttribute markMale() {
    addAttribute("gender", "male");
    return this;
  }

  public AudioAttribute markFemale() {
    addAttribute("gender", "female");
    return this;
  }

  public boolean isFast() {
    return matches("speed", "regular");
  }

  public boolean isSlow() {
    return matches("speed", "slow");
  }

  public boolean isMale() {
    return matches("gender", "male");
  }

  public boolean isFemale() {
    return matches("gender", "female");
  }

  public boolean isRegularSpeed() {
    String speed = getAttributes().get("speed");
    return speed != null && speed.equalsIgnoreCase("regular");
  }

  public boolean hasOnlySpeed() {
    return attributes.size() == 1 && attributes.containsKey("speed");
  }

  public boolean matches(String name, String value) {
    return attributes.containsKey(name) && attributes.get(name).equals(value);
  }

  public void addAttribute(String name, String value) {
    if (attributes == null) attributes = new HashMap<String, String>();
    if (attributes.containsKey(name)) {
      String s = attributes.get(name);
      if (!s.equals("regular")) System.out.println("replacing value at " + name + " was " + s + " now " + value);
    }
    attributes.put(name, value);
  }

  public String getAudioRef() {
    return audioRef;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public List<String> getAnnotations() {
    return annotations;
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

  @Override
  public String toString() {
    return "Audio " + audioRef + " attrs " + attributes;
  }
}
