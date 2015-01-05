package swg.crafting.resources.types;

import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.types.SWGOrganic;
import swg.crafting.Stat;

/*
 * Represents a resource class of type "Flora Resources"
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
public class SWGFloraResources extends SWGOrganic {

  private static final long serialVersionUID = 110710L;

  private static final int[] minStats = {0, 0, 1, 0, 1, 0, 200, 1, 1, 1, 1};
  private static final int[] maxStats = {0, 0, 1000, 0, 1000, 0, 1000, 1000, 1000, 700, 800};

  private static final SWGFloraResources INSTANCE = new SWGFloraResources();

  SWGFloraResources() { super(); }

  public static SWGFloraResources getInstance() { return INSTANCE; }

  public int expectedStats() { return 7; }
  public int sortIndex() { return 318; }
  public int rcID() { return 13; }
  public String rcName() { return "Flora Resources";}
  public String rcToken() { return "frs";}
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}