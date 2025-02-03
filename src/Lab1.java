import TSim.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.lang.Math;

public class Lab1 {
  public static final int SWITCH_LEFT = 0x01;
  public static final int SWITCH_RIGHT = 0x02;

  // java -cp bin Main "Lab1.map" 5 10 20
  public Lab1(int speed1, int speed2) throws InterruptedException {
    TSimInterface tsi = TSimInterface.getInstance();
    Semaphore[] sems = makeSems();

    TrainHandler train1 = new TrainHandler(tsi, 1, speed1, Section.North, Heading.South, sems);
    TrainHandler train2 = new TrainHandler(tsi, 2, speed2, Section.South, Heading.North, sems);

    Thread t1 = new Thread(train1);
    Thread t2 = new Thread(train2);

    t1.start();
    t2.start();

    // sempahore 0: upperTopStation - North 1
    // sempahore 1: upperBotStation - North 2
    // sempahore 2: - East
    // sempahore 3: - Center 1
    // sempahore 4: - Center 2
    // sempahore 5: - West
    // sempahore 6: belowTopStation - South 1
    // sempahore 7: belowBotStation - South 2
    // sempahore 8: - crossing

  }

  private Semaphore[] makeSems() {
    Semaphore[] sems = new Semaphore[9];
    for (int i = 0; i < sems.length; i++) {
      sems[i] = new Semaphore(1);
    }
    return sems;
  }

  class TrainHandler implements Runnable {
    private static final int MAXSPEED = 30;

    private final TSimInterface tsi;
    private final int id;
    private int speed;

    private boolean criticalSection, upperStation, belowStation;
    private int currentSpeed;
    private Section currentTrack;
    private Heading dir;

    private final Semaphore[] sems;

    private final Pos[] switches = { new Pos(17, 7), new Pos(15, 9), new Pos(4, 9), new Pos(3, 11) };
    private final Pos[] sensors = {
        new Pos(14, 7), new Pos(15, 8), new Pos(19, 8), // switches[0]
        new Pos(17, 9), new Pos(12, 9), new Pos(13, 10), // switches[1]
        new Pos(7, 9), new Pos(6, 10), new Pos(2, 9), // Switches[2]
        new Pos(4, 13), new Pos(6, 11), new Pos(1, 10) }; // switches[3]
    private final Map<Pos, Pos> switchLookUp; // map for getting position of switch associated with
    private final Map<Pos, SwitchFacing> switchFacing;

    TrainHandler(TSimInterface tsi, int id, int initSpeed, Section startSec, Heading dir, Semaphore[] sems) {
      this.tsi = tsi;
      this.id = id;
      this.speed = initSpeed;

      this.criticalSection = false;
      this.upperStation = false;
      this.belowStation = false;

      this.currentSpeed = initSpeed;
      this.currentTrack = startSec;
      this.dir = dir;

      this.sems = sems;
      this.switchLookUp = genSwitchMap();
      this.switchFacing = genFacingMap();
    }

    // generates a lookup table /kv map for finding switches given sensor position
    private Map<Pos, Pos> genSwitchMap() {
      Map<Pos, Pos> lookup = new HashMap<>();

      for (int i = 0; i < switches.length; i++) {
        lookup.put(sensors[3 * i + 0], switches[i]);
        lookup.put(sensors[3 * i + 1], switches[i]);
        lookup.put(sensors[3 * i + 2], switches[i]);
      }
      return lookup;
    }

    // generates a kv map for getting the ''facing'' of each switch, facing,
    // combined
    // with train heading, can be used to get what direction switch should be set to
    private Map<Pos, SwitchFacing> genFacingMap() {
      Map<Pos, SwitchFacing> lookup = new HashMap<>();
      /*
       * [0] Switch 17 7 : N-E : W-facing : Heading S => Right
       * [1] Switch 15 9 : E-C : W-facing : Heading N => Left
       * [2] Switch 4 9 : C-W : E-facing : Heading S => Left
       * [3] Switch 3 11 : W-S : E-facing : Heading N => Right
       */
      lookup.put(switches[0], SwitchFacing.West);
      lookup.put(switches[1], SwitchFacing.West);
      lookup.put(switches[2], SwitchFacing.East);
      lookup.put(switches[3], SwitchFacing.East);
      return lookup;
    }

