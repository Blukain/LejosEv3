import lejos.hardware.sensor.EV3ColorSensor;
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
    private float walllimit = 0.211f;
    private float pickupDistance = 0.080f;
    private float objectAnalysisDistance = 0.130f;
    private float binAnalysisDistance = 0.147f;
    private float binDistance = 0.050f;
    private boolean objectAcquired = false;
    private boolean pickup = false;
    private float objectDistance;
    private float wallDistance;
    private boolean done = false;
    private float[] objectSample;
    private float[] wallSample;
    private boolean automatic = false;
    private boolean turn;
    private int seconds;
    private int LEFT = 0;
    private int RIGHT = 1;
    private int[] turncount = {0,0};
    private int lastturn;
    private boolean inverse = false;
    private int turnlimit = 2;
    private int scanlimit = 5;
    private Clock clock;
    private boolean objectAvoided;
    private int LONG = 5;
    private int SHORT = 3;
    private int objectlimit = SHORT;
    private float lastwallsample;
    private float lastobjsample;

    /** Fall Detection*/
    private EV3ColorSensor fallSensor;
    private float limit;
    private float floor;
    private float[] intensitySample;

    public ObjectDetection(EV3ColorSensor fallSensor, EV3UltrasonicSensor object, EV3UltrasonicSensor wall, Movement movement, ArmControl armControl)
    {
        super("ObjectDetector");
        this.object = object;
        this.movement = movement;
        this.armControl = armControl;
        this.wall = wall;
        objectSample = new float[object.getDistanceMode().sampleSize()];
        wallSample = new float[wall.getDistanceMode().sampleSize()];
        /** Fall*/
        this.fallSensor = fallSensor;
        intensitySample = new float[fallSensor.getRedMode().sampleSize()];
    }

    @Override
    public void run()
    {
        limit = getIntensity(); // to be effective has to be started on darkest part of pavement
        while (go)
        {
            if (automatic)
            {
                objectDistance = getObjectDistance();
                wallDistance = getWallDistance();
                floor = getIntensity();
                System.out.println("obj: " + objectDistance);
                System.out.println("wall: " + wallDistance);
                System.out.println("floor: " + floor);
                if (wallDistance <= walllimit || objectDistance <= binAnalysisDistance
                        || (floor == 0.00 || floor < (limit - 0.02)) || (turn && (seconds > turnlimit))
                        || seconds > scanlimit || (objectAvoided && (seconds > objectlimit)))
                {
                    movement.getControl();
                    movement.brake();
                    if (objectDistance <= binAnalysisDistance && wallDistance > walllimit) //Object detected
                    {
                        System.out.println("obj distance: " + objectDistance);
                        System.out.println("obj limit: " + binAnalysisDistance);
                        System.out.println("Object detected");
                        System.out.println("Proceding to investigate");
                        armControl.getControl();
                        armControl.check();
                        /*if(armControl.isAnalyzed()){
                            moveTo(objectAnalysisDistance);
                            armControl.check();
                        }
                        armControl.check();*/
                        if (!armControl.isHoldingObject())
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
                                objectAvoided();
                                System.out.println("object avoided");
                            }
                        }
                        else
                        {// Already holding an object
                            System.out.println("Already holding object");
                            if (armControl.getCurrentBinColor() == armControl.getCurrentObjectColor())
                            {// object have to be released
                                System.out.println("have to release object");
                                moveTo(binDistance);
                                armControl.action();
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
                            objectAvoided();
                            System.out.println("object avoided");
                        }
                    }
                    else
                    {//Wall detected
                        if(wallDistance <= walllimit || (floor == 0.00 || floor < (limit - 0.02)))
                        {//controllare
                            timerStop();
                            if(floor == 0.00 || floor < (limit - 0.02)){
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
                            }
                            if(wallDistance <= walllimit) System.out.println("wall distance: " + wallDistance);
                            System.out.println("wall limit: " + walllimit);
                            System.out.println("Wall detected");
                            if(lastturn == LEFT)
                            {
                                System.out.println("lastturn left");
                                if(turncount[LEFT] == 4){
                                    System.out.println("turncount 4");
                                    lastturn = RIGHT;
                                    movement.turn90DegDx();
                                    movement.forward();
                                    turncount[LEFT] = 0;
                                }
                                else if(inverse){
                                    System.out.println("inverse");
                                    lastturn = RIGHT;
                                    movement.turn90DegDx();
                                    movement.forward();
                                    inverse = false;
                                }
                                else if(turn || objectAvoided){
                                    System.out.println("turn");
                                    lastturn = LEFT;
                                    inverse = true;
                                    movement.turn90DegSx();
                                    movement.turn90DegSx();
                                    movement.forward();
                                    turn = false;
                                }
                                else
                                {
                                    System.out.println("else");
                                    lastturn = RIGHT;
                                    movement.turn90DegDx();
                                    movement.forward();
                                    turn = true;
                                    timerStart();
                                }
                            }
                            else
                            {
                                System.out.println("lastturn right");
                                if(turncount[RIGHT] == 4){
                                    System.out.println("turncount 4");
                                    lastturn = LEFT;
                                    movement.turn90DegSx();
                                    movement.forward();
                                    turncount[RIGHT] = 0;
                                }
                                else if(inverse){
                                    System.out.println("inverse");
                                    lastturn = LEFT;
                                    movement.turn90DegSx();
                                    movement.forward();
                                    inverse = false;
                                }
                                else if(turn || objectAvoided){
                                    System.out.println("turn");
                                    lastturn = RIGHT;
                                    inverse = true;
                                    movement.turn90DegDx();
                                    movement.turn90DegDx();
                                    movement.forward();
                                    turn = false;
                                }
                                else
                                {
                                    System.out.println("else");
                                    lastturn = LEFT;
                                    movement.turn90DegSx();// oppure 180
                                    movement.forward();
                                    turn = true;
                                    timerStart();
                                }
                            }
                            System.out.println("wall avoided");
                        }
                        if((turn && (seconds >= turnlimit)))
                        {
                            System.out.println("timer turn timeout");
                            timerStop();
                            turn = false;
                            if(lastturn == LEFT){
                                System.out.println("lastturn left");
                                if(objectAvoided){
                                    System.out.println("obj avoided");
                                    lastturn = RIGHT;
                                    movement.turn90DegDx();
                                    movement.forward();
                                }
                                else{
                                    System.out.println("else");
                                    lastturn = LEFT;
                                    movement.turn90DegSx();
                                    movement.forward();
                                }
                            }
                            else {
                                System.out.println("lastturn right");
                                if(objectAvoided){
                                    System.out.println("obj avoided");
                                    lastturn = LEFT;
                                    movement.turn90DegSx();
                                    movement.forward();
                                }
                                else{
                                    System.out.println("else");
                                    lastturn = RIGHT;
                                    movement.turn90DegDx();
                                    movement.forward();
                                }
                            }
                        }
                        if(objectAvoided && (seconds >= objectlimit)){
                            System.out.println("");
                            System.out.println("timer objavoided timeout");
                            timerStop();
                            if(lastturn == LEFT){
                                System.out.println("lastturn left");
                                if(turncount[LEFT] > turncount[RIGHT]){
                                    lastturn = RIGHT;
                                    turncount[RIGHT]++;
                                    movement.turn90DegDx();
                                    movement.forward();
                                    objectlimit = LONG;
                                }
                                else
                                {
                                    lastturn = LEFT;
                                    turncount[LEFT]++;
                                    movement.turn90DegSx();
                                    movement.forward();
                                    objectlimit = SHORT;
                                }
                            }
                            else {
                                System.out.println("lastturn right");
                                if(turncount[RIGHT] > turncount[LEFT]){
                                    lastturn = LEFT;
                                    turncount[LEFT]++;
                                    movement.turn90DegSx();
                                    movement.forward();
                                    objectlimit = LONG;
                                }
                                else
                                {
                                    lastturn = RIGHT;
                                    turncount[RIGHT]++;
                                    movement.turn90DegDx();
                                    movement.forward();
                                    objectlimit = SHORT;
                                }
                            }
                            if(turncount[LEFT] == 2 && turncount[RIGHT] == 2)
                            {
                                turncount[0] = 0;
                                turncount[1] = 0;
                                objectlimit = SHORT;
                                objectAvoided = false;
                            }
                            else timerStart();
                        }
                    }
                    movement.controlDone();
                }
            }
        }
    }

    public float getIntensity()
    {
        fallSensor.getRedMode().fetchSample(intensitySample,0);
        return intensitySample[0];
    }

    private void objectAvoided()
    {
        turn = false;
        objectAvoided = true;
        if(lastturn == LEFT)
        {
            System.out.println("avoided left");
            turncount[LEFT]++;
            movement.turn90DegSx();
            movement.forward();
        }
        else
        {
            System.out.println("avoided right");
            turncount[RIGHT]++;
            movement.turn90DegDx();
            movement.forward();
        }
        timerStart();
    }

    private void timerStop()
    {
        System.out.println("Timer stopped");
        if(clock != null){
            clock.terminate();
            clock = null;
        }
        seconds=0;
    }

    private void timerStart()
    {
        System.out.println("Timer started");
        if(clock==null){
            clock = new Clock();
            clock.start();
        }
        else{
            seconds = 0;
        }
    }

    public class Clock extends Thread
    {
        boolean timer = true;

        @Override
        public void run()
        {
            while (timer)
            {
                try
                {
                    sleep(1000);
                    seconds++;
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }

        public void terminate()
        {
            timer = false;
        }
    }

    private void moveTo(float distance)
    {
        movement.forward();
        while (getObjectDistance() > distance){}
        movement.brake();
    }

    public float getObjectDistance()
    {
        object.getDistanceMode().fetchSample(objectSample, 0);
        return objectSample[0];
    }

    public float getWallDistance()
    {
        wall.getDistanceMode().fetchSample(wallSample, 0);
        return wallSample[0];
    }

    public void terminate()
    {
        go=false;
    }

    /** Bluetooth method*/

    public void setAuto(){
        automatic = true;
    }

    public void setManual(){
        automatic = false;
    }
}
