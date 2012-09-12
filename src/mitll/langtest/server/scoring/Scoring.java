package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import mitll.langtest.shared.scoring.NetPronImageType;
import pronz.dirs.Dirs;
import pronz.speech.ASRParameters;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 9/12/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class Scoring {
  private static final String WINDOWS_CONFIGURATIONS = "windowsConfig";
  private static final String LINUX_CONFIGURATIONS = "mtexConfig";

  protected Dirs dirs;
  private static final float MIN_AUDIO_SECONDS = 0.3f;
  private static final float MAX_AUDIO_SECONDS = 15.0f;

  protected String scoringDir;
  protected String os;
  protected String configFullPath;

  public Scoring(String deployPath) {
    String property = System.getProperty("os.name").toLowerCase();
    this.os = property.contains("win") ? "win32" : property
        .contains("mac") ? "macos"
        : property.contains("linux") ? System
        .getProperty("os.arch").contains("64") ? "linux64"
        : "linux" : "linux";

    scoringDir = deployPath + File.separator + "scoring";

    configFullPath = scoringDir + File.separator + (os.equals("win32") ? WINDOWS_CONFIGURATIONS : LINUX_CONFIGURATIONS);   // TODO point at os specific config file

    dirs = pronz.dirs.Dirs$.MODULE$.apply(scoringDir + File.separator
        + "tmp", "", scoringDir, new Log(null, true));
  }

  protected Map<NetPronImageType, String> writeTranscripts(String imageOutDir, int imageWidth, int imageHeight, String noSuffix) {
    String pathname = noSuffix + ".wav";

    // These may not all exist. The speech file is created only by multisv
    // right now. writeTranscriptImages() ignores missing files.
    String phoneLabFile = noSuffix + ".phones.lab";
    String speechLabFile = noSuffix + ".speech.lab";
    String wordLabFile = noSuffix + ".words.lab";
    Map<ImageType, String> typeToFile = new HashMap<ImageType, String>();

    if (new File(phoneLabFile).exists()) typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLabFile);
    if (new File(wordLabFile).exists()) typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLabFile);
    if (new File(speechLabFile).exists()) {
      typeToFile.put(ImageType.SPEECH_TRANSCRIPT, speechLabFile);
      System.out.println("found " + new File(speechLabFile).getAbsolutePath());
    }

    Map<ImageType, String> typeToImageFile = new ImageWriter().writeTranscripts(pathname, imageOutDir, imageWidth, imageHeight, typeToFile);
    Map<NetPronImageType, String> sTypeToImage = new HashMap<NetPronImageType, String>();
    for (Map.Entry<ImageType, String> kv : typeToImageFile.entrySet()) {
      String name = kv.getKey().toString();
      NetPronImageType key = NetPronImageType.valueOf(name);
      //System.out.println("key is " + name + "/" + key + " -> " +kv.getValue());
      sTypeToImage.put(key, kv.getValue());
    }
    return sTypeToImage;
  }
}
