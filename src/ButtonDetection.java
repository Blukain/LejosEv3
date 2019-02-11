class ButtonDetection extends Thread
{
    private boolean go;
    private Movement movement;
    private FallDetection fallDetection;
    private ObjectDetection objectDetection;
    private ArmControl armControl;
    private BluetoothThread bluetooth;

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
    {/*
        while(go)
        {
            if(Button.waitForAnyPress()==Button.ID_ENTER)
            {
                if (movement != null)
                {
                    movement.terminate();
                }
                if (armControl != null)
                {
                    //armControl.terminate();
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
        }*/
        try
        {
            sleep(15000);
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
