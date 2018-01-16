import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class PulseSensor_Visualizer_Record-Playback extends PApplet {


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
 // clear the IBI data array by pressing c key
  case 'C':
    for (int i=ibiWindowWidth-1; i>=0; i--){  // reset the data array to default value
       IBI[i][0] = 1000;
       // reset the breath traces too
     }
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
        //println("i got " + token);
        // String[] s = splitTokens(readDataLine, ", ");
        int newPPG = PApplet.parseInt(readDataLine); //int(s[0]);            // convert ascii string to integer
        for (int i = 0; i < PPG.length-1; i++){
          PPG[i] = PPG[i+1]; // move the Y coordinates of the pulse wave one pixel left
        } // new data enters on the right at pulseY.length-1 scale and constrain incoming Pulse Sensor value to fit inside the pulse window
        PPG[PPG.length-1] = PApplet.parseInt(map(newPPG,0,1023,(ppgWindowYcenter+ppgWindowHeight/2),(ppgWindowYcenter-ppgWindowHeight/2)));
        // print("midline = " + ppgWindowYcenter + "\t");  println("ppg = " + PPG[PPG.length-1]);
        break;

     case 'B':
        BPM = PApplet.parseInt(readDataLine);             // convert the string to usable int
        beat = true;                         // set beat flag to advance heart rate graph
        heart = 20;                          // begin heart image 'swell' timer
        break;
     case 'Q':         // leading 'Q' means IBI data packet
        freshIBI = PApplet.parseInt(readDataLine);        // convert ascii string to integer
        pulse = true;                        // set the pulse flag
        // IBI[ibiWindowWidth-1][1] = 0;     // clear the peak detector
        break;
     default:
       break;
     }  // end of switch
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
      pressed = true;
      for(int i=0; i<radios.length; i++){
        if(i != me){ radios[i].pressed = false; }
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean overRadio(float mx, float my){
    if (dist(_x, _y, mx, my) < size/2){
      over = true;
      for(int i=0; i<radios.length; i++){
        if(i != me){ radios[i].over = false; }
      }
      return true;
    } else {
//      over = false;
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
        Serial.print("SerialEvent: token error got "); Serial.print(token);
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
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "PulseSensor_Visualizer_Record-Playback" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
