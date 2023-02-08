package application.map;

public class CloseThread extends Thread {
	public CloseThread(JFXOpenStreetMap jfxosm) {
		while (!jfxosm.windowInitialized()) {
			System.out.println("runnable");
		}
		
		jfxosm.ensureWindowClose();
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}
