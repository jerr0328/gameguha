import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.concurrent.*;

public final class GUI implements KeyListener//, FrameListener
{
	public static final int screenWidth = 160;
	public static final int screenHeight = 144;
	private static Semaphore sem;
	private static Semaphore sndSem;
	private static Sound snd;
	private static CPU cpu;
	private static Graphics g;
	private static BufferedImage screen;
	private static int[] imgBuffer;
	private static int[] staticScreen;
	private static int zoom;
	private static int delayZoom;
	private static GraphicsEnvironment ge;
	private static GraphicsDevice gd;
	private static Frame frame;
	private static MenuBar mb;
	private static Insets ins;
	private static Point prevCoord;
	private static ScreenRenderer render;
	private static int filter;
	private static boolean fullScreen = false;
	private static boolean buttonLEFT = false;
	private static boolean buttonRIGHT = false;
	private static boolean buttonUP = false;
	private static boolean buttonDOWN = false;
	private static boolean buttonA = false;
	private static boolean buttonB = false;
	private static boolean buttonSTART = false;
	private static boolean buttonSELECT = false;
	private static boolean buttonTHROTTLE = false;
	private static int keyLEFT = KeyEvent.VK_LEFT;
	private static int keyRIGHT = KeyEvent.VK_RIGHT;
	private static int keyUP = KeyEvent.VK_UP;
	private static int keyDOWN = KeyEvent.VK_DOWN;
	private static int keyA = KeyEvent.VK_X;
	private static int keyB = KeyEvent.VK_Z;
	private static int keySTART = KeyEvent.VK_ENTER;
	private static int keySELECT = KeyEvent.VK_SPACE;
	private static int keyTHROTTLE = KeyEvent.VK_BACK_QUOTE;
	private static int keySAVE = KeyEvent.VK_F1;
	private static int keyLOAD = KeyEvent.VK_F2;
	
	public static void main(String[] args)
	{
		new GUI().go();
	}
	
	public void go()
	{
		ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gd = ge.getDefaultScreenDevice();
		System.out.println(gd.isFullScreenSupported());
	
		mb = new MenuBar();
		mb.add(new FileMenu(frame, this));
		mb.add(new OptionsMenu());
		mb.add(new SoundMenu());
	    	
		zoom = 1;
		delayZoom = 1;
		
		sem = new Semaphore(0);
		render = new ScreenRenderer(sem);
		
		toggleFullScreen(fullScreen);
		
		//screen = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
		//imgBuffer = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();

		int frames = 0;
		long startT = System.nanoTime();

		staticScreen = new int[screenWidth*screenHeight];
		/*while(true)
		{
			frames++;
			if (frames == 60)
			{
				//System.out.println((System.nanoTime()-startT)/1000000000.0 + " seconds");
				frames = 0;
				startT = System.nanoTime();
			}
			
			int[] arr = {0};
			drawFrame(arr);
		}*/
		
		render.start();
		
		sndSem = new Semaphore(0);
		snd = new Sound(sndSem);
		snd.start();
		
	}
	
	public void genSound()
	{
		sndSem.release();
	}
	
