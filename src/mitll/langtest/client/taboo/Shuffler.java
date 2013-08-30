package mitll.langtest.client.taboo;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class Shuffler {
  public void shuffle(List<ExerciseShell> list, Random rnd) {
    int size = list.size();
    ExerciseShell arr[] = list.toArray(new ExerciseShell[size]);

    if (size < 5) {
      for (int i = size; i > 1; i--)
        swap(arr, i - 1, rnd.nextInt(i));
    } else {
      // Shuffle array
      for (int i = size; i > 1; i--)
        swap(arr, i - 1, rnd.nextInt(i));

      // Dump array back into list
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

  public void shuffle2(List<Exercise.QAPair> list, Random rnd) {
    int size = list.size();
    Exercise.QAPair arr[] = list.toArray(new Exercise.QAPair[size]);

    if (size < 5) {
      for (int i = size; i > 1; i--)
        swap2(arr, i - 1, rnd.nextInt(i));
    } else {
      // Shuffle array
      for (int i = size; i > 1; i--)
        swap2(arr, i - 1, rnd.nextInt(i));

      // Dump array back into list
      ListIterator<Exercise.QAPair> it = list.listIterator();
      for (Exercise.QAPair anArr : arr) {
        it.next();
        it.set(anArr);
      }
    }
  }

  /**
   * Swaps the two specified elements in the specified array.
   */
  private void swap2(Exercise.QAPair[] arr, int i, int j) {
    Exercise.QAPair tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }
}