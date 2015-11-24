/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.audio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 8/24/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProcessRunner {
  // private static final Logger logger = Logger.getLogger(ProcessRunner.class);
  //private static final boolean SHOW_OUTPUT = true;

  public boolean runProcess(ProcessBuilder shellProc) throws IOException {
    return runProcess(shellProc, false);
  }

  public boolean runProcess(ProcessBuilder shellProc, boolean showOutput) throws IOException {
    //logger.debug(new Date() + " : proc " + shellProc.command() + " started...");
    shellProc.redirectErrorStream(true);
    Process process2 = shellProc.start();

    // read the output
    InputStream stdout = process2.getInputStream();
    readFromStream(stdout, showOutput);
    InputStream errorStream = process2.getErrorStream();
    readFromStream(errorStream, true);

    try {
      int i = process2.waitFor();
      if (i != 0) {
        //  log.warning("got exit status " + i + " from " + shellProc.command());
        return false;
      }
      //System.out.println("got " + i + " for " +shellProc);
    } catch (InterruptedException e) {
      // log.warning("got " +e);
    }

    process2.destroy();
    return true;
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
