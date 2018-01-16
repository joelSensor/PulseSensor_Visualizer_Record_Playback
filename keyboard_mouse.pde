
void mousePressed(){
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
        }else{
          println("selected to read a file");
          selectInput("Select a folder to process:", "folderSelected");
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