import org.ev3dev.hardware.sensors.ColorSensor;

class FallDetection extends Thread
{
    private ColorSensor fallSensor;
    private boolean go = true;
    private float limit;
    private float floor;
    private Movement movement;

    public FallDetection(ColorSensor fallSensor, Movement movement)
    {
        super("FallDetector");
        this.fallSensor = fallSensor;
        this.movement = movement;
    }

    public FallDetection(ColorSensor fallSensor, float limit, Movement movement)
    {
        this(fallSensor,movement);
        this.limit = limit;
    }

    @Override
    public void run()
    {
        fallSensor.setMode(ColorSensor.SYSFS_REFLECTED_LIGHT_INTENSITY_MODE);
        limit = fallSensor.getReflectedLightIntensity(); // to be effective has to be started on darkest part of pavement
        while(go)
        {
            floor = fallSensor.getReflectedLightIntensity();
            if (floor == 0 || floor < (limit - 2)){ // this has to be checked and limit is get on thread start, in dark brown limit can be 1;
                movement.operate();
                System.out.println("fall detected");
                movement.backward();
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                movement.turnSx();
                movement.forward();
                movement.operateDone();
                System.out.println("fall avoided");
            }
        }
    }

    public void terminate()
    {
        go=false;
    }
}
