package mitll.langtest.server;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;

import java.io.*;

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

  /**
   * Use lame to write an mp3 file.
   * @param pathToWav
   */
  public void writeOGG(String pathToWav) {
    String oggFile = pathToWav.replace(".wav",".ogg");
    File source = new File(pathToWav);
    File target = new File(oggFile);
    AudioAttributes audio = new AudioAttributes();
    audio.setCodec("libvorbis");

    EncodingAttributes attrs = new EncodingAttributes();
 /*   attrs.setFormat("ogg");
    audio.setBitRate(44100);
    audio.setSamplingRate(44100);
    audio.setChannels(new Integer(1));*/
    attrs.setAudioAttributes(audio);
    Encoder encoder = new Encoder();
    try {
      encoder.encode(source, target, attrs);
    } catch (EncoderException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
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
      runProcess(lameProc);
      //     System.out.println("writeMP3 exited  lame" + lameProc);
    } catch (IOException e) {
      System.err.println("Couldn't run " + lameProc);
      e.printStackTrace();
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      System.err.println("didn't write MP3 : " + testMP3.getAbsolutePath());
    } else {
      //   System.out.println("Wrote to " + testMP3);
    }
  }

  private void runProcess(ProcessBuilder shellProc) throws IOException {
    //System.out.println(new Date() + " : proc " + shellProc.command() + " started...");

    shellProc.redirectErrorStream(true);
    Process process2 = shellProc.start();

    // read the output
    InputStream stdout = process2.getInputStream();
    readFromStream(stdout, false);
    InputStream errorStream = process2.getErrorStream();
    readFromStream(errorStream, true);

    process2.destroy();
    //System.out.println(new Date() + " : proc " + shellProc.command() + " finished");
  }

  private void readFromStream(InputStream is2, boolean showOutput) throws IOException {
    InputStreamReader isr2 = new InputStreamReader(is2);
    BufferedReader br2 = new BufferedReader(isr2);
    String line2;
    while ((line2 = br2.readLine()) != null) {
      if (showOutput) System.err.println(line2);
    }
    br2.close();
  }

  public static void main(String[] arg) {
    new AudioConversion().writeMP3("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-001\\1\\subject-460\\answer_1345134729569.wav");
    //new AudioConversion().writeOGG("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-001\\1\\subject-460\\answer_1345134729569.wav");

  }
}
