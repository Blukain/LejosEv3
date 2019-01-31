import org.ev3dev.hardware.sensors.UltrasonicSensor;

class ObjectDetection extends Thread
{
    /**
     * Bin heoght 9cm, 2cm of border to be recognized
     * object dimension 3.5cm
     */
    private UltrasonicSensor wall;
    private UltrasonicSensor object;
    private Movement movement;
    private ArmControl armControl;
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

    public ObjectDetection(UltrasonicSensor object, UltrasonicSensor wall, Movement movement, ArmControl armControl)
    {
        super("ObjectDetector");
        this.object = object;
        this.movement = movement;
        this.armControl=armControl;
        this.wall = wall;
    }

    public ObjectDetection(UltrasonicSensor object, float objectlimit, UltrasonicSensor wall, Movement movement, ArmControl armControl)
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
            if(wallDistance <= walllimit || objectDistance <= objectlimit)
            {
                movement.operate();
                System.out.println("obj distance: "+objectDistance+"; limit: "+objectlimit);
                System.out.println("wall distance: "+wallDistance+"; limit: "+walllimit);
                if (objectDistance <= objectlimit)
                { //Object detected
                    System.out.println("Object detected");
                    if (!objectAcquired) //Not holding an object
                    {
                        movement.turnOff();
                        System.out.println("Proceding to investigate");
                        // mettersi a bindistance e controllare a bindelta, se negativo, alzare,
                        // porsi a objectlimit, controllare a liftdelta e controllare
                        armControl.operate();
                        armControl.check();
                        pickup = armControl.checkObject();
                        if (pickup)
                        {
                            System.out.println("picking object");
                            while (object.getDistanceCentimeters() > pickupDistance)
                            {
                                movement.turnOn(this.getName());
                            }
                            movement.turnOff();
                            armControl.pickup();
                            armControl.picked();
                        }
                        else armControl.notPicked();
                        objectAcquired = armControl.isObject();
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
                    else
                    {// Already holding an object
                        System.out.println("Already holding object");
                        while (object.getDistanceCentimeters() > binAnalysisDistance)//binDistance)
                        {
                            movement.turnOn(this.getName());
                        }
                        movement.turnOff();
                        armControl.operate();
                        armControl.check();
                        if (ArmControl.release)
                        {
                            System.out.println("have to release object");
                            while (object.getDistanceCentimeters() > binDistance)
                            {
                                movement.turnOn(this.getName());
                            }
                            movement.turnOff();
                            armControl.check();
                            movement.backward();
                            try
                            {
                                Thread.sleep(1000);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                            System.out.println("Object released");
                        }
                        else
                        {
                            System.out.println("Object not released");
                        }
                        armControl.done();
                        objectAcquired = armControl.isObject();
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

    public void terminate()
    {
        go=false;
    }
}
