package swg.crafting.resources.types;

import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.types.SWGBeans;
import swg.crafting.Stat;

/*
 * Represents a resource class of type "Yavinian Vegetable Beans"
 *
 * <b>WARNING:</b>
 * This class is generated by SWGResourceClassGenerator.
 * Do not manually modify this class as your changes are
 * erased when the classes are re-generated.
 *
 * @author Steven M. Doyle <shadow@triwizard.net>
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a>
 * aka Chimaera.Zimoon
 */
@SuppressWarnings("all")
public final class SWGYavinianVegetableBeans extends SWGBeans {

  private static final long serialVersionUID = 4238164L;

  private static final int[] minStats = {0, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0};
  private static final int[] maxStats = {0, 0, 1000, 0, 1000, 0, 0, 1000, 1000, 0, 0};

  private static final SWGYavinianVegetableBeans INSTANCE = new SWGYavinianVegetableBeans();

  SWGYavinianVegetableBeans() { super(); }

  public static SWGYavinianVegetableBeans getInstance() { return INSTANCE; }

  public int expectedStats() { return 4; }
  public int sortIndex() { return 456; }
  public int rcID() { return 535; }
  public String rcName() { return "Yavinian Vegetable Beans";}
  public String rcToken() { return "yabns";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}
