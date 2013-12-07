package firstblood;
import firstblood.FirstBlood;

public class Main {
	
	public static void main(String argv[]) {
      FirstBlood firstblood = new FirstBlood();
	   try {
	      firstblood.run();
	   }
	   catch(Exception e) {
	      e.printStackTrace();
	   }
	}
}
