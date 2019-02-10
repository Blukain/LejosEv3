import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;

import static java.lang.Thread.sleep;

public class Main
{
    private static String S1 = "S1";
    private static String S2 = "S2";
    private static String S3 = "S3";
    private static String S4 = "S4";
    private static int speed = 100;
    private static String starting = "starting";
    private static String settingup = "\"setting up\"";
    private static String settingOk = "\"Set up completed!\"";
    private static float intensityFloor;

    public static void main(String[] args)
    {
        /** sensori */
        EV3ColorSensor surfaceSensor = new EV3ColorSensor(LocalEV3.get().getPort(S2));
        EV3UltrasonicSensor objectSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort(S1));
        EV3UltrasonicSensor wallSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort(S3));
        EV3ColorSensor colorSensor = new EV3ColorSensor(LocalEV3.get().getPort(S4));

        EV3LargeRegulatedMotor motorDx = new EV3LargeRegulatedMotor(MotorPort.A);
        EV3LargeRegulatedMotor lift = new EV3LargeRegulatedMotor(MotorPort.B);
        EV3MediumRegulatedMotor clamp = new EV3MediumRegulatedMotor(MotorPort.C);
        EV3LargeRegulatedMotor motorSx = new EV3LargeRegulatedMotor(MotorPort.D);

        Movement movement = new Movement(motorSx,motorDx);
        System.out.println("Creato Movement");

        FallDetection fallDetection = new FallDetection(surfaceSensor, movement);
        System.out.println("Creato FallDetector");

        ArmControl armControl = new ArmControl(lift, clamp, colorSensor);
        System.out.println("Creato ArmController");

        ObjectDetection objectDetection = new ObjectDetection(objectSensor, wallSensor, movement ,armControl);
        System.out.println("Creato ObjectDetector");

        BluetoothThread bluetoothThread = new BluetoothThread(movement,armControl,fallDetection,objectDetection);
        System.out.println("Creato Bluetooth");

        //ButtonDetection buttonDetection = new ButtonDetection(bluetoothThread);
        /*ButtonDetection buttonDetection = new ButtonDetection(movement,fallDetection,objectDetection,armControl,bluetoothThread);
        System.out.println("Creato ButtonDetector");*/

        /***
         * If needed this block checkObject distances for setting
         */
        /*intensityFloor = fallDetection.getIntensity();
        System.out.println("Intensita rilevata: " + intensityFloor);
        float distanceObject = objectDetection.getObjectDistance();
        System.out.println("DistanzaOggetto rilevata: " + distanceObject);
        float distanceWall = objectDetection.getWallDistance();
        System.out.println("DistanzaMuro rilevata: " + distanceWall);*/

        /** Parte Thread*/
        armControl.start();
        System.out.println("partito Arm");

        fallDetection.start();
        System.out.println("partito Fall");

        objectDetection.start();
        System.out.println("partito Object");

        movement.start();
        System.out.println("partito Movement");

        bluetoothThread.start();
        System.out.println("partito Bluetooth");

        /*buttonDetection.start();
        System.out.println("partito Button");*/

        try
        {
            armControl.join();
            fallDetection.join();
            objectDetection.join();
            movement.join();
            bluetoothThread.join();
            //buttonDetection.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Main finished");
    }
/*
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
    }*/

    public static void shutDown(){
        System.out.println("Shutting down!!!");
        System.exit(0);
    }
}
