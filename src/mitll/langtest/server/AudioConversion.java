package mitll.langtest.server;

import java.io.File;
import java.io.IOException;

/**
 * Does mp3 conversion using a shell call to lame.
 * Partial attempt at ogg vorbis conversion.
 *
 * User: go22670
 * Date: 8/22/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioConversion {
  private static final String LAME_PATH_WINDOWS = "C:\\Users\\go22670\\lame\\lame.exe";
  private static final String LAME_PATH_LINUX = "/usr/local/bin/lame";

  private static final String FFMPEG_PATH_WINDOWS = "C:\\Users\\go22670\\ffmpeg\\bin\\ffmpeg.exe";
  private static final String FFMPEG_PATH_LINUX = "/usr/local/bin/ffmpeg";

  /**
   * @see LangTestDatabaseImpl#ensureWriteMP3(String)
   * @see LangTestDatabaseImpl#writeAudioFile(String, String, String, String, String)
   * @param pathToWav
   */
  public void writeOGG(String pathToWav) {
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
    writeOGG(exePath, pathToWav, mp3File);
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

  private void writeMP3(String lamePath, String pathToAudioFile, String mp3File) {
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, pathToAudioFile, mp3File);
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
      System.err.println("didn't write MP3 : " + testMP3.getAbsolutePath());
    } else {
  //    System.out.println("Wrote to " + testMP3 + " length " + testMP3.getTotalSpace());
    }
  }

  private void writeOGG(String path, String pathToAudioFile, String mp3File) {
    ProcessBuilder lameProc = new ProcessBuilder(path, "-i", pathToAudioFile, mp3File);
    try {
      System.out.println("writeOGG running ffmpeg" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
      //     System.out.println("writeMP3 exited  lame" + lameProc);
    } catch (IOException e) {
      System.err.println("Couldn't run " + lameProc);
      e.printStackTrace();
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      System.err.println("didn't write OGG : " + testMP3.getAbsolutePath());
    } else {
      //   System.out.println("Wrote to " + testMP3);
    }
  }

  public static void main(String[] arg) {
    //new AudioConversion().writeMP3("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-001\\1\\subject-460\\answer_1345134729569.wav");
    new AudioConversion().writeOGG("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-001\\1\\subject-460\\answer_1345134729569.wav");

  }
}
