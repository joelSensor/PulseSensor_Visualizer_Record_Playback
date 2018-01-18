



void serialEvent(Serial port){
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
     	  Sensor = int(inData);                // convert the string to usable int
	      break;
      case 'B':          // leading 'B' for BPM data
      	saveLine = true;
     	  BPM = int(inData);                   // convert the string to usable int
     	  beat = true;                         // set beat flag to advance heart rate graph
	      heart = 20;                          // begin heart image 'swell' timer
	      break;
      case  'Q':            // leading 'Q' means IBI data
        saveLine = true;
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
       dataWriter.print(writeDataLine);
       print(writeDataLine);
     }
}// END OF SERIAL EVENT
