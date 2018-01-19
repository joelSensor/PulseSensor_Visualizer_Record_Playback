



void serialEvent(Serial port){
boolean saveLine = false;

try{
   String inData = port.readStringUntil('\n');  // read the ascii data into a String
   char token = inData.charAt(0);
   inData = inData.substring(1);        // cut off the leading char
   inData = trim(inData);                 // cut off white space (carriage return)
   // writeDataLine += inData + ",";  // copy to file save buffer

    switch(token){
      case 'S':           // leading 'S' means Pulse Sensor data
        Sensor = int(inData);                // convert the string to usable int
        //println("i got " + token);
        // collumn++;
        // println(collumn);
        // if (collumn > 3){
        //   println("reset");
          saveLine = true;
        // writeDataLine += "\n";
        //   collumn = 0;
        // } else {
        //   saveLine = false;
        // }
	      break;
      case 'B':          // leading 'B' for BPM data
      	// saveLine = true;
        // collumn++;
        // println(collumn);
     	  BPM = int(inData);                   // convert the string to usable int
     	  beat = true;                         // set beat flag to advance heart rate graph
	      heart = 20;                          // begin heart image 'swell' timer
        b = 1;
	      break;
      case  'Q':            // leading 'Q' means IBI data
        // collumn++;
        // println(collumn);
        // saveLine = true;
        // writeDataLine += "\n";
        IBI = int(inData);                   // convert the string to usable int
        break;
      default:
        print("SerialEvent: token error got "); println(token);
        break;
      }
} catch(Exception e) {
  // println(e.toString());
}
     if(saveLine){
       // dataWriter.print("*");
       writeDataLine = (Sensor +","+ BPM +","+ IBI +","+ b +"\n");
       b = 0;
       dataWriter.print(writeDataLine);
       // println(writeDataLine);
     }
}// END OF SERIAL EVENT
