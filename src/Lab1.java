import TSim.*;
import java.util.concurrent.Semaphore;
import java.lang.Math;

public class Lab1 {

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
    }

    void setSpeed(int speed) {
      currentSpeed = Math.clamp(speed, -MAXSPEED, MAXSPEED);
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
          // sensorHandler(sensor);
        }
      } catch (CommandException | InterruptedException e) {
        e.printStackTrace(); // or only e.getMessage() for the error
      }
    }

    public void sensorHandler(SensorEvent sens, Semaphore sem) {
      int x = sens.getXpos();
      int y = sens.getYpos();

    }

    private Semaphore[] nextSem(Section nextSec) {
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
}
