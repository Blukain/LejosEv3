import lejos.hardware.Button;

class ButtonDetection extends Thread
{
    private boolean go;
    private Movement movement;
    private FallDetection fallDetection;
    private ObjectDetection objectDetection;
    private ArmControl armControl;
    private BluetoothThread bluetooth;
    private int position;

    public ButtonDetection(BluetoothThread bluetooth)
    {
        this.bluetooth = bluetooth;
    }

    public ButtonDetection(Movement movement, FallDetection fallDetection, ObjectDetection objectDetection, ArmControl armControl)
    {
        super("ButtonDetector");
        go = true;
        this.movement = movement;
        this.fallDetection = fallDetection;
        this.objectDetection = objectDetection;
        this.armControl = armControl;
    }

    public ButtonDetection(Movement movement, FallDetection fallDetection, ObjectDetection objectDetection, ArmControl armControl, BluetoothThread bluetooth)
    {
        this(movement,fallDetection,objectDetection,armControl);
        this.bluetooth = bluetooth;
    }

    @Override
    public void run()
    {
        position = 0;
        while(go)
        {
            int id = Button.waitForAnyPress();
            if(id == Button.ID_ENTER){
                movement.setAuto();
                objectDetection.setAuto();
                //fallDetection.setAuto();
            }
            if(id == Button.ID_ESCAPE)
            {
                if (movement != null)
                {
                    movement.terminate();
                }
                if (armControl != null)
                {
                    armControl.terminate();
                }
                if (fallDetection != null)
                {
                    fallDetection.terminate();
                }
                if (objectDetection != null)
                {
                    objectDetection.terminate();
                }
                if (bluetooth != null)
                {
                    bluetooth.terminate();
                }
                terminate();
            }
            if(id == Button.ID_RIGHT) armControl.openClamp();
            if(id == Button.ID_LEFT) armControl.closeClamp();
            if(id == Button.ID_UP){
                armControl.liftUp();
                position = 0;
            }
            if(id == Button.ID_DOWN){
                if(position<100){
                    position = position+10;
                    armControl.liftBluetooth(position);
                }
            }
        }
        try
        {
            sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        Main.shutDown();
    }

    public void terminate()
    {
        go=false;
    }
}
