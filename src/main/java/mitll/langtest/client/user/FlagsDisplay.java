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

package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.StartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class FlagsDisplay {
//  private static final int FLAG_DIM = 32;
  private static final int COLUMNS = 5;
  private final List<LangCC> ccs = new ArrayList<>();

  void getFlags(StartupInfo startupInfo) {
    Set<Language> seen = new HashSet<>();

    startupInfo.getProjects().forEach(slimProject -> {
      Language language = slimProject.getLanguage();
      if (slimProject.getStatus() == ProjectStatus.PRODUCTION) {
        if (!seen.contains(language)) {
          ccs.add(new LangCC(slimProject.getCountryCode(), language.toDisplay(), language.getActualName()));
          seen.add(language);
        }
      }
    });
    Collections.sort(ccs);
  }

  @NotNull
  Grid getFlagsDisplay() {
    int row = (int) Math.ceil((float) ccs.size() / (float) COLUMNS);
    Grid langs = new Grid(row, COLUMNS);
    int r = 0;

    for (LangCC langCC : ccs) {
      DivWidget both = new DivWidget();
      both.addStyleName("bottomFiveMargin");
      both.addStyleName("rightFiveMargin");
      //  both.addStyleName("inlineFlex");

      {
        //com.google.gwt.user.client.ui.Image flag = getFlag(langCC.cc);

        HTML flag = new HTML(langCC.getActualName());

        flag.addStyleName("rightFiveMargin");
        Style style = flag.getElement().getStyle();
        style.setColor("darkblue");
        style.setProperty("fontSize", "larger");
//        flag.setHeight(FLAG_DIM + "px");
//        flag.setWidth(FLAG_DIM + "px");
        both.add(flag);
      }

      {
        HTML w = new HTML(langCC.language);
        // w.addStyleName("blueColor");
        w.addStyleName("rightTenMargin");
        w.addStyleName("topFiveMargin");
        both.add(w);
      }
      langs.setWidget(r / COLUMNS, r % COLUMNS, both);
      r++;
    }

    langs.getElement().getStyle().setMarginTop(30, Style.Unit.PX);
    return langs;
  }

  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    return new com.google.gwt.user.client.ui.Image("langtest/cc/" + cc + ".png");
  }


  /**
   * Sorts by language
   */
  private static class LangCC implements Comparable<LangCC> {
    private final String cc;
    private final String language;
    private final String actualName;

    LangCC(String cc, String language, String actualName) {
      this.cc = cc;
      this.language = language;
      this.actualName = actualName;
    }

    @Override
    public int compareTo(@NotNull LangCC o) {
      return language.toLowerCase().compareTo(o.language.toLowerCase());
    }

    public String getCc() {
      return cc;
    }

    public String getLanguage() {
      return language;
    }

    public String getActualName() {
      return actualName;
    }
  }
}
