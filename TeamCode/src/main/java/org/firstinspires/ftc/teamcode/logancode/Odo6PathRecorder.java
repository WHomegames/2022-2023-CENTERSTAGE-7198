package org.firstinspires.ftc.teamcode.logancode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ReadWriteFile;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.kaicode.Odo1;
import org.firstinspires.ftc.teamcode.kinematics.PoseVelocity2D;
import org.firstinspires.ftc.teamcode.odometry._7198_OdoController;

import java.io.File;

@TeleOp(name = "Odo6PathRecorder_Logan", group = "Debug/Tool")
public class Odo6PathRecorder extends LinearOpMode {

    private DcMotor leftBack, leftFront, rightBack, rightFront, encoderBack, encoderLeft, encoderRight;

    //Odometry
    private _7198_OdoController kaiOdo;
    public  Odo6Path path = new Odo6Path();

    double xOld = 0;
    double yOld = 0;
    double rOld = 0;
    private PoseVelocity2D lastPath;
    private long lastTime;

    int i = 0;

    @Override
    public void runOpMode() throws InterruptedException {

        mapHardware();
        resetDriveEncoder();
        kaiOdo = new _7198_OdoController();
        kaiOdo.initializeHardware(hardwareMap);

        lastPath = new PoseVelocity2D(0,0,0,0,0,0);
        lastTime = System.currentTimeMillis();

        waitForStart();

        while(opModeIsActive())
        {
            kaiOdo.update();

            double deltaTime = System.currentTimeMillis() - lastTime;
            PoseVelocity2D p =  new PoseVelocity2D(kaiOdo.getX(), kaiOdo.getY(),kaiOdo.getHeadingRad(), (kaiOdo.getX() - xOld) * deltaTime, (kaiOdo.getY() - yOld) * deltaTime, (kaiOdo.getHeadingRad() - rOld) * deltaTime);
            if(p.distance(lastPath) > 0.25 + kaiOdo.acessOdo6().getDeltaPose().getMagnitude())
            {
                path.addPathMarker(p);
                telemetry.addLine("New Path Generated at: " + p);
                lastPath = new PoseVelocity2D(p.getX(),p.getY(), p.getHeadingRad(),p.getVx(),p.getVy(),p.getVheadingRad());
            }
            telemetry.addLine("Path to path dist: " + p.distance(lastPath));
            telemetry.addLine("Delta val: " + (kaiOdo.acessOdo6().getDeltaPose().getMagnitude()));

            lastTime = System.currentTimeMillis();

            telemetry.addLine("Internal Position:");
            telemetry.addData("(X, Y, Radians)", Math.round(kaiOdo.getX() *10)/10d + " : " + Math.round(kaiOdo.getY() *10)/10d + " : " + Math.round(kaiOdo.getHeadingRad() *10)/10d);
            kaiOdo.telemOdometry(telemetry);

            telemetry.addLine("Path count: " + path.length());

            xOld = kaiOdo.getX();
            yOld = kaiOdo.getY();
            rOld = kaiOdo.getHeadingRad();

            if(gamepad1.a)
            {
                String filename = "path_recorder_output" + i + ".txt";
                File file = AppUtil.getInstance().getSettingsFile(filename);
                ReadWriteFile.writeFile(file, path.serialize());
                telemetry.log().add("saved to '%s'", filename);
                i++;
                path = new Odo6Path();
            }

            PerformLocalMovement(-gamepad1.left_stick_x, gamepad1.left_stick_y, -gamepad1.right_stick_x);


            telemetry.update();
        }
    }

    public void PerformLocalMovement(double x, double y, double rx)
    {
        x = LogsUtils.exponentialRemapAnalog(LogsUtils.deadZone(x,0.02),2);
        y = LogsUtils.exponentialRemapAnalog(LogsUtils.deadZone(y,0.02),2);
        rx = LogsUtils.exponentialRemapAnalog(LogsUtils.deadZone(rx,0.02),2);

        double leftBackPower;
        double rightBackPower;
        double leftFrontPower;
        double rightFrontPower;

        //rx += poleCenter();

        double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);

        leftBackPower   = (y - x + rx) / denominator;
        rightBackPower  = (y + x - rx) / denominator;
        leftFrontPower  = (y + x + rx) / denominator;
        rightFrontPower = (y - x - rx) / denominator;

        leftBackPower = Math.cbrt(leftBackPower);
        rightBackPower = Math.cbrt(rightBackPower);
        leftFrontPower = Math.cbrt(leftFrontPower);
        rightFrontPower = Math.cbrt(rightFrontPower);

//        leftBackPower = (leftBackPower);
//        rightBackPower = (rightBackPower);
//        leftFrontPower = (leftFrontPower);
//        rightFrontPower = (rightFrontPower);

        leftBack.setPower(leftBackPower);
        rightBack.setPower(rightBackPower);
        leftFront.setPower(leftFrontPower);
        rightFront.setPower(rightFrontPower);

    }

    public void resetDriveEncoder()
    {
        leftBack.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftBack.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        leftFront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftFront.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        rightBack.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightBack.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        rightFront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightFront.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    public void mapHardware()
    {
        leftFront = hardwareMap.get(DcMotor.class, "frontLeft");
        rightFront = hardwareMap.get(DcMotor.class, "frontRight");
        leftBack = hardwareMap.get(DcMotor.class, "backLeft");
        rightBack = hardwareMap.get(DcMotor.class, "backRight");

        //encoderBack = rightFront;
        //encoderLeft = leftBack;
        //encoderRight = leftFront;

        //colorMan = hardwareMap.get(ColorRangeSensor.class, "colorSensor");

        leftFront.setDirection(DcMotorSimple.Direction.REVERSE);
        leftBack.setDirection(DcMotorSimple.Direction.REVERSE);

        leftFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftBack.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightBack.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }
}
