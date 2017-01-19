package mitll.langtest.server.audio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class TrimmerAIS extends AudioInputStream {
  private final AudioInputStream stream;
  private long startByte;
  private final long endByte;
  private long t_bytesRead = 0;

  public TrimmerAIS(AudioFormat audioFormat, AudioInputStream audioInputStream, long startMilli, long endMilli) {
    super(new ByteArrayInputStream(new byte[0]), audioFormat, audioInputStream.getFrameLength());

    System.out.println("\tTrimmerAIS " + audioFormat);

    stream = audioInputStream;
    //calculate where to start and where to end
    startByte = (long) ((startMilli / 1000) * stream.getFormat().getFrameRate() * stream.getFormat().getFrameSize());

    System.out.println("start " + startMilli + " start " + startByte);

    try {
      long skip = stream.skip(startByte);
      startByte = 0;
      System.out.println("skip " + skip + " start " + startByte + " t_bytesRead " + t_bytesRead);

      t_bytesRead += skip;
      System.out.println("skip " + skip + " start " + startByte + " t_bytesRead " + t_bytesRead);

    } catch (IOException e) {
      e.printStackTrace();
    }
    endByte = (long) ((endMilli / 1000) * stream.getFormat().getFrameRate() * stream.getFormat().getFrameSize());


    try {
      System.out.println("\tread frame pos " + framePos + " size " + frameSize + " length " + getFrameLength() + " avail " + available()+ " t_bytesRead " + t_bytesRead);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public int available() throws IOException {
    return (int) (endByte - startByte - t_bytesRead);
  }

  public int read(byte[] abData, int nOffset, int nLength) throws IOException {
//    System.out.println("read " + nOffset + " nLength " + nLength);
    int bytesRead = 0;

    int c = 0;
    /*if (t_bytesRead < startByte) {
      System.out.println("\tread before " + t_bytesRead + " startByte " + startByte);

      do {
        long chunkToSkip = startByte - t_bytesRead;

        chunkToSkip = Math.min(2048, chunkToSkip);

        long check = chunkToSkip;
        if ((check % frameSize) != 0) {
          check -= (check % frameSize);
          System.out.println("\t\tread check " + check);
        }

        if( frameLength != AudioSystem.NOT_SPECIFIED ) {
          // don't skip more than our set length in frames.
          if( (check/frameSize) > (frameLength-framePos) ) {
            check = (frameLength-framePos) * frameSize;
            System.out.println("\t\tread 2 check " + check);

          }
        }

        long skip = skip(chunkToSkip);
        bytesRead = (int) skip;
        if (c++ < 10) {
          System.out.println("\t\tread check " + check);
          System.out.println("\tread skip " + chunkToSkip + "/" + skip +
              " bytesRead " + bytesRead + " frame pos " + framePos + " size " + frameSize + " length " + getFrameLength() + " " + available());
        }
        t_bytesRead += bytesRead;
      } while (t_bytesRead < startByte);
    }*/
    if (t_bytesRead >= endByte)//end reached. signal EOF
      return -1;

    bytesRead = stream.read(abData, 0, nLength);
    if (bytesRead == -1)
      return -1;
    else if (bytesRead == 0)
      return 0;

    t_bytesRead += bytesRead;
    if (t_bytesRead >= endByte)// "trim" the extra by altering the number of bytes read
      bytesRead = (int) (bytesRead - (t_bytesRead - endByte));

    return bytesRead;
  }

  public static void trimFile(String inputFile, String outputFile, int startMilli, int endMilli) throws UnsupportedAudioFileException, IOException {
    File file = new File(inputFile);
    easyTrimFile(file, outputFile, startMilli, endMilli);
  }

  public static void easyTrimFile(File file, String outputFile, int startMilli, int endMilli) throws UnsupportedAudioFileException, IOException {
    AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
//    int startMilli = 0;
//    int endMilli = 15000;
    AudioInputStream outputStream = new TrimmerAIS(inputStream.getFormat(), inputStream, startMilli, endMilli);
    AudioSystem.write(outputStream, AudioFileFormat.Type.WAVE, new File(outputFile));
  }

  public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
    String inputFile = args[0];
    String outputFile = args[1];

    trimFile(inputFile, outputFile, 0, 15000);
  }
}