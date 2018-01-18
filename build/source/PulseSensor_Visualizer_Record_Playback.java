import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class PulseSensor_Visualizer_Record_Playback extends PApplet {

/*
THIS PROGRAM WORKS WITH PulseSensorAmped_Arduino ARDUINO CODE
THE PULSE DATA WINDOW IS SCALEABLE WITH SCROLLBAR AT BOTTOM OF SCREEN
PRESS 'S' OR 's' KEY TO SAVE A PICTURE OF THE SCREEN IN SKETCH FOLDER (.jpg)
MADE BY JOEL MURPHY AUGUST, 2012
UPDATED BY JOEL MURPHY SUMMER 2016 WITH SERIAL PORT LOCATOR TOOL
UPDATED BY JOEL MURPHY WINTER 2017 WITH IMPROVED SERIAL PORT SELECTOR TOOL

THIS CODE PROVIDED AS IS, WITH NO CLAIMS OF FUNCTIONALITY OR EVEN IF IT WILL WORK
      WYSIWYG
*/



DisposeHandler dh;
PFont font;
PFont portsFont;
Scrollbar scaleBar;

Serial port;

int Sensor;      // HOLDS PULSE SENSOR DATA FROM ARDUINO
int IBI;         // HOLDS TIME BETWEN HEARTBEATS FROM ARDUINO
int BPM;         // HOLDS HEART RATE VALUE FROM ARDUINO
int[] RawY;      // HOLDS HEARTBEAT WAVEFORM DATA BEFORE SCALING
int[] ScaledY;   // USED TO POSITION SCALED HEARTBEAT WAVEFORM
int[] rate;      // USED TO POSITION BPM DATA WAVEFORM
float zoom;      // USED WHEN SCALING PULSE WAVEFORM TO PULSE WINDOW
float offset;    // USED WHEN SCALING PULSE WAVEFORM TO PULSE WINDOW
int eggshell = color(255, 253, 248);
int heart = 0;   // This variable times the heart image 'pulse' on screen
//  THESE VARIABLES DETERMINE THE SIZE OF THE DATA WINDOWS
int PulseWindowWidth = 490;
int PulseWindowHeight = 512;
int BPMWindowWidth = 180;
int BPMWindowHeight = 340;
boolean beat = false;    // set when a heart beat is detected, then cleared when the BPM graph is advanced

// SERIAL PORT STUFF TO HELP YOU FIND THE CORRECT SERIAL PORT
String serialPort;
String[] serialPorts = new String[Serial.list().length];
boolean dataSourceFound = false;
Radio[] button = new Radio[Serial.list().length+1];
int numPorts = serialPorts.length;
boolean refreshPorts = false;

// FILE READ/WRITE STUFF
PrintWriter dataWriter;
boolean readingFromFile = false;
boolean onAir = false;
BufferedReader dataReader;
String readDataLine;
String writeDataLine;
String convertedLine;
String thisLine;
String h;
float[] floatData = new float[20];
String[] hexNums;
String logFileName;
boolean writingToOpenFile = false;
File playbackFile;


public void setup() {
    // Stage size
  frameRate(100);
  font = loadFont("Arial-BoldMT-24.vlw");
  textFont(font);
  textAlign(CENTER);
  rectMode(CENTER);
  ellipseMode(CENTER);
  background(0);
  noStroke();
  // DisposeHandler will save any data file when you close the program
  dh = new DisposeHandler(this);

  // Scrollbar constructor inputs: x,y,width,height,minVal,maxVal
  scaleBar = new Scrollbar (400, 575, 180, 12, 0.5f, 1.0f);  // set parameters for the scale bar
  RawY = new int[PulseWindowWidth];          // initialize raw pulse waveform array
  ScaledY = new int[PulseWindowWidth];       // initialize scaled pulse waveform array
  rate = new int [BPMWindowWidth];           // initialize BPM waveform array
  zoom = 0.75f;                               // initialize scale of heartbeat window

  zeroDataLines();  // set the visualizer lines to 0
  // DRAW OUT THE PULSE WINDOW AND BPM WINDOW RECTANGLES
  drawDataWindows();
  drawHeart();

  // GO FIND THE ARDUINO
  fill(eggshell);
  text("Select Your Serial Port",245,30);
  listAvailablePorts();

}

public void draw() {
if(dataSourceFound){
  // ONLY RUN THE VISUALIZER AFTER THE PORT IS CONNECTED
  background(0);
  noStroke();
  drawDataWindows();
  drawPulseWaveform();
  drawBPMwaveform();
  drawHeart();
// PRINT THE DATA AND VARIABLE VALUES
  fill(eggshell);                                       // get ready to print text
  text("Pulse Sensor Amped Visualizer v1.5",245,30);    // tell them what you are
  text("IBI " + IBI + "mS",600,585);                    // print the time between heartbeats in mS
  text(BPM + " BPM",600,200);                           // print the Beats Per Minute
  text("Pulse Window Scale " + nf(zoom,1,2), 150, 585); // show the current scale of Pulse Window

//  DO THE SCROLLBAR THINGS
  scaleBar.update (mouseX, mouseY);
  scaleBar.display();

} else { // SCAN BUTTONS TO FIND THE SERIAL PORT

  autoScanPorts();

  if(refreshPorts){
    refreshPorts = false;
    drawDataWindows();
    // drawHeart();
    listAvailablePorts();
  }

  for(int i=0; i<button.length; i++){
    button[i].overRadio(mouseX,mouseY);
    button[i].displayRadio();
  }

}

}  // end of draw loop


public void drawDataWindows(){
    // DRAW OUT THE PULSE WINDOW AND BPM WINDOW RECTANGLES
    fill(eggshell);  // color for the window background
    rect(255,height/2,PulseWindowWidth,PulseWindowHeight);
    rect(600,385,BPMWindowWidth,BPMWindowHeight);
}

public void drawPulseWaveform(){
  // DRAW THE PULSE WAVEFORM
  // prepare pulse data points
  RawY[RawY.length-1] = (1023 - Sensor) - 212;   // place the new raw datapoint at the end of the array
  zoom = scaleBar.getPos();                      // get current waveform scale value
  offset = map(zoom,0.5f,1,150,0);                // calculate the offset needed at this scale
  for (int i = 0; i < RawY.length-1; i++) {      // move the pulse waveform by
    RawY[i] = RawY[i+1];                         // shifting all raw datapoints one pixel left
    float dummy = RawY[i] * zoom + offset;       // adjust the raw data to the selected scale
    ScaledY[i] = constrain(PApplet.parseInt(dummy),44,556);   // transfer the raw data array to the scaled array
  }
  stroke(250,0,0);                               // red is a good color for the pulse waveform
  noFill();
  beginShape();                                  // using beginShape() renders fast
  for (int x = 1; x < ScaledY.length-1; x++) {
    vertex(x+10, ScaledY[x]);                    //draw a line connecting the data points
  }
  endShape();
}

public void drawBPMwaveform(){
// DRAW THE BPM WAVE FORM
// first, shift the BPM waveform over to fit then next data point only when a beat is found
 if (beat == true){   // move the heart rate line over one pixel every time the heart beats
   beat = false;      // clear beat flag (beat flag waset in serialEvent tab)
   for (int i=0; i<rate.length-1; i++){
     rate[i] = rate[i+1];                  // shift the bpm Y coordinates over one pixel to the left
   }
// then limit and scale the BPM value
   BPM = min(BPM,200);                     // limit the highest BPM value to 200
   float dummy = map(BPM,0,200,555,215);   // map it to the heart rate window Y
   rate[rate.length-1] = PApplet.parseInt(dummy);       // set the rightmost pixel to the new data point value
 }
 // GRAPH THE HEART RATE WAVEFORM
 stroke(250,0,0);                          // color of heart rate graph
 strokeWeight(2);                          // thicker line is easier to read
 noFill();
 beginShape();
 for (int i=0; i < rate.length-1; i++){    // variable 'i' will take the place of pixel x position
   vertex(i+510, rate[i]);                 // display history of heart rate datapoints
 }
 endShape();
}

public void drawHeart(){
  // DRAW THE HEART AND MAYBE MAKE IT BEAT
    fill(250,0,0);
    stroke(250,0,0);
    // the 'heart' variable is set in serialEvent when arduino sees a beat happen
    heart--;                    // heart is used to time how long the heart graphic swells when your heart beats
    heart = max(heart,0);       // don't let the heart variable go into negative numbers
    if (heart > 0){             // if a beat happened recently,
      strokeWeight(8);          // make the heart big
    }
    smooth();   // draw the heart with two bezier curves
    bezier(width-100,50, width-20,-20, width,140, width-100,150);
    bezier(width-100,50, width-190,-20, width-200,140, width-100,150);
    strokeWeight(1);          // reset the strokeWeight for next time
}

public void listAvailablePorts(){
  println(Serial.list());    // print a list of available serial ports to the console
  serialPorts = Serial.list();
  fill(0);
  textFont(font,16);
  textAlign(LEFT);
  // set a counter to list the ports backwards
  int yPos = 0;
  int xPos = 150;
  for(int i=serialPorts.length-1; i>=0; i--){
    button[i] = new Radio(xPos, 95+(yPos*20),12,color(180),color(80),color(255),i,button);
    text(serialPorts[i],xPos+15, 100+(yPos*20));
    yPos++;
  }
  // int p = numPorts; // adding one more radio button
   fill(233,0,0);
  button[serialPorts.length] = new Radio(xPos, 95+(yPos*20),12,color(180),color(80),color(255),serialPorts.length,button);
  text("Select Playback File",xPos+15, 100+(yPos*20));
  textFont(font);
  textAlign(CENTER);
}

public void autoScanPorts(){
  if(Serial.list().length != numPorts){
    if(Serial.list().length > numPorts){
      println("New Ports Opened!");
      int diff = Serial.list().length - numPorts;	// was serialPorts.length
      serialPorts = expand(serialPorts,diff);
      numPorts = Serial.list().length;
    }else if(Serial.list().length < numPorts){
      println("Some Ports Closed!");
      numPorts = Serial.list().length;
    }
    refreshPorts = true;
    return;
  }
}

public void zeroDataLines(){
  for (int i=0; i<rate.length; i++){
   rate[i] = 555;      // Place BPM graph line at bottom of BPM Window
  }
  for (int i=0; i<RawY.length; i++){
     RawY[i] = height/2; // initialize the pulse window data line to V/2
  }
}




public class DisposeHandler {

  DisposeHandler(PApplet pa)
  {
    pa.registerMethod("dispose", this);
  }

  public void dispose()
  {
    println("Closing sketch");
    // Place here the code you want to execute on exit
    if(writingToOpenFile){
      dataWriter.flush();
      dataWriter.close();
      println("closed Data File");
    }  
  }
}



public void folderSelected(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
  } else {
    println("User selected " + selection.getAbsolutePath());
    playbackFile = selection;
    dataReader = createReader(playbackFile.getAbsolutePath()); //
    readingFromFile = true;
    onAir = true;
    dataSourceFound = true;
    zeroDataLines();
  }
}