	public void newGUIFrame(boolean undecorated)
	{
		if (frame != null)
		{
			if (!fullScreen)
				prevCoord = frame.getLocation();
			frame.dispose();
		}
		
		frame = new Frame("GameGuha");
		frame.setBackground(Color.BLACK);
		
		if (undecorated)
			frame.setUndecorated(true);
		else
		{
			frame.setMenuBar(mb);
			if (prevCoord != null)
				frame.setLocation(prevCoord);
		}
		
		frame.setResizable(false);
		frame.setVisible(true);
		
		frame.addKeyListener(this);
	
		ins = frame.getInsets();
		System.out.printf("top:%d bot:%d left:%d right:%d\n", ins.top, ins.bottom, ins.left, ins.right);
		changeZoom();
		
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
        		System.exit(0);
     		}
		});
	}
	
	public void newFrame(int[] gbScreen)
	{
		if (zoom != delayZoom)
			changeZoom();
		
		if (zoom == 1) // Hopefully fast enough to finish during v-blank
			render.setGBVideo(gbScreen);
		else
		{
			System.arraycopy(gbScreen, 0, staticScreen, 0, staticScreen.length);
			render.setGBVideo(staticScreen);
		}
		sem.drainPermits();
		sem.release();
		//render.requestFrame();
	}
	
	public void setFilter(int filter)
	{
		this.filter = filter;
		changeZoom();
	}
	
	public void setZoom(int delayZoom)
	{
		this.delayZoom = delayZoom;
		
		if (cpu == null || cpu.getWaiting())
			changeZoom();
	}
	
	private void changeZoom()
	{
		zoom = delayZoom;
		
		frame.setSize(screenWidth*zoom + ins.left + ins.right, screenHeight*zoom + ins.top + ins.bottom);
		g = frame.getGraphics();
		
		screen = new BufferedImage(screenWidth*zoom, screenHeight*zoom, BufferedImage.TYPE_INT_RGB);
		imgBuffer = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();
		
		render.setReferences(imgBuffer, frame, zoom, filter, fullScreen, screen);
	}
	
	public void toggleFullScreen(boolean set)
	{
		newGUIFrame(set);
		fullScreen = set;
		if (fullScreen)
		{
			int[] pixels = new int[16 * 16];
			Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
			Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisibleCursor");
			frame.setCursor(transparentCursor);

			gd.setFullScreenWindow(frame);
		}
		else
			gd.setFullScreenWindow(null);
		
		g = frame.getGraphics();
		render.setReferences(imgBuffer, frame, zoom, filter, fullScreen, screen);
	}
	
	//This will register which keys are being pressed and released.
	public void keyPressed(KeyEvent key)
	{
		//System.out.println(KeyEvent.getKeyText(key.getKeyCode()));
		
		
			
		if (cpu == null || !cpu.isAlive())
			return;
		else if (key.isAltDown() && key.getKeyCode() == KeyEvent.VK_ENTER)
			toggleFullScreen(!fullScreen);
		else if(key.getKeyCode() == keyLEFT)
		{
		   buttonLEFT = true;
			cpu.joypadInt();
		}
		else if(key.getKeyCode() == keyRIGHT)
		{	
		   buttonRIGHT = true;
			cpu.joypadInt();
		}
		else if(key.getKeyCode() == keyUP)
		{
		   buttonUP = true;
			cpu.joypadInt();
		}
		else if(key.getKeyCode() == keyDOWN)
		{
		   buttonDOWN = true;
			cpu.joypadInt();
		}
		else if(key.getKeyCode() == keyA)
		{
		   buttonA = true;
			cpu.joypadInt();
		}
		else if(key.getKeyCode() == keyB)
		{
		   buttonB = true;
			cpu.joypadInt();
		}
		else if(key.getKeyCode() == keySTART)
		{
		   buttonSTART = true;
			cpu.joypadInt();
		}
		else if(key.getKeyCode() == keySELECT)
		{
		   buttonSELECT = true;
			cpu.joypadInt();
		}
		else if(key.getKeyCode() == keyTHROTTLE)
		{
		   buttonTHROTTLE = true;
		}
	}
	
	public void keyReleased(KeyEvent key)
	{
	   if(key.getKeyCode() == keyLEFT)
		   buttonLEFT = false;
			
		else if(key.getKeyCode() == keyRIGHT)
		   buttonRIGHT = false;
			
      	else if(key.getKeyCode() == keyUP)
		   buttonUP = false;
		
		else if(key.getKeyCode() == keyDOWN)
		   buttonDOWN = false;
		
		else if(key.getKeyCode() == keyA)
		   buttonA = false;
		
		else if(key.getKeyCode() == keyB)
		   buttonB = false;
			
		else if(key.getKeyCode() == keySTART)
		   buttonSTART = false;
		
		else if(key.getKeyCode() == keySELECT)
		   buttonSELECT = false;
		  
		else if(key.getKeyCode() == keyTHROTTLE)
			buttonTHROTTLE = false;
	   
		else if(key.getKeyCode() == keySAVE)
			cpu.setSaveState(true);
	   
		else if(key.getKeyCode() == keyLOAD)
			cpu.setLoadState(true);
		
	}
	
	public void keyTyped(KeyEvent key){ //empty method cause I need it <dumb>
	}
	
	//Next 8 methods all retrieve the booleans for the 8 buttons.
	public boolean getUp()
	{
	   return buttonUP;
	}
	public boolean getDown()
	{
	   return buttonDOWN;
	}
	public boolean getLeft()
	{
	   return buttonLEFT;
	}
	public boolean getRight()
	{
	   return buttonRIGHT;
	}
	public boolean getA()
	{
	   return buttonA;
	}
	public boolean getB()
	{
	   return buttonB;
	}
	public boolean getStart()
	{
	   return buttonSTART;
	}
	public boolean getSelect()
	{
	   return buttonSELECT;
	}
	public boolean throttleOff()
	{
	   return buttonTHROTTLE;
	}
	
	//End of the key registering stuff. 	
	private class FileMenu extends Menu implements ActionListener {
		Frame mw;
		GUI gui;
			
		public FileMenu(Frame m, GUI g){
			super("File");
			mw = m;
			gui = g;
			MenuItem mi; 
		    add(mi = new MenuItem("Open ROM")); 
		    mi.addActionListener(this);
		    add(mi = new MenuItem("Load state")); 
		    mi.addActionListener(this);
		    add(mi = new MenuItem("Save state")); 
		    mi.addActionListener(this);
			add(mi = new MenuItem("Pause"));
			mi.addActionListener(this);
		 	add(mi = new MenuItem("Resume"));
			mi.addActionListener(this);
		    add(mi = new MenuItem("Exit")); 
		    mi.addActionListener(this); 
		}
		
		public void actionPerformed(ActionEvent e) { 
			String item = e.getActionCommand(); 
			if (item.equals("Open ROM")){
				//mw.exit(); 
				FileDialog f = new FileDialog(mw, "Open ROM");
				f.setVisible(true);
				String file = f.getFile();
				if(file != null){
					if(cpu!=null)
					{
						// Resume if paused
						if(cpu.getWaiting())
							synchronized(cpu)
							{
								cpu.setWaiting(false);
								cpu.notify();
							}
						// Done resume
						
						synchronized(cpu)
						{
							System.out.println("Thread: "+cpu+ " Halted");
							cpu.setHalt(true);	
						}
						
						while(cpu != null && cpu.isAlive())
						{
							System.out.println("waiting");
							try
							{
								Thread.sleep(10);
							}
							catch(InterruptedException ie) {}
						}
					}
					
					cpu = new CPU(f.getDirectory()+file, gui);
					cpu.setSound(snd);
					cpu.start();
					
					
				}
			}
			else if(item.equals("Load state")){
				if(cpu!=null){
					synchronized(cpu){
						cpu.setLoadState(true);
					}
				}
			}
			else if(item.equals("Save state")){
				if(cpu!=null){
					synchronized(cpu){
						cpu.setSaveState(true);
					}
				}
			}
			else if(item.equals("Resume")){
				if(cpu!=null)
				{
					if(!cpu.getWaiting())
						System.out.println("Thread: "+cpu+ " is Running");
					else
						synchronized(cpu)
						{
							cpu.setWaiting(false);
							cpu.notify();
							System.out.println("Thread: "+cpu+ " Resumed");
						}
				}
				else
					System.out.println("No Thread Running");
			}
			else if(item.equals("Pause")){
				if(cpu !=null)
				{
					if(!cpu.getWaiting())
						{
							synchronized(cpu)
							{
								cpu.setWaiting(true);
								System.out.println("Thread: "+cpu+" Paused");
							}
						}
					else
						System.out.println("Thread: "+cpu+" Already Paused");
				}
				else
					System.out.println("No Thread Running");
			}
			else if(item.equals("Exit")){
				if(cpu!=null)
					synchronized(cpu)
					{
						System.out.println("Thread: "+cpu+" Halted");
						cpu.setHalt(true);
						cpu=null;
					}
				else
					System.out.println("No Thread Running");
				System.exit(0); //messy, probably should pass this a window event
								//not that I know how... :x
			}
			else
				System.out.println("Selected FileMenu " + item); 
		} 
	}
	
	private class OptionsMenu extends Menu implements ItemListener, ActionListener
	{
		private CheckboxMenuItem zoom1;
		private CheckboxMenuItem zoom2;
		private CheckboxMenuItem zoom3;
		private CheckboxMenuItem zoom4;
		private CheckboxMenuItem throttle;
		
		public OptionsMenu()
		{
			super("Options");
			
			MenuItem mi;
			add(mi = new MenuItem("Controls"));
			mi.addActionListener(this);
			
			add(throttle = new CheckboxMenuItem("Throttle", true)); 
			throttle.addItemListener(this);
			
			add(zoom1 = new CheckboxMenuItem("Zoom 1x", true));
		    zoom1.addItemListener(this); 
		    add(zoom2 = new CheckboxMenuItem("Zoom 2x")); 
		    zoom2.addItemListener(this); 
		    add(zoom3 = new CheckboxMenuItem("Zoom 3x")); 
		    zoom3.addItemListener(this); 
		    add(zoom4 = new CheckboxMenuItem("Zoom 4x")); 
			zoom4.addItemListener(this);
			
			add(new FilterMenu());
		}
		
		public void actionPerformed(ActionEvent e) { 
			String item = e.getActionCommand(); 
			if(item.equals("Controls"))
			{
			   Controls keyset = new Controls();  
			}
			
		}
		
		public void itemStateChanged(ItemEvent e)
		{ 
			System.out.println(e.paramString());
			
			if (e.getItemSelectable() == zoom1)
			{
				zoom1.setState(true);
				zoom2.setState(false);
				zoom3.setState(false);
				zoom4.setState(false);
				setZoom(1);
			}
			else if (e.getItemSelectable() == zoom2)
			{
				zoom1.setState(false);
				zoom2.setState(true);
				zoom3.setState(false);
				zoom4.setState(false);
				setZoom(2);
			}
			else if (e.getItemSelectable() == zoom3)
			{
				zoom1.setState(false);
				zoom2.setState(false);
				zoom3.setState(true);
				zoom4.setState(false);
				setZoom(3);
			}
			else if (e.getItemSelectable() == zoom4)
			{
				zoom1.setState(false);
				zoom2.setState(false);
				zoom3.setState(false);
				zoom4.setState(true);
				setZoom(4);
			}
			else if (e.getItemSelectable() == throttle)
			{
				cpu.setThrottle(throttle.getState());
			}
		}
	}
	
	private class FilterMenu extends Menu implements ItemListener
	{
		private CheckboxMenuItem simple;
		private CheckboxMenuItem advmame;
		private CheckboxMenuItem eagle;
		
		public FilterMenu()
		{
			super("Filter");
			
			add(simple = new CheckboxMenuItem("None",true));
		    simple.addItemListener(this); 
		    add(advmame = new CheckboxMenuItem("AdvMAME (2x/3x/4x)")); 
		    advmame.addItemListener(this); 
			add(eagle = new CheckboxMenuItem("Eagle (2x/4x)")); 
		    eagle.addItemListener(this);
		}
		
		public void itemStateChanged(ItemEvent e)
		{
			System.out.println(e.paramString());
			
			if (e.getItemSelectable() == simple)
			{
				simple.setState(true);
				advmame.setState(false);
				eagle.setState(false);
				setFilter(0);
			}
			else if (e.getItemSelectable() == advmame)
			{
				simple.setState(false);
				advmame.setState(true);
				eagle.setState(false);
				setFilter(1);
			}
			else if (e.getItemSelectable() == eagle)
			{
				simple.setState(false);
				advmame.setState(false);
				eagle.setState(true);
				setFilter(2);
			}
		}
	}
	
	// // This is broken: see my ViewMenu for a correct example of handling CheckBoxMenuItems
	private class SoundMenu extends Menu implements ItemListener {
		//Frame mw;
		private CheckboxMenuItem channel1;
		private CheckboxMenuItem channel2;
		private CheckboxMenuItem channel3;
		private CheckboxMenuItem channel4;
		private CheckboxMenuItem mute;
		public SoundMenu(){
			super("Sound");
			//mw = m;
			add(mute = new CheckboxMenuItem("Mute"));
		    mute.addItemListener(this); 
		    add(channel1 = new CheckboxMenuItem("Channel 1",true)); 
		    channel1.addItemListener(this); 
		    add(channel2 = new CheckboxMenuItem("Channel 2",true)); 
		    channel2.addItemListener(this);  
		    add(channel3 = new CheckboxMenuItem("Channel 3",true)); 
		    channel3.addItemListener(this); 
		    add(channel4 = new CheckboxMenuItem("Channel 4",true)); 
		    channel4.addItemListener(this); 
		}
		public void itemStateChanged(ItemEvent e)
		{  
			System.out.println(e.paramString());
			if (e.getItemSelectable() == mute){
				System.out.println("* "+mute.getState());
				snd.setSoundEnable(!mute.getState());
				System.out.println("** "+mute.getState());
				//if(!mute.getState())snd.setSampleRate(44100);
			}
			else if(e.getItemSelectable() == channel1){
				// Toggle channel
				snd.setChan1(channel1.getState());
			}
			else if(e.getItemSelectable() == channel2){
				// Toggle channel
				snd.setChan2(channel2.getState());
			}
			else if(e.getItemSelectable() == channel3){
				// Toggle channel
				snd.setChan3(channel3.getState());
			}
			else if(e.getItemSelectable() == channel4){
				// Toggle channel
				snd.setChan4(channel4.getState());
			}
		}
	}
	
   public class Controls  implements ActionListener, KeyListener {
	   public int keyLEFT, keyRIGHT, keyUP, keyDOWN, keyA, keyB, keySTART, keySELECT, keySAVE, keyLOAD;
		private Label leftL, rightL, upL, downL, aL, bL, startL, selectL, saveL, loadL;
		private TextField leftT, rightT, upT, downT, aT, bT, startT, selectT, saveT, loadT;
		private Button OK, Cancel;
		private Frame frame;
		
		public Controls()
		{
			frame = new Frame("Controls");
			LayoutManager layout;
			
			keyLEFT = GUI.keyLEFT;
			keyRIGHT = GUI.keyRIGHT;
			keyUP = GUI.keyUP;
			keyDOWN = GUI.keyDOWN;
			keyA = GUI.keyA;
			keyB = GUI.keyB;
			keySTART = GUI.keySTART;
			keySELECT = GUI.keySELECT;
			keySAVE = GUI.keySAVE;
			keyLOAD = GUI.keyLOAD;
			
			leftL = new Label("Left");
			rightL = new Label("Right");
			upL = new Label("Up");
			downL = new Label("Down");
			aL = new Label("A");
			bL = new Label("B");
			startL = new Label("Start");
			selectL = new Label("Select");
			saveL = new Label("Save");
			loadL = new Label("Load");
			
			leftT = new TextField(KeyEvent.getKeyText(keyLEFT), 5);
			leftT.addKeyListener(this);
			leftT.setEditable(false);
			rightT = new TextField(KeyEvent.getKeyText(keyRIGHT), 5);
			rightT.addKeyListener(this);
			rightT.setEditable(false);
			upT = new TextField(KeyEvent.getKeyText(keyUP), 5);
			upT.addKeyListener(this);
			upT.setEditable(false);
			downT = new TextField(KeyEvent.getKeyText(keyDOWN), 5);
			downT.addKeyListener(this);
			downT.setEditable(false);
			aT = new TextField(KeyEvent.getKeyText(keyA), 5);
			aT.addKeyListener(this);
			aT.setEditable(false);
			bT = new TextField(KeyEvent.getKeyText(keyB), 5);
			bT.addKeyListener(this);
			bT.setEditable(false);
			startT = new TextField(KeyEvent.getKeyText(keySTART), 5);
			startT.addKeyListener(this);
			startT.setEditable(false);
			selectT = new TextField(KeyEvent.getKeyText(keySELECT), 5);
			selectT.addKeyListener(this);
			selectT.setEditable(false);
			saveT = new TextField(KeyEvent.getKeyText(keySAVE), 5);
			saveT.addKeyListener(this);
			saveT.setEditable(false);
			loadT = new TextField(KeyEvent.getKeyText(keyLOAD), 5);
			loadT.addKeyListener(this);
			loadT.setEditable(false);
			
			OK = new Button("OK");
			OK.setActionCommand("OK");
			OK.addActionListener(this);
			Cancel = new Button("Cancel");
			Cancel.setActionCommand("Cancel");
			Cancel.addActionListener(this);
			
			frame.setSize(250, 280);
		   frame.setLocation(400, 200);
			
			layout = new GridLayout(11, 2, 10, 5);
			frame.setLayout(layout);
			
			frame.add(leftL);
			frame.add(leftT);
			frame.add(rightL);
			frame.add(rightT);
			frame.add(upL);
			frame.add(upT);
			frame.add(downL);
			frame.add(downT);
			frame.add(aL);
			frame.add(aT);
			frame.add(bL);
			frame.add(bT);
			frame.add(startL);
			frame.add(startT);
			frame.add(selectL);
			frame.add(selectT);
			frame.add(saveL);
			frame.add(saveT);
			frame.add(loadL);
			frame.add(loadT);
			frame.add(OK);
			frame.add(Cancel);
			
			frame.setVisible(true);
			frame.addWindowListener(new WindowAdapter()
		   {
			   public void windowClosing(WindowEvent we)
			   {
           		frame.dispose();
     		   }
		   });
		}
		
		public void actionPerformed(ActionEvent e)
		{
		   if(e.getActionCommand().equals("OK"))
			{
			   GUI.keyLEFT = keyLEFT;
				GUI.keyRIGHT = keyRIGHT;
				GUI.keyUP = keyUP;
				GUI.keyDOWN = keyDOWN;
				GUI.keyA = keyA;
				GUI.keyB = keyB;
				GUI.keySTART = keySTART;
				GUI.keySELECT = keySELECT;
				GUI.keySAVE = keySAVE;
				GUI.keyLOAD = keyLOAD;
				//System.out.println("Controls -- Up: " + GUI.keyUP + "  Down: " + GUI.keyDOWN + " Left: " + GUI.keyLEFT + " Right: " + GUI.keyRIGHT
				  //  + " A: " + GUI.keyA + " B: " + GUI.keyB + "  START: " + GUI.keySTART + "  SELECT: " + GUI.keySELECT);
			   frame.dispose();
			}
			if(e.getActionCommand().equals("Cancel"))
			{
			  // System.out.println("Controls -- Up: " + GUI.keyUP + "  Down: " + GUI.keyDOWN + " Left: " + GUI.keyLEFT + " Right: " + GUI.keyRIGHT
				   // + " A: " + GUI.keyA + " B: " + GUI.keyB + "  START: " + GUI.keySTART + "  SELECT: " + GUI.keySELECT);
			   frame.dispose();
			}
		}
		
		public void keyReleased(KeyEvent e){}
		public void keyTyped(KeyEvent e){}
		
		public void keyPressed(KeyEvent e)
		{
		   if(e.getComponent() == leftT)
			{
			   keyLEFT = e.getKeyCode();
				leftT.setText(KeyEvent.getKeyText(keyLEFT));
			}
			if(e.getComponent() == rightT)
			{
			   keyRIGHT = e.getKeyCode();
				rightT.setText(KeyEvent.getKeyText(keyRIGHT));
			}
			if(e.getComponent() == upT)
			{
				keyUP = e.getKeyCode();
				upT.setText(KeyEvent.getKeyText(keyUP));
			}
			if(e.getComponent() == downT)
			{
			   keyDOWN = e.getKeyCode();
				downT.setText(KeyEvent.getKeyText(keyDOWN));
			}
			if(e.getComponent() == aT)
			{
			   keyA = e.getKeyCode();
				aT.setText(KeyEvent.getKeyText(keyA));
			}
			if(e.getComponent() == bT)
			{
			   keyB = e.getKeyCode();
				bT.setText(KeyEvent.getKeyText(keyB));
			}
			if(e.getComponent() == startT)
			{
			   keySTART = e.getKeyCode();
				startT.setText(KeyEvent.getKeyText(keySTART));
			}
		   if(e.getComponent() == selectT)
			{
			   keySELECT = e.getKeyCode();
				selectT.setText(KeyEvent.getKeyText(keySELECT));
			}
		   if(e.getComponent() == saveT)
			{
			   keySAVE = e.getKeyCode();
				saveT.setText(KeyEvent.getKeyText(keySAVE));
			}
		   if(e.getComponent() == loadT)
			{
			   keyLOAD = e.getKeyCode();
			   loadT.setText(KeyEvent.getKeyText(keyLOAD));
			}
		}
	}
}
