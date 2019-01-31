import org.ev3dev.hardware.sensors.GyroSensor;

class GyroDetection extends Thread
{
    private GyroSensor gyroSensor;
    private boolean go = true;
    private Movement movement;
    private int startingAngle;
    private int direction;
    private boolean stop;
    public static final int LEFT = 1;
    public static final int RIGHT = -1;

    private int degree;

    public GyroDetection(GyroSensor gyroSensor, Movement movement)
    {
        super("Gyro");
        this.gyroSensor = gyroSensor;
        this.movement = movement;
        stop = false;
    }

    @Override
    public void run()
    {
        reset();
        while(go)
        {
            degree = gyroSensor.getAngle();
            if(stop){
                if(direction==LEFT && (degree > startingAngle+(90*direction))){
                    movement.turnOff();
                    stop = false;
                }
                if(direction==RIGHT && (degree < startingAngle+(90*direction))){
                    movement.turnOff();
                    stop = false;
                }
                //movement.forward()
            }
        }
    }

    public int getDegree()
    {
        return degree;
    }

    public void reset(){
        if (gyroSensor.getMode().equals(GyroSensor.SYSFS_ANGLE_MODE))
        {
            gyroSensor.setMode(GyroSensor.SYSFS_RATE_MODE);
            gyroSensor.setMode(GyroSensor.SYSFS_ANGLE_MODE);
        }
        else {
            gyroSensor.setMode(GyroSensor.SYSFS_ANGLE_MODE);
            gyroSensor.setMode(GyroSensor.SYSFS_RATE_MODE);
        }

    }

    public void setStartingDegree(int turn)
    {
        direction = turn;
        startingAngle = getDegree();
        System.out.println("starting angle: "+startingAngle);
        stop = true;
    }

    public void terminate()
    {
        go=false;
    }
}
