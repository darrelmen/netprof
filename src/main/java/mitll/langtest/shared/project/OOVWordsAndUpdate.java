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

package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.exercise.CommonExercise;

import java.util.HashSet;
import java.util.Set;

/**
 * @see mitll.langtest.client.project.ProjectEditForm#addDominoProject
 */
public class OOVWordsAndUpdate implements IsSerializable {
  private Set<String> oov = new HashSet<>();
  private boolean didUpdate = false;
  private boolean isPossible = true;
  private boolean foundExercise = true;
  private boolean allEnglish = false;
  private boolean noEnglish = false;
  private boolean checkValid = false;
  private String normalizedText;

  public OOVWordsAndUpdate() {
  }

  public OOVWordsAndUpdate(boolean foundExercise) {
    this.foundExercise = foundExercise;
  }

  /**
   * @param dominoID
   * @param name
   * @param first
   * @param secondType
   * @param normText
   * @see mitll.langtest.server.audio.AudioFileHelper#checkAllExercises
   */
  public OOVWordsAndUpdate(boolean didUpdate, Set<String> oov, boolean isPossible, String normText) {
    this.didUpdate = didUpdate;
    this.oov = oov;
    this.isPossible = isPossible;
    this.normalizedText = normText;
  }

  public Set<String> getOov() {
    return oov;
  }

  public boolean isDidUpdate() {
    return didUpdate;
  }

  public boolean isPossible() {
    return isPossible;
  }

  public OOVWordsAndUpdate setPossible(boolean possible) {
    isPossible = possible;
    return this;
  }

  private boolean isFoundExercise() {
    return foundExercise;
  }

  public String getNormalizedText() {
    return normalizedText;
  }

  public boolean isAllEnglish() {
    return allEnglish;
  }

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#isValid
   * @param allEnglish
   */
  public void setAllEnglish(boolean allEnglish) {
    this.allEnglish = allEnglish;
  }

  public boolean isNoEnglish() {
    return noEnglish;
  }

  /**
   * @see mitll.langtest.server.audio.tools.OOVWordsHelper#get(CommonExercise, String, Project, Project)
   * @param noEnglish
   */
  public void setNoEnglish(boolean noEnglish) {
    this.noEnglish = noEnglish;
  }

  private boolean isCheckValid() {
    return checkValid;
  }

  public void setCheckValid(boolean checkValid) {
    this.checkValid = checkValid;
  }

  public String toString() {
    return "OOVWordsAndUpdate : " +
        "\n\toov        " + getOov() +
        "\n\tnorm       " + getNormalizedText() +
        "\n\tdidUpdate   " + isDidUpdate() +
        "\n\tisPossible  " + isPossible() +
        "\n\tall english " + isAllEnglish() +
        "\n\tno  english " + isNoEnglish() +
        "\n\tcheck       " + isCheckValid() +
        "\n\tisFound     " + isFoundExercise()
        ;
  }
}
