package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.*;
import org.firstinspires.ftc.teamcode.kaicode.PIDFArmController;

@TeleOp
@Config
public class CCTeleOpController extends OpMode {
    DcMotor leftFront;
    DcMotor rightFront;
    DcMotor leftBack;
    DcMotor rightBack;
    DcMotor pixelArm;
    PIDFArmController pidfArmController;
    double pixelArmToRadiansConstant = -1 / 50.9 * 13.1 * (Math.PI/180);

    public static double p = -1.3;
    public static double kg = -1;
    public static double i;
    public static double d;
    public static double kv = -1.5;
    public static double ka;
    DcMotor hookArm;
    Servo hookElbow;
    Servo fingerRight;
    Servo fingerLeft;
    boolean fingers;
    boolean oldXButton;
    boolean oldAButtonC2;
    boolean oldBButtonC2;
    boolean elbow;
    boolean oldCircle;
    boolean oldTriangle;

    int pixelPlacerState;

    ColorRangeSensor colorMan;

    double speedCoefficient = 1;

    boolean[] savedStates1 = {false, false};

    @Override
    public void init() {

        leftFront = hardwareMap.get(DcMotor.class, "frontLeft");
        rightFront = hardwareMap.get(DcMotor.class, "frontRight");
        leftBack = hardwareMap.get(DcMotor.class, "backLeft");
        rightBack = hardwareMap.get(DcMotor.class, "backRight");

        //colorMan = hardwareMap.get(ColorRangeSensor.class, "colorSensor");

        leftFront.setDirection(DcMotorSimple.Direction.REVERSE);
        leftBack.setDirection(DcMotorSimple.Direction.REVERSE);

        fingerRight = hardwareMap.get(Servo.class, "fingerRight");
        fingerLeft = hardwareMap.get(Servo.class, "fingerLeft");

        pixelArm = hardwareMap.get(DcMotor.class, "pixelArm");
        pixelArm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        pixelArm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        pidfArmController = new PIDFArmController(p,i,d,kv,ka,kg,0);
        //kg:-1
        //kv:-1.5
        //p:-1.2
        pidfArmController.launch(-28 *Math.PI/180,System.currentTimeMillis());

        //pixelArm.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, new PIDFCoefficients(0, 0, 0, 0));

        hookArm = hardwareMap.get(DcMotorEx.class, "hookArm");
        hookArm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        hookArm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        hookElbow = hardwareMap.get(Servo.class, "hookElbow");

        hookElbow.setPosition(1);

    }

    @Override
    public void loop() {
        //leftFront.setPower(0.9);
        Controller1();
        Controller2();
        //telemetry();
    }

    public void Controller1() {
        driveMechanum();
        //controlPixelPlacer();
    }

    public void Controller2() {

        controlPixelPlacer();
        fingerControl();
        hangerControl();
    }

    public void controlPixelPlacer() {

        telemetry.addData("Current arm position: ", pixelArm.getCurrentPosition() * (pixelArmToRadiansConstant / Math.PI) * 180);

        if(gamepad2.right_bumper)
        {
            pidfArmController = new PIDFArmController(0, 0, 0, 0, 0, 0, 0);
            pidfArmController.launch(-28 * Math.PI / 180, System.currentTimeMillis());
        }

        double targetPosition;
        if (pixelPlacerState == 2) // up
        {
            //pixelArm.setTargetPosition(-568);
            //pixelArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            //pixelArm.setPower(-0.6);
            targetPosition = 160 * Math.PI/180;

        }
        else if (pixelPlacerState == 1) // down
        {
            //pixelArm.setTargetPosition(0);
            //pixelArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            //pixelArm.setPower(0.6);
            targetPosition = -35 * Math.PI/180;
        }
        else // middle
        {
            //pixelArm.setTargetPosition(-30);
            //pixelArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            ///pixelArm.setPower(-0.6);
            targetPosition = 0 * Math.PI/180;
        }
        telemetry.addData("target: ", targetPosition / Math.PI * 180);
        telemetry.addData("error: ", targetPosition -  (pixelArm.getCurrentPosition() * pixelArmToRadiansConstant));
        //50.9 to 1 ; 105*Math.PI/180
        double correction = pidfArmController.updateArm(targetPosition, (pixelArm.getCurrentPosition() * pixelArmToRadiansConstant), 0,System.currentTimeMillis());
        telemetry.addData("correction: ", correction);
        pixelArm.setPower(correction);

        if (gamepad2.a != oldAButtonC2 && gamepad2.a)
            pixelPlacerState = 1;
        else if (gamepad2.b != oldBButtonC2 && gamepad2.b)
            pixelPlacerState = 2;
        else if (gamepad2.left_bumper)
            pixelPlacerState = 0;

        telemetry.addData("Pixel state: ", pixelPlacerState);

        //always keep at end
        oldAButtonC2 = gamepad2.a;
        oldBButtonC2 = gamepad2.b;

        //pixelArm.setPower(gamepad2.left_stick_y);
    }

