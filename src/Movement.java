import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

class Movement extends Thread
{
    private int speed = 200;
    private int acceleration = 6000;
    private int angle = 415;
    private static final String FORWARD = "Forward";
    private static final String BACKWARD = "Backward";
    private boolean go = true;
    private EV3LargeRegulatedMotor sx, dx;
    private String direction;
    private boolean mutex = true;
    private boolean automatic = false;
    private boolean manual = false;
    private boolean auto = false;

    /***
     * Constructor for Motor control Thread
     * @param sx Left motor
     * @param dx Right motor
     */
    public Movement(EV3LargeRegulatedMotor sx, EV3LargeRegulatedMotor dx)
    {
        super("MovementController");
        this.dx = dx;
        this.sx = sx;
        direction = FORWARD;
    }

    @Override
    public void run()
    {
        setup();
        while(go)
        {
            if(automatic)
            {
                if(!auto)
                {
                    System.out.println("Automatic activated");
                    LCD.clear();
                    forward();
                    auto = true;
                }
            }
            else {
                if(!manual){
                    System.out.println("Manual activated");
                    LCD.clear();
                    brake();
                    manual = true;
                }
            }
        }
        brake();
    }

    public void turn90DegDx()
    {
        brake();
        System.out.println("turndx");
        LCD.clear();
        dx.rotate(angle,true);
        sx.rotate(-angle);
        brake();
    }

    void turn90DegSx()
    {
        brake();
        System.out.println("turnsx");
        LCD.clear();
        sx.rotate(angle,true);
        dx.rotate(-angle);
        brake();
    }

    public void turnDx()
    {
        brake();
        System.out.println("turndx");
        LCD.clear();
        dx.forward();
        sx.backward();
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        brake();
    }

    void turnSx()
    {
        brake();
        System.out.println("turnsx");
        LCD.clear();
        dx.backward();
        sx.forward();
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        brake();
    }

    public void brake()
    {
        System.out.println("Brake");
        LCD.clear();
        sx.stop(true);
        dx.stop(true);
    }

    public void forward()
    {
        brake();
        System.out.println("forward");
        LCD.clear();
        dx.backward();
        sx.backward();
    }

    void backward()
    {
        brake();
        System.out.println("backward");
        LCD.clear();
        dx.forward();
        sx.forward();
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

    public void changeDirection(){
        if(getDirection().equals(FORWARD)){
            brake();
            backward();
            setDirection(BACKWARD);
        }
        else {
            brake();
            forward();
            setDirection(FORWARD);
        }
    }

    public void setSpeed(int speed){
        dx.setSpeed(speed);
        sx.setSpeed(speed);
    }

    public void setup(){
        dx.resetTachoCount();
        sx.resetTachoCount();
        setSpeed(speed);
        setAcceleration(acceleration);
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

    public void setAuto(){
        auto = false;
        automatic = true;
    }

    public void setManual(){
        manual = false;
        automatic = false;
    }

    public void getTacho()
    {
        System.out.println("tacho dx: "+dx.getTachoCount());
        System.out.println("tacho sx: "+sx.getTachoCount());
    }

    public void getSpeed()
    {
        System.out.println("speed dx: "+dx.getSpeed());
        System.out.println("speed sx: "+sx.getSpeed());
    }

    public void getAcceleration()
    {
        System.out.println("Acc dx: "+dx.getAcceleration());
        System.out.println("Acc sx: "+sx.getAcceleration());
    }

    public void getRotSpeed()
    {
        System.out.println("Rot dx: "+dx.getRotationSpeed());
        System.out.println("Rot sx: "+sx.getRotationSpeed());
    }

    private void setAcceleration(int acceleration)
    {
        dx.setAcceleration(acceleration);
        sx.setAcceleration(acceleration);
    }

    /**
     * Bluetooth function to set which elements to pickup
     **/

    public void turnDxBluetooth()
    {
        System.out.println("turndx");
        LCD.clear();
        brake();
        dx.forward();
        sx.backward();
    }

    void turnSxBluetooth()
    {
        System.out.println("turnsx");
        LCD.clear();
        brake();
        dx.backward();
        sx.forward();
    }

    public void brakeBluetooth()
    {
        brake();
    }

    public void forwardBluetooth()
    {
        forward();
    }

    void backwardBluetooth()
    {
        backward();
    }

}
