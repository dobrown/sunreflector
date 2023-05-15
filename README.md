# sunreflector
Sun Reflector: a solar insolation and reflection simulator

This is a Java program whose primary purpose is to simulate reflections from a solar panel. Reflections are important because they can be quite bright and if directed toward residences or streets they may cause annoying and possibly dangerous glare. 

A secondary purpose is to determine the daily sun hours incident on the panel. A skyline of mountains, trees and buildings can be drawn by the user to make this determination more accurate.

The Java code is built on the Open Source Physics (OSP) framework and requires the OSP Core Library available in the <a href="https://github.com/OpenSourcePhysics/osp" target="_blank">OpenSourcePhysics/osp</a> repository.

Sun Reflector obtains sun position data from the <a href="https://gml.noaa.gov/grad/solcalc/calcdetails.html" target="_blank">NOAA solar position calculator</a>, an Excel spreadsheet. To access the spreadsheet it uses the following jars from the Apache Software Foundation: poi-3.17.jar, poi-ooxml-3.17.jar, poi-ooxml-schemas-3.17.jar, xmlbeans-2.6.0.jar, commons-collections4-4.1.jar.

The docs/SunReflector.zip file contains the ready-to-run application, including executable jar, NOAA spreadsheet and Apache library files. To run, download and unzip SunReflector.zip, open the SunReflector folder, and double-click sunreflector.jar.
