import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*; // random colors look cool! but slow to generate

public class GUI
{
	private static int screenWidth = 160*4;
	private static int screenHeight = 144*4;
    
	public static void main(String[] args)
	{
    	Frame frame = new Frame("GameGuha");
    	MenuBar mb = new MenuBar();
    	mb.add(new Menu("File"));
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