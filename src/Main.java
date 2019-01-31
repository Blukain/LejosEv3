import org.ev3dev.hardware.LED;
import org.ev3dev.hardware.Speaker;
import org.ev3dev.hardware.motors.LargeMotor;
import org.ev3dev.hardware.motors.MediumMotor;
import org.ev3dev.hardware.ports.LegoPort;
import org.ev3dev.hardware.sensors.ColorSensor;
import org.ev3dev.hardware.sensors.UltrasonicSensor;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import java.io.IOException;

public class Main
{
    private static float floor;
    static int speed = 200;
    static String starting = "starting";
    static String settingup = "\"setting up\"";
    static String settingOk = "\"Set up completed!\"";
    private static final String NORMAL = "normal";
    private static final String INVERSED = "inversed";
    private static Speaker speaker = new Speaker();
    private static float intensityFloor;

    public static void main(String[] args)
    {
        /** Bluetooth part*/
        /*try
        {
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            System.out.println("Name: "+localDevice.getFriendlyName());
            System.out.println("Address: "+localDevice.getBluetoothAddress());
            System.out.println("Powered: "+LocalDevice.isPowerOn());
            DiscoveryAgent discoveryAgent = localDevice.getDiscoveryAgent();
            RemoteDevice remoteDevice[] = discoveryAgent.retrieveDevices(DiscoveryAgent.PREKNOWN);
            for (RemoteDevice rm: remoteDevice)
            {
                System.out.println("Remote Name: "+rm.getFriendlyName(false));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }*/
        /*Main ev3 = new Main();*/
        /*speaker.setSpeed(100);/*
        speak(starting);
        speak(settingup);
        ev3.setUp();*/

        ColorSensor surfaceSensor = new ColorSensor(new LegoPort(LegoPort.INPUT_2));
        UltrasonicSensor objectSensor = new UltrasonicSensor(new LegoPort(LegoPort.INPUT_1));
        UltrasonicSensor wallSensor = new UltrasonicSensor(new LegoPort(LegoPort.INPUT_3));
        ColorSensor colorSensor = new ColorSensor(new LegoPort(LegoPort.INPUT_4));
        //GyroSensor gyroSensor = new GyroSensor(new LegoPort(LegoPort.INPUT_4));

        /***
         * If needed this block checkObject distances for setting
         */
        /*intensityFloor = surfaceSensor.getReflectedLightIntensity();
        System.out.println("Intensita rilevata: " + intensityFloor);*/
        /*float distanceObject = objectSensor.getDistanceCentimeters();
        System.out.println("DistanzaOggetto rilevata: " + distanceObject);
        float distanceWall = wallSensor.getDistanceCentimeters();
        System.out.println("DistanzaMuro rilevata: " + distanceWall);*/

        LegoPort A = new LegoPort(LegoPort.OUTPUT_A);
        LegoPort B = new LegoPort(LegoPort.OUTPUT_B);
        LegoPort C = new LegoPort(LegoPort.OUTPUT_C);
        LegoPort D = new LegoPort(LegoPort.OUTPUT_D);

        //speak(settingOk);


        try
        {
            LargeMotor motorDx = new LargeMotor(A);
            LargeMotor lift = new LargeMotor(B);
            MediumMotor clamp = new MediumMotor(C);
            LargeMotor motorSx = new LargeMotor(D);

            Movement movement = new Movement(motorSx,motorDx,speed,Movement.INVERSED);
            System.out.println("Creato Movement");

            FallDetection fallDetection = new FallDetection(surfaceSensor, movement);
            System.out.println("Creato FallDetector");
/*
            ArmControl armControl = new ArmControl(lift, clamp, colorSensor);
            System.out.println("Creato ArmController");
            armControl.addColor(ArmControl.YELLOW);*/
            ArmControlFinal armControl = new ArmControlFinal(lift, clamp, colorSensor);
            System.out.println("Creato ArmController");
            armControl.addColor(ArmControl.YELLOW);

            /*ObjectDetection objectDetection = new ObjectDetection(objectSensor, wallSensor, movement ,armControl);
            System.out.println("Creato ObjectDetector");*/
            ObjectDetectionFinal objectDetection = new ObjectDetectionFinal(objectSensor, wallSensor, movement ,armControl);
            System.out.println("Creato ObjectDetector");

            /*WallDetection wallDetection = new WallDetection(wallSensor,movement);
            System.out.println("Creato WallDetector");*/

            ButtonDetection buttonDetection = new ButtonDetection(movement,fallDetection,objectDetection,armControl);
            System.out.println("Creato ButtonDetector");

            /*GyroDetection gyroDetection = new GyroDetection(gyroSensor,movement);
            System.out.println("Creato GyroDetector");
            movement.setGyroDetection(gyroDetection);*/

            /*System.out.println("Variabili motore sx: "+ movement.isMotorSxGo());
            System.out.println("Variabili motore dx: "+ movement.isMotorDxGo());*/

            fallDetection.start();
            System.out.println("partito Fall");

            /*wallDetection.start();
            System.out.println("partito Wall");*/

            armControl.start();
            System.out.println("partito Arm");

            objectDetection.start();
            System.out.println("partito Object");

            /*gyroDetection.start();
            System.out.println("partito Gyro");*/

            buttonDetection.start();
            System.out.println("partito Button");

            movement.start();
            System.out.println("partito Movement");

            /*speak("Ready");
            Thread.sleep(1000);*/
            speaker.playText("Go");
            movement.turnOn("Main");
            movement.join();
            fallDetection.join();
            armControl.join();
            objectDetection.join();
            buttonDetection.join();
            /*System.out.println("Variabili motore sx: "+ movement.isMotorSxGo());
            System.out.println("Variabili motore dx: "+ movement.isMotorDxGo());
            int i = 30;
            System.out.println("attendo "+i+" secondi");
            //System.out.println(i);
            while(i>0){
                Thread.sleep(1000);
                i--;
                //if(i<=3) System.out.println(i);
            }
            System.out.println("TERMINATING.....");
            movement.terminate();
            Thread.sleep(1000);
            System.out.println("Movement alive: "+ movement.isAlive());
            fallDetection.terminate();
            Thread.sleep(1000);
            System.out.println("Fall alive: "+fallDetection.isAlive());
            objectDetection.terminate();
            Thread.sleep(1000);
            System.out.println("Object alive: "+objectDetection.isAlive());
            armControl.terminate();
            Thread.sleep(1000);
            System.out.println("Arm alive: "+armControl.isAlive());
            /*gyroDetection.terminate();
            Thread.sleep(1000);
            System.out.println("Gyro alive: "+gyroDetection.isAlive());*/
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Main finished");
    }

    public void setUp()
    {
        LED leftGreen = new LED(LED.LEFT,LED.GREEN);
        LED rightGreen = new LED(LED.RIGHT,LED.GREEN);
        LED leftRed = new LED(LED.LEFT,LED.RED);
        LED rightRed = new LED(LED.RIGHT,LED.RED);
        leftGreen.setBrightness(0);
        rightGreen.setBrightness(0);
        for (int i=0; i<5; i++)
        {
            leftRed.setBrightness(255);
            rightRed.setBrightness(255);
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            leftRed.setBrightness(0);
            rightRed.setBrightness(0);
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        leftGreen.setBrightness(255);
        rightGreen.setBrightness(255);
    }

    public static void shutDown(){
        System.out.println("Shutting down!!!");
        System.exit(0);
    }
}
