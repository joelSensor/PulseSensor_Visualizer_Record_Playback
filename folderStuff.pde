


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
   logFileName = "PulseSensor Data/PS_"+month()+"-"+day()+"_"+hour()+"-"+minute()+".csv";
   dataWriter = createWriter(logFileName);
   dataWriter.println("%Pulse Sensor Data Log " + month()+"/"+day()+" "+hour()+":"+minute());
   dataWriter.println("%https://github.com/biomurph/PulseSensor_Visualizer_Record-Playback");
   dataWriter.println("%Data formatted for playback in Processing Visualizer");
   dataWriter.println("%Sample Rate 500Hz");
   dataWriter.println("%comma separated values");
   dataWriter.println("%Signal, BPM, IBI");
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
    readingFromFile = false;
    dataSourceFound = false;
    refreshPorts = true;
    zeroDataLines();
    //
  } else {
    //        println(dataLine);

   readDataLine = trim(readDataLine);               // trim the \n off the end
   if(readDataLine.charAt(0) == '%'){
     println(readDataLine);
     return;
   }
   String[] s = splitTokens(readDataLine, ","); // inData, ", ");
   // char token = readDataLine.charAt(0);
   // readDataLine = readDataLine.substring(1);        // cut off the leading 'S' or other
   Sensor = int(s[0]);

   int _bpm = int(s[1]);
   if(BPM != _bpm){
     BPM = _bpm;
   }
   if(IBI != int(s[2])){
     IBI = int(s[2]);
   }
  int p = int(s[3]);
  if(p == 1){
    beat = true;          // set beat flag to advance heart rate graph
    heart = 20;           // expand heart
  }
 }
}