    public void driveMechanum() {
        double leftBackPower;
        double rightBackPower;
        double leftFrontPower;
        double rightFrontPower;

        double x = gamepad1.left_stick_x;
        double y = -gamepad1.left_stick_y;
        double rx = gamepad1.right_stick_x;

        updateSpeedCoefficient();

        x = deadZone(x);
        y = deadZone(y);
        rx = deadZone(rx);

        //rx += poleCenter();

        double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);

        leftBackPower = (y - x + rx) / denominator;
        rightBackPower = (y + x - rx) / denominator;
        leftFrontPower = (y + x + rx) / denominator;
        rightFrontPower = (y - x - rx) / denominator;

        leftBackPower = Math.cbrt(leftBackPower);
        rightBackPower = Math.cbrt(rightBackPower);
        leftFrontPower = Math.cbrt(leftFrontPower);
        rightFrontPower = Math.cbrt(rightFrontPower);

        leftBackPower = (leftBackPower * speedCoefficient);
        rightBackPower = (rightBackPower * speedCoefficient);
        leftFrontPower = (leftFrontPower * speedCoefficient);
        rightFrontPower = (rightFrontPower * speedCoefficient);// * 0.75 //

        leftBack.setPower(leftBackPower);
        rightBack.setPower(rightBackPower);
        leftFront.setPower(leftFrontPower);
        rightFront.setPower(rightFrontPower);
    }

    public void telemetry() {
        telemetry.addData("Color Data: ", colorMan.red() + " " + colorMan.green() + " " + colorMan.blue());
        telemetry.update();
    }

    public void updateSpeedCoefficient() {
        //Speed coefficient dpad up n down
        if (gamepad1.dpad_up) {
            if (!savedStates1[0]) {
                if (speedCoefficient < 1)
                    speedCoefficient += 0.2;
                savedStates1[1] = true;
            }
        } else {
            savedStates1[1] = false;
        }
        if (gamepad1.dpad_down) {
            if (!savedStates1[0]) {
                if (speedCoefficient > 0.2)
                    speedCoefficient -= 0.2;
                savedStates1[0] = true;
            }
        } else {
            savedStates1[0] = false;
        }
    }

    public double deadZone(double input) {
        if (Math.abs(input) < 0.1) {
            return 0;
        } else {
            return input;
        }
    }

    private void fingerControl() {
        boolean fingerToggle = gamepad2.x;
        if (fingerToggle && !oldXButton) {
            fingers = !fingers;
            if (fingers) {
                fingerRight.setPosition(1);
                fingerLeft.setPosition(1);
            } else {
                fingerRight.setPosition(0);
                fingerLeft.setPosition(0);
            }
        }
        oldXButton = fingerToggle;
    }

    private void hangerControl() {
        //double position = hookArm.getCurrentPosition();
        //double arm = -gamepad2.right_stick_y;
        boolean elbowToggle = gamepad2.dpad_up;
        if (elbowToggle && !oldTriangle)
        {
            elbow = !elbow;
            if (elbow)
            {
                hookArm.setTargetPosition(2400);
                hookArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                hookArm.setPower(1);
                hookElbow.setPosition(0);
            }
            else
            {
                hookArm.setTargetPosition(0);
                hookArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                hookArm.setPower(-1);


            }
            if(gamepad2.dpad_down) //panic reset
            {
                hookArm.setTargetPosition(0);
                hookArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                hookArm.setPower(-1);
                hookElbow.setPosition(1);
            }
        }
        //hookElbow.setPosition(0.5);
        telemetry.addData("pos: ", hookArm.getCurrentPosition());
        oldTriangle = elbowToggle;
    }
}

