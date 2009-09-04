/* GameGuha
 * ROM Loader class
 */

import java.io.*;

public class ROM{
	public int[] MEM = new int[0x10000]; 
	
	// String constructor (usually for testing)
	public ROM(String filename){
		load(new File(filename));
	}
	// For use when we have a GUI ROM loading
	public ROM(File file){
		load(file);
	}
	
	// Loads the ROM into memory
	private void load(File file){
		try{
			BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
			for(int i = 0; i < 0x8000; i++){
				MEM[i]=buf.read();
			}
			buf.close();
		}
		catch(Exception e){ e.printStackTrace(); }
		
	}
	
	// For now, just returns get Memory from ROM at specified address
	public int getMem(int index)
	{
		return MEM[index];
	}
	
	// Dumps some data... use for debugging
	public void dumpData(){
		System.out.println("Dump:");
		for(int i = 0; i < 0x10; i++){
			System.out.print(Integer.toHexString(MEM[i])+" ");
			if(i%16==0)
				System.out.println();
		}
	}
	
	// Returns true if the ROM is a Color GB
	public boolean isCGB(){
		if(MEM[0x0143]==0){
			return false;
		}
		return true;
	}
	
	// Prints cartridge type, as specified in docs
	public void printCartType(){
		String out="";
		switch(MEM[0x0147]){
			case 0x0: out="0 ROM ONLY"; break;
			case 0x1: out="1 ROM+MBC1"; break;
			case 0x2: out="2 ROM+MBC1+RAM"; break;
			case 0x3: out="3 ROM+MBC1+RAM+BATT"; break;
			case 0x5: out="5 ROM+MBC2"; break;
			case 0x6: out="6 ROM+MBC2+BATTERY"; break;
			case 0x8: out="8 ROM+RAM"; break;
			case 0x9: out="9 ROM+RAM+BATTERY"; break;
			case 0xB: out="B ROM+MMM01"; break;
			case 0xC: out="C ROM+MMM01+SRAM"; break;
			case 0xD: out="D ROM+MMM01+SRAM+BATT"; break;
			case 0x12: out="12 ROM+MBC3+RAM"; break;
			case 0x13: out="13 ROM+MBC3+RAM+BATT"; break;
			case 0x19: out="19 ROM+MBC5"; break;
			case 0x1A: out="1A ROM+MBC5+RAM"; break;
			case 0x1B: out="1B ROM+MBC5+RAM+BATT"; break;
			case 0x1C: out="1C ROM+MBC5+RUMBLE"; break;
			case 0x1D: out="1D ROM+MBC5+RUMBLE+SRAM"; break;
			case 0x1E: out="1E ROM+MBC5+RUMBLE+SRAM+BATT"; break;
			case 0x1F: out="1F Pocket Camera"; break;
			case 0xFD: out="FD Bandai TAMA5"; break;
			case 0xFE: out="FE Hudson HuC-3"; break;
			
		}
		System.out.println("Cartridge type: "+out);
	}
	// Prints ROM size, as specified in docs
	public void printROMSize(){
		String out="";
		switch(MEM[0x148]){
			case 0x0: out="32KB (no bank)"; break;
			case 0x1: out="64KB (4 bank)"; break;
			case 0x2: out="128KB (8 banks)"; break;
			case 0x3: out="256KB (16 banks)"; break;
			case 0x4: out="512KB (32 banks)"; break;
			case 0x5: out="1MB (64 banks)"; break;
			case 0x6: out="2MB (128 banks)"; break;
			case 0x7: out="4MB (256 banks)"; break;
			case 0x52: out="1.1MB (72 banks)"; break;
			case 0x53: out="1.2MB (80 banks)"; break;
			case 0x54: out="1.5MB (96 banks)"; break;
			
		}
		System.out.println("ROM Size: "+out);
	}
	
	public void printRAMSize(){
		String out="";
		switch(MEM[0x149]){
			case 0x0: out="no RAM"; break;
			case 0x1: out="2KB RAM"; break;
			case 0x2: out="8KB RAM"; break;
			case 0x3: out="32KB RAM (8x4)"; break;
			case 0x4: out="128KB RAM (8x16)"; break;
		
		}
		System.out.println("RAM Size: "+out);
	}

	// Verify checksum of ROM, prints result
	// Returns true if checksum is valid, otherwise false
	public boolean verifyChecksum(){
		final int[] nintyBitmap = 
		{0xCE,0xED,0x66,0x66,0xCC,0x0D,0x00,0x0B,0x03,
		 0x73,0x00,0x83,0x00,0x0C,0x00,0x0D,0x00,0x08,
		 0x11,0x1F,0x88,0x89,0x00,0x0E,0xDC,0xCC,0x6E,
		 0xE6,0xDD,0xDD,0xD9,0x99,0xBB,0xBB,0x67,0x63,
		 0x6E,0x0E,0xEC,0xCC,0xDD,0xDC,0x99,0x9F,0xBB, 
		 0xB9,0x33,0x3E};
	  
		int ptr=0x0104;
		int x=0;
		
		while (ptr <= 0x0133){
			if(MEM[ptr]!=nintyBitmap[ptr-0x0104])
				{
					System.out.println("Bitmap Invalid");
					System.out.println("Header Checksum Invalid");
					break;
				}
		ptr++;
		}
		if(ptr==0x0134)
		{
			//x=0:FOR i=0134h TO 014Ch:x=x-MEM[i]-1:NEXT
	  		for(;ptr<=0x014C;ptr++)
			{
				x=x-MEM[ptr]-1; //checksum algorithm
			}
			if((x&0xFF) == MEM[ptr]){
				System.out.println("Header Checksum Valid");
				return true;
			}
			// else
			System.out.println("Header Checksum Invalid");
		}
		return false;
	}
	
	// Prints game title
	public void printTitle(){
		System.out.print("ROM Title: ");
		int ptr=0x0134;
		while(ptr < 0x0143 && MEM[ptr]!=0){
			System.out.print((char)MEM[ptr]);
			ptr++;
		}
		System.out.println();
	}
}