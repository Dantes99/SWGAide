package swg.crafting.resources.types;

import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.types.SWGNonFerrousMetal;
import swg.crafting.Stat;

/*
 * Represents a resource class of type "Smelted Non-Ferrous Metal"
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
public final class SWGSmeltedNonFerrousMetal extends SWGNonFerrousMetal {

  private static final long serialVersionUID = 6539101L;

  private static final int[] minStats = {200, 200, 200, 0, 0, 200, 200, 200, 0, 200, 200};
  private static final int[] maxStats = {200, 200, 200, 0, 0, 200, 200, 200, 0, 200, 200};

  private static final SWGSmeltedNonFerrousMetal INSTANCE = new SWGSmeltedNonFerrousMetal();

  SWGSmeltedNonFerrousMetal() { super(); }

  public static SWGSmeltedNonFerrousMetal getInstance() { return INSTANCE; }

  public int expectedStats() { return 8; }
  public int sortIndex() { return 670; }
  public int rcID() { return 826; }
  public String rcName() { return "Smelted Non-Ferrous Metal";}
  public String rcToken() { return "snfr";}
  public boolean isSpaceOrRecycled()  { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}