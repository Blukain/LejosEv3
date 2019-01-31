import org.ev3dev.hardware.sensors.UltrasonicSensor;

class ObjectDetectionFinal extends Thread
{
    /**
     * Bin heoght 9cm, 2cm of border to be recognized
     * object dimension 3.5cm
     */
    private UltrasonicSensor wall;
    private UltrasonicSensor object;
    private Movement movement;
    private ArmControlFinal armControl;
    private boolean go = true;
    private float walllimit = 190; // 190
    private float objectlimit = 140; // 140
    private float pickupDistance = 80; // 120
    private float binAnalysisDistance = 140; // 130
    private float binDistance = 50; // 50
    private boolean objectAcquired = false;
    private boolean pickup = false;
    private float objectDistance;
    private float wallDistance;

    public ObjectDetectionFinal(UltrasonicSensor object, UltrasonicSensor wall, Movement movement, ArmControlFinal armControl)
    {
        super("ObjectDetector");
        this.object = object;
        this.movement = movement;
        this.armControl=armControl;
        this.wall = wall;
    }

    public ObjectDetectionFinal(UltrasonicSensor object, float objectlimit, UltrasonicSensor wall, Movement movement, ArmControlFinal armControl)
    {
        this(object,wall,movement,armControl);
        this.objectlimit = objectlimit;
    }

    @Override
    public void run()
    {
        while (go)
        {
            objectDistance = object.getDistanceCentimeters();
            wallDistance = wall.getDistanceCentimeters();
            if(wallDistance <= walllimit || objectDistance <= binAnalysisDistance)
            {
                movement.operate();
                System.out.println("obj distance: "+objectDistance+"; limit: "+objectlimit);
                System.out.println("wall distance: "+wallDistance+"; limit: "+walllimit);
                if (objectDistance <= binAnalysisDistance)
                { //Object detected
                    System.out.println("Object detected");
                    movement.turnOff();
                    System.out.println("Proceding to investigate");
                    armControl.operate();
                    armControl.check();
                    if (!armControl.isHoldingObject()) // da completare
                    {//Not holding an object
                        pickup = armControl.checkObject();
                        if (pickup)
                        {
                            System.out.println("picking object");
                            moveToDistance(pickupDistance);
                            armControl.pickup();
                            armControl.picked();
                        }
                        else armControl.notPicked();
                        objectAcquired = armControl.isHoldingObject();
                        armControl.done();
                        if (objectAcquired){
                            movement.turnOn(this.getName());
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
                        if(armControl.getCurrentBinColor() == armControl.getCurrentObjectColor())
                        {// object have to be released
                            System.out.println("have to release object");
                            moveToDistance(binDistance);
                            armControl.check();

                            System.out.println("Object released");
                        }
                        else
                        {
                            System.out.println("Object not released");
                        }
                        armControl.restart();
                        objectAcquired = armControl.isHoldingObject();
                        armControl.done();
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
                movement.operateDone();
            }
        }

    }

    private void moveToDistance(float distance)
    {
        while (object.getDistanceCentimeters() > distance)
        {
            movement.turnOn(this.getName());
        }
        movement.turnOff();
    }

    public void terminate()
    {
        go=false;
    }
}
