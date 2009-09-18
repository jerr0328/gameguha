import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*; // random colors look cool! but slow to generate

public class GUI
{
	private static int screenWidth = 160*4;
	private static int screenHeight = 144*4;
	protected static CPU cpu;
	
	public static void main(String[] args)
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

		while(true)
		{
			frames++;
			
			if (frames == 60)
			{
				System.out.println((System.nanoTime()-startT)/1000000000.0 + " seconds");
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
					GUI.cpu = new CPU(rom);
					GUI.cpu.start();
				}
				// Should we output something for "Invalid ROM?"
			}
		}
		if(item.equals("Run")){
			if(!GUI.cpu.getWaiting()){}
			//Dunno what to do here for now
			else
				synchronized(GUI.cpu){
				GUI.cpu.setWaiting(false);
				GUI.cpu.notify();
				}
		}
		if(item.equals("Pause")){
			if(!GUI.cpu.getWaiting())
				{
					synchronized(GUI.cpu){ GUI.cpu.setWaiting(true); }
				}
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