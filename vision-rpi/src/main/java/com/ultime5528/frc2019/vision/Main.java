package com.ultime5528.frc2019.vision;

import java.util.ArrayList;
import java.util.Random;

/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ultime5528.frc2019.vision.ConfigReader.CameraConfig;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;

public final class Main {
  private static String configFile = "/boot/frc.json";

  public static int team;
  public static boolean server;

  public static ArrayList<UsbCamera> cameras = new ArrayList<UsbCamera>();

  public static MyPipeline pipeline;

  private static NetworkTableEntry timeEntry;
  private static NetworkTableEntry rouleauEntry;
  private static NetworkTableEntry isautoEntry;

  private Main() {
  }

  /**
   * Start running the camera.
   * 
   */
  public static UsbCamera startCamera(CameraConfig config) {
    System.out.println("Starting camera '" + config.name + "' on " + config.path);
    CameraServer inst = CameraServer.getInstance();
    UsbCamera camera = new UsbCamera(config.name, config.path);
    //MjpegServer server = inst.startAutomaticCapture(camera);

    Gson gson = new GsonBuilder().create();

    camera.setConfigJson(gson.toJson(config.config));
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    /*if (config.streamConfig != null) {
      server.setConfigJson(gson.toJson(config.streamConfig));
    }*/

    return camera;
  }
  
  /**
   * Main.
   * 
   * @throws InterruptedException
   */
  public static void main(String... args) throws InterruptedException {
    if (args.length > 0) {
      configFile = args[0];
      ConfigReader.configFile = configFile;
    }

    // lire la congig
    if (!ConfigReader.readConfig()) {
      return;
    }

    // démarre NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    if (server) {
      System.out.println("Setting up NetworkTables server");
      ntinst.startServer();
    } else {
      System.out.println("Setting up NetworkTables client for team " + team);
      ntinst.startClientTeam(team);
    }

    //crée pipeline de vision
    pipeline = new MyPipeline(ntinst);

    timeEntry = ntinst.getEntry("TIME");
    rouleauEntry = ntinst.getEntry("ROULEAU_ON");
    isautoEntry = ntinst.getEntry("IS_AUTO");

    //démarre caméra
    for (CameraConfig config : ConfigReader.getCameraConfigs()) {
      cameras.add(startCamera(config));
    }
    //camera = startCamera();

    //démarre la loop() de vision
    loop();
  }
  
  private static void loop(){
    //Vision
    CvSink sourceVision = CameraServer.getInstance().getVideo(cameras.get(K.VISION_CAMERA_PORT));
    CvSource outputVideoVision = CameraServer.getInstance().putVideo("OutputVision", K.WIDTH, K.HEIGHT);
    outputVideoVision.setFPS(20);

    MjpegServer serverVision = (MjpegServer) CameraServer.getInstance().getServer("serve_OutputVision");
    
    serverVision.getProperty("compression").set(100);
    serverVision.getProperty("fps").set(20);

    Mat inputVision = new Mat(K.HEIGHT,K.WIDTH,CvType.CV_8UC3);    

    //Pilote
    CvSink sourcePilote = CameraServer.getInstance().getVideo(cameras.get(K.PILOTE_CAMERA_PORT));
    CvSource outputVideoPilote = CameraServer.getInstance().putVideo("OutputPilote", K.WIDTH,(int)(K.HEIGHT * (1 + K.TIME_BAR_PROPORTION)));
    outputVideoPilote.setFPS(20);

    MjpegServer serverPilote = (MjpegServer) CameraServer.getInstance().getServer("serve_OutputPilote");
    
    serverPilote.getProperty("compression").set(100);
    serverPilote.getProperty("fps").set(20);

    Mat inputPilote = new Mat(K.HEIGHT,K.WIDTH,CvType.CV_8UC3); 
    Mat outputPilote = new Mat((int)(K.HEIGHT * (1 + K.TIME_BAR_PROPORTION)),K.WIDTH,CvType.CV_8UC3); 
    //Mat outputPilote = new Mat((int)(K.HEIGHT),K.WIDTH,CvType.CV_8UC3); 

    int currentTime;
    boolean rouleauON;
    boolean isauto;

    while(true){
      try {
        //obtenir l'image des caméras
        sourceVision.grabFrame(inputVision);
        sourcePilote.grabFrame(inputPilote);

        //traiter l'image de la vision
        pipeline.process(inputVision);

        currentTime = (int)timeEntry.getDouble(135);
        rouleauON = rouleauEntry.getBoolean(false);
        isauto = isautoEntry.getBoolean(false);

        //écrire les infos sur la vision du pilote
        inputPilote.copyTo(outputPilote.rowRange(0, K.HEIGHT));
        PiloteView.write(outputPilote, currentTime, rouleauON, isauto);

        //afficher l'image
        outputVideoVision.putFrame(inputVision);
        outputVideoPilote.putFrame(outputPilote); 

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