public void createFile(){
   logFileName = "PulseSensor Data/"+month()+"-"+day()+"_"+hour()+"-"+minute()+".csv";
   dataWriter = createWriter(logFileName);
   dataWriter.println("%Pulse Sensor Data Log " + month()+"/"+day()+" "+hour()+":"+minute());
   dataWriter.println("%Data formatted for playback in Processing Visualizer");
   dataWriter.println("%https://github.com/biomurph/PulseSensor_Visualizer_Record-Playback");
}



public void readDataLineFromFile(){
  try {
    readDataLine = dataReader.readLine();
  }
  catch (IOException e) {
    e.printStackTrace();
    readDataLine = null;
  }

  if (readDataLine == null) {
    // Stop reading because of an error or file is empty
    // Start again from the beginning?
    // Press 'r' for replay?
    // readingFromFile = false;
    println("nothing left in file");
    onAir = false;
    //
  } else {
    //        println(dataLine);
   char token = readDataLine.charAt(0);
   readDataLine = readDataLine.substring(1);        // cut off the leading 'S' or other
   readDataLine = trim(readDataLine);               // trim the \n off the end

    switch(token){
      case '%':
        println(readDataLine);
        break;
      case 'S':           // leading 'S' means Pulse Sensor and maybe breath data packet
        Sensor = PApplet.parseInt(readDataLine);
        //println("i got " + token);
        // String[] s = splitTokens(readDataLine, ", ");
        // int newPPG = int(readDataLine); //int(s[0]);            // convert ascii string to integer
        // for (int i = 0; i < PPG.length-1; i++){
        //   PPG[i] = PPG[i+1]; // move the Y coordinates of the pulse wave one pixel left
        // } // new data enters on the right at pulseY.length-1 scale and constrain incoming Pulse Sensor value to fit inside the pulse window
        // PPG[PPG.length-1] = int(map(newPPG,0,1023,(ppgWindowYcenter+ppgWindowHeight/2),(ppgWindowYcenter-ppgWindowHeight/2)));
        // // print("midline = " + ppgWindowYcenter + "\t");  println("ppg = " + PPG[PPG.length-1]);
        break;

     case 'B':
        BPM = PApplet.parseInt(readDataLine);             // convert the string to usable int
        beat = true;                         // set beat flag to advance heart rate graph
        heart = 20;                          // begin heart image 'swell' timer
        break;
     case 'Q':         // leading 'Q' means IBI data packet
        IBI = PApplet.parseInt(readDataLine);        // convert ascii string to integer
        // IBI[ibiWindowWidth-1][1] = 0;     // clear the peak detector
        break;
     default:
       break;
     }  // end of switch
  }

}

