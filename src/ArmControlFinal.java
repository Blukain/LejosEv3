import org.ev3dev.hardware.motors.LargeMotor;
import org.ev3dev.hardware.motors.MediumMotor;
import org.ev3dev.hardware.sensors.ColorSensor;

import java.util.ArrayList;

class ArmControlFinal extends Thread
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
    private boolean holdingObject = false;
    private boolean down = false;
    private boolean pickUp = false;
    private boolean open = false;
    private boolean check = false;
    private boolean picked = false;
    private boolean pickedDone = false;
    public static boolean release = false;
    private static ArrayList<Integer> objectToPickUp = new ArrayList<>();
    private static int[] objectPickedUp = {0,0,0,0,0,0,0};
    private Integer currentObjectColor;
    private boolean notPicked = false;
    private boolean mutex = true;
    private boolean identified = false;
    private Integer currentBinColor;
    private boolean analyzed = false;
    private boolean isObject = false;
    private boolean isBin = false;
    private boolean bin = false;
    private boolean object = false;

    /***
     * Constructor for Motor control Thread
     * @param liftmotor the one responsible to lift up and down the crane arm
     * @param clampmotor the one responsible to open and close the clamp
     * @param colorSensor the one responsible to analyze object encountered
     */
    public ArmControlFinal(LargeMotor liftmotor, MediumMotor clampmotor, ColorSensor colorSensor)
    {
        super("ArmController");
        this.clampmotor = clampmotor;
        this.liftmotor = liftmotor;
        this.colorSensor=colorSensor;
        go = true;
    }

    /***
     * @param liftmotor the one responsible to lift up and down the crane arm
     * @param clampmotor the one responsible to open and close the clamp
     * @param colorSensor the one responsible to analyze object encountered
     * @param liftdelta the delta in rotation to lift up/down limit
     * @param clampdelta the delta in rotation to close/open limit
     */

    public ArmControlFinal(LargeMotor liftmotor, int liftdelta, MediumMotor clampmotor, int clampdelta, ColorSensor colorSensor)
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
            if(check)
            {
                /** Unknown Object **/
                if (!identified)
                {
                    if (!analyzed) /** Analyze upper level */
                    {
                        identified = firstAnalysis(binDelta);
                        if(identified){
                            isBin = true;
                        }
                        else analyzed = true;
                    }
                    else /** Analyze lower level */
                    {
                        identified = firstAnalysis(liftdelta);
                        if(identified){
                            isObject = true;
                        }
                        analyzed = false;
                    }
                }
                else /** Object identified **/
                {
                    if(isObject){
                        if(colorAnalyze("obj")){
                            object = true;
                            isObject = false;
                        }
                        checkDone();
                    }
                    if(isBin) {
                        if(colorAnalyze("bin")){
                            liftItUp();
                            bin = true;
                            isBin = false;
                        }
                        checkDone();
                    }
                    if (object)
                    {
                        if (notPicked)
                        {
                            System.out.println("lift up call extra");
                            liftItUp();
                            System.out.println("lifted up");
                            notPickedDone();
                        }
                        if (pickUp)
                        {
                            System.out.println("lift up call");
                            liftItUp();
                            System.out.println("lifted up");
                            pickUp=false;
                            pickedDone();
                        }
                    }
                    if(bin)
                    {
                        /**
                         * se già raccolto un oggetto rilascia oggetto
                         */
                        if(holdingObject)
                        {
                            releaseObject();
                            holdingObject = false;
                            checkDone();
                        }
                    }
                }
            }
        }
        shutDown();
    }

    public void restart(){
        bin = false;
        object = false;
        identified = false;
    }

    private void liftItDown(int delta)
    {
        liftDown(delta);
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
    }

    private void liftItUp()
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

    private void shutDown()
    {
        clampmotor.stop();
        liftmotor.stop();
        if(down)
        {
            liftItUp();
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

    private void releaseObject()
    {
        openClamp();
        while (!open)
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
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        closeClamp();
        while (open)
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
    private synchronized boolean firstAnalysis(int delta)
    {
        System.out.println("lift down");
        liftItDown(delta);
        System.out.println("First Analysis...");
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        if (colorSensor.getColor() > 0)
        {
            identified = true;
        }
        else
        {
            identified = false;
        }
        processing();
        return identified;
    }


    public synchronized boolean colorAnalyze(String object)
    {
        System.out.println("Color analisys");
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        boolean recognized = false;
        for (Integer i:objectToPickUp)
        {
            if (colorSensor.getColor() == i)
            {
                recognized = true;
                if(object.equals("bin")) setCurrentBinColor(i);
                else{
                    if(!holdingObject) pickUp = true;
                    setCurrentObjectColor(i);
                }
                break;
            }
        }
        processing();
        return recognized;
    }

    private void processing()
    {
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

    public void liftDown(int liftdelta)
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

    public void setup(){
        liftmotor.reset();
        clampmotor.reset();
        colorSensor.setMode(ColorSensor.SYSFS_COLOR_MODE);
    }

    public boolean isHoldingObject()
    {
        return holdingObject;
    }

    public synchronized void check()
    {
        check = true;
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
        check = false;
        done = true;
        notifyAll();
    }

    public synchronized boolean checkObject()
    {
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
    }

    public synchronized void picked(){
        check = true;
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
        pickedDone=false;
    }

    private synchronized void pickedDone(){
        check = false;
        pickedDone = true;
        holdingObject = true;
        notifyAll();
    }

    public synchronized void notPicked(){
        notPicked = true;
        check = true;
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
        check = false;
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

    public int getCurrentObjectColor()
    {
        return currentObjectColor;
    }

    public int getCurrentBinColor()
    {
        return currentBinColor;
    }

    public void setCurrentObjectColor(Integer currentObjectColor)
    {
        this.currentObjectColor = currentObjectColor;
    }

    public void setCurrentBinColor(Integer currentBinColor)
    {
        this.currentBinColor = currentBinColor;
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

    public boolean setAmountPickedColor(int color, int amount){
        if(color<0 || color>objectPickedUp.length) return false;
        else
        {
            objectPickedUp[color]++;
            return true;
        }
    }

    public boolean addPickedColor(int color){
        if(color<0 || color>objectPickedUp.length) return false;
        else
        {
            objectPickedUp[color]++;
            return true;
        }
    }

    public boolean resetPickedColor(int i){
        for (i=0;i<objectPickedUp.length; i++){
            objectPickedUp[i] = 0;
        }
        return true;
    }
}
