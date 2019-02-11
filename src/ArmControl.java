import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import java.util.ArrayList;

class ArmControl extends Thread
{
    /** deltas setting */
    private static int clampdelta = -114;
    private static int liftdelta = -2000;
    private static int clampSpeed = 60;
    private static int liftSpeed = 150;
    private static int binDelta = -1000;

    /** Color set as recognized from Ev3 */
    public static final int	BLACK = 7;
    public static final int	BLUE = 2;
    public static final int	BROWN = 13;
    public static final int	CYAN = 12;
    public static final int	DARK_GRAY = 11;
    public static final int	GRAY = 9;
    public static final int	GREEN = 1;
    public static final int	LIGHT_GRAY = 10;
    public static final int	MAGENTA = 4;
    public static final int	NONE = -1;
    public static final int	ORANGE = 5;
    public static final int	PINK = 8;
    public static final int	RED = 0;
    public static final int	WHITE = 6;
    public static final int	YELLOW = 3;

    /**
     * Color Arrays: using positionin system
     * - 0: Black<br>
     * - 1: Blue<br>
     * - 2: Green<br>
     * - 3: Yellow<br>
     * - 4: Red<br>
     * - 5: White<br>
     * - 6: Brown
     * */
    private static int[] objectPickedUp = {0,0,0,0,0,0,0};
    private static int[] colerRequested = {0,0,0,0,0,0,0};
    public static final int	ENCODEDBLACK = 0;
    public static final int	ENCODEDBLUE = 1;
    public static final int	ENCODEDGREEN = 2;
    public static final int	ENCODEDYELLOW = 3;
    public static final int	ENCODEDRED = 4;
    public static final int	ENCODEDWHITE = 5;
    public static final int	ENCODEDBROWN = 6;

    /** Variable and objects */
    public static final String CLOSE = "normal";
    public static final String OPEN = "inversed";
    private boolean go;
    private EV3LargeRegulatedMotor liftmotor;
    private EV3MediumRegulatedMotor clampmotor;
    private EV3ColorSensor colorSensor;
    private boolean done = false;
    private boolean holdingObject = false;
    private boolean down = false;
    private boolean pickUp = false;
    private boolean open = false;
    private boolean check = false;
    private boolean pickedDone = false;
    private static ArrayList<Integer> objectToPickUp = new ArrayList<>();
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
    private boolean state = false;

    /***
     * Constructor for Motor control Thread
     * @param liftmotor the one responsible to lift up and down the crane arm
     * @param clampmotor the one responsible to open and close the clamp
     * @param colorSensor the one responsible to analyze object encountered
     */
    public ArmControl(EV3LargeRegulatedMotor liftmotor, EV3MediumRegulatedMotor clampmotor, EV3ColorSensor colorSensor)
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

