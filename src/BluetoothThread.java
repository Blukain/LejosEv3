import lejos.hardware.Bluetooth;
import lejos.hardware.LocalBTDevice;
import lejos.remote.nxt.BTConnection;
import lejos.remote.nxt.BTConnector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BluetoothThread extends Thread
{
    /** Bluetooth connection variable */
    private int NOTCONNECTED = 0;
    private int SOCKETOPENED = 1;
    private int CONNECTED = 2;
    private int ERROR = 3;
    private int TERMINATE = 4;
    private int state=NOTCONNECTED;
    private int timeout = 10000;
    private boolean go = true;
    private BTConnection btConnection = null;
    private BTConnector btConnector = null;
    private String message = null;
    private DataInputStream reader = null;
    private DataOutputStream writer = null;
    private int data;
    private byte[] inBuffer;
    private byte[] outBuffer;
    /** Bluetooth message variable*/
    private byte STOP = 0;
    private byte FORWARD = 1;
    private byte BACKWARD = 2;
    private byte TURNSX = 3;
    private byte TURNDX = 4;
    private byte ARM = 5;
    private byte CLAMP = 6;
    private byte AUTO = 7;
    private byte MANUAL = 8;

    private byte COLORE = 9;
    private byte NOCOLOR = 10;
    private byte BLACK = 11;
    private byte BLUE = 12;
    private byte GREEN = 13;
    private byte YELLOW = 14;
    private byte RED = 15;
    private byte WHITE = 16;
    private byte BROWN = 17;

    /** Robot Variable */
    private Movement movement;
    private ArmControl armControl;
    private FallDetection fallDetection;
    private ObjectDetection objectDetection;
    private static int[] objectPickedUp;

    /** Lcd messages */
    private String waiting = "Waiting for\n connection..";
    private String connected = "Connected";
    private String closing = "Closing connection";
    private String error = "Connection error";
    private String terminated = "Terminated";
    private int bytes;
    private String socket = "Socket opened";

    public BluetoothThread(Movement movement, ArmControl armControl, FallDetection fallDetection, ObjectDetection objectDetection)
    {
        this.movement = movement;
        this.armControl = armControl;
        this.objectDetection = objectDetection;
        this.fallDetection = fallDetection;
    }

    @Override
    public void run()
    {
        while(go && state!=TERMINATE)
        {
            if(state==NOTCONNECTED)
            {
                startConnection();
            }
            else if(state==SOCKETOPENED)
            {
                WaitMessage();
            }
            else if(state == CONNECTED)
            {
                int length=0;
                System.out.println("waiting command");
                try
                {
                    length = reader.readInt(); //read amount of byte
                }
                catch (IOException e)
                {
                    System.out.println("Error length: "+length+" stack: "+e);
                    state = ERROR;
                }
                inBuffer = new byte[length];
                if(length>0)
                {
                    //System.out.println("length > 0");
                    try
                    {
                        bytes = reader.read(inBuffer, 0, inBuffer.length);// bytes read from incoming message
                    }
                    catch (IOException e)
                    {
                        System.out.println("ERROR read: " + e);
                        state = ERROR;
                        //interruptConnection(btConnector);
                    }
                    if (bytes > 0)
                    {
                        if (inBuffer[0] == MANUAL)
                        {
                            System.out.println("manual command");
                            movement.setManual();
                            fallDetection.setManual();
                            objectDetection.setManual();
                        }
                        else if (inBuffer[0] == AUTO)
                        {
                            System.out.println("auto command");
                            armControl.getColorRequested();
                            movement.setAuto();
                            fallDetection.setAuto();
                            objectDetection.setAuto();
                        }
                        else if (inBuffer[0] == TURNSX)
                        {
                            movement.turnSxBluetooth();
                        }
                        else if (inBuffer[0] == TURNDX)
                        {
                            movement.turnDxBluetooth();
                        }
                        else if (inBuffer[0] == FORWARD)
                        {
                            movement.forwardBluetooth();
                        }
                        else if (inBuffer[0] == BACKWARD)
                        {
                            movement.backwardBluetooth();
                        }
                        else if (inBuffer[0] == STOP)
                        {
                            movement.brakeBluetooth();
                        }
                        else if (inBuffer[0] == CLAMP)
                        {
                            if (length==2)
                            {
                                armControl.clampBluetooth(inBuffer[1]);
                            }
                        }
                        else if (inBuffer[0] == ARM)
                        {
                            if (length==2)
                            {
                                armControl.liftBluetooth(inBuffer[1]);
                            }
                        }

                        else if (inBuffer[0] == COLORE)
                        {
                            if (length==3)
                            {
                                System.out.println("Colori Command");
                                armControl.setColorRequestedBluetooth(inBuffer[1], inBuffer[2]);
                            }
                        }
                        else
                        {
                            System.out.println("Unrecognized Command");
                        }
                    }
                    if (armControl.checkState())
                    {
                        objectPickedUp = armControl.getObjectPickedUp();
                        outBuffer = new byte[objectPickedUp.length+1];
                        outBuffer[0] = COLORE;
                        for (int i = 0; i < objectPickedUp.length; i++)
                        {
                            //outBuffer[i+1] = colorConvert(i);
                            outBuffer[i+1] = (byte) objectPickedUp[i];
                        }
                        try
                        {
                            writer.writeInt(outBuffer.length);
                            writer.write(outBuffer);
                            writer.flush();
                        }
                        catch (IOException e)
                        {
                            System.out.println("Color send ERROR: is stream open? STACK: "+e);
                        }
                        armControl.setState(false);
                    }
                }
                inBuffer = null;
            }
            else if(state == ERROR)
            {
                System.out.println(error);
                interruptConnection();
                state = NOTCONNECTED;
            }
        }
        System.out.println(terminated);
        System.out.println("Bluetooth Thread ENDED");
    }

    private void WaitMessage()
    {
        try
        {
            System.out.println("Waiting message");
            while ((message = getMessage()) != null)
            {
                System.out.println("Received: " + message);
                if (message.equals("hello"))
                {
                    System.out.println("Sent: welcome");
                    writer.writeUTF("welcome");
                    writer.flush();
                    System.out.println("Activating App...");
                    state = CONNECTED;
                    System.out.println(connected);
                    break;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("ERROR: BT Socket crashed!");
            interruptConnection();
        }
    }

    private void interruptConnection()
    {
        if(state==NOTCONNECTED){
            System.out.println("Cancel Connection");
            btConnector.cancel();
        }
        else
        {
            System.out.println(closing);
            stopConnection();
            System.out.println("Closed");
        }
    }

    public void startConnection () {
        LocalBTDevice localBTDevice = Bluetooth.getLocalDevice();
        if (!localBTDevice.getVisibility())
        {
            localBTDevice.setVisibility(true);
        }
        btConnector = new BTConnector();
        try
        {
            System.out.println(waiting);
            btConnection = btConnector.waitForConnection(timeout, BTConnection.RAW);
            reader = new DataInputStream(btConnection.openInputStream());
            writer = new DataOutputStream(btConnection.openDataOutputStream());
            state = SOCKETOPENED;
            System.out.println(socket);
        }
        catch (Exception ex)
        {
            System.out.println("Connection timedout: waited 10 sec");
            interruptConnection();
        }
    }

    public String getMessage(){
        try
        {
            message = reader.readUTF();
        }
        catch (IOException e)
        {
            System.out.println("Error getting message is socket still opened?");
        }
        return message;
    }
/*
    public byte getData(){
        try
        {
            data = reader.readByte();
        }
        catch (IOException e)
        {
            System.out.println("Error EOF Reached");
            return -1;
        }
        return data;
    }*/

    public int getData(){
        try
        {
            bytes = reader.read(inBuffer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return btConnection.read(inBuffer,bytes);
    }

    public void stopConnection() {
        try
        {
            reader.close();
            writer.close();
            btConnection.close();
        }
        catch (IOException e)
        {
            System.out.println("Error Closing Stream");
        }
    }

    /***
     * Color detected by the sensor, categorized by overall value. <br>
     * - 0: No color<br>
     * - 1: Black<br>
     * - 2: Blue<br>
     * - 3: Green<br>
     * - 4: Yellow<br>
     * - 5: Red<br>
     * - 6: White<br>
     * - 7: Brown
     * Color converter number to String
     * @param color integer number
     * @return String name of corresponding color number
     */
    private byte colorConvert (int color){
        if (color == 1) return BLACK;
        else if (color == 2) return BLUE;
        else if (color == 3) return GREEN;
        else if (color == 4) return YELLOW;
        else if (color == 5) return RED;
        else if (color == 6) return WHITE;
        else if (color == 7) return BROWN;
        else return NOCOLOR;
    }

    private int colorConvert (byte color){
        if (color == BLACK) return armControl.BLACK;
        else if (color == BLUE) return armControl.BLUE;
        else if (color == GREEN) return armControl.GREEN;
        else if (color == YELLOW) return armControl.YELLOW;
        else if (color == RED) return armControl.RED;
        else if (color == WHITE) return armControl.WHITE;
        else if (color == BROWN) return armControl.BROWN;
        else return NOCOLOR;
    }

    public void terminate()
    {
        go=false;
    }
}
