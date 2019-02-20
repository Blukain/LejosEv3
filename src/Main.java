import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Key;
import lejos.hardware.Keys;
import lejos.hardware.ev3.EV3;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.internal.ev3.EV3Key;
import lejos.internal.ev3.EV3Port;

import static java.lang.Thread.sleep;

public class Main
{
    private static String S1 = "S1";
    private static String S2 = "S2";
    private static String S3 = "S3";
    private static String S4 = "S4";
    private static String starting = "starting";
    private static String settingup = "\"setting up\"";
    private static String settingOk = "\"Set up completed!\"";
    private static float intensityFloor;

    public static void main(String[] args)
    {
        /** sensori */
        EV3ColorSensor surfaceSensor = new EV3ColorSensor(SensorPort.S1);
        EV3UltrasonicSensor objectSensor = new EV3UltrasonicSensor(SensorPort.S2);
        EV3UltrasonicSensor wallSensor = new EV3UltrasonicSensor(SensorPort.S3);
        EV3ColorSensor colorSensor = new EV3ColorSensor(SensorPort.S4);

        /** Motori*/
        EV3LargeRegulatedMotor motorDx = new EV3LargeRegulatedMotor(MotorPort.A);
        EV3LargeRegulatedMotor liftmotor = new EV3LargeRegulatedMotor(MotorPort.B);
        EV3MediumRegulatedMotor clampmotor = new EV3MediumRegulatedMotor(MotorPort.C);
        EV3LargeRegulatedMotor motorSx = new EV3LargeRegulatedMotor(MotorPort.D);

        /** test part*/

        /*System.out.println("tachocount: " + motorDx.getTachoCount());
        System.out.println("tachocount: " + motorSx.getTachoCount());
        motorDx.rotate(480,true);
        motorSx.rotate(-480);
        System.out.println("tachocount: " + motorDx.getTachoCount());
        System.out.println("tachocount: " + motorSx.getTachoCount());*/

        /*int t = 0;
        while (t<10){
            System.out.println(getDistance(objectSensor));
            try
            {
                sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            t++;
        }*/

        /*System.out.println("position: " + liftmotor.getPosition());
        System.out.println("tachocount: " + liftmotor.getTachoCount());
        while(liftmotor.getPosition() > -113) liftmotor.backward();
        liftmotor.stop();
        System.out.println("position: " + liftmotor.getPosition());
        System.out.println("tachocount: " + liftmotor.getTachoCount());
        while(liftmotor.getPosition() < 0) liftmotor.forward();
        liftmotor.stop();*/

        /*System.out.println("position: " + clampmotor.getPosition());
        System.out.println("tachocount: " + clampmotor.getTachoCount());
        while(clampmotor.getPosition() > -113) clampmotor.backward();
        clampmotor.stop();
        System.out.println("position: " + clampmotor.getPosition());
        System.out.println("tachocount: " + clampmotor.getTachoCount());
        while(clampmotor.getPosition() < 0) clampmotor.forward();
        clampmotor.stop();*/

        /** Threads Creation*/
        Movement movement = new Movement(motorSx,motorDx);
        System.out.println("Creato Movement");

        FallDetection fallDetection = new FallDetection(surfaceSensor, movement);
        System.out.println("Creato FallDetector");

        ArmControl armControl = new ArmControl(liftmotor, clampmotor, colorSensor);
        System.out.println("Creato ArmController");

        ObjectDetection objectDetection = new ObjectDetection(surfaceSensor, objectSensor, wallSensor, movement ,armControl);
        System.out.println("Creato ObjectDetector");

        BluetoothThread bluetoothThread = new BluetoothThread(movement,armControl,fallDetection,objectDetection);
        System.out.println("Creato Bluetooth");

        ButtonDetection buttonDetection = new ButtonDetection(movement,fallDetection,objectDetection,armControl,bluetoothThread);
        System.out.println("Creato ButtonDetector");

        /***
         * If needed this block checkObject distances for setting
         */
        /*intensityFloor = fallDetection.getIntensity();
        System.out.println("Intensita rilevata: " + intensityFloor);
        float distanceObject = objectDetection.getObjectDistance();
        System.out.println("DistanzaOggetto rilevata: " + distanceObject);
        float distanceWall = objectDetection.getWallDistance();
        System.out.println("DistanzaMuro rilevata: " + distanceWall);*/

        /** Thread Starting*/
        armControl.start();
        System.out.println("partito Arm");

        /*fallDetection.start();
        System.out.println("partito Fall");*/

        objectDetection.start();
        System.out.println("partito Object");

        movement.start();
        System.out.println("partito Movement");

        bluetoothThread.start();
        System.out.println("partito Bluetooth");

        buttonDetection.start();
        System.out.println("partito Button");

        try
        {
            armControl.join();
            //fallDetection.join();
            objectDetection.join();
            movement.join();
            bluetoothThread.join();
            buttonDetection.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        /** Sensor Closing*/
        System.out.println("Closing Sensors");
        objectSensor.close();
        wallSensor.close();
        surfaceSensor.close();
        colorSensor.close();

        /** Motor closing*/
        System.out.println("Closing Motors");
        motorDx.close();
        motorSx.close();
        clampmotor.close();
        liftmotor.close();

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

    public static float getDistance(EV3UltrasonicSensor sensor)
    {
        float[] objectSample = new float[sensor.sampleSize()];
        sensor.getDistanceMode().fetchSample(objectSample,0);
        return objectSample[0];
    }
}
