package mitll.langtest.server.audio;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 8/24/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProcessRunner {
  private static final Logger logger = Logger.getLogger(ProcessRunner.class);

  private static final boolean SHOW_OUTPUT = false;

  public void runProcess(ProcessBuilder shellProc) throws IOException {
    //logger.debug(new Date() + " : proc " + shellProc.command() + " started...");

    shellProc.redirectErrorStream(true);
    Process process2 = shellProc.start();

    // read the output
    InputStream stdout = process2.getInputStream();
    readFromStream(stdout, SHOW_OUTPUT);
    InputStream errorStream = process2.getErrorStream();
    readFromStream(errorStream, true);

    process2.destroy();
  //  System.out.println(new Date() + " : proc " + shellProc.command() + " finished");
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
}
