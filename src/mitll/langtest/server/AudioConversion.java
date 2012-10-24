package mitll.langtest.server;

import audio.imagewriter.AudioConverter;
import audio.tools.FileCopier;
import mitll.langtest.client.LangTestDatabase;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Does mp3 conversion using a shell call to lame.
 *  Uses ffmpeg to convert to webm format.
 *
 * User: go22670
 * Date: 8/22/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioConversion {
  private static Logger logger = Logger.getLogger(AudioConversion.class);
  private static final String LAME_PATH_WINDOWS = "C:\\Users\\go22670\\lame\\lame.exe";
  private static final String LAME_PATH_LINUX = "/usr/local/bin/lame";

  private static final String FFMPEG_PATH_WINDOWS = "C:\\Users\\go22670\\ffmpeg\\bin\\ffmpeg.exe";
  private static final String FFMPEG_PATH_LINUX = "/usr/local/bin/ffmpeg";

  public static final String LINUX_SOX_BIN_DIR = "/usr/local/bin";
  public static final String WINDOWS_SOX_BIN_DIR = "C:\\Users\\go22670\\sox-14-3-2";
  public static final String SIXTEEN_K_SUFFIX = "_16K";

  private AudioCheck audioCheck = new AudioCheck();

  /**
   * Also writes an MP3 file equivalent.
   *
   * @param base64EncodedString audio bytes from the client
   * @param file where we want to write the wav file to
   * @return true if audio is valid (not too short, not silence)
   */
  public boolean convertBase64ToAudioFiles(String base64EncodedString, File file) {
    File parentFile = file.getParentFile();
    // System.out.println("making dir " + parentFile.getAbsolutePath());

    parentFile.mkdirs();

    byte[] byteArray = getBytesFromBase64String(base64EncodedString);

    writeToFile(byteArray, file);

    if (!file.exists()) {
      System.err.println("writeAudioFile : huh? can't find " + file.getAbsolutePath());
    }
    boolean valid = isValid(file);

    AudioConversion audioConversion = new AudioConversion();
    audioConversion.writeMP3(file.getAbsolutePath());
    if (LangTestDatabase.WRITE_ALTERNATE_COMPRESSED_AUDIO) {
      audioConversion.writeCompressed(file.getAbsolutePath());
    }
    return valid;
  }

  /**
   * Decode Base64 string
   *
   * @param base64EncodedByteArray
   * @return
   */
  private byte[] getBytesFromBase64String(String base64EncodedByteArray) {
    Base64 decoder = new Base64();
    byte[] decoded = null;
    //System.out.println("postArray : got " + base64EncodedByteArray.substring(0,Math.min(base64EncodedByteArray.length(), 20)) +"...");
    // decoded = (byte[])decoder.decode(base64EncodedByteArray);

    try {
      decoded = (byte[]) decoder.decode(base64EncodedByteArray);
    } catch (Exception e1) {   // just b/c eclipse seems to insist
      e1.printStackTrace();
    }
    return decoded;
  }

  private void writeToFile(byte[] byteArray, File file) {
    try {
      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
      outputStream.write(byteArray);
      outputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean isValid(File file) {
    try {
      return audioCheck.checkWavFile(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Checks if the sample rate is not 16K (as required by things like sv).
   * If not, then uses sox to make an audio file with the right sample rate.
   *
   * @param testAudioDir directory for audio
   * @param testAudioFileNoSuffix name without suffix
   * @return
   */
  public String convertTo16Khz(String testAudioDir, String testAudioFileNoSuffix) throws UnsupportedAudioFileException {
    String pathname = testAudioDir + File.separator + testAudioFileNoSuffix + ".wav";
    File wavFile = new File(pathname);

    wavFile = convertTo16Khz(wavFile);
    String name1 = wavFile.getName();
    return removeSuffix(name1);
  }

  public File convertTo16Khz(File wavFile) throws UnsupportedAudioFileException {
    //String pathname = testAudioDir + File.separator + testAudioFileNoSuffix + ".wav";
    //File wavFile = new File(pathname);
    if (!wavFile.exists()) {
      System.err.println("convertTo16Khz " + wavFile + " doesn't exist");
      return wavFile;
    }
    try {
      AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(wavFile);
      float sampleRate = audioFileFormat.getFormat().getSampleRate();
      if (sampleRate != 16000f) {
        String binPath = WINDOWS_SOX_BIN_DIR;
        if (! new File(binPath).exists()) binPath = LINUX_SOX_BIN_DIR;
        String s = new AudioConverter().convertTo16KHZ(binPath, wavFile.getAbsolutePath());
        String name1 = wavFile.getName();
        String sampled = wavFile.getParent() + File.separator + removeSuffix(name1) + SIXTEEN_K_SUFFIX + ".wav";
        if (new FileCopier().copy(s, sampled)) {
          wavFile = new File(sampled);
        }
      }
    } /*catch (UnsupportedAudioFileException e) {
     // e.printStackTrace();
      throw e;
    }*/ catch (IOException e) {
      e.printStackTrace();
    }
    return wavFile;
  }

  private String removeSuffix(String name1) {
    return name1.substring(0,name1.length()-4);
  }

  /**
   * @see LangTestDatabaseImpl#ensureWriteMP3(String)
   * @see LangTestDatabaseImpl#writeAudioFile(String, String, String, String, String)
   * @param pathToWav
   */
  private void writeOGG(String pathToWav) {
    String mp3File = pathToWav.replace(".wav",".ogg");
    String exePath = FFMPEG_PATH_WINDOWS;    // Windows
    if (!new File(exePath).exists()) {
      exePath = FFMPEG_PATH_LINUX;
    }
    if (!new File(exePath).exists()) {
      System.err.println("no lame installed at " + exePath + " or " +FFMPEG_PATH_WINDOWS);
    }

/*    System.out.println("using " +exePath +" audio :'" +pathToWav +
        "' mp3 '" +mp3File+
        "'");*/
    writeWithFFMPEG(exePath, pathToWav, mp3File);
  }

  /**
   * Writes a WEBM file.  This is an open source format supported by Firefox (which doesn't handle mp3).
   *
   * @see LangTestDatabaseImpl#ensureWriteMP3(String)
   * @see LangTestDatabaseImpl#writeAudioFile(String, String, String, String, String)
   * @param path
   */
  public boolean writeCompressed(String path) {
    String outputFile = path.replace(".wav", ".webm");
    File ogg = new File(outputFile);
    return ogg.exists() || writeWEBMM(path);
  }

  private boolean writeWEBMM(String pathToWav) {
    String mp3File = pathToWav.replace(".wav",".webm");
    String exePath = FFMPEG_PATH_WINDOWS;    // Windows
    if (!new File(exePath).exists()) {
      exePath = FFMPEG_PATH_LINUX;
    }
    if (!new File(exePath).exists()) {
      System.err.println("no lame installed at " + exePath + " or " +FFMPEG_PATH_WINDOWS);
    }

/*    System.out.println("using " +exePath +" audio :'" +pathToWav +
        "' mp3 '" +mp3File+
        "'");*/
    return writeWithFFMPEG(exePath, pathToWav, mp3File);
  }

  public void ensureWriteMP3(String pathToWav,  String realContextPath) {
    File absolutePathToWav = getAbsoluteFile(pathToWav,realContextPath);

    String mp3File = absolutePathToWav.getAbsolutePath().replace(".wav",".mp3");
    File mp3 = new File(mp3File);
    if (!mp3.exists()) {
      writeMP3(absolutePathToWav.getAbsolutePath());
    }
  }

  private File getAbsoluteFile(String filePath,  String realContextPath) {
    return getAbsolute(filePath, realContextPath);
  }

  private File getAbsolute(String filePath, String realContextPath) {
    File file = new File(realContextPath, filePath);
    assert(file.exists());
    return file;
  }

  /**
   * Use lame to write an mp3 file.
   * @param pathToWav
   */
  public void writeMP3(String pathToWav) {
    String mp3File = pathToWav.replace(".wav",".mp3");
    String lamePath = LAME_PATH_WINDOWS;    // Windows
    if (!new File(lamePath).exists()) {
      lamePath = LAME_PATH_LINUX;
    }
    if (!new File(lamePath).exists()) {
      System.err.println("no lame installed at " + lamePath + " or " +LAME_PATH_WINDOWS);
    }

/*    System.out.println("using " +lamePath +" audio :'" +pathToWav +
        "' mp3 '" +mp3File+
        "'");*/
    writeMP3(lamePath, pathToWav, mp3File);
  }

  public File convertMP3ToWav(String pathToWav) {
    assert(pathToWav.endsWith(".mp3"));
    String mp3File = pathToWav.replace(".mp3",".wav");
    String lamePath = LAME_PATH_WINDOWS;    // Windows
    if (!new File(lamePath).exists()) {
      lamePath = LAME_PATH_LINUX;
    }
    if (!new File(lamePath).exists()) {
      System.err.println("no lame installed at " + lamePath + " or " +LAME_PATH_WINDOWS);
    }


    File file = writeWavFromMP3(lamePath, pathToWav, mp3File);
/*    System.out.println("convertMP3ToWav using " +lamePath +" audio :'" +pathToWav +
        "' out '" +mp3File+
        "' = '" +file.getName()+
        "'");*/
    return file;
  }

  private File writeWavFromMP3(String lamePath, String pathToAudioFile, String mp3File) {
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, "--decode", pathToAudioFile, mp3File);
    try {
      //    System.out.println("writeMP3 running lame" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
      //     System.out.println("writeMP3 exited  lame" + lameProc);
    } catch (IOException e) {
      System.err.println("Couldn't run " + lameProc);
      e.printStackTrace();
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      //  System.err.println("didn't write MP3 : " + testMP3.getAbsolutePath());
    } else {
      //    System.out.println("Wrote to " + testMP3 + " length " + testMP3.getTotalSpace());
    }
    return testMP3;
  }

  private File writeMP3(String lamePath, String pathToAudioFile, String mp3File) {
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, pathToAudioFile, mp3File);
    try {
      logger.info("running lame" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
      //     System.out.println("writeMP3 exited  lame" + lameProc);
    } catch (IOException e) {
      System.err.println("Couldn't run " + lameProc);
      e.printStackTrace();
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
    //  System.err.println("didn't write MP3 : " + testMP3.getAbsolutePath());
    } else {
  //    System.out.println("Wrote to " + testMP3 + " length " + testMP3.getTotalSpace());
    }
    return testMP3;
  }

  private boolean writeWithFFMPEG(String path, String pathToAudioFile, String mp3File) {
    ProcessBuilder lameProc = new ProcessBuilder(path, "-i", pathToAudioFile, mp3File);
    try {
      //System.out.println("writeWithFFMPEG running ffmpeg" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
      //System.out.println("writeWithFFMPEG exited  ffmpeg" + lameProc);
    } catch (IOException e) {
      System.err.println("Couldn't run " + lameProc);
      e.printStackTrace();
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      System.err.println("didn't write output file : " + testMP3.getAbsolutePath());
      return false;
    } else {
      //   System.out.println("Wrote to " + testMP3);
      return true;
    }
  }

  public static void main(String[] arg) {
    //new AudioConversion().writeMP3("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-001\\1\\subject-460\\answer_1345134729569.wav");
    new AudioConversion().writeCompressed("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-001\\1\\subject-460\\answer_1345134729569.wav");

  }
}
