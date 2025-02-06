import TSim.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.lang.Math;

public class Lab1 {
  public static final int SWITCH_LEFT = 0x01;
  public static final int SWITCH_RIGHT = 0x02;

  // make all; java -cp bin Main "Lab1.map" 5 10 20

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

    // sempahore 0: - North 1
    // sempahore 1: - North 2
    // sempahore 2: - East
    // sempahore 3: - Center 1
    // sempahore 4: - Center 2
    // sempahore 5: - West
    // sempahore 6: - South 1
    // sempahore 7: - South 2
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
    private static final int MAXSPEED = 22; // from testing this is max, unless we move sensors

    private final TSimInterface tsi;
    private final int id;

    // private boolean criticalSection, upperStation, belowStation;
    private int currentSpeed;
    private Section currentSec;
    private Heading currentDir;

    private final Semaphore[] sems;

    // provide these in constructor?
    private final Pos[] switches = { new Pos(17, 7), new Pos(15, 9), new Pos(4, 9), new Pos(3, 11) };
    private final Pos[] switchSensors = {
        new Pos(14, 7), new Pos(15, 8), new Pos(19, 8), // switches[0]
        new Pos(17, 9), new Pos(12, 9), new Pos(13, 10), // switches[1]
        new Pos(7, 9), new Pos(6, 10), new Pos(2, 9), // Switches[2]
        new Pos(1, 10), new Pos(4, 13), new Pos(6, 11) }; // switches[3]
    private final Pos[] crossingSensors = { // sensors at crossing
        new Pos(6, 6), new Pos(9, 5), new Pos(11, 7), new Pos(11, 8) };
    private final Pos[] stationSensors = {
        new Pos(14, 3), new Pos(14, 5), new Pos(14, 11), new Pos(14, 13) };
    private final Map<Pos, Pos> switchLookUp; // map for getting position of switch associated with
    private final Map<Pos, SwitchFacing> switchFacing;

    private final int[][] nsSensorsInd = {
        { 2, 4, 5, 8, 10, 11 },
        { 0, 1, 3, 6, 7, 9 } };

    private Semaphore currentSem;
    private Semaphore semToRelease;
    private boolean onSideTrack;