    public ArmControl(EV3LargeRegulatedMotor liftmotor, int liftdelta, EV3MediumRegulatedMotor clampmotor, int clampdelta, EV3ColorSensor colorSensor)
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
                    if (notPicked)
                    {
                        System.out.println("Object not Identified");
                        liftUp();
                        notPickedDone();
                    }
                    else
                    {
                        if (!analyzed) /** Analyze upper level */
                        {
                            identified = identify(binDelta); //true if color present
                            if (identified)
                            {
                                System.out.println("Object Identified");
                                isBin = true;
                            }
                            else
                            {
                                System.out.println("Object Unidentified");
                                analyzed = true;
                            }
                        }
                        else /** Analyze lower level */
                        {
                            identified = identify(liftdelta);
                            if (identified)
                            {
                                System.out.println("Object Unidentified");
                                isObject = true;
                            }
                            else
                            {
                                restart();
                                checkDone();
                            }
                        }
                    }
                }
                else /** Object identified **/
                {
                    if(isObject){ /** Object identified is a object **/
                        if(colorAnalyze("obj")){ /** Object of interested color*/
                            object = true;
                            isObject = false;
                        }
                        checkDone();
                    }
                    if(isBin) { /** Object identified is a bin **/
                        if(colorAnalyze("bin")){ /** Bin of interested color*/
                            liftUp();
                            bin = true;
                            isBin = false;
                        }
                        else {
                            isBin = false;
                        }
                        checkDone();
                    }
                    if (notPicked)
                    {
                        System.out.println("Object not to be picked up");
                        liftUp();
                        restart();
                        notPickedDone();
                    }
                    if (object)
                    {
                        if (pickUp)
                        {
                            pickup();
                            liftUp();
                            pickUp = false;
                            restart();
                            pickedDone();
                        }
                        else {
                            System.out.println("identified-object-!pickup");
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
                            decreaseColorRequested(colorEncode(currentObjectColor));
                            addPickedColor(colorEncode(currentObjectColor));
                            currentObjectColor = NONE;
                            currentBinColor = NONE;
                            holdingObject = false;
                        }
                        else {
                            System.out.println("identified-bin-!holdingobject");
                        }
                        checkDone();
                    }
                }
            }
        }
        shutDown();
    }

    public void restart(){
        this.bin = false;
        this.object = false;
        this.identified = false;
        this.analyzed = false;
        this.isObject = false;
        this.isBin = false;
        this.notPicked = false;
    }

    private void shutDown()
    {
        clampmotor.stop();
        liftmotor.stop();
        if(down)
        {
            liftUp();
        }
        if(open)
        {
            closeClamp();
        }
    }

    private void releaseObject()
    {
        openClamp();
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        closeClamp();
    }

    public void terminate()
    {
        go = false;
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
    private synchronized boolean identify(int delta)
    {
        boolean recognized = false;
        System.out.println("lift down to: ");
        System.out.println(delta);
        liftDown(delta);
        System.out.println("Identifying...");
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        if (colorSensor.getColorID() != -1 && colorSensor.getColorID() != 7)
        {
            recognized = true;
        }
        else
        {
            recognized = false;
        }
        processing();
        return recognized;
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
            if (colorSensor.getColorID() == i)
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
        System.out.println("COLOR: "+toColorName(colorSensor.getColorID()));
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
        clampmotor.rotateTo(clampdelta);
        open=true;
    }

    public synchronized void closeClamp()
    {
        int position = 0;
        clampmotor.rotateTo(0,true);
        while(clampmotor.getPosition()<0)
        {
            System.out.println("position: " + clampmotor.getPosition());
            System.out.println("tachocount: " + clampmotor.getTachoCount());
            position = (int) (clampmotor.getPosition() + 30);
            break;
        }
        clampmotor.stop();
        clampmotor.rotateTo(position,true);
        open=false;
    }

    public void liftDown(int liftdelta)
    {
        liftmotor.rotateTo(liftdelta);
        down=true;
    }

    public void liftUp()
    {
        liftmotor.rotateTo(0);
        down=false;
    }

    public void setup(){
        liftmotor.resetTachoCount();
        clampmotor.resetTachoCount();
        currentBinColor = NONE;
        currentObjectColor = NONE;
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
        //System.out.println("pickup: "+pickUp);
        if (pickUp)
        {
            openClamp();
            return true;
        }
        else return false;
    }

    public synchronized void pickup(){
        closeClamp();
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
        notifyAll();
    }

    public synchronized void getControl()
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

    public synchronized void controlDone(){
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

    public String toColorName (int color)
    {
        if (color == BLACK) return "Black";
        else if (color == BLUE) return "Blue";
        else if (color == GREEN) return "Green";
        else if (color == YELLOW) return "Yellow";
        else if (color == RED) return "Red";
        else if (color == WHITE) return "White";
        else if (color == BROWN) return "Brown";
        else return "Nocolor";
    }

    /**
     * Bluetooth function to set which elements to pickup
     **/
    public boolean addColor(int i){
        if(!objectToPickUp.contains(i)){
            objectToPickUp.add(i);
            return true;
        }
        else return false;
    }

    public boolean removeColor(int i){
        if(objectToPickUp.contains(i)){
            objectToPickUp.remove(i);
            return true;
        }
        return false;
    }

    public boolean setAmountPickedColor(int color, int amount){
        if(color<0 || color>objectPickedUp.length) return false;
        else
        {
            objectPickedUp[color]=-amount; //correggere
            return true;
        }
    }

    public static int[] getObjectPickedUp()
    {
        return objectPickedUp;
    }

    public int colorEncode (int color)
    {
        int encoded = -1;
        switch (color)
        {
            case BLACK: encoded = ENCODEDBLACK; break;
            case BLUE: encoded = ENCODEDBLUE; break;
            case GREEN: encoded = ENCODEDGREEN; break;
            case YELLOW: encoded = ENCODEDYELLOW; break;
            case RED: encoded = ENCODEDRED; break;
            case WHITE: encoded = ENCODEDWHITE; break;
            case BROWN: encoded = ENCODEDBROWN; break;
        }
        return encoded;
    }

    public boolean addPickedColor(int color){
        if(color>=0 || color<=6)
        {
            objectPickedUp[color]++;
            setState(true);
            return true;
        }
        else return false;
    }

    public boolean resetPickedColor(int i){
        for (i=0;i<objectPickedUp.length; i++){
            objectPickedUp[i] = 0;
        }
        return true;
    }

    public synchronized void clampBluetooth(int position)
    {
        clampmotor.rotateTo(-position);
    }

    public synchronized void liftBluetooth(int position)
    {
        liftmotor.rotateTo(-position);
    }

    public boolean checkState(){
        return state;
    }

    public void setState(boolean state)
    {
        this.state = state;
    }

    public void setColorRequestedBluetooth(int color, int amount)
    {
        colerRequested[color-1] = amount;
    }

    public void decreaseColorRequested(int color)
    {
        if(color>=0 || color<=6)
        {
            if (colerRequested[color] > 0) colerRequested[color]--;
        }
    }

    public void getColorRequested()
    {
        for (int i=0;i<colerRequested.length;i++){
            if(colerRequested[i]>0){
                addColor(i);
            }
            else {
                removeColor(i);
            }
        }
    }
}
