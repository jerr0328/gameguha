import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*; // random colors look cool! but slow to generate

public class GUI
{
	private int screenWidth = 160*4;
	private int screenHeight = 144*4;
	private CPU cpu;
	
	public static void main(String[] args)
	{
		new GUI().go();
	}
	
	public void go()
	{
    	Frame frame = new Frame("GameGuha");
    	MenuBar mb = new MenuBar();
    	mb.add(new FileMenu(frame));
    	mb.add(new SoundMenu(frame));
    	frame.setMenuBar(mb);
    	
		frame.setVisible(true); 
	
		Insets ins = frame.getInsets();
		System.out.printf("top:%d bot:%d left:%d right:%d\n", ins.top, ins.bottom, ins.left, ins.right);
		frame.setSize(screenWidth + ins.left + ins.right, screenHeight + ins.top + ins.bottom); 
			
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
        		System.exit(0);
     		}
		});
		
		Graphics g = frame.getGraphics();
		BufferedImage screen = new BufferedImage(screenWidth,screenHeight,BufferedImage.TYPE_INT_RGB);
		int[] buffer = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();
		
		Random gen = new Random();
		int frames = 0;
		long startT = System.nanoTime();

		while(frames < 0) // change back to while(true) for pretty colors
		{
			frames++;
			
			if (frames == 60)
			{
			//	System.out.println((System.nanoTime()-startT)/1000000000.0 + " seconds");
				frames = 0;
				startT = System.nanoTime();
			}
			
			int xPixel, x, yPixel, y1 = 0, y2 = -320, y3 = -480, y4 = -640;
			
			/* 1x Zoom
			for(yPixel = 0; yPixel < 144; yPixel++)
			{
				for (xPixel = 0; xPixel < 160; xPixel++)
				{
					buffer[xPixel+y1] = 0x0000FF00;
				}
				y1 += 160;
			}
			*/
			
			/* 2x Zoom
			for(yPixel = 0; yPixel < 144; yPixel++)
			{
				y1 = y2+320;
				y2 = y1+320;
				
				for (x = 0; x < 320; x++)
				{
					xPixel = x >> 1;

					buffer[x + y1] = 0x0000FF00;
					buffer[x + y2] = 0x0000FF00;
					
					x++;
					
					buffer[x + y1] = 0x0000FF00;
					buffer[x + y2] = 0x0000FF00;
				}
			}
			*/
			
			/* 3x Zoom
			for(yPixel = 0; yPixel < 144; yPixel++)
			{
				y1 = y3+480;
				y2 = y1+480;
				y3 = y2+480;
				x = 0;
				
				for (xPixel = 0; xPixel < 160; xPixel++)
				{
					buffer[x + y1] = 0x0000FF00;
					buffer[x + y2] = 0x0000FF00;
					buffer[x + y3] = 0x0000FF00;
					
					x++;
					
					buffer[x + y1] = 0x0000FF00;
					buffer[x + y2] = 0x0000FF00;
					buffer[x + y3] = 0x0000FF00;
					
					x++;
					
					buffer[x + y1] = 0x0000FF00;
					buffer[x + y2] = 0x0000FF00;
					buffer[x + y3] = 0x0000FF00;
					
					x++;
				}
			}
			*/
			
			for(yPixel = 0; yPixel < 144; yPixel++)
			{
				y1 = y4+640;
				y2 = y1+640;
				y3 = y2+640;
				y4 = y3+640; 
				
				for (x = 0; x < 640; x++)
				{
					xPixel = x >> 2;
					
					int randCol = gen.nextInt();

					buffer[x + y1] = randCol;
					buffer[x + y2] = randCol;
					buffer[x + y3] = randCol;
					buffer[x + y4] = randCol;
					
					x++;
					
					buffer[x + y1] = randCol;
					buffer[x + y2] = randCol;
					buffer[x + y3] = randCol;
					buffer[x + y4] = randCol;;
					
					x++;
					
					buffer[x + y1] = randCol;
					buffer[x + y2] = randCol;
					buffer[x + y3] = randCol;
					buffer[x + y4] = randCol;
					
					x++;
					
					buffer[x + y1] = randCol;
					buffer[x + y2] = randCol;
					buffer[x + y3] = randCol;
					buffer[x + y4] = randCol;
				}
			}
			
			g.drawImage(screen,ins.left,ins.top,null); 
		}
	}
	
	class FileMenu extends Menu implements ActionListener {
		Frame mw;
			
		public FileMenu(Frame m){
			super("File");
			mw = m;
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
					rom.printRAMSize();
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
						
						cpu = new CPU(rom);
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
	
	class SoundMenu extends Menu implements ActionListener {
		Frame mw;
		public SoundMenu(Frame m){
			super("Sound");
			mw = m;
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
