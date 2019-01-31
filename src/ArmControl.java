import org.ev3dev.hardware.motors.LargeMotor;
import org.ev3dev.hardware.motors.MediumMotor;
import org.ev3dev.hardware.sensors.ColorSensor;

import java.util.ArrayList;

class ArmControl extends Thread
{
    public static int NOCOLOR = 0;
    public static int BLACK = 1;
    public static int BLUE = 2;
    public static int GREEN = 3;
    public static int YELLOW = 4;
    public static int RED = 5;
    public static int WHITE = 6;
    public static int BROWN = 7;
    public static final String CLOSE = "normal";
    public static final String OPEN = "inversed";
    private static int clampSpeed = 110;
    private static int liftSpeed = 150;
    private static int binDelta = 1000;
    private boolean lift;
    private boolean clamp;
    private boolean go;
    private static int clampdelta = 120;
    private static int liftdelta = 1800;
    private LargeMotor liftmotor;
    private MediumMotor clampmotor;
    private ColorSensor colorSensor;
    private String direction;
    private boolean done = false;
    private boolean object = false;
    private boolean down = false;
    private boolean pickUp = false;
    private boolean open = false;
    private boolean operate = false;
    private boolean picked = false;
    private boolean pickedDone = false;
    public static boolean release = false;
    private static ArrayList<Integer> objectToPickUp = new ArrayList<>();
    public static int currentObjectColor;
    private boolean notPicked = false;
    private boolean mutex = true;
    private boolean downBin =false;

    /***
     * Constructor for Motor control Thread
     * @param liftmotor the one responsible to lift up and down the crane arm
     * @param clampmotor the one responsible to open and close the clamp
     * @param liftdelta the delta in rotation to lift up/down limit
     * @param clampdelta the delta in rotation to close/open limit
     */
    public ArmControl(LargeMotor liftmotor, MediumMotor clampmotor, ColorSensor colorSensor)
    {
        super("ArmController");
        this.clampmotor = clampmotor;
        this.liftmotor = liftmotor;
        this.colorSensor=colorSensor;
        go = true;
    }

    public ArmControl(LargeMotor liftmotor, int liftdelta, MediumMotor clampmotor, int clampdelta, ColorSensor colorSensor)
    {
        this(liftmotor,clampmotor, colorSensor);
        this.clampdelta=clampdelta;
        this.liftdelta=liftdelta;
    }

