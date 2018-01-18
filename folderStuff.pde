


void folderSelected(File selection) {
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

void createFile(){
   logFileName = "PulseSensor Data/"+month()+"-"+day()+"_"+hour()+"-"+minute()+".csv";
   dataWriter = createWriter(logFileName);
   dataWriter.println("%Pulse Sensor Data Log " + month()+"/"+day()+" "+hour()+":"+minute());
   dataWriter.println("%Data formatted for playback in Processing Visualizer");
   dataWriter.println("%https://github.com/biomurph/PulseSensor_Visualizer_Record-Playback");
}



void readDataLineFromFile(){
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
        Sensor = int(readDataLine);
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
        BPM = int(readDataLine);             // convert the string to usable int
        beat = true;                         // set beat flag to advance heart rate graph
        heart = 20;                          // begin heart image 'swell' timer
        break;
     case 'Q':         // leading 'Q' means IBI data packet
        IBI = int(readDataLine);        // convert ascii string to integer
        // IBI[ibiWindowWidth-1][1] = 0;     // clear the peak detector
        break;
     default:
       break;
     }  // end of switch
  }

}
