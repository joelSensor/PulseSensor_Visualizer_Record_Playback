



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
