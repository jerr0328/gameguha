import java.awt.*;
import java.awt.image.*;
import java.util.concurrent.*;

public final class ScreenRenderer extends Thread
{
	//private static long lastUsedMem;
	private final Semaphore sem;
	private static int[] imgBuffer;
	private static int[] gbScreen;
	private static Frame frame;
	private static Graphics g;
	private static int zoom;
	private static boolean fullScreen;
	private static BufferedImage screen;
	private static Insets ins;
	private static int drawWidth;
	private static int drawHeight;	
	private static int xOffset;
	private static int yOffset;
	private static int filter;

	public ScreenRenderer(Semaphore sem)
	{
		this.sem = sem;
	}
	
	public void setGBVideo(int[] gbScreen, int filter)
	{
		this.gbScreen = gbScreen;
		this.filter = filter;
	}
	
	public void setReferences(int[] imgBuffer, Frame frame, int zoom, boolean fullScreen, BufferedImage screen)
	{
		this.imgBuffer = imgBuffer;
		this.frame = frame;
		this.zoom = zoom;
		this.fullScreen = fullScreen;
		this.screen = screen;
		
		this.g = frame.getGraphics();
		this.ins = frame.getInsets();
		
		if (fullScreen)
		{
			int width = frame.getWidth() - ins.left - ins.right;
			int height = frame.getHeight() - ins.top - ins.bottom;
			
			drawWidth = Math.min(width, (int)((160.0/144.0)*height + 0.5));
			drawHeight = Math.min(height, (int)((144.0/160.0)*width + 0.5));	
			xOffset = ins.left + ((width-drawWidth) >> 1);
			yOffset = ins.top + ((height-drawHeight) >> 1);
		}
	}
	
	public void run()
	{
		for (;;)
		{
			/*while (!newFrame)
			{
				try 
				{
					Thread.sleep(1);
				}
				catch (Exception e)
				{
				}
			}*/
			try
			{
				sem.acquire();
			}
			catch (InterruptedException e)
			{}
		
			int[] buffer = imgBuffer;
					
			int xPixel;
			int yPixel;
			int mult;
			int col;
			int x;
			int y1, y2, y3, y4;
			
			switch (zoom)
			{
				case 1:
					System.arraycopy(gbScreen, 0, buffer, 0, buffer.length);
				break;
				
				case 2:
					switch (filter)
					{
						case 0:
							y2 = -(GUI.screenWidth*2);
							for(yPixel = 0; yPixel < 144; yPixel++)
							{
								mult = yPixel*GUI.screenWidth;
								y1 = y2+320;
								y2 = y1+320;
								
								for (x = 0; x < 320; x++)
								{
									xPixel = x >> 1;
									
									col = gbScreen[mult + xPixel];
				
									buffer[x + y1] = col;
									buffer[x + y2] = col;
									
									x++;
									
									buffer[x + y1] = col;
									buffer[x + y2] = col;
								}
							}
						break;
						
						case 1: // Scale2x (AdvMAME2x)
						
							for (xPixel = 1; xPixel < GUI.screenWidth-1; xPixel++)
								for (yPixel = 1; yPixel < GUI.screenHeight-1; yPixel++)
								{
									int A = gbScreen[(yPixel-1)*GUI.screenWidth + (xPixel-1)];
									int B = gbScreen[(yPixel-1)*GUI.screenWidth + xPixel];
									int C = gbScreen[(yPixel-1)*GUI.screenWidth + (xPixel+1)];
									int D = gbScreen[yPixel*GUI.screenWidth + (xPixel-1)];
									int E = gbScreen[yPixel*GUI.screenWidth + xPixel];
									int F = gbScreen[yPixel*GUI.screenWidth + (xPixel+1)];
									int G = gbScreen[(yPixel+1)*GUI.screenWidth + (xPixel-1)];
									int H = gbScreen[(yPixel+1)*GUI.screenWidth + xPixel];
									int I = gbScreen[(yPixel+1)*GUI.screenWidth + (xPixel+1)];
									
									int E0 = (yPixel << 2)*GUI.screenWidth + (xPixel << 1);
									int E1 = E0 + 1;
									int E2 = E0 + (GUI.screenWidth << 1);
									int E3 = E2 + 1;
									
									if (B != H && D != F)
									{
										buffer[E0] = (D == B ? D : E);
										buffer[E1] = (B == F ? F : E);
										buffer[E2] = (D == H ? D : E);
										buffer[E3] = (H == F ? F : E);
									}
									else
									{
										buffer[E0] = E;
										buffer[E1] = E;
										buffer[E2] = E;
										buffer[E3] = E;
									}
								}
						break;
					}
				break;
				
				case 3:
					y3 = -(GUI.screenWidth*3);
					for(yPixel = 0; yPixel < 144; yPixel++)
					{
						mult = yPixel*GUI.screenWidth;
						y1 = y3+480;
						y2 = y1+480;
						y3 = y2+480;
						x = 0;
						
						for (xPixel = 0; xPixel < 160; xPixel++)
						{
							col = gbScreen[mult + xPixel];
							
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
					y4 = -(GUI.screenWidth*4);
					for(yPixel = 0; yPixel < 144; yPixel++)
					{
						mult = yPixel*GUI.screenWidth;
						y1 = y4+640;
						y2 = y1+640;
						y3 = y2+640;
						y4 = y3+640; 
						
						for (x = 0; x < 640; x++)
						{
							xPixel = x >> 2;
							
							col = gbScreen[mult + xPixel];
		
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
			
			//((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			//Insets ins = frame.getInsets();
			
			if (fullScreen)
				g.drawImage(screen, xOffset, yOffset, drawWidth, drawHeight, null);
			else
				g.drawImage(screen, ins.left, ins.top, null);
			

			/*String s = "";
			for (int i = 0; i < 1000000; i++)
				s = Integer.toString(i);
			System.out.println(s);*/
			//newFrame = false;
		}
		
     /*System.gc();  
     System.gc();  
     System.gc();  
    
     // measure memory usage & change:  
     Runtime rt = Runtime.getRuntime();  
     long totalMem = rt.totalMemory();  
     long freeMem = rt.freeMemory();  
     long usedMem = totalMem - freeMem;  
     long diff = usedMem -lastUsedMem;  
     lastUsedMem = usedMem;  
    
     // report:  
     System.out.print("Memory used: " + usedMem);  
     System.out.println("  increased by: " + ((diff >= 0) ? "+" : "") + diff);  
		//System.out.println("returning");*/
	}
}
