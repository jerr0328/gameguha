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
	private static int[] temp;

	public ScreenRenderer(Semaphore sem)
	{
		this.sem = sem;
	}
	
	public void setGBVideo(int[] gbScreen)
	{
		this.gbScreen = gbScreen;
	}
	
	public void setReferences(int[] imgBuffer, Frame frame, int zoom, int filter, boolean fullScreen, BufferedImage screen)
	{
		if (fullScreen)
		{
			int width = frame.getWidth() - ins.left - ins.right;
			int height = frame.getHeight() - ins.top - ins.bottom;
			
			drawWidth = Math.min(width, (int)((160.0/144.0)*height + 0.5));
			drawHeight = Math.min(height, (int)((144.0/160.0)*width + 0.5));	
			xOffset = ins.left + ((width-drawWidth) >> 1);
			yOffset = ins.top + ((height-drawHeight) >> 1);
		}
		
		this.imgBuffer = imgBuffer;
		this.frame = frame;
		this.zoom = zoom;
		this.filter = filter;
		this.fullScreen = fullScreen;
		this.screen = screen;
		
		this.g = frame.getGraphics();
		this.ins = frame.getInsets();
	}
	
	public void run()
	{
		int[] buffer;
		int[] myTemp;
		int xPixel;
		int yPixel;
		int mult;
		int col;
		int x;
		int y1, y2, y3, y4;
		
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
			{
				e.printStackTrace();
			}
			
			buffer = imgBuffer;
			if (zoom == 4 && filter != 0)
			{
				if (temp == null)
					temp = new int[4*GUI.screenWidth*GUI.screenHeight];
			}
			else
				temp = null;
				
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
						
							for (yPixel = 1; yPixel < GUI.screenHeight-1; yPixel++)
								for (xPixel = 1; xPixel < GUI.screenWidth-1; xPixel++)
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
									
									int E0 = yPixel*(4*GUI.screenWidth) + (xPixel << 1);
									int E1 = E0 + 1;
									int E2 = E0 + (2*GUI.screenWidth);
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
						
						case 2: // Eagle
						
							for (yPixel = 1; yPixel < GUI.screenHeight-1; yPixel++)
								for (xPixel = 1; xPixel < GUI.screenWidth-1; xPixel++)
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
									
									int E0 = yPixel*(4*GUI.screenWidth) + (xPixel << 1);
									int E1 = E0 + 1;
									int E2 = E0 + (2*GUI.screenWidth);
									int E3 = E2 + 1;
									
									buffer[E0] = (D==A && A==B) ? A : E;
									buffer[E1] = (B==C && C==F) ? C : E;
									buffer[E2] = (D==G && G==H) ? G : E;
									buffer[E3] = (F==I && I==H) ? I : E;
								}
						
						break;
					}
				break;
				
				case 3:
					switch (filter)
					{
						case 0:
						case 2:
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
						
						case 1:
							
							for (yPixel = 1; yPixel < GUI.screenHeight-1; yPixel++)
								for (xPixel = 1; xPixel < GUI.screenWidth-1; xPixel++)
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
									
									int E0 = yPixel*(9*GUI.screenWidth) + 3*xPixel;
									int E1 = E0+1;
									int E2 = E1+1;
									int E3 = E0 + (3*GUI.screenWidth);
									int E4 = E3+1;
									int E5 = E4+1;
									int E6 = E3 + (3*GUI.screenWidth);
									int E7 = E6+1;
									int E8 = E7+1;
									
									if (B != H && D != F)
									{
										buffer[E0] = D == B ? D : E;
										buffer[E1] = (D == B && E != C) || (B == F && E != A) ? B : E;
										buffer[E2] = B == F ? F : E;
										buffer[E3] = (D == B && E != G) || (D == H && E != A) ? D : E;
										buffer[E4] = E;
										buffer[E5] = (B == F && E != I) || (H == F && E != C) ? F : E;
										buffer[E6] = D == H ? D : E;
										buffer[E7] = (D == H && E != I) || (H == F && E != G) ? H : E;
										buffer[E8] = H == F ? F : E;
									}
									else
									{
										buffer[E0] = E;
										buffer[E1] = E;
										buffer[E2] = E;
										buffer[E3] = E;
										buffer[E4] = E;
										buffer[E5] = E;
										buffer[E6] = E;
										buffer[E7] = E;
										buffer[E8] = E;
									}
								}
							
						break;
					}
				break;
				
				case 4:
					switch (filter)
					{
						case 0:
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
						
						case 1:
							
							myTemp = temp;
							
							for (yPixel = 1; yPixel < GUI.screenHeight-1; yPixel++)
								for (xPixel = 1; xPixel < GUI.screenWidth-1; xPixel++)
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
									
									int E0 = yPixel*(4*GUI.screenWidth) + (xPixel << 1);
									int E1 = E0 + 1;
									int E2 = E0 + (2*GUI.screenWidth);
									int E3 = E2 + 1;
									
									if (B != H && D != F)
									{
										myTemp[E0] = (D == B ? D : E);
										myTemp[E1] = (B == F ? F : E);
										myTemp[E2] = (D == H ? D : E);
										myTemp[E3] = (H == F ? F : E);
									}
									else
									{
										myTemp[E0] = E;
										myTemp[E1] = E;
										myTemp[E2] = E;
										myTemp[E3] = E;
									}
								}
							
							for (yPixel = 2; yPixel < (2*GUI.screenHeight)-2; yPixel++)
								for (xPixel = 2; xPixel < (2*GUI.screenWidth)-2; xPixel++)
								{
									//buffer[GUI.screenWidth*4*yPixel + xPixel] = temp[GUI.screenWidth*2*yPixel + xPixel];
									int A = myTemp[(yPixel-1)*(2*GUI.screenWidth) + (xPixel-1)];
									int B = myTemp[(yPixel-1)*(2*GUI.screenWidth) + xPixel];
									int C = myTemp[(yPixel-1)*(2*GUI.screenWidth) + (xPixel+1)];
									int D = myTemp[yPixel*(2*GUI.screenWidth) + (xPixel-1)];
									int E = myTemp[yPixel*(2*GUI.screenWidth) + xPixel];
									int F = myTemp[yPixel*(2*GUI.screenWidth) + (xPixel+1)];
									int G = myTemp[(yPixel+1)*(2*GUI.screenWidth) + (xPixel-1)];
									int H = myTemp[(yPixel+1)*(2*GUI.screenWidth) + xPixel];
									int I = myTemp[(yPixel+1)*(2*GUI.screenWidth) + (xPixel+1)];
									
									int E0 = yPixel*(8*GUI.screenWidth) + (xPixel << 1);
									int E1 = E0 + 1;
									int E2 = E0 + (4*GUI.screenWidth);
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
						
						case 2:
							
							myTemp = temp;
							
							for (yPixel = 1; yPixel < GUI.screenHeight-1; yPixel++)
								for (xPixel = 1; xPixel < GUI.screenWidth-1; xPixel++)
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
									
									int E0 = yPixel*(4*GUI.screenWidth) + (xPixel << 1);
									int E1 = E0 + 1;
									int E2 = E0 + (2*GUI.screenWidth);
									int E3 = E2 + 1;
									
									myTemp[E0] = (D==A && A==B) ? A : E;
									myTemp[E1] = (B==C && C==F) ? C : E;
									myTemp[E2] = (D==G && G==H) ? G : E;
									myTemp[E3] = (F==I && I==H) ? I : E;
								}
							
							for (yPixel = 2; yPixel < (2*GUI.screenHeight)-2; yPixel++)
								for (xPixel = 2; xPixel < (2*GUI.screenWidth)-2; xPixel++)
								{
									//buffer[GUI.screenWidth*4*yPixel + xPixel] = temp[GUI.screenWidth*2*yPixel + xPixel];
									int A = myTemp[(yPixel-1)*(2*GUI.screenWidth) + (xPixel-1)];
									int B = myTemp[(yPixel-1)*(2*GUI.screenWidth) + xPixel];
									int C = myTemp[(yPixel-1)*(2*GUI.screenWidth) + (xPixel+1)];
									int D = myTemp[yPixel*(2*GUI.screenWidth) + (xPixel-1)];
									int E = myTemp[yPixel*(2*GUI.screenWidth) + xPixel];
									int F = myTemp[yPixel*(2*GUI.screenWidth) + (xPixel+1)];
									int G = myTemp[(yPixel+1)*(2*GUI.screenWidth) + (xPixel-1)];
									int H = myTemp[(yPixel+1)*(2*GUI.screenWidth) + xPixel];
									int I = myTemp[(yPixel+1)*(2*GUI.screenWidth) + (xPixel+1)];
									
									int E0 = yPixel*(8*GUI.screenWidth) + (xPixel << 1);
									int E1 = E0 + 1;
									int E2 = E0 + (4*GUI.screenWidth);
									int E3 = E2 + 1;
									
									buffer[E0] = (D==A && A==B) ? A : E;
									buffer[E1] = (B==C && C==F) ? C : E;
									buffer[E2] = (D==G && G==H) ? G : E;
									buffer[E3] = (F==I && I==H) ? I : E;
								}
							
						break;
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
