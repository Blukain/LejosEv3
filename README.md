# LejosEv3
A Java Lejos Ev3 Robot project


### README
This is a repo for a Lego EV3 programming using Java. Lego Ev3 have to use an external firmware, Lejos from Lejos.org, 
you will find there detailed instruction on how to install the firmware into a microSD card to use with your Lego EV3 Brick.

#### CURRENTLY
Not working on it now, it has been completed, this project was made for a university exam, content will be
uploaded as soon as possible, switched to this firmware from ev3dev due to a bluetooth problem not solved before the exam.
Lejos well support Ev3's bluetooth usage and command, in fact the Android app made together with this robot code, works well via BT,
and communicate to send and receive data from and to app.

#### INSTALLATION
I personally use IntelliJ Idea from JetBrains, so this instructions are based on it.

First of all, clone the repo, and start from there, every library needed is included in lib folder.
Changes to be made interest controller class.
The main class just create sensors and motors, and threads pass them what needed.
All the other classes are Thread class indipendent, some of them controls others.
