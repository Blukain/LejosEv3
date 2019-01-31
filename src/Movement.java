import org.ev3dev.hardware.motors.Motor;

class Movement extends Thread
{
    public static final String NORMAL = "normal";
    public static final String INVERSED = "inversed";
    public static final String FORWARD = "Forward";
    public static final String BACKWARD = "Backward";
    private boolean motorSxGo;
    private boolean motorDxGo;
    private boolean go=true;
    private Motor sx, dx;
    private String direction;
    private int speed;
    private String way;
    private boolean mutex=true;

    /***
     * Constructor for Motor control Thread
     * @param sx Left motor
     * @param dx Right motor
     */
    public Movement(Motor sx, Motor dx, int speed, String way)
    {
        super("MovementController");
        this.dx = dx;
        this.sx = sx;
        this.speed=speed;
        this.way=way;
        direction = FORWARD;
    }

    @Override
    public void run()
    {
        setup();
        while(go)
        {
            if (motorDxGo) dx.runForever();
            else dx.stop();
            if (motorSxGo) sx.runForever();
            else sx.stop();
        }
        brake();
    }

    public void turnDx()
    {
        turnOff();
        if(direction.equals(FORWARD)) sx.setPolarity(sx.getPolarity().equals("normal") ? "inversed" : "normal");
        else dx.setPolarity(dx.getPolarity().equals("normal") ? "inversed" : "normal");
        //setSpeed_SP();
        sx.setPolarity(INVERSED);
        dx.setPolarity(NORMAL);
        sx.setSpeed_SP(speed);
        dx.setSpeed_SP(speed);
        turnOn(this.getName());
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        turnOff();
    }

    void turnSx()
    {
        turnOff();
        if(direction.equals(FORWARD)) sx.setPolarity(sx.getPolarity().equals("normal") ? "inversed" : "normal");
        else dx.setPolarity(dx.getPolarity().equals("normal") ? "inversed" : "normal");
        //setSpeed_SP();
        dx.setPolarity(INVERSED);
        sx.setPolarity(NORMAL);
        sx.setSpeed_SP(speed);
        dx.setSpeed_SP(speed);
        turnOn(this.getName());
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        turnOff();
    }

    void turnSxRel()
    {
        int pos = sx.getPosition();
        int delta=430;
        turnOff();
        sx.setSpeed_SP(speed);
        dx.setSpeed_SP(speed);
        sx.setPosition_SPInt(delta);
        dx.setPosition_SPInt(-delta);
        sx.runToRelPos();
        dx.runToRelPos();
        while (pos<pos+delta){
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

    void turnDxRel()
    {
        int pos = dx.getPosition();
        int delta=430;
        turnOff();
        sx.setSpeed_SP(speed);
        dx.setSpeed_SP(speed);
        dx.setPosition_SPInt(delta);
        sx.setPosition_SPInt(-delta);
        sx.runToRelPos();
        dx.runToRelPos();
        while (pos<pos+delta){
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

    public void brake()
    {
        sx.stop();
        dx.stop();
    }

    void turnOff()
    {
        setMotorDxGo(false);
        setMotorSxGo(false);
    }

    void turnOn(String name){
        //System.out.println("TURNED ON BY: "+name);
        setMotorDxGo(true);
        setMotorSxGo(true);
    }

    public void forward()
    {
        turnOff();
        dx.setPolarity(INVERSED);
        sx.setPolarity(INVERSED);
        setSpeed_SP();
        if (!isMotorDxGo() && !isMotorSxGo()) turnOn(this.getName());
    }

    void backward()
    {
        turnOff();
        dx.setPolarity(NORMAL);
        sx.setPolarity(NORMAL);
        setSpeed_SP();
        if (!isMotorDxGo() && !isMotorSxGo()) turnOn(this.getName());
    }

    public boolean isMotorSxGo()
    {
        return motorSxGo;
    }

    public boolean isMotorDxGo()
    {
        return motorDxGo;
    }

    public void setMotorSxGo(boolean motorSxGo)
    {
        this.motorSxGo = motorSxGo;
    }

    public void setMotorDxGo(boolean motorDxGo)
    {
        this.motorDxGo = motorDxGo;
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
        if (isMotorDxGo() && isMotorSxGo()) turnOff();
        dx.setPolarity(dx.getPolarity().equals("normal") ? "inversed" : "normal");
        sx.setPolarity(sx.getPolarity().equals("normal") ? "inversed" : "normal");
        setDirection(getDirection().equals(FORWARD) ? BACKWARD : FORWARD);
        setSpeed_SP();
        turnOn(this.getName());
    }

    public void setSpeed_SP(){
        dx.setSpeed_SP(speed);
        sx.setSpeed_SP(speed);
    }

    public void setup(){
        dx.reset();
        sx.reset();
        dx.setPolarity(way);
        sx.setPolarity(way);
        dx.setSpeed_SP(speed);
        sx.setSpeed_SP(speed);
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

    public synchronized void operateDone(){
        mutex=true;
        notifyAll();
    }
}