    // sets speed, if given outside of bounds, it's set to max allowed
    void setSpeed(int speed) {
      currentSpeed = Math.clamp(speed, -MAXSPEED, MAXSPEED); // not allowed to provide negative speed?
      try {
        this.tsi.setSpeed(this.id, this.currentSpeed);
      } catch (CommandException e) {
        e.printStackTrace(); // or only e.getMessage() for the error
        System.exit(1);
      }
    }

    @Override
    public void run() {
      setSpeed(this.currentSpeed);
      try {
        while (true) {
          SensorEvent sensor = tsi.getSensor(id);
          sensorHandler(sensor);
        }
      } catch (CommandException | InterruptedException e) {
        e.printStackTrace(); // or only e.getMessage() for the error
      }
    }

    public void sensorHandler(SensorEvent sens) {
      int x = sens.getXpos();
      int y = sens.getYpos();

    }

    private Semaphore[] nextSecSems(Section nextSec) {
      Semaphore[] nextSems;
      switch (nextSec) {
        case Section.North:
          nextSems = new Semaphore[] { sems[0], sems[1] };
          break;
        case Section.East:
          nextSems = new Semaphore[] { sems[2] };
          break;
        case Section.Center:
          nextSems = new Semaphore[] { sems[3], sems[4] };
          break;
        case Section.West:
          nextSems = new Semaphore[] { sems[5] };
          break;
        case Section.South:
          nextSems = new Semaphore[] { sems[6], sems[7] };
          break;
        default:
          nextSems = null;
          break;
      }
      return nextSems;
    }

    // takes sensor pos and heading, ensures train can pass associated switch
    private void trackSwitch(Pos sensor, Heading dir) {
      /*
       * Switch 17 7 : N-E : W-facing : Heading S => Right
       * Switch 15 9 : E-C : W-facing : Heading N => Left
       * Switch 4 9 : C-W : E-facing : Heading S => Left
       * Switch 3 11 : W-S : E-facing : Heading N => Right
       */
      Pos sPos = switchLookUp.get(sensor);
      try {
        tsi.setSwitch(sPos.x, sPos.y, switchDir(switchFacing.get(sPos), dir));
      } catch (CommandException e) {
        e.printStackTrace();
      }
    }

    private int switchDir(SwitchFacing facing, Heading dir) {
      int sDir = -1;
      if (facing == SwitchFacing.West && dir == Heading.North) {
        sDir = SWITCH_LEFT;
      } else if (facing == SwitchFacing.West && dir == Heading.South) {
        sDir = SWITCH_RIGHT;
      } else if (facing == SwitchFacing.East && dir == Heading.North) {
        sDir = SWITCH_RIGHT;
      } else if (facing == SwitchFacing.East && dir == Heading.South) {
        sDir = SWITCH_LEFT;
      }
      return sDir;
    }

  }

  private enum Section {
    North(0), /* North2(0), */
    East(1),
    Center(2), /* Center2(2), */
    West(3),
    South(4)/* , South2(4) */;

    private final int id;
    private final int[][] next = { { 1, 1 }, // position / section , direction
        { 0, 2 },
        { 1, 3 },
        { 2, 4 },
        { 3, 3 } };

    Section(int id) {
      this.id = id;
    }

    int n() {
      return this.id;
    }

    Section next(Heading dir) {
      return fromId(next[this.id][dir.n()]);
    }

    Section fromId(int id) {
      Section sec;
      switch (id) {
        case 0:
          sec = Section.North;
          break;
        case 1:
          sec = Section.East;
          break;
        case 2:
          sec = Section.Center;
          break;
        case 3:
          sec = Section.West;
          break;
        case 4:
          sec = Section.South;
          break;

        default:
          sec = null;
          break;
      }
      return sec;
    }
  }

  private enum Heading {
    North(0), South(1);

    private final int id;

    Heading(int id) {
      this.id = id;
    }

    int n() {
      return this.id;
    }
  }

  private class Pos {
    final int x;
    final int y;

    Pos(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getEnclosingInstance().hashCode();
      result = prime * result + x;
      result = prime * result + y;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Pos other = (Pos) obj;
      if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
        return false;
      if (x != other.x)
        return false;
      if (y != other.y)
        return false;
      return true;
    }

    private Lab1 getEnclosingInstance() {
      return Lab1.this;
    }

  }

  private enum SwitchFacing {
    East, West;
  }

}