public void mousePressed(){
  scaleBar.press(mouseX, mouseY);
  if(!dataSourceFound){
    for(int i=0; i<button.length; i++){
      if(button[i].pressRadio(mouseX,mouseY)){
        if(i < serialPorts.length){
          try{
            port = new Serial(this, Serial.list()[i], 115200);  // make sure Arduino is talking serial at this baud rate
            delay(1000);
            println(port.read());
            port.clear();            // flush buffer
            port.bufferUntil('\n');  // set buffer full flag on receipt of carriage return
            dataSourceFound = true;
            createFile();
            writingToOpenFile = true;
          }
          catch(Exception e){
            println("Couldn't open port " + Serial.list()[i]);
            drawDataWindows();
            listAvailablePorts();
            fill(255,0,0);
            textFont(font,16);
            textAlign(LEFT);
            text("Couldn't open port " + Serial.list()[i],60,70);
            textFont(font);
            textAlign(CENTER);
          }
        }else{
          println("selected to read a file");
          selectInput("Select a folder to process:", "folderSelected");
        }
      }
    }
  }
}

public void mouseReleased(){
  scaleBar.release();
}

public void keyPressed(){

 switch(key){
   case 's':    // pressing 's' or 'S' will take a jpg of the processing window
   case 'S':
     saveFrame("heartLight-####.jpg");    // take a shot of that!
     break;
   case 'p':  // user request to pause playback
     if(onAir){
       onAir = false;
     }else{
       onAir = true;
     }
     break;
   case 'r':  // user request to replay playback file
     if(readingFromFile){
       zeroDataLines();
       dataReader = createReader(playbackFile.getAbsolutePath()); //
       onAir = true;
       dataSourceFound = true;
     }
     break;
   default:
     break;
 }
}


