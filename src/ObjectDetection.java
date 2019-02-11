import lejos.hardware.sensor.EV3UltrasonicSensor;

class ObjectDetection extends Thread
{
    /**
     * Bin heoght 9cm, 2cm of border to be recognized
     * object dimension 3.5cm
     */
    private EV3UltrasonicSensor wall;
    private EV3UltrasonicSensor object;
    private Movement movement;
    private ArmControl armControl;
    private boolean go = true;
    private double walllimit = 0.211; // 190
    private double objectlimit = 0.113; // 140
    private double pickupDistance = 0.075; // 120
    private double binAnalysisDistance = 0.135; // 130
    private double binDistance = 0.050; // 50
    private boolean objectAcquired = false;
    private boolean pickup = false;
    private float objectDistance;
    private float wallDistance;
    private boolean done = false;
    private float[] objectSample;
    private float[] wallSample;
    private boolean automatic = false;

    public ObjectDetection(EV3UltrasonicSensor object, EV3UltrasonicSensor wall, Movement movement, ArmControl armControl)
    {
        super("ObjectDetector");
        this.object = object;
        this.movement = movement;
        this.armControl = armControl;
        this.wall = wall;
        objectSample = new float[object.getDistanceMode().sampleSize()];
        wallSample = new float[wall.getDistanceMode().sampleSize()];
    }

    public ObjectDetection(EV3UltrasonicSensor object, float objectlimit, EV3UltrasonicSensor wall, Movement movement, ArmControl armControl)
    {
        this(object,wall,movement,armControl);
        this.objectlimit = objectlimit;
    }

    @Override
    public void run()
    {
        while (go)
        {
            if (automatic)
            {
                objectDistance = getObjectDistance();
                wallDistance = getWallDistance();
                if (wallDistance <= walllimit || objectDistance <= binAnalysisDistance)
                {
                    movement.getControl();
                    movement.brake();
                    System.out.println("obj distance: " + objectDistance + "; limit: " + objectlimit);
                    System.out.println("wall distance: " + wallDistance + "; limit: " + walllimit);
                    if (objectDistance <= binAnalysisDistance) //Object detected
                    {
                        System.out.println("Object detected");
                        System.out.println("Proceding to investigate");
                        armControl.getControl();
                        armControl.check();
                        if (!armControl.isHoldingObject()) // da completare
                        {//Not holding an object
                            pickup = armControl.checkObject();
                            if (pickup)
                            {
                                System.out.println("picking object");
                                moveTo(pickupDistance);
                                armControl.picked();
                            }
                            else
                            {
                                armControl.notPicked();
                                System.out.println("Object not picked");
                            }
                            armControl.restart();
                            objectAcquired = armControl.isHoldingObject();
                            armControl.controlDone();
                            if (objectAcquired)
                            {
                                movement.forward();
                                System.out.println("object acquired");
                            }
                            else
                            {
                                movement.turnSx();
                                movement.forward();
                                System.out.println("object avoided");
                            }
                        }
                        else // completo
                        {// Already holding an object
                            System.out.println("Already holding object");
                            if (armControl.getCurrentBinColor() == armControl.getCurrentObjectColor())
                            {// object have to be released
                                System.out.println("have to release object");
                                try
                                {
                                    sleep(1000);
                                }
                                catch (InterruptedException e)
                                {
                                    e.printStackTrace();
                                }
                                moveTo(binDistance);
                                try
                                {
                                    sleep(2000);
                                }
                                catch (InterruptedException e)
                                {
                                    e.printStackTrace();
                                }
                                armControl.check();
                                System.out.println("Object released");
                            }
                            else
                            {
                                armControl.notPicked();
                                System.out.println("Object not released");
                            }
                            armControl.restart();
                            objectAcquired = armControl.isHoldingObject();
                            armControl.controlDone();
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
                        }
                    }
                    else
                    {   //Wall detected
                        System.out.println("Wall detected");
                        movement.turnSx();
                        movement.forward();
                        System.out.println("wall avoided");
                    }
                    movement.controlDone();
                }
            }
        }
    }

    private void moveTo(double distance)
    {
        movement.forward();
        while (getObjectDistance() > distance){}
        movement.brake();
    }

    public float getObjectDistance()
    {
        object.getDistanceMode().fetchSample(objectSample,0);
        return objectSample[0];
    }

    public float getWallDistance()
    {
        wall.getDistanceMode().fetchSample(wallSample, 0);
        return wallSample[0];
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
