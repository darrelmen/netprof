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
  private Map<String,String> attributes;
  private List<String> annotations;

  public AudioAttribute() {}

  public AudioAttribute(String audioRef) {
    this.audioRef = audioRef;
    markFast();
  }

  public AudioAttribute markSlow() { addAttribute("speed","slow"); return this; }
  public AudioAttribute markFast() { addAttribute("speed","regular"); return this;}
  public AudioAttribute markMale() { addAttribute("gender","male"); return this;}
  public AudioAttribute markFemale() { addAttribute("gender","female"); return this;}

  public boolean isFast() { return matches("speed","regular");}
  public boolean isSlow() { return matches("speed","slow");}
  public boolean isMale() { return matches("gender","male");}
  public boolean isFemale() { return matches("gender","female");}

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

  @Override
  public String toString() {
    return "Audio " + audioRef + " attrs " + attributes;
  }
}
