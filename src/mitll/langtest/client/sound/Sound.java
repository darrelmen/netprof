/**
 * 
 */
package mitll.langtest.client.sound;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author gregbramble
 *
 */
public class Sound {
	public AudioControl parent;
	public JavaScriptObject sound;
	
	public Sound(AudioControl parent){
		this.parent = parent;
	}
}
