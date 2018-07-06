package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.StartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class FlagsDisplay {
  private static final int FLAG_DIM = 32;
  private static final int COLUMNS = 4;
  private List<LangCC> ccs = new ArrayList<>();

  void getFlags(StartupInfo startupInfo) {
    Set<String> seen = new HashSet<>();

    startupInfo.getProjects().forEach(slimProject -> {
      String language = slimProject.getLanguage();
      if (slimProject.getStatus() == ProjectStatus.PRODUCTION) {
        if (!seen.contains(language)) {
          ccs.add(new LangCC(slimProject.getCountryCode(), language.substring(0, 1).toUpperCase() + language.substring(1)));
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
      both.addStyleName("inlineFlex");

      {
        com.google.gwt.user.client.ui.Image flag = getFlag(langCC.cc);
        flag.addStyleName("rightFiveMargin");
        flag.setHeight(FLAG_DIM + "px");
        flag.setWidth(FLAG_DIM + "px");
        both.add(flag);
      }

      {
        HTML w = new HTML(langCC.language);
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
    String cc, language;

    LangCC(String cc, String language) {
      this.cc = cc;
      this.language = language;
    }

    @Override
    public int compareTo(@NotNull LangCC o) {
      return language.toLowerCase().compareTo(o.language.toLowerCase());
    }
  }
}
