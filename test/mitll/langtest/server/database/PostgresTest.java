package mitll.langtest.server.database;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/14.
 */
public class PostgresTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(PostgresTest.class);
  //public static final boolean DO_ONE = false;

  @Test
  public void testSpanishEventCopy() {
     getDatabase("spanish");
  }

}
