package mitll.langtest.client.taboo;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.StimulusAnswerPair;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/15/13
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class SinglePlayerRobot {
  private final LangTestDatabaseAsync service;
  //private Collection<String> types;
  List<SectionNode> nodes = Collections.emptyList();
 // List<ExerciseShell> exercisesRemaining;
  List<ExerciseShell> exercisesRemaining = Collections.emptyList();
  Exercise currentExercise = null;
  List<String> synonymSentences = Collections.emptyList();
 // int nodeIndex, exerciseIndex, stimIndex;

  public SinglePlayerRobot(LangTestDatabaseAsync service) {
    this.service = service;
  }

  /**
   * Get the chapters, exercises, run through each (randomly choose exercises),
   * then randomly choose a stimulus.
   * All client side?
   *
   *
   * @paramx fuserid
   */
  public void doSinglePlayer(/*final long fuserid*/) {
    if (nodes == null || nodes.isEmpty()) {
      System.out.println("---> doSinglePlayer getting nodes ");

      service.getSectionNodes(new AsyncCallback<List<SectionNode>>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("getSectionNodes can't contact server. got " + caught);
        }

        @Override
        public void onSuccess(List<SectionNode> result) {
          nodes = result;
          System.out.println("\n\ngetSectionNodes.onSuccess : got nodes " + nodes);
       //   getExercisesForNextChapter(userid, async);
        }
      });
    }
  }

  /**
   * @see ReceiverExerciseFactory.ReceiverPanel#checkForStimulus
   * @param userid
   * @param async
   */
  public void checkForStimulus(long userid, AsyncCallback<StimulusAnswerPair> async) {
    if (exercisesRemaining.isEmpty()) {
      if (nodes.isEmpty()) {
        async.onSuccess(null); // no more chapters, no more exercises, we're done -- TODO : start over?
      }
      else {
        SectionNode chapter = null;
        chapter = nodes.remove(0);
        System.out.println("checkForStimulus.getExercisesForNextChapter : next chapter " + nodes.get(0));
        //final SectionNode chapter = nodes.get(0);

        getExercisesForNextChapter(userid, chapter, async);
      }
    }
    else {
      getNextExercise(async);
    }
  //  getNextExercise();
  }

  private void getExercisesForNextChapter(final long fuserid, final SectionNode chapter, final AsyncCallback<StimulusAnswerPair> async) {
    //final SectionNode firstChapter = nodes.get(0);

    Map<String, Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
    typeToSection.put(chapter.getType(), Arrays.asList(chapter.getName()));
    System.out.println("requesting for " + typeToSection);

    service.getExercisesForSelectionState(0, typeToSection, fuserid, new AsyncCallback<ExerciseListWrapper>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      @Override
      public void onSuccess(ExerciseListWrapper result) {
        System.out.println("getExercisesForSelectionState.onSuccess got " + result.exercises.size());
        exercisesRemaining = result.exercises;
        Random rand = new Random();
        shuffle(exercisesRemaining, rand);

        if (exercisesRemaining.isEmpty()) {
          System.err.println("huh? no exercises for " + chapter + "???");
        } else {
          getNextExercise(async);
        }
      }
    });
  }

  /**
   * @see #checkForStimulus(long, com.google.gwt.user.client.rpc.AsyncCallback)
   * @param async
   */
  private void getNextExercise(final AsyncCallback<StimulusAnswerPair> async) {
    if (synonymSentences.isEmpty()) {
      ExerciseShell nextExerciseShell = exercisesRemaining.get(0);
      service.getExercise(nextExerciseShell.getID(), new AsyncCallback<Exercise>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Couldn't contact server.");
        }

        @Override
        public void onSuccess(Exercise result) {
          System.out.println("getExercise.onSuccess got " + result);

          currentExercise = result;
          exercisesRemaining.remove(0);

          synonymSentences = currentExercise.getSynonymSentences();

          String refSentence = currentExercise.getRefSentence().trim();
          if (synonymSentences.isEmpty()) {
            System.err.println("huh? no stim sentences for " + currentExercise);
            async.onSuccess(new StimulusAnswerPair(result.getID(), "Data error on server, please report.", refSentence));
          } else {
            String rawStim = synonymSentences.remove(0);
            async.onSuccess(new StimulusAnswerPair(result.getID(), getObfuscated(rawStim,refSentence), refSentence));
          }
        }
      });
    } else {
      String rawStim = synonymSentences.remove(0);
      String refSentence = currentExercise.getRefSentence().trim();

      async.onSuccess(new StimulusAnswerPair(currentExercise.getID(), getObfuscated(rawStim,refSentence), refSentence));
    }
  }

  private String getObfuscated(String exampleToSend, String refSentence) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < refSentence.length(); i++) builder.append('_');
    if (!exampleToSend.contains(refSentence)) {
      System.err.println("huh? '" + exampleToSend + "' doesn't contain '" + refSentence + "'");
    }
    return exampleToSend.replaceAll(refSentence, builder.toString());
  }


  private void shuffle(List<ExerciseShell> list, Random rnd) {
    int size = list.size();
    ExerciseShell arr[] = list.toArray(new ExerciseShell[size]);

    if (size < 5) {
      for (int i=size; i>1; i--)
        swap(arr, i-1, rnd.nextInt(i));
    } else {
      //Object arr[] = list.toArray();

      // Shuffle array
      for (int i=size; i>1; i--)
        swap(arr, i-1, rnd.nextInt(i));

      // Dump array back into list
      //ListIterator<ExerciseShell> exerciseShellListIterator = list.listIterator();
      ListIterator<ExerciseShell> it = list.listIterator();
      for (ExerciseShell anArr : arr) {
        it.next();
        it.set(anArr);
      }
    }
  }

  /**
   * Swaps the two specified elements in the specified array.
   */
  private void swap(ExerciseShell[] arr, int i, int j) {
    ExerciseShell tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }

  public void registerAnswer(boolean correct) {
    if (correct) synonymSentences = Collections.emptyList();
  }
}
