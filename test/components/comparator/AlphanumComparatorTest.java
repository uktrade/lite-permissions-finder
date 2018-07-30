package components.comparator;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AlphanumComparatorTest {

  @Test
  public void sortControlCode1() {

    List<String> listToSort = Arrays.asList("ML3", "ML1a12a", "ML1b", "ML16", "ML1a2", "ML12", "ML4", "1B001", "ML1a1a",
        "ML2", "ML1", "ML1a", "1A0a1", "PL9001");

    List<String> expected = Arrays.asList("1A0a1", "1B001", "ML1", "ML1a", "ML1a1a", "ML1a2", "ML1a12a", "ML1b", "ML2",
        "ML3", "ML4", "ML12", "ML16", "PL9001");

    listToSort.sort(new AlphanumComparator());
    Assert.assertEquals(expected, listToSort);
  }

  @Test
  public void sortControlCode2() {

    List<String> listToSort = Arrays.asList("ML2", "ML1a1");
    List<String> expected = Arrays.asList("ML1a1", "ML2");

    listToSort.sort(new AlphanumComparator());
    Assert.assertEquals(expected, listToSort);
  }

  @Test
  public void sortControlCode3() {

    List<String> listToSort = Arrays.asList("PL9001", "ML1");
    List<String> expected = Arrays.asList("ML1", "PL9001");

    listToSort.sort(new AlphanumComparator());
    Assert.assertEquals(expected, listToSort);
  }

  @Test
  public void sortControlCode4() {

    List<String> listToSort = Arrays.asList("ML2", "ML1");
    List<String> expected = Arrays.asList("ML1", "ML2");

    listToSort.sort(new AlphanumComparator());
    Assert.assertEquals(expected, listToSort);
  }

  @Test
  public void sortControlCode5() {

    List<String> listToSort = Arrays.asList("ML1", "1A001");
    List<String> expected = Arrays.asList("1A001", "ML1");

    listToSort.sort(new AlphanumComparator());
    Assert.assertEquals(expected, listToSort);
  }

}
