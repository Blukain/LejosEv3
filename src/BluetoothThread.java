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
    private Sender sender;
    private BTConnector btConnector = null;
    private String message = null;
    private DataInputStream reader = null;
    private DataOutputStream writer = null;
    private int bytes;
    private int data;
    private byte[] inBuffer;
    private byte[] outBuffer;
    private BTConnection btConnection;

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

    private byte DETECTED = 20;
    private byte PICKED = 21;
    private byte NOTPICKED = 22;

    private byte RESET = 99;

    /** Robot Variable */
    private Movement movement;
    private boolean go = true;
    private ArmControl armControl;
    private FallDetection fallDetection;
    private ObjectDetection objectDetection;
    private static int[] objectPickedUp;
    private int objectDetected = 0;
    private int objectNotPicked = 0;
    private int objectPicked = 0;

    /** Lcd messages */
    private String waiting = "Waiting for\n connection..";
    private String connected = "Connected";
    private String closing = "Closing connection";
    private String error = "Connection error";
    private String terminated = "Terminated";
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
        setup();
        while(go && state!=TERMINATE)
        {
            if(state==NOTCONNECTED)
            {
                System.out.println(waiting);
                btConnection = btConnector.waitForConnection(timeout, BTConnection.RAW);
                if(btConnection != null)
                {
                    openStream();
                }
                else {
                    System.out.println("Connection timed out: waited 10 sec");
                }
            }
            else if(state==SOCKETOPENED)
            {
                System.out.println("Waiting message");
                WaitMessage();
            }
            else if(state == CONNECTED)
            {
                if(sender == null)
                {
                    sender = new Sender();
                    sender.start();
                }
                else {
                    if (sender.getState()!=State.RUNNABLE){
                        sender.start();
                    }
                }
                System.out.println("waiting command");
                int length=0;
                length = ReadMessageLength(length);
                inBuffer = new byte[length];
                if(length>0)
                {
                    bytes = ReadMessage();
                    System.out.println("Bytes Read: "+bytes);
                    if (bytes > 0)
                    {
                        if (inBuffer[0] == RESET)
                        {
                            System.out.println("Reset Command");
                        }
                        else if (inBuffer[0] == MANUAL)
                        {
                            System.out.println("manual command");
                            setRobotManual();
                        }
                        else if (inBuffer[0] == AUTO)
                        {
                            System.out.println("auto command");
                            armControl.getColorRequested();
                            setRobotAuto();
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
                                armControl.clampStopBluetooth();
                                armControl.clampBluetooth(inBuffer[1]);
                            }
                        }
                        else if (inBuffer[0] == ARM)
                        {
                            if (length==2)
                            {
                                armControl.liftStopBluetooth();
                                armControl.liftBluetooth(inBuffer[1]);
                            }
                        }

                        else if (inBuffer[0] == COLORE)
                        {
                            if (length==3)
                            {
                                System.out.println("Colori Command");
                                System.out.println("Color: "+inBuffer[1]);
                                System.out.println("Amount: "+inBuffer[2]);
                                armControl.setColorRequestedBluetooth(inBuffer[1], inBuffer[2]);
                            }
                        }
                        else
                        {
                            System.out.println("Unrecognized Command");
                        }
                    }
                }
                else{
                    state = ERROR;
                }
                inBuffer = null;
            }
            else if(state == ERROR)
            {
                System.out.println(error);
                cancelConnection();
                closeStream();
                //closeConnection(btConnection);
                state = NOTCONNECTED;
            }
        }
        System.out.println(terminated);
        System.out.println("Bluetooth Thread ENDED");
    }

    private void cancelConnection()
    {
        btConnector.cancel();
    }

    private void setRobotAuto()
    {
        //fallDetection.setAuto();
        objectDetection.setAuto();
        movement.setAuto();
    }

    private void setRobotManual()
    {
        movement.setManual();
        objectDetection.setManual();
        //fallDetection.setManual();
    }

    private void sendSingleData(byte type, int variable)
    {
        outBuffer = new byte[2];
        outBuffer[0] = type;
        outBuffer[1] = (byte) variable;
        sendData();
    }

    private void sendMultipleData(byte type, int[] array,int length)
    {
        outBuffer = new byte[length + 1];
        outBuffer[0] = type;
        for (int i = 0; i < length; i++)
        {
            outBuffer[i + 1] = (byte) array[i];
        }
        sendData();
    }

    private void sendData()
    {
        System.out.println("Sending data");
        try
        {
            writer.writeInt(outBuffer.length);
            writer.write(outBuffer);
            writer.flush();
            System.out.println("Sent:");
            System.out.println("length: "+outBuffer.length);
            System.out.println("data: "+outBuffer);
        }
        catch (IOException e)
        {
            System.out.println("SEND ERROR: is stream open?");
        }
        outBuffer = null;
    }

    private int ReadMessage()
    {
        int bytes = -1;
        try
        {
            bytes = reader.read(inBuffer, 0, inBuffer.length);// bytes read from incoming message
        }
        catch (IOException e)
        {
            System.out.println("ERROR reading message: " + e);
            state = ERROR;
        }
        return bytes;
    }

    private int ReadMessageLength(int length)
    {
        try
        {
            length = reader.readInt(); //read amount of byte
        }
        catch (IOException e)
        {
            System.out.println("Error reading length");
            state = ERROR;
            length = 0;
        }
        return length;
    }

    private void WaitMessage()
    {
        try
        {
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
            closeStream();
        }
    }

    private void closeConnection()
    {
        if (btConnection != null)
        {
            System.out.println(closing);
            try
            {
                btConnection.close();
                state = NOTCONNECTED;
                System.out.println("Closed");
            }
            catch (IOException e)
            {
                System.out.println("Error closing connection");
                e.printStackTrace();
            }
        }
    }

    public void setup() {
        LocalBTDevice localBTDevice = Bluetooth.getLocalDevice();
        if (!localBTDevice.getVisibility())
        {
            localBTDevice.setVisibility(true);
        }
        if(btConnector == null) btConnector = new BTConnector();
    }

    private void openStream()
    {
        try
        {
            System.out.println("Opening Stream");
            reader = new DataInputStream(btConnection.openInputStream());
            writer = new DataOutputStream(btConnection.openDataOutputStream());
            state = SOCKETOPENED;
            System.out.println(socket);
        }
        catch (Exception ex)
        {
            System.out.println("Error opening stream");
            ex.printStackTrace();
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
            state = ERROR;
        }
        return message;
    }

    public void closeStream() {
        System.out.println("Closing stream");
        try
        {
            if(reader!=null)
            {
                reader.close();
                reader = null;
            }
            if(writer!=null)
            {
                writer.close();
                writer = null;
            }
            System.out.println("Closed");
        }
        catch (IOException e)
        {
            System.out.println("Error Closing Stream");
        }
    }

    public void terminate()
    {
        go=false;
    }

    public class Sender extends Thread{
        @Override
        public void run()
        {
            System.out.println("Sender Running");
            while (state == CONNECTED)
            {
                if (armControl.checkState())
                {
                    System.out.println("State Send");
                    /*objectPickedUp = convertArray(armControl.getObjectPickedUp());*/
                    objectPickedUp = armControl.getObjectPickedUp();
                    sendMultipleData(COLORE, objectPickedUp, objectPickedUp.length);
                    objectNotPicked = armControl.getObjectNotPicked();
                    sendSingleData(NOTPICKED, objectNotPicked);
                    objectPicked = armControl.getObjectPicked();
                    sendSingleData(PICKED, objectPicked);
                    objectDetected = armControl.getObjectDetected();
                    sendSingleData(DETECTED, objectDetected);
                    armControl.setState(false);
                }
            }
            System.out.println("Sender Terminated");
        }
    }
}
