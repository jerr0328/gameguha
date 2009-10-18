import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*; // random colors look cool! but slow to generate

public class GUI implements KeyListener//, FrameListener
{
	public static final int screenWidth = 160;
	public static final int screenHeight = 144;
	private static CPU cpu;
	private static Graphics g;
	private static BufferedImage screen;
	private static int[] imgBuffer;
	private static int zoom;
	private static int delayZoom;
	private static Frame frame;
	private static Insets ins;
	private static Random gen;
	private static boolean buttonLEFT = false;
	private static boolean buttonRIGHT = false;
	private static boolean buttonUP = false;
	private static boolean buttonDOWN = false;
	private static boolean buttonA = false;
	private static boolean buttonB = false;
	private static boolean buttonSTART = false;
	private static boolean buttonSELECT = false;
	
	public static void main(String[] args)
	{
		new GUI().go();
	}
	
	public void go()
	{
    	frame = new Frame("GameGuha");
    	MenuBar mb = new MenuBar();
    	mb.add(new FileMenu(frame, this));
    	mb.add(new ViewMenu());
    	mb.add(new SoundMenu());
    	frame.setMenuBar(mb);
    	
		frame.setResizable(false);
		frame.setVisible(true); 
		
		frame.addKeyListener(this);
		
	
		ins = frame.getInsets();
		System.out.printf("top:%d bot:%d left:%d right:%d\n", ins.top, ins.bottom, ins.left, ins.right);
		frame.setSize(screenWidth + ins.left + ins.right, screenHeight + ins.top + ins.bottom); 
			
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
        		System.exit(0);
     		}
		});
		
		g = frame.getGraphics();
		
		screen = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
		imgBuffer = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();

		gen = new Random();
		int frames = 0;
		long startT = System.nanoTime();
		
		zoom = 1;
		delayZoom = 1;

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
	}
	
	public void newFrame(int[] gbScreen)
	{
		if (zoom != delayZoom)
			changeZoom();
		
		int[] buffer = imgBuffer;
				
		int xPixel, yPixel, col;
		
		switch (zoom)
		{
			case 1:
				System.arraycopy(gbScreen, 0, buffer, 0, buffer.length);
				/*int y1 = 0;
				for(yPixel = 0; yPixel < 144; yPixel++)
				{
					for (xPixel = 0; xPixel < 160; xPixel++)
					{
						col = gen.nextInt();
						
						buffer[xPixel+y1] = col;
					}
					y1 += 160;
				}*/
			break;
			
			case 2:
				int x;
				int y1;
				int y2 = -(screenWidth*2);
				for(yPixel = 0; yPixel < 144; yPixel++)
				{
					y1 = y2+320;
					y2 = y1+320;
					
					for (x = 0; x < 320; x++)
					{
						xPixel = x >> 1;
						
						col = gbScreen[yPixel*screenWidth + xPixel];
	
						buffer[x + y1] = col;
						buffer[x + y2] = col;
						
						x++;
						
						buffer[x + y1] = col;
						buffer[x + y2] = col;
					}
				}
			break;
			
			case 3:
				int y3 = -(screenWidth*3);
				for(yPixel = 0; yPixel < 144; yPixel++)
				{
					y1 = y3+480;
					y2 = y1+480;
					y3 = y2+480;
					x = 0;
					
					for (xPixel = 0; xPixel < 160; xPixel++)
					{
						col = gbScreen[yPixel*screenWidth + xPixel];
						
						buffer[x + y1] = col;
						buffer[x + y2] = col;
						buffer[x + y3] = col;
						
						x++;
						
						buffer[x + y1] = col;
						buffer[x + y2] = col;
						buffer[x + y3] = col;
						
						x++;
						
						buffer[x + y1] = col;
						buffer[x + y2] = col;
						buffer[x + y3] = col;
						
						x++;
					}
				}
			break;
			
			case 4:
				int y4 = -(screenWidth*4);
				for(yPixel = 0; yPixel < 144; yPixel++)
				{
					y1 = y4+640;
					y2 = y1+640;
					y3 = y2+640;
					y4 = y3+640; 
					
					for (x = 0; x < 640; x++)
					{
						xPixel = x >> 2;
						
						col = gbScreen[yPixel*screenWidth + xPixel];
	
						buffer[x + y1] = col;
						buffer[x + y2] = col;
						buffer[x + y3] = col;
						buffer[x + y4] = col;
						
						x++;
						
						buffer[x + y1] = col;
						buffer[x + y2] = col;
						buffer[x + y3] = col;
						buffer[x + y4] = col;;
						
						x++;
						
						buffer[x + y1] = col;
						buffer[x + y2] = col;
						buffer[x + y3] = col;
						buffer[x + y4] = col;
						
						x++;
						
						buffer[x + y1] = col;
						buffer[x + y2] = col;
						buffer[x + y3] = col;
						buffer[x + y4] = col;
					}
				}
			break;
			
			default: throw new AssertionError("Zoom mode not supported");
		}
		
		g.drawImage(screen, ins.left, ins.top, null);
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
	}	
	
	//This will register which keys are being pressed and released.
	public void keyPressed(KeyEvent key)
	{
	   switch(key.getKeyCode())
		{
		   case KeyEvent.VK_LEFT:
			   buttonLEFT = true;
			break;
			
			case KeyEvent.VK_RIGHT:
			   buttonRIGHT = true;
			break;
			
			case KeyEvent.VK_UP:
			   buttonUP = true;
			break;
			
			case KeyEvent.VK_DOWN:
			   buttonDOWN = true;
			break;
			
			case KeyEvent.VK_X:
			   buttonA = true;
			break;
			
			case KeyEvent.VK_Z:
			   buttonB = true;
			break;
			
			case KeyEvent.VK_ENTER:
			   buttonSTART = true;
			break;
			
			case KeyEvent.VK_SPACE:
			   buttonSELECT = true;
			break;  
		}
	}
	
	public void keyReleased(KeyEvent key)
	{
	   switch(key.getKeyCode())
		{
		   case KeyEvent.VK_LEFT:
			   buttonLEFT = false;
			break;
			
			case KeyEvent.VK_RIGHT:
			   buttonRIGHT = false;
			break;
			
			case KeyEvent.VK_UP:
			   buttonUP = false;
			break;
			
			case KeyEvent.VK_DOWN:
			   buttonDOWN = false;
			break;
			
			case KeyEvent.VK_X:
			   buttonA = false;
			break;
			
			case KeyEvent.VK_Z:
			   buttonB = false;
			break;
			
			case KeyEvent.VK_ENTER:
			   buttonSTART = false;
			break;
			
			case KeyEvent.VK_SPACE:
			   buttonSELECT = false;
			break;  
		}
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
	
	
	//End of the key registering stuff. 	
	private class FileMenu extends Menu implements ActionListener {
		Frame mw;
		GUI gui;
			
		public FileMenu(Frame m, GUI g){
			super("File");
			mw = m;
			gui = g;
			MenuItem mi; 
		    add(mi = new MenuItem("Open")); 
		    mi.addActionListener(this);
		 	add(mi = new MenuItem("Run"));
			mi.addActionListener(this);
			add(mi = new MenuItem("Pause"));
			mi.addActionListener(this);
		    add(mi = new MenuItem("Exit")); 
		    mi.addActionListener(this); 
	
		}
		public void actionPerformed(ActionEvent e) { 
			String item = e.getActionCommand(); 
			if (item.equals("Open")){
				//mw.exit(); 
				FileDialog f = new FileDialog(mw, "Open ROM");
				f.setVisible(true);
				String file = f.getFile();
				if(file != null){
					ROM rom = new ROM(f.getDirectory()+file);
			        rom.printTitle();
			        rom.printCartType();
					rom.getRAMSize();
			        System.out.print("Color: ");
			        if(rom.isCGB())
			            System.out.println("Yes");
			        else
			            System.out.println("No");
			        // So this starts the CPU if it's a valid ROM
					if(rom.verifyChecksum()){
						/* Since we need to pass the ROM, unless we put some
						 * global variable for the ROM, we can't have it in
						 * another "start" option for now.
						 */
						
						if(cpu != null){
							synchronized(cpu)
							{
							System.out.println("Thread: "+cpu+ " Halted");	
							cpu.setHalt(true);
							cpu = null;
							}
						}
						
						cpu = new CPU(rom, gui);
						//cpu.addFrameListener(this);
						cpu.start();
					}
					// Should we output something for "Invalid ROM?"
				}
			}
			else if(item.equals("Run")){
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
	
	private class ViewMenu extends Menu implements ItemListener
	{
		private CheckboxMenuItem zoom1;
		private CheckboxMenuItem zoom2;
		private CheckboxMenuItem zoom3;
		private CheckboxMenuItem zoom4;
		
		public ViewMenu()
		{
			super("View");
			
			add(zoom1 = new CheckboxMenuItem("Zoom 1x",true));
		    zoom1.addItemListener(this); 
		    add(zoom2 = new CheckboxMenuItem("Zoom 2x")); 
		    zoom2.addItemListener(this); 
		    add(zoom3 = new CheckboxMenuItem("Zoom 3x")); 
		    zoom3.addItemListener(this); 
		    add(zoom4 = new CheckboxMenuItem("Zoom 4x")); 
			zoom4.addItemListener(this); 
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
		}
	}
	
	// This is broken: see my ViewMenu for a correct example of handling CheckBoxMenuItems
	private class SoundMenu extends Menu implements ActionListener {
		//Frame mw;
		public SoundMenu(){
			super("Sound");
			//mw = m;
			MenuItem mi; 
			add(mi = new CheckboxMenuItem("Sound Enable",true));
		    mi.addActionListener(this); 
		    add(mi = new CheckboxMenuItem("Channel 1")); 
		    mi.addActionListener(this); 
		    add(mi = new CheckboxMenuItem("Channel 2")); 
		    mi.addActionListener(this); 
		    add(mi = new CheckboxMenuItem("Channel 3")); 
		    mi.addActionListener(this); 
		    add(mi = new CheckboxMenuItem("Channel 4")); 
		    mi.addActionListener(this); 
		}
		public void actionPerformed(ActionEvent e) { 
			String item = e.getActionCommand(); 
			if (item.equals("Sound Enable")){
				System.out.println("***");
				// Toggle sound
			}
			else if(item.equals("Channel 1")){
				// Toggle channel
			}
			else if(item.equals("Channel 2")){
				// Toggle channel
			}
			else if(item.equals("Channel 3")){
				// Toggle channel
			}
			else if(item.equals("Channel 4")){
				// Toggle channel
			}
		}
	}
}