class Radio {
  int _x,_y;
  int size, dotSize;
  int baseColor, overColor, pressedColor;
  boolean over, pressed;
  int me;
  Radio[] radios;

  Radio(int xp, int yp, int s, int b, int o, int p, int m, Radio[] r) {
    _x = xp;
    _y = yp;
    size = s;
    dotSize = size - size/3;
    baseColor = b;
    overColor = o;
    pressedColor = p;
    radios = r;
    me = m;
  }

  public boolean pressRadio(float mx, float my){
    if (dist(_x, _y, mx, my) < size/2){
      for(int i=0; i<radios.length; i++){
        radios[i].pressed = true;
        if(i != me){ radios[i].pressed = false; }
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean overRadio(float mx, float my){
    if (dist(_x, _y, mx, my) < size/2){
      for(int i=0; i<radios.length; i++){
        radios[i].over = true;
        if(i != me){ radios[i].over = false; }
      }
      return true;
    } else {
      return false;
    }
  }

  public void displayRadio(){
    noStroke();
    fill(baseColor);
    ellipse(_x,_y,size,size);
    if(over){
      fill(overColor);
      ellipse(_x,_y,dotSize,dotSize);
    }
    if(pressed){
      fill(pressedColor);
      ellipse(_x,_y,dotSize,dotSize);
    }
  }
}

/*
    THIS SCROLLBAR OBJECT IS BASED ON THE ONE FROM THE BOOK "Processing" by Reas and Fry
*/

class Scrollbar{
 int x,y;               // the x and y coordinates
 float sw, sh;          // width and height of scrollbar
 float pos;             // position of thumb
 float posMin, posMax;  // max and min values of thumb
 boolean rollover;      // true when the mouse is over
 boolean locked;        // true when it's the active scrollbar
 float minVal, maxVal;  // min and max values for the thumb

 Scrollbar (int xp, int yp, int w, int h, float miv, float mav){ // values passed from the constructor
  x = xp;
  y = yp;
  sw = w;
  sh = h;
  minVal = miv;
  maxVal = mav;
  pos = x - sh/2;
  posMin = x-sw/2;
  posMax = x + sw/2;  // - sh;
 }

 // updates the 'over' boolean and position of thumb
 public void update(int mx, int my) {
   if (over(mx, my) == true){
     rollover = true;            // when the mouse is over the scrollbar, rollover is true
   } else {
     rollover = false;
   }
   if (locked == true){
    pos = constrain (mx, posMin, posMax);
   }
 }

 // locks the thumb so the mouse can move off and still update
 public void press(int mx, int my){
   if (rollover == true){
    locked = true;            // when rollover is true, pressing the mouse button will lock the scrollbar on
   }else{
    locked = false;
   }
 }

 // resets the scrollbar to neutral
 public void release(){
  locked = false;
 }

 // returns true if the cursor is over the scrollbar
 public boolean over(int mx, int my){
  if ((mx > x-sw/2) && (mx < x+sw/2) && (my > y-sh/2) && (my < y+sh/2)){
   return true;
  }else{
   return false;
  }
 }

 // draws the scrollbar on the screen
 public void display (){

  noStroke();
  fill(255);
  rect(x, y, sw, sh);      // create the scrollbar
  fill (250,0,0);
  if ((rollover == true) || (locked == true)){
   stroke(250,0,0);
   strokeWeight(8);           // make the scale dot bigger if you're on it
  }
  ellipse(pos, y, sh, sh);     // create the scaling dot
  strokeWeight(1);            // reset strokeWeight
 }

 // returns the current value of the thumb
 public float getPos() {
  float scalar = sw / sw;  // (sw - sh/2);
  float ratio = (pos-(x-sw/2)) * scalar;
  float p = minVal + (ratio/sw * (maxVal - minVal));
  return p;
 }
 }




public void serialEvent(Serial port){
boolean saveLine = false;
try{
   String inData = port.readStringUntil('\n');  // read the ascii data into a String
   writeDataLine = inData;  // copy to file save buffer
   char token = inData.charAt(0);
   inData = inData.substring(1);        // cut off the leading char
   inData = trim(inData);                 // cut off white space (carriage return)

    switch(token){
      case 'S':           // leading 'S' means Pulse Sensor data
        //println("i got " + token);
        saveLine = true;
     	  Sensor = PApplet.parseInt(inData);                // convert the string to usable int
	      break;
      case 'B':          // leading 'B' for BPM data
      	saveLine = true;
     	  BPM = PApplet.parseInt(inData);                   // convert the string to usable int
     	  beat = true;                         // set beat flag to advance heart rate graph
	      heart = 20;                          // begin heart image 'swell' timer
	      break;
      case  'Q':            // leading 'Q' means IBI data
        saveLine = true;
        IBI = PApplet.parseInt(inData);                   // convert the string to usable int
        break;
      default:
        print("SerialEvent: token error got "); println(token);
        break;
   }
} catch(Exception e) {
  // println(e.toString());
}
     if(saveLine){
       dataWriter.print(writeDataLine);
       print(writeDataLine);
     }
}// END OF SERIAL EVENT
  public void settings() {  size(700, 600); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "PulseSensor_Visualizer_Record_Playback" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
