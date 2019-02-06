/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST license file in the root directory of     */
/* the project.                                                               */
/*---------------------------------------------------- -----------------------*/

package com.ultime5528.frc2019.subsystems;

import com.ultime5528.frc2019.K;
import com.ultime5528.sensors.DFRobotTFmini;
import com.ultime5528.util.LinearInterpolator;
import com.ultime5528.util.Point;

import badlog.lib.BadLog;
import edu.wpi.first.wpilibj.AnalogPotentiometer;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.command.PIDSubsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Elevateur extends PIDSubsystem {

  private VictorSP moteur;
  private AnalogPotentiometer pot;
  private Point[] pointsMonter, pointsDescendre;
  private DFRobotTFmini lidar;

  public Elevateur() {

    super(K.Elevateur.P, K.Elevateur.I, K.Elevateur.D);

    moteur = new VictorSP(K.Ports.ELEVATEUR_MOTEUR);
    addChild("Moteur", moteur);

    pot = new AnalogPotentiometer(K.Ports.ELEVATEUR_POTENTIOMETRE);
    addChild("Potentiomètre", pot);

    pointsMonter = new Point[] { new Point(1.38, -0.8), new Point(1.50, -0.4), };

    pointsDescendre = new Point[] { new Point(0.1, 0.15), new Point(0.15, 0.35), new Point(0.65, 0.45),
        new Point(0.8, 0.25) };

    BadLog.createTopic("Elevateur/Valeur Potentiometre", "V", () -> pot.get());
    BadLog.createTopic("Elevateur/Puissance moteur", "%", () -> moteur.get());

    lidar = new DFRobotTFmini();
  }

  @Override
  public void periodic() {
    super.periodic();
    SmartDashboard.putNumber("distance", lidar.getDistance());
  }

  @Override
  protected double returnPIDInput() {
    return pot.pidGet();
  }

  @Override
  protected void usePIDOutput(double output) {

    moteur.set(output);

  }

  public void monter(LinearInterpolator interpolator) {

    moteur.set(interpolator.interpolate(pot.get()));
  }

  public void descendre(LinearInterpolator interpolator) {
    moteur.set(interpolator.interpolate(pot.get()));
  }

  public void monter() {

    moteur.set(K.Elevateur.VITESSE_ELEVATEUR);
  }

  public void descendre() {

    moteur.set(-0.65);
  }

  public void stop() {
    moteur.set(0);
  }

  public boolean atteintMin() {

    return (pot.get() <= K.Elevateur.HAUTEUR_MIN);

  }

  public boolean atteintMax() {

    return (pot.get() >= K.Elevateur.HAUTEUR_MAX);

  }

  public double getHauteur() {
    return pot.get();
  }

  @Override
  protected void initDefaultCommand() {

  }
}
