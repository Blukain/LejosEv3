import lejos.hardware.sensor.EV3ColorSensor;

class FallDetection extends Thread
{
    private EV3ColorSensor fallSensor;
    private boolean go = true;
    private float limit;
    private float floor;
    private Movement movement;
    private float[] intensitySample;
    private boolean automatic = false;

    public FallDetection(EV3ColorSensor fallSensor, Movement movement)
    {
        super("FallDetector");
        this.fallSensor = fallSensor;
        this.movement = movement;
        intensitySample = new float[fallSensor.getRedMode().sampleSize()];
    }

    public FallDetection(EV3ColorSensor fallSensor, float limit, Movement movement)
    {
        this(fallSensor,movement);
        this.limit = limit;
    }

    @Override
    public void run()
    {
        limit = getIntensity(); // to be effective has to be started on darkest part of pavement
        while(go)
        {
            if(automatic)
            {
                floor = getIntensity();
                if (floor == 0.00 || floor < (limit - 0.02))
                { // this has to be checked and limit is get on thread start, in dark brown limit can be 1;
                    movement.getControl();
                    System.out.println("fall detected");
                    movement.backward();
                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    movement.turnSx();
                    movement.forward();
                    movement.controlDone();
                    System.out.println("fall avoided");
                }
            }
        }
    }

    public float getIntensity()
    {
        fallSensor.getRedMode().fetchSample(intensitySample,0);
        return intensitySample[0];
    }

    public void setAuto(){
        automatic = true;
    }

    public void setManual(){
        automatic = false;
    }

    public void terminate()
    {
        go=false;
    }
}