    TrainHandler(TSimInterface tsi, int id, int initSpeed, Section initSec, Heading initDir, Semaphore[] sems) {
      this.tsi = tsi;
      this.id = id;

      this.currentSpeed = initSpeed;
      this.currentSec = initSec;
      this.currentDir = initDir;

      this.sems = sems;
      this.switchLookUp = genSwitchMap();
      this.switchFacing = genFacingMap();

      this.onSideTrack = false;

      int s = this.currentSec == Section.North ? 0 : 6;
      try {
        this.currentSem = sems[s];
        this.currentSem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // generates a lookup table /kv map for finding switches given sensor position
    private Map<Pos, Pos> genSwitchMap() {
      Map<Pos, Pos> lookup = new HashMap<>();

      for (int i = 0; i < switches.length; i++) {
        lookup.put(switchSensors[3 * i + 0], switches[i]);
        lookup.put(switchSensors[3 * i + 1], switches[i]);
        lookup.put(switchSensors[3 * i + 2], switches[i]);
      }
      return lookup;
    }

    // generates a kv map for getting the ''facing'' of each switch, facing,
    // combined with train heading, can be used to get what direction switch
    // should be set to
    private Map<Pos, SwitchFacing> genFacingMap() {
      Map<Pos, SwitchFacing> lookup = new HashMap<>();
      /*
       * [0] Switch 17 7 : N-E : W-facing
       * [1] Switch 15 9 : E-C : W-facing
       * [2] Switch 4 9 : C-W : E-facing
       * [3] Switch 3 11 : W-S : E-facing
       */
      lookup.put(switches[0], SwitchFacing.West);
      lookup.put(switches[1], SwitchFacing.West);
      lookup.put(switches[2], SwitchFacing.East);
      lookup.put(switches[3], SwitchFacing.East);
      return lookup;
    }

    // sets speed, if given outside of bounds, it's set to max allowed
    void setSpeed(int speed) {
      this.currentSpeed = Math.clamp(speed, -MAXSPEED, MAXSPEED); // not allowed to provide negative speed?
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
        e.printStackTrace();
      }
    }

    public void sensorHandler(SensorEvent sens) {
      int x = sens.getXpos();
      int y = sens.getYpos();
      Pos sensor = new Pos(x, y);
      if (sens.getStatus() == SensorEvent.ACTIVE) {// only hande sensors upon activation? maybe change later

        // check if heading into crossing
        if (sensor.in(crossingSensors)) {
          handleCrossing(sensor);
        } else // update switch upon activating sensor
        if (sensor.in(switchSensors) && headingIntoSwitch(sensor)) {
          semAndSwitch(sensor);
        } else // at station
        if (arrivingAtStation() && sensor.in(stationSensors)) {
          stopAtStation();
        } else // passing a sensor but leaving the track switch
        if (sensor.in(switchSensors) && !headingIntoSwitch(sensor)) {
          semToRelease.release();
        }
      }
    }

    private boolean headingIntoSwitch(Pos sens) {
      boolean res = false;
      int j = this.currentDir == Heading.North ? 0 : 1;
      for (int i : nsSensorsInd[j]) {
        if (switchSensors[i].equals(sens)) {
          res = true;
        }
      }
      return res;
    }

    private boolean arrivingAtStation() {
      return (this.currentSec == Section.North && this.currentDir == Heading.North)
          || (this.currentSec == Section.South && this.currentDir == Heading.South);
    }

    private void stopAtStation() {
      int speed = currentSpeed;
      setSpeed(0);
      try {
        Thread.sleep(1000 + (20 * Math.abs(speed)));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      this.currentDir = this.currentDir.reverse();

      setSpeed(-1 * speed);
    }

    private Semaphore[] secSems(Section nextSec) {
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
    // if track is avaliable
    private void semAndSwitch(Pos sensor) {

      boolean exitAlt = onSideTrack;

      Section nextSec = this.currentSec.next(this.currentDir);
      Semaphore[] nextSems = secSems(nextSec);

      boolean aquired = false;
      boolean enterAlt = false;
      for (int i = 0; !aquired && i < nextSems.length; i++) {
        aquired = nextSems[i].tryAcquire();
        if (aquired) {
          this.semToRelease = this.currentSem;
          this.currentSem = nextSems[i];
          enterAlt = i != 0;
        }
      }

      onSideTrack = enterAlt;

      Pos sPos = switchLookUp.get(sensor);
      if (!aquired) {
        stopAndWait(nextSems[0]);// thread sleeps untill train can enter section
        // should only happen on single line tracks
      }
      switchTrack(sPos, exitAlt || enterAlt, nextSec);
    }

    private void switchTrack(Pos sens, boolean alt, Section nextSec) {
      try {
        tsi.setSwitch(sens.x, sens.y, switchDir(switchFacing.get(sens), alt));
      } catch (CommandException e) {
        e.printStackTrace();
      }
      // is now entering next section
      this.currentSec = nextSec;
    }

    private void stopAndWait(Semaphore sem) {
      int speed = currentSpeed;
      setSpeed(0);
      try {
        sem.acquire();
        this.semToRelease = this.currentSem;
        this.currentSem = sem;
        setSpeed(speed);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // alt = true if train should enter or exits secondary track
    private int switchDir(SwitchFacing facing, boolean alt) {
      boolean switchLeft = true;
      if (facing == SwitchFacing.West && this.currentDir == Heading.North) {
        switchLeft = alt;
      } else if (facing == SwitchFacing.West && this.currentDir == Heading.South) {
        switchLeft = alt;
      } else if (facing == SwitchFacing.East && this.currentDir == Heading.North) {
        switchLeft = !alt;
      } else if (facing == SwitchFacing.East && this.currentDir == Heading.South) {
        switchLeft = !alt;
      }
      return switchLeft ? SWITCH_LEFT : SWITCH_RIGHT;
    }

    private void handleCrossing(Pos sensor) {
      if (headingIntoCrossing(sensor)) {
        tryEnterCrossing();
      } else {
        sems[8].release();
      }
    }

    private void tryEnterCrossing() {
      if (!sems[8].tryAcquire()) {
        int speed = currentSpeed;
        setSpeed(0);
        try {
          sems[8].acquire();
          setSpeed(speed);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    private boolean headingIntoCrossing(Pos sensor) {
      boolean res;
      switch (this.currentDir) {
        case Heading.South:
          res = sensor.in(new Pos[] { crossingSensors[0], crossingSensors[1] });
          break;
        case Heading.North:
          res = sensor.in(new Pos[] { crossingSensors[2], crossingSensors[3] });
          break;
        default:
          res = false;
          break;
      }
      return res;
    }

  }

  private enum Section {
    North(0), 
    East(1),
    Center(2), 
    West(3),
    South(4);

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

    Heading reverse() {
      return this == Heading.North ? Heading.South : Heading.North;
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

    public boolean in(Pos[] arr) {
      boolean res = false;
      for (Pos pos : arr) {
        if (pos.equals(this)) {
          res = true;
        }
      }
      return res;
    }

    @Override
    public String toString() {
      return "(" + x + "," + y + ")";
    }

  }

  private enum SwitchFacing {
    East, West;
  }

}
