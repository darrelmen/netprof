/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.audio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/24/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
class ProcessRunner {
  // private static final Logger logger = LogManager.getLogger(ProcessRunner.class);
  //private static final boolean SHOW_OUTPUT = true;

  boolean runProcess(ProcessBuilder shellProc) throws IOException {
    return runProcess(shellProc, false);
  }

  boolean runProcess(ProcessBuilder shellProc, boolean showOutput) throws IOException {
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