    @Override
    public void run()
    {
        setup();
        while(go)
        {
            if(operate)
            {
                /**
                 * Se nessun oggetto raccolto
                 */
                System.out.println("var object: "+object);
                System.out.println("var notpicked: "+notPicked);
                System.out.println("var pickup: "+pickUp);
                System.out.println("var picked: "+picked);
                if (!object)
                {
                    if(notPicked){
                        System.out.println("lift up call extra");
                        liftUp();
                        while (down){
                            try
                            {
                                wait();
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("lifted up");
                        notPickedDone();
                    }
                    else if(!picked)
                    {
                        System.out.println("lift down call");
                        liftDown();
                        while (!down)
                        {
                            try
                            {
                                wait();
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("lifted down");
                        checkDone();
                    }
                    else {
                        System.out.println("lift up call");
                        liftUp();
                        while (down){
                            try
                            {
                                wait();
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("lifted up");
                        pickedDone();
                    }
                }
                else
                {
                    /**
                     * se già raccolto un oggetto rilascia oggetto
                     */
                    if(!release)
                    {
                        System.out.println("lift down bin");
                        liftDownBin();
                        while (!downBin)
                        {
                            try
                            {
                                wait();
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Bin Analysis...");
                        try
                        {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                        if (colorSensor.getColor() == ArmControl.currentObjectColor)
                        {
                            release = true;
                        }
                        else release = false;
                        System.out.println("Calculating...");
                        try
                        {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                        System.out.println("analysis Done!");
                        System.out.println("lift up bin");
                        liftUpBin();
                        while (downBin)
                        {
                            try
                            {
                                wait();
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        checkDone();
                    }
                    else {
                        openClamp();
                        while (!open){
                            try
                            {
                                wait();
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        try
                        {
                            Thread.sleep(2000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                        closeClamp();
                        while (open){
                            try
                            {
                                wait();
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        release=false;
                        object=false;
                        checkDone();
                    }
                }
            }
        }
        clampmotor.stop();
        liftmotor.stop();
        if(down)
        {
            liftUp();
            while (down)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        if(open)
        {
            closeClamp();
            while (open){
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void open()
    {
        clampmotor.setPolarity(clampmotor.getPolarity() == "normal" ? "inversed" : "normal");
    }

    public void close()
    {
        liftmotor.setPolarity(liftmotor.getPolarity() == "normal" ? "inversed" : "normal");
    }

    public void brake()
    {
        liftmotor.stop();
        clampmotor.stop();
    }

    public void pause()
    {
        setClamp(false);
        setLift(false);
    }

    public boolean isLift()
    {
        return lift;
    }

    public boolean isClamp()
    {
        return clamp;
    }

    public void setLift(boolean lift)
    {
        this.lift = lift;
    }

    public void setClamp(boolean clamp)
    {
        this.clamp = clamp;
    }

    public void terminate()
    {
        go = false;
    }

    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    public String getDirection()
    {
        return direction;
    }

    /**
    * Color detected by the sensor, categorized by overall value. <br>
     * - 0: No color<br>
     * - 1: Black<br>
     * - 2: Blue<br>
     * - 3: Green<br>
     * - 4: Yellow<br>
     * - 5: Red<br>
     * - 6: White<br>
     * - 7: Brown
    **/
    public synchronized void analyze()
    {
        System.out.println("Analyzing...");
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        for (Integer i:objectToPickUp)
        {
            if (colorSensor.getColor() == i)
            {
                pickUp = true;
                currentObjectColor = i;
                break;
            }
            else pickUp=false;
        }
        System.out.println("Calculating...");
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        System.out.println("analysis Done!");
    }

    public synchronized void openClamp()
    {
        clampmotor.setPosition_SPInt(-clampdelta);
        clampmotor.setPolarity(OPEN);
        clampmotor.setSpeed_SP(clampSpeed);
        while (clampmotor.getPosition()<clampdelta)
        {
            clampmotor.runForever();
        }
        clampmotor.stop();
        open=true;
    }

    public synchronized void closeClamp()
    {
        clampmotor.setPosition_SPInt(0);
        clampmotor.setPolarity(CLOSE);
        clampmotor.setSpeed_SP(clampSpeed);
        while (clampmotor.getPosition()<0){
            clampmotor.runForever();
            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            if(clampmotor.getPosition()<0){
                clampmotor.setPosition_SPInt(clampmotor.getPosition()+30);
                break;
            }
        }
        clampmotor.stop();
        clampmotor.runToRelPos();
        open=false;
    }

    public void liftDown()
    {
        liftmotor.setPosition_SPInt(-liftdelta);
        liftmotor.setPolarity("inversed");
        liftmotor.setSpeed_SP(liftSpeed);
        while (liftmotor.getPosition()<liftdelta){
            liftmotor.runForever();
        }
        liftmotor.stop();
        down=true;
    }

    public void liftUp()
    {
        liftmotor.setPosition_SPInt(0);
        liftmotor.setPolarity("normal");
        liftmotor.setSpeed_SP(liftSpeed);
        liftmotor.runToRelPos();
        while (liftmotor.getPosition()<0){
            liftmotor.runForever();
        }
        liftmotor.stop();
        down=false;
    }

    public void liftDownBin()
    {
        liftmotor.setPosition_SPInt(-binDelta);
        liftmotor.setPolarity("inversed");
        liftmotor.setSpeed_SP(liftSpeed);
        while (liftmotor.getPosition()<binDelta){
            liftmotor.runForever();
        }
        liftmotor.stop();
        downBin=true;
    }

    public void liftUpBin()
    {
        liftmotor.setPosition_SPInt(0);
        liftmotor.setPolarity("normal");
        liftmotor.setSpeed_SP(liftSpeed);
        liftmotor.runToRelPos();
        while (liftmotor.getPosition()<0){
            liftmotor.runForever();
        }
        liftmotor.stop();
        downBin=false;
    }

    public void setup(){
        liftmotor.reset();
        clampmotor.reset();
        colorSensor.setMode(ColorSensor.SYSFS_COLOR_MODE);
    }

    public boolean isObject()
    {
        return object;
    }

    public synchronized void check()
    {
        operate = true;
        while (!done){
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        done = false;
    }

    private synchronized void checkDone()
    {
        operate = false;
        done = true;
        notifyAll();
    }

    public synchronized boolean checkObject()
    {
        analyze();
        System.out.println("pickup: "+pickUp);
        if (pickUp)
        {
            openClamp();
            while (!open){
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            return true;
        }
        else return false;
    }

    public synchronized void pickup(){
        closeClamp();
        while (open){
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        picked = true;
    }

    public synchronized void picked(){
        operate = true;
        while (!pickedDone){
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        picked = false;
        pickedDone=false;
    }

    private synchronized void pickedDone(){
        operate = false;
        pickedDone = true;
        object = true;
        notifyAll();
    }

    public synchronized void notPicked(){
        notPicked = true;
        operate = true;
        while (notPicked){
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private synchronized void notPickedDone(){
        operate = false;
        notPicked = false;
        notifyAll();
    }

    public synchronized void operate()
    {
        while (!mutex){
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        mutex=false;
    }

    public synchronized void done(){
        mutex=true;
        notifyAll();
    }

    /**
     * Bluetooth function to set which elemnts to pickup
     **/
    public boolean addColor(int i){
        if(!objectToPickUp.contains(i)) objectToPickUp.add(i);
        return true;
    }

    public boolean removeColor(int i){
        if(objectToPickUp.contains(i)) objectToPickUp.remove(i);
        return false;
    }
}
