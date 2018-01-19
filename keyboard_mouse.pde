
void mousePressed(){
  scaleBar.press(mouseX, mouseY);
  if(!dataSourceFound){
    for(int i=0; i<=numPorts; i++){
      if(button[i].pressRadio(mouseX,mouseY)){
        if(i < numPorts){ // serialPorts.length){
          try{
            port = new Serial(this, Serial.list()[i], 115200);  // make sure Arduino is talking serial at this baud rate
            // port.clear();
            delay(500);
            // println(port.read());
            port.clear();            // flush buffer
            port.bufferUntil('\n');  // set buffer full flag on receipt of carriage return
            dataSourceFound = true;
            createFile();
            writingToOpenFile = true;
            // println("made port and file");
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
          frameRate(150);
        }
      }
    }
  }
}

void mouseReleased(){
  scaleBar.release();
}

void keyPressed(){

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
