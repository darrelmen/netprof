/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database;

import mitll.langtest.server.scoring.TextNormalizer;
import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class NormalizerTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(NormalizerTest.class);

  @Test
  public void testNormalization() {
    {
      TextNormalizer textNormalizer = new TextNormalizer(Language.SPANISH.name());
      List<String> strings = Arrays.asList(
          "そして, 何を飲みましたか",

          "Don't go to the store.",

          "¿Cuándo  visita " +
              "a " +
              "sus " +
              "padres?",
          "l'assurance",

          "A l'hôpital le médecin m'a fait une piqûre pour m'anesthésier.");
      strings.forEach(fl -> logger.info("from " + fl + " -> " + textNormalizer.getNorm(fl)));
    }

    {
      TextNormalizer textNormalizer = new TextNormalizer(Language.FRENCH.name());
      List<String> strings = Arrays.asList(
          "そして, 何を飲みましたか",
          "Don't go to the store.",
          "l'assurance",

          "A l'hôpital le médecin m'a fait une piqûre pour m'anesthésier.",
          "Cette histoire est dingue ! Qui aurait pu penser qu'iI survivrait à un tel accident ?");
      strings.forEach(fl -> logger.info("from " + fl + " -> " + textNormalizer.getNorm(fl)));
    }

    {
      String turkish = "İki  Bin  Bir  Uzay  Macerası ";

      TextNormalizer textNormalizer = new TextNormalizer(Language.TURKISH.name());
      List<String> strings = Arrays.asList(
          turkish);
      strings.forEach(fl -> logger.info("from " + fl + " -> '" + textNormalizer.getNorm(fl) + "'"));
    }
  }

  @Test
  public void toLC() {
    String dobar = "Dobar tek!";
    String banco = "Ella quiere robar un banco.";

    {
      TextNormalizer textNormalizer = new TextNormalizer(Language.CROATIAN.name());
      List<String> strings = Arrays.asList(dobar);
      strings.forEach(fl -> logger.info("from " + fl + " -> '" + textNormalizer.getNorm(fl) + "'"));
    }
    {
      TextNormalizer textNormalizer = new TextNormalizer(Language.SPANISH.name());
      List<String> strings = Arrays.asList(banco);
      strings.forEach(fl -> logger.info("from " + fl + " -> '" + textNormalizer.getNorm(fl) + "'"));
    }
  }

}
