import org.ev3dev.hardware.sensors.UltrasonicSensor;

class WallDetection extends Thread
{
    private UltrasonicSensor wall;
    private boolean go = true;
    private float walllimit = 190; // 190
    private Movement movement;
    private float wallDistance;

    public WallDetection(UltrasonicSensor wall, Movement movement)
    {
        super("WallDetector");
        this.movement = movement;
        this.wall = wall;
    }

    public WallDetection(UltrasonicSensor wall, float walllimit, Movement movement)
    {
        this(wall,movement);
        this.wall = wall;
    }

    @Override
    public void run()
    {
        while (go)
        {
            wallDistance = wall.getDistanceCentimeters();
            if(wallDistance <= walllimit)
            {
                System.out.println("Wall detected");
                movement.operate();
                movement.turnSx();
                movement.forward();
                movement.operateDone();
                System.out.println("wall avoided");
            }
        }
    }

    public void terminate()
    {
        go=false;
    }
}
