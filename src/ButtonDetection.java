import org.ev3dev.hardware.Button;

class ButtonDetection extends Thread
{
    private boolean go;
    private Movement movement;
    private FallDetection fallDetection;
    private ObjectDetectionFinal objectDetection;
    private ArmControlFinal armControl;
    private WallDetection wallDetection;
    private Button right = new Button(Button.BUTTON_RIGHT);
    private Button left = new Button(Button.BUTTON_LEFT);
    private Button esc = new Button(Button.BUTTON_BACKSPACE);

    public ButtonDetection(Movement movement, FallDetection fallDetection, ObjectDetectionFinal objectDetection, ArmControlFinal armControl)
    {
        super("ButtonDetector");
        go = true;
        this.movement = movement;
        this.fallDetection = fallDetection;
        this.objectDetection = objectDetection;
        this.armControl = armControl;
    }

    public ButtonDetection(Movement movement, FallDetection fallDetection, ObjectDetectionFinal objectDetection, ArmControlFinal armControl, WallDetection wallDetection)
    {
        this(movement,fallDetection,objectDetection,armControl);
        this.wallDetection = wallDetection;
    }

    @Override
    public void run()
    {
        //System.out.println("Awaiting starting sequence button.....");
        while(go)
        {
            if (right.isPressed()){
                movement.operate();
                movement.turnOn(this.getName());
                movement.operateDone();
            }
            if (left.isPressed()){
                movement.operate();
                movement.turnOff();
                movement.operateDone();
            }
            if (esc.isPressed()){
                movement.terminate();
                armControl.terminate();
                fallDetection.terminate();
                objectDetection.terminate();
                terminate();
            }
        }
        Main.shutDown();
    }

    public void terminate()
    {
        go=false;
    }
}
