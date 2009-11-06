public final class CPU extends Thread
{
/*	private static class ThreadLocalWaiting extends ThreadLocal
	{
		protected boolean pleaseWait=false;
		protected synchronized Object InitialValue()
		{
			return pleaseWait;
		}
		
		public synchronized Object get()
		{
			return pleaseWait;
		}
		
		protected synchronized Object set(boolean isWaiting)
		{
			pleaseWait = isWaiting;
		}
	}*/
	//boolean[] visited = new boolean[0x10000];
	
	private static final boolean throttle = true;
	
	public static final int BIT7 = 1<<7;
 	public static final int BIT6 = 1<<6;
	public static final int BIT5 = 1<<5;
	public static final int BIT4 = 1<<4;
	public static final int BIT3 = 1<<3;
	public static final int BIT2 = 1<<2;
	public static final int BIT1 = 1<<1;
	public static final int BIT0 = 1<<0;
	
	private static final int ZERO       = BIT7;
	private static final int SUBTRACT   = BIT6;
	private static final int HALF_CARRY = BIT5;
	private static final int CARRY      = BIT4;
	
	private static final int CYCLES_PER_LINE = 114; // (1048576 Hz/ 9198 Hz)
	private static final long nsPerFrame = (long)(1000000000/59.73);
	
	private static final int[] color = {0xFFFFFFFF, 0xFFC0C0C0, 0xFF404040, 0xFF000000}; // WHITE, LIGHT_GRAY, DARK_GRAY, BLACK
	
	// P1; // $FF00
	// SB; // $FF01
	// SC; // $FF02
	// DIV; // $FF04
	// TIMA; // $FF05
	// TMA; // $FF06
	// TAC; // $FF07
	// IF; // $FF0F
	// NR21; // $FF16 bits 6-7
	// NR22; // $FF17 entire byte
	// NR24; // $FF19 bit 6
	// LCDC = 0x91; // $FF40
	// STAT; // $FF41
	// SCY; // $FF42
	// SCX; // $FF43
	// LY; // $FF44
	// LYC; // $FF45
	// DMA; // $FF46
	// BGP = 0xFC; // $FF47
	// OBP0 = 0xFF; // $FF48
	// OBP1 = 0xFF; // $FF49
	// WY; // $FF4A
	// WX; // $FF4B
	// IE; // $FFFF

	private static ROM rom;
	private static GUI gui;
	private static Sound snd;
	private static int mbc;
	private static int mbcMode = 0;
	private static int mbcSig = 0;
	private static boolean pleaseWait;
	private static boolean halt;
	private static int newSerialInt;
	private static boolean joypadFlag;
	private static boolean colorsChanged;
	
	private static final int[] colorBG = new int[4];
	private static final int[] colorSP0 = new int[4];
	private static final int[] colorSP1 = new int[4];
	
	private static final int[] dirtyTiles1 = new int[16];
	private static final int[] dirtyTiles2 = new int[16];
	
	//private EventListenerList frameListeners = new EventListenerList();
	
	public CPU(String fileName, GUI guiPointer)
	{
		// Reset static variables
		rom = new ROM(fileName);
		mbc = rom.getCartType(false);
		System.out.println("mbc num: " + mbc);
		gui = guiPointer;
		snd = new Sound();
		pleaseWait = false; // Not in a waiting state on first run
		halt = false;
	}
	
	public String toString()
	{
		return rom.getTitle();
	}
	
	public boolean getHalt()
	{
		return halt;
	}
	
	public void setHalt(boolean flag) {
		halt = flag;
	}
	
	public boolean getWaiting()
	{
		return pleaseWait;
	}
	
	public void setWaiting(boolean flag)
	{
		pleaseWait = flag;
	}
	
	public void joypadInt()
	{
		//System.out.println("joypad interrupt requested");
		joypadFlag = true;
	}
	
	private static final void genFlagTable(final int[] FLAG_ADD, final int[] FLAG_SUB, final int[] FLAG_INC, final int[] FLAG_DEC)
	{
		int result, flag;
		for (int a = 0; a <= 256; a++)
			for (int b = 0; b <= 255; b++)
			{
				result = b+a;
				flag = 0;
				if ((b & 0x0F) + (a & 0x0F) > 0x0F) // carry from bit[3]
					flag |= HALF_CARRY;
				if (result > 0xFF)
				{
					result &= 0xFF;
					flag |= CARRY;
				}
				if (result == 0)
					flag |= ZERO;
				FLAG_ADD[(a << 8) | b] = flag;
				
				result = b-a;
				flag = SUBTRACT;
				if ((b & 0x0F) < (a & 0x0F)) // borrow from bit[4]
					flag |= HALF_CARRY;
				if (result < 0)
				{
					result &= 0xFF;
					flag |= CARRY;
				}
				if (result == 0)
					flag |= ZERO;
				FLAG_SUB[(a << 8) | b] = flag;
			}
		
		for (int a = 0; a <= 255; a++)
		{
				flag = 0;
				if ((a & 0x0F) == 0x0F)
					flag |= HALF_CARRY;
				if (a == 255)
					flag |= ZERO;
				FLAG_INC[a] = flag;
				
				flag = SUBTRACT;
				if ((a & 0x0F) == 0)
					flag |= HALF_CARRY;
				if (a == 1)
					flag |= ZERO;
				FLAG_DEC[a] = flag;
		}
	}
	
	private static final int readMem(final int[][] mem, final int index)
	{
		return mem[index >> 13][index & 0x1FFF];
	}
	
	private static final int writeMem(final int[][] mem, final int index, final int val)
	{
		switch (index >> 8)
		{
			case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
			case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: case 0x0D: case 0x0E: case 0x0F:
			case 0x10: case 0x11: case 0x12: case 0x13: case 0x14: case 0x15: case 0x16: case 0x17:
			case 0x18: case 0x19: case 0x1A: case 0x1B: case 0x1C: case 0x1D: case 0x1E: case 0x1F:
				return val;
			
			case 0x20: case 0x21: case 0x22: case 0x23: case 0x24: case 0x25: case 0x26: case 0x27:
			case 0x28: case 0x29: case 0x2A: case 0x2B: case 0x2C: case 0x2D: case 0x2E: case 0x2F:
			case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: case 0x35: case 0x36: case 0x37:
			case 0x38: case 0x39: case 0x3A: case 0x3B: case 0x3C: case 0x3D: case 0x3E: case 0x3F:
				switch(mbc)
				{
					case 0:
						return val;
					
					case 1:
						int bank = (val & 0x1F);
						if (bank == 0)
							bank = 1;
						bank |= mbcSig;
						//System.out.println(Integer.toBinaryString(val) + " out of " + Integer.toBinaryString(rom.numROMBanks));
						if (bank < rom.numROMBanks)
						{
							mem[2] = rom.getROM(bank, 0);
							mem[3] = rom.getROM(bank, 1);
						}
						return val;
					
					case 2:
						if ((index & 0x10) == 0)
							return val;
						bank = (val & 0x0F);
						if (bank == 0)
							bank = 1;
						mem[2] = rom.getROM(bank, 0);
						mem[3] = rom.getROM(bank, 1);
						return val;
					
					case 3:
						bank = (val & 0x7F);
						if (bank == 0)
							bank = 1;
						mem[2] = rom.getROM(bank, 0);
						mem[3] = rom.getROM(bank, 1);
						return val;
					
					case 5:
						//...
						return val;
					
					default:
						throw new AssertionError("Invalid MBC Type");		
				}
			
			case 0x40: case 0x41: case 0x42: case 0x43: case 0x44: case 0x45: case 0x46: case 0x47:
			case 0x48: case 0x49: case 0x4A: case 0x4B: case 0x4C: case 0x4D: case 0x4E: case 0x4F:
			case 0x50: case 0x51: case 0x52: case 0x53: case 0x54: case 0x55: case 0x56: case 0x57:
			case 0x58: case 0x59: case 0x5A: case 0x5B: case 0x5C: case 0x5D: case 0x5E: case 0x5F:
				switch(mbc)
				{
					case 0:
						return val;
					
					case 1:
						if (mbcMode == 0)
							mbcSig = (val & 0x03) << 5;
						else
							mem[5] = rom.getRAM(val & 0x03);
						return val;
					
					case 2:
						return val;
					
					case 3:
						mem[5] = rom.getRAM(val & 0x03);
						return val;
					
					case 5:
						//...
						return val;
					
					default:
						throw new AssertionError("Invalid MBC Type");
				}
			
			case 0x60: case 0x61: case 0x62: case 0x63: case 0x64: case 0x65: case 0x66: case 0x67:
			case 0x68: case 0x69: case 0x6A: case 0x6B: case 0x6C: case 0x6D: case 0x6E: case 0x6F:
			case 0x70: case 0x71: case 0x72: case 0x73: case 0x74: case 0x75: case 0x76: case 0x77:
			case 0x78: case 0x79: case 0x7A: case 0x7B: case 0x7C: case 0x7D: case 0x7E: case 0x7F:
				switch(mbc)
				{
					case 0:
						return val;
					
					case 1:
						mbcMode = val & 0x01;
						if (mbcMode == 0)
							mem[5] = rom.getRAM(0);
						else
							mbcSig = 0;
						return val;
					
					case 2:
					case 3:
						return val;
					
					case 5:
						//...
						return val;
					
					default:
						throw new AssertionError("Invalid MBC Type");
				}
			
			case 0x80:
				dirtyTiles1[0] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x81:
				dirtyTiles1[1] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x82:
				dirtyTiles1[2] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x83:
				dirtyTiles1[3] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x84:
				dirtyTiles1[4] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x85:
				dirtyTiles1[5] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x86:
				dirtyTiles1[6] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x87:
				dirtyTiles1[7] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
				
			case 0x88:
				dirtyTiles1[8] |= 1 << ((index & 0x00F0) >> 4);
				dirtyTiles2[0] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x89:
				dirtyTiles1[9] |= 1 << ((index & 0x00F0) >> 4);
				dirtyTiles2[1] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x8A:
				dirtyTiles1[10] |= 1 << ((index & 0x00F0) >> 4);
				dirtyTiles2[2] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x8B:
				dirtyTiles1[11] |= 1 << ((index & 0x00F0) >> 4);
				dirtyTiles2[3] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x8C:
				dirtyTiles1[12] |= 1 << ((index & 0x00F0) >> 4);
				dirtyTiles2[4] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x8D:
				dirtyTiles1[13] |= 1 << ((index & 0x00F0) >> 4);
				dirtyTiles2[5] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x8E:
				dirtyTiles1[14] |= 1 << ((index & 0x00F0) >> 4);
				dirtyTiles2[6] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x8F:
				dirtyTiles1[15] |= 1 << ((index & 0x00F0) >> 4);
				dirtyTiles2[7] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			
			case 0x90:
				dirtyTiles2[8] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x91:
				dirtyTiles2[9] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x92:
				dirtyTiles2[10] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x93:
				dirtyTiles2[11] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x94:
				dirtyTiles2[12] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x95:
				dirtyTiles2[13] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x96:
				dirtyTiles2[14] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);
			case 0x97:
				dirtyTiles2[15] |= 1 << ((index & 0x00F0) >> 4);
				return (mem[4][index & 0x1FFF] = val);

			case 0x98: case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9E: case 0x9F:
				return (mem[4][index & 0x1FFF] = val);
			
			case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4: case 0xA5: case 0xA6: case 0xA7:
			case 0xA8: case 0xA9: case 0xAA: case 0xAB: case 0xAC: case 0xAD: case 0xAE: case 0xAF:
			case 0xB0: case 0xB1: case 0xB2: case 0xB3: case 0xB4: case 0xB5: case 0xB6: case 0xB7:
			case 0xB8: case 0xB9: case 0xBA: case 0xBB: case 0xBC: case 0xBD: case 0xBE: case 0xBF:
				return (mem[5][index & 0x1FFF] = val);
			
			case 0xC0: case 0xC1: case 0xC2: case 0xC3: case 0xC4: case 0xC5: case 0xC6: case 0xC7:
			case 0xC8: case 0xC9: case 0xCA: case 0xCB: case 0xCC: case 0xCD: case 0xCE: case 0xCF:
			case 0xD0: case 0xD1: case 0xD2: case 0xD3: case 0xD4: case 0xD5: case 0xD6: case 0xD7:
			case 0xD8: case 0xD9: case 0xDA: case 0xDB: case 0xDC: case 0xDD: case 0xDE: case 0xDF:
				return (mem[6][index & 0x1FFF] = val);
			
			case 0xE0: case 0xE1: case 0xE2: case 0xE3: case 0xE4: case 0xE5: case 0xE6: case 0xE7:
			case 0xE8: case 0xE9: case 0xEA: case 0xEB: case 0xEC: case 0xED: case 0xEE: case 0xEF:
			case 0xF0: case 0xF1: case 0xF2: case 0xF3: case 0xF4: case 0xF5: case 0xF6: case 0xF7:
			case 0xF8: case 0xF9: case 0xFA: case 0xFB: case 0xFC: case 0xFD: case 0xFE:
				return (mem[7][index & 0x1FFF] = val);
			
			case 0xFF:
				switch(index)
				{
					// Handle IO ports
					case 0xFF00: //Joypad
						/*Button controls
						7    6      5       4       3     2     1    0
						[NA][NA][Sel Btn][Sel Dir][D/St][U/Sel][L/B][R/A]*/
						// Check for Joypad presses
						// check 5 or 4
						{int temp = (val | 0x0F);
						if((val & BIT5) == 0)
						{
							if(gui.getStart())
								temp &= ~BIT3;

							if(gui.getSelect())
								temp &= ~BIT2;
				
							if(gui.getB())
								temp &= ~BIT1;
				
							if(gui.getA())
								temp &= ~BIT0;
						}
						else if((val & BIT4) == 0)
						{
							if(gui.getDown())
								temp &= ~BIT3;
				
							if(gui.getUp())
								temp &= ~BIT2;

							if(gui.getLeft())
								temp &= ~BIT1;

							if(gui.getRight())
								temp &= ~BIT0;
						}
						return (mem[7][0x1F00] = temp);}
					case 0xFF01:
						return mem[7][0x1F01];
					case 0xFF02:
						if ((val & BIT7) != 0 && (val & BIT0) != 0)
							newSerialInt = 10;
						return (mem[7][0x1F02] = val);
					case 0xFF04:
						return (mem[7][0x1F04] = val);
					case 0xFF05:
						return (mem[7][0x1F05] = val);
					case 0xFF06:
						return (mem[7][0x1F06] = val);
					case 0xFF07:
						return (mem[7][0x1F07] = val);
					case 0xFF16: // Channel 2 Sound Length/Wave Pattern Duty (W)
						snd.channel2.setSoundLength(val & ~(BIT6 | BIT7));
						snd.channel2.setWavePatternDuty((val & (BIT6 | BIT7) >> 6));
						return (mem[7][0x1F16] = (val & (BIT6 | BIT7)));
					case 0xFF17: // Channel 2 Volume Envelope (R/W)
						snd.channel2.setVolumeEnvelope(
						((val & (BIT7|BIT6|BIT5|BIT4) ) >> 4),(val & BIT3),(val & (BIT2|BIT1|BIT0)));
						return val;
					case 0xFF18: // Channel 2 Frequency Lo (W)
						snd.channel2.setFrequencyLo(val);
						return val;
					case 0xFF19: // Channel 2 Frequency Hi (R/W)
						snd.channel2.setSoundLength(val & BIT7);
						snd.channel2.setCounter(val & BIT6);
						snd.channel2.setFrequencyHi(val & (BIT2|BIT1|BIT0));
						if((val & BIT7) == -1)
							snd.channel2.setSoundLength(-1);
						
						return (mem[7][0x1F19] = (val & BIT6));
					
					case 0xFF0F:
						return (mem[7][0x1F0F] = val);
					case 0xFF40:
						return (mem[7][0x1F40] = val);
					case 0xFF41:
						return (mem[7][0x1F41] = ((val & 0xF8) | (mem[7][0x1F41] & 0x07)));
					case 0xFF42:
						return (mem[7][0x1F42] = val);
					case 0xFF43:
						return (mem[7][0x1F43] = val);
					case 0xFF44:
						return mem[7][0x1F44]; // read only
					case 0xFF45:
						return (mem[7][0x1F45] = val);
					case 0xFF46:
						{int start = val << 8;
						for (int i = 0; i < 0xA0; i++)
							mem[7][0x1E00 | i] = readMem(mem, start | i);
						return (mem[7][0x1F46] = val);}
					case 0xFF47:
						if (mem[7][0x1F47] == val)
							return val;
						//System.out.println("colors changed");
						colorsChanged = true;
						colorBG[0] = color[val & (BIT1 | BIT0)];
						colorBG[1] = color[(val & (BIT3 | BIT2)) >> 2];
						colorBG[2] = color[(val & (BIT5 | BIT4)) >> 4];
						colorBG[3] = color[val >> 6];
						return (mem[7][0x1F47] = val);
					case 0xFF48:
						colorSP0[1] = color[(val & (BIT3 | BIT2)) >> 2];
						colorSP0[2] = color[(val & (BIT5 | BIT4)) >> 4];
						colorSP0[3] = color[val >> 6];
						return (mem[7][0x1F48] = val);
					case 0xFF49:
						colorSP1[1] = color[(val & (BIT3 | BIT2)) >> 2];
						colorSP1[2] = color[(val & (BIT5 | BIT4)) >> 4];
						colorSP1[3] = color[val >> 6];
						return (mem[7][0x1F49] = val);
					case 0xFF4A:
						return (mem[7][0x1F4A] = val);
					case 0xFF4B:
						return (mem[7][0x1F4B] = val);
					case 0xFFFF:
						return (mem[7][0x1FFF] = val);
					
					default:
						return (mem[7][index & 0x1FFF] = val);
				}
			
			default:
				throw new AssertionError("Invalid memory address");
		}
	}
	
	public void run()
	{
		int PC=0x0100;
		int SP=0xFFFE;
		int AREG=0x01;
		int FREG=0xB0;
		int BREG=0x00;
		int CREG=0x13;
		int DREG=0x00;
		int EREG=0xD8;
		int HREG=0x01;
		int LREG=0x4D;
		boolean IME = true;
		
		final int[][] mem = new int[8][0x2000];
		
		mem[0] = rom.getDefaultROM(0);
		mem[1] = rom.getDefaultROM(1);
		mem[2] = rom.getROM(1, 0);
		mem[3] = rom.getROM(1, 1);
		mem[4] = new int[0x2000];
		mem[5] = rom.getRAM(0); // can be null
		if (mem[5] == null)
			mem[5] = new int[0x2000];
		mem[6] = new int[0x2000];
		mem[7] =new int[0x2000];
		
		final int[] FLAG_ADD = new int[257*256]; // max 255 + 1 (carry) = 256;
		final int[] FLAG_SUB = new int[257*256];
		final int[] FLAG_INC = new int[256];
		final int[] FLAG_DEC = new int[256];
		
		final int[] background = new int[256*256];
		final int[] screen = new int[GUI.screenWidth * GUI.screenHeight];
		final int[] prevTiles = new int[32*32];
		int windowOffset = 0;
		int prevTileMap = -1;
		colorsChanged = true;
		
		int val;
		int memval;
		int index;
		int frameCount = 0;
		int numCycles = 0;
		int scanline = 0;
		int nextHBlank = CYCLES_PER_LINE;
		//int nextVBlank = CYCLES_PER_LINE*144;
		int[] myColor;
		final int[] VRAM = mem[4];
		final int[] HRAM = mem[7];
		
		genFlagTable(FLAG_ADD, FLAG_SUB, FLAG_INC, FLAG_DEC);
		
		/*for (int i = 0; i < 16; i++)
		{
			dirtyTiles1[i] = 0x0000FFFF;
			dirtyTiles2[i] = 0x0000FFFF;
		}*/
		
		/*for (int q = 0; q < 256; q++)
		{
			System.out.println(q + ": " + Integer.toBinaryString(FLAG_DEC[q]));
		}*/
		
		long startT = System.nanoTime();
		long prevFrame = System.nanoTime();
		
		for(;;) // loop until thread stops
		{
			// Check if should perform actions
			synchronized (this) {
		    	while (pleaseWait) {
			    	try {
			        	wait();
			        } catch (Exception e) {/*maybe print something here*/}
			    }
		    	if(halt){
		    		return;
		    	}
			}
			
			// Start drawing background
			if ((HRAM[0x1F40] & BIT0) != 0)
			{
				final boolean redraw;
				if (colorsChanged || prevTileMap != (HRAM[0x1F40] & BIT4))
				{
					prevTileMap = (HRAM[0x1F40] & BIT4);
					redraw = true;
					//for (int i = 0; i < 1024; i++)
					//	prevTiles[i] = -1;
				}
				else
					redraw = false;
		
				myColor = colorBG;
			
				for (int upperByte = 0; upperByte < 0x10000; upperByte += 0x800)
				{
					for (int xPix = 0; xPix < 0x100; xPix+=8)
					{
						int tileNum = VRAM[0x1800 + ((HRAM[0x1F40] & BIT3) << 7) + ((upperByte >> 6) | (xPix >> 3))];
						
						int tileIndex;
						if ((HRAM[0x1F40] & BIT4) != 0)
						{
							if (!redraw && (prevTiles[(upperByte >> 6) | (xPix >> 3)] == tileNum) && ((dirtyTiles1[tileNum >> 4] & (1 << (tileNum & 0x0F))) == 0))
								continue;
							//if ()
							//	continue;
							tileIndex = tileNum << 4; // tileNum * 16
						}
						else
						{
							/*if (tileNum >= 128)
								tileNum -= 128;
							else
								tileNum += 128;*/
							tileNum = ((byte)(tileNum)) + 128;
							if (!redraw && (prevTiles[(upperByte >> 6) | (xPix >> 3)] == tileNum) && ((dirtyTiles2[tileNum >> 4] & (1 << (tileNum & 0x0F))) == 0))
								continue;
							//if ((dirtyTiles2[tileNum >> 4] & (1 << (tileNum & 0x0F))) == 0)
							//	continue;
							tileIndex = 0x800 + (tileNum << 4);
						}
						
						prevTiles[(upperByte >> 6) | (xPix >> 3)] = tileNum;
						
						int byteIndex = upperByte | xPix;
						int bitSet = BIT7;
						int byte0 = VRAM[tileIndex];
						int byte1 = VRAM[tileIndex+1];
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 7) | ((byte1 & bitSet) >> 6)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 6) | ((byte1 & bitSet) >> 5)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 5) | ((byte1 & bitSet) >> 4)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 4) | ((byte1 & bitSet) >> 3)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 3) | ((byte1 & bitSet) >> 2)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 2) | ((byte1 & bitSet) >> 1)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 1) | (byte1 & bitSet)];
						bitSet >>= 1;
						
						background[byteIndex] = myColor[(byte0 & bitSet) | ((byte1 & bitSet) << 1)];
						
						byteIndex += 249;
						bitSet = BIT7;
						byte0 = VRAM[tileIndex+2];
						byte1 = VRAM[tileIndex+3];
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 7) | ((byte1 & bitSet) >> 6)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 6) | ((byte1 & bitSet) >> 5)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 5) | ((byte1 & bitSet) >> 4)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 4) | ((byte1 & bitSet) >> 3)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 3) | ((byte1 & bitSet) >> 2)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 2) | ((byte1 & bitSet) >> 1)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 1) | (byte1 & bitSet)];
						bitSet >>= 1;
						
						background[byteIndex] = myColor[(byte0 & bitSet) | ((byte1 & bitSet) << 1)];
						
						byteIndex += 249;
						bitSet = BIT7;
						byte0 = VRAM[tileIndex+4];
						byte1 = VRAM[tileIndex+5];
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 7) | ((byte1 & bitSet) >> 6)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 6) | ((byte1 & bitSet) >> 5)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 5) | ((byte1 & bitSet) >> 4)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 4) | ((byte1 & bitSet) >> 3)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 3) | ((byte1 & bitSet) >> 2)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 2) | ((byte1 & bitSet) >> 1)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 1) | (byte1 & bitSet)];
						bitSet >>= 1;
						
						background[byteIndex] = myColor[(byte0 & bitSet) | ((byte1 & bitSet) << 1)];
						
						byteIndex += 249;
						bitSet = BIT7;
						byte0 = VRAM[tileIndex+6];
						byte1 = VRAM[tileIndex+7];
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 7) | ((byte1 & bitSet) >> 6)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 6) | ((byte1 & bitSet) >> 5)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 5) | ((byte1 & bitSet) >> 4)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 4) | ((byte1 & bitSet) >> 3)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 3) | ((byte1 & bitSet) >> 2)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 2) | ((byte1 & bitSet) >> 1)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 1) | (byte1 & bitSet)];
						bitSet >>= 1;
						
						background[byteIndex] = myColor[(byte0 & bitSet) | ((byte1 & bitSet) << 1)];
						
						byteIndex += 249;
						bitSet = BIT7;
						byte0 = VRAM[tileIndex+8];
						byte1 = VRAM[tileIndex+9];
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 7) | ((byte1 & bitSet) >> 6)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 6) | ((byte1 & bitSet) >> 5)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 5) | ((byte1 & bitSet) >> 4)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 4) | ((byte1 & bitSet) >> 3)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 3) | ((byte1 & bitSet) >> 2)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 2) | ((byte1 & bitSet) >> 1)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 1) | (byte1 & bitSet)];
						bitSet >>= 1;
						
						background[byteIndex] = myColor[(byte0 & bitSet) | ((byte1 & bitSet) << 1)];
						
						byteIndex += 249;
						bitSet = BIT7;
						byte0 = VRAM[tileIndex+10];
						byte1 = VRAM[tileIndex+11];
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 7) | ((byte1 & bitSet) >> 6)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 6) | ((byte1 & bitSet) >> 5)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 5) | ((byte1 & bitSet) >> 4)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 4) | ((byte1 & bitSet) >> 3)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 3) | ((byte1 & bitSet) >> 2)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 2) | ((byte1 & bitSet) >> 1)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 1) | (byte1 & bitSet)];
						bitSet >>= 1;
						
						background[byteIndex] = myColor[(byte0 & bitSet) | ((byte1 & bitSet) << 1)];
						
						byteIndex += 249;
						bitSet = BIT7;
						byte0 = VRAM[tileIndex+12];
						byte1 = VRAM[tileIndex+13];
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 7) | ((byte1 & bitSet) >> 6)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 6) | ((byte1 & bitSet) >> 5)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 5) | ((byte1 & bitSet) >> 4)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 4) | ((byte1 & bitSet) >> 3)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 3) | ((byte1 & bitSet) >> 2)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 2) | ((byte1 & bitSet) >> 1)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 1) | (byte1 & bitSet)];
						bitSet >>= 1;
						
						background[byteIndex] = myColor[(byte0 & bitSet) | ((byte1 & bitSet) << 1)];
						
						byteIndex += 249;
						bitSet = BIT7;
						byte0 = VRAM[tileIndex+14];
						byte1 = VRAM[tileIndex+15];
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 7) | ((byte1 & bitSet) >> 6)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 6) | ((byte1 & bitSet) >> 5)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 5) | ((byte1 & bitSet) >> 4)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 4) | ((byte1 & bitSet) >> 3)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 3) | ((byte1 & bitSet) >> 2)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 2) | ((byte1 & bitSet) >> 1)];
						bitSet >>= 1;
						
						background[byteIndex++] = myColor[((byte0 & bitSet) >> 1) | (byte1 & bitSet)];
						bitSet >>= 1;
						
						background[byteIndex] = myColor[(byte0 & bitSet) | ((byte1 & bitSet) << 1)];
					}
				}
				
				/* TEMPORARY CODE
				int SCY = HRAM[0x1F42];
				int SCX = HRAM[0x1F43];
				
				for (int yPix = 0; yPix < GUI.screenHeight; yPix++)
				{
					int upper = ((yPix+SCY) & 0xFF) << 8;
					int mult = yPix*GUI.screenWidth;
					
					for (int xPix = 0; xPix < GUI.screenWidth; xPix++)
					{
						// Draw 16 pixels						
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
						xPix++;
						screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
					}
				}
				END TEMPORARY CODE */
				
				colorsChanged = false;
				for (int i = 0; i < 16; i++)
				{
					dirtyTiles1[i] = 0;
					dirtyTiles2[i] = 0;
				}
			}
			// Done drawing background
			
	 		while (scanline <= 153) // from 144 to 153 is v-blank period
			{
				/*if (!visited[PC])
				{
					//System.out.println(Integer.toBinaryString(P1));
			   		System.out.printf("A: %02X, B: %02X, C: %02X, D: %02X, E: %02X, F: %02X, H: %02X, L: %02X, SP: %04X\n", AREG, BREG, CREG, DREG, EREG, FREG, HREG, LREG, SP);
					System.out.printf("Instruction %02X at %04X\n", readMem(mem, PC), PC);
			   		visited[PC] = true;
				}*/
			
				//if (PC > 0x219)
				/*{
				if(readMem(mem, PC)!=0xCB)
					System.out.format("Operation 0x%02X at 0x%04X\n",readMem(mem, PC),PC);
				else
					System.out.format("Operation 0x%02X 0x%02X at 0x%04X\n",readMem(mem, PC),readMem(mem, PC+1),PC);
				}*/
				//if (numCycles > 2000) return;
				
				switch(readMem(mem, PC++))
				{
					case 0x00: //NOP
						numCycles++;
					break;
					
					case 0x01: //LD BC,nn
						numCycles+=3;
						CREG = readMem(mem, PC++);
						BREG = readMem(mem, PC++);
					break;
					
					case 0x02: //LD (BC),A
						numCycles+=2;
						writeMem(mem, (BREG << 8) | CREG, AREG);
					break;
					
					case 0x03: // INC BC
						numCycles+=2;
						CREG++;
						if (CREG > 0xFF)
						{
							CREG = 0;
							BREG = (BREG+1) & 0xFF;
						}
					break;
					
					case 0x04: // INC B
						numCycles++;
						FREG = FLAG_INC[BREG] | (FREG & CARRY);
						BREG = (BREG+1) & 0xFF;
					break;
					
					case 0x05: // DEC B
						numCycles++;
						FREG = FLAG_DEC[BREG] | (FREG & CARRY);
						BREG = (BREG-1) & 0xFF;
					break;
					
					case 0x06: //LD B,n
						numCycles+=2;
						BREG = readMem(mem, PC++);
					break;
					
					case 0x07: // RLCA
						numCycles++;
						FREG = (AREG & BIT7) >> 3;
						AREG = ((AREG << 1) | (AREG >> 7)) & 0xFF;
					break;
					
					case 0x08: //LD (nn),SP
						numCycles+=5;
						index = readMem(mem, PC++) | (readMem(mem, PC++) << 8);
						writeMem(mem, index, SP & 0x00FF);
						writeMem(mem, index+1, SP >> 8);
					break;
					
					case 0x09: // ADD HL,BC
						numCycles+=2;
						FREG &= ZERO;
						LREG += CREG;
						HREG += BREG + (LREG >> 8);
						LREG &= 0xFF;
						if (HREG > 0xFF)
						{
							HREG &= 0xFF;
							FREG |= CARRY;
						}
					break;
					
					case 0x0A: //LD A,(BC)
						numCycles+=2;
						AREG = readMem(mem,  (BREG << 8) | CREG );
					break;
					
		 			case 0x0B: // DEC BC
						numCycles+=2;
						CREG--;
						if (CREG < 0)
						{
							CREG = 0xFF;
							BREG = (BREG-1) & 0xFF;
						}
					break;
					
					case 0x0C: // INC C
						numCycles++;
						FREG = FLAG_INC[CREG] | (FREG & CARRY);
						CREG = (CREG+1) & 0xFF;
					break;
					
					case 0x0D: // DEC C
						numCycles++;
						FREG = FLAG_DEC[CREG] | (FREG & CARRY);
						CREG = (CREG-1) & 0xFF;
					break;
						
					case 0x0E: //LD C,n
						numCycles+=2;
						CREG=readMem(mem, PC++);
					break;
					
					case 0x0F: // RRCA
						numCycles++;
						FREG = (AREG & BIT0) << 4;
						AREG = ((AREG >> 1) | (AREG << 7)) & 0xFF;
					break;
					
					case 0x10: // STOP
						numCycles++;
						//PC++? (not all assemblers insert 0x00)
						// *TODO: give control to key listener*
					break;
					
					case 0x11: //LD DE,nn
						numCycles+=3;
						EREG = readMem(mem, PC++);
						DREG = readMem(mem, PC++);
					break;
					
					case 0x12: //LD (DE),A
						numCycles+=2;
						writeMem(mem,  (DREG << 8) | EREG, AREG);
					break;
					
					case 0x13: // INC DE
						numCycles+=2;
						EREG++;
						if (EREG > 0xFF)
						{
							EREG = 0;
							DREG = (DREG+1) & 0xFF;
						}
					break;
					
					case 0x14: // INC D
						numCycles++;
						FREG = FLAG_INC[DREG] | (FREG & CARRY);
						DREG = (DREG+1) & 0xFF;
					break;
					
					case 0x15: // DEC D
						numCycles++;
						FREG = FLAG_DEC[DREG] | (FREG & CARRY);
						DREG = (DREG-1) & 0xFF;
					break;
					
					case 0x16: //LD D,n
						numCycles+=2;
						DREG=readMem(mem, PC++);
					break;
					
					case 0x17: // RLA
						numCycles++;
						val = (FREG & CARRY) >> 4;
						FREG = (AREG & BIT7) >> 3;
						AREG = ((AREG << 1) | val) & 0xFF;
					break;
					
					case 0x18: // JR n
						numCycles+=3;
						PC += (byte)readMem(mem, PC) + 1; // signed immediate
					break;
					
					case 0x19: // ADD HL,DE
						numCycles+=2;
						FREG &= ZERO;
						LREG += EREG;
						HREG += DREG + (LREG >> 8);
						LREG &= 0xFF;
						if (HREG > 0xFF)
						{
							HREG &= 0xFF;
							FREG |= CARRY;
						}
					break;
						
					case 0x1A: //LD A,(DE)
						numCycles+=2;
						AREG = readMem(mem,  ( DREG << 8 ) | EREG );
					break;
					
					case 0x1B: // DEC DE
						numCycles+=2;
						EREG--;
						if (EREG < 0)
						{
							EREG = 0xFF;
							DREG = (DREG-1) & 0xFF;
						}
					break;
					
					case 0x1C: // INC E
						numCycles++;
						FREG = FLAG_INC[EREG] | (FREG & CARRY);
						EREG = (EREG+1) & 0xFF;
					break;
					
					case 0x1D: // DEC E
						numCycles++;
						FREG = FLAG_DEC[EREG] | (FREG & CARRY);
						EREG = (EREG-1) & 0xFF;
					break;
				
					case 0x1E: //LD E,n
						numCycles+=2;
						EREG=readMem(mem, PC++);
					break;
					
					case 0x1F: // RRA
						numCycles++;
						val = (FREG & CARRY) << 3;
						FREG = (AREG & BIT0) << 4;
						AREG = (AREG >> 1) | val;
					break;
					
					case 0x20: // JR NZ,n
						if ((FREG & ZERO) == 0)
						{
							numCycles+=3;
							PC += (byte)readMem(mem, PC) + 1; // signed immediate
						}
						else
						{
							numCycles+=2;
							PC++;
						}
					break;
					
					case 0x21: //LD HL,nn
						numCycles+=3;
						LREG = readMem(mem, PC++);
						HREG = readMem(mem, PC++);
					break;
					
					case 0x22: //LDI (HL),A
						numCycles+=2;
						index = (HREG << 8) | LREG;
						writeMem(mem, index, AREG);
						index = (index+1) & 0xFFFF;
						HREG = index >> 8;
						LREG = index & 0x00FF;
					break;
					
					case 0x23: // INC HL
						numCycles+=2;
						LREG++;
						if (LREG > 0xFF)
						{
							LREG = 0;
							HREG = (HREG+1) & 0xFF;
						}
					break;
					
					case 0x24: // INC H
						numCycles++;
						FREG = FLAG_INC[HREG] | (FREG & CARRY);
						HREG = (HREG+1) & 0xFF;
					break;
					
					case 0x25: // DEC H
						numCycles++;
						FREG = FLAG_DEC[HREG] | (FREG & CARRY);
						HREG = (HREG-1) & 0xFF;
					break;
					
					case 0x26: //LD H,n
						numCycles+=2;
						HREG=readMem(mem, PC++);
					break;
					
					case 0x27: // DAA
						numCycles++;
						if ((FREG & SUBTRACT) != 0)
						{
							if ((AREG & 0x0F) > 0x09 || (FREG & HALF_CARRY) != 0)
								AREG -= 0x06;
							if ((AREG & 0xF0) > 0x90 || (FREG & CARRY) != 0)
							{
								AREG -= 0x60;
								FREG |= CARRY;
							}
							else
								FREG &= ~CARRY;
						}
						else
						{
							if ((AREG & 0x0F) > 0x09 || (FREG & HALF_CARRY) != 0)
								AREG += 0x06;
							if ((AREG & 0xF0) > 0x90 || (FREG & CARRY) != 0)
							{
								AREG += 0x60;
								FREG |= CARRY;
							}
							else
								FREG &= ~CARRY;
						}
						FREG &= ~HALF_CARRY;
						AREG &= 0xFF;
						if (AREG == 0)
							FREG |= ZERO;
						else
							FREG &= ~ZERO;
					break;
					
					case 0x28: // JR Z,n
						if ((FREG & ZERO) != 0)
						{
							numCycles+=3;
							PC += (byte)readMem(mem, PC) + 1; // signed immediate
						}
						else
						{
							numCycles+=2;
							PC++;
						}
					break;
					
					case 0x29: // ADD HL,HL
						numCycles+=2;
						FREG &= ZERO;
						LREG += LREG;
						HREG += HREG + (LREG >> 8);
						LREG &= 0xFF;
						if (HREG > 0xFF)
						{
							HREG &= 0xFF;
							FREG |= CARRY;
						}
					break;
					
					case 0x2A: //LDI A,(HL)
						numCycles+=2;
						index = (HREG << 8) | LREG;
						AREG = readMem(mem, index);
						index = (index+1) & 0xFFFF;
						HREG = index >> 8;
						LREG = index & 0x00FF;
					break;
					
					case 0x2B: // DEC HL
						numCycles+=2;
						LREG--;
						if (LREG < 0)
						{
							LREG = 0xFF;
							HREG = (HREG-1) & 0xFF;
						}
					break;
					
					case 0x2C: // INC L
						numCycles++;
						FREG = FLAG_INC[LREG] | (FREG & CARRY);
						LREG = (LREG+1) & 0xFF;
					break;
					
					case 0x2D: // DEC L
						numCycles++;
						FREG = FLAG_DEC[LREG] | (FREG & CARRY);
						LREG = (LREG-1) & 0xFF;
					break;
					
					case 0x2E: //LD L,n
						numCycles+=2;
						LREG=readMem(mem, PC++);
					break;
					
					case 0x2F: // CPL
						numCycles++;
						FREG |= (SUBTRACT | HALF_CARRY);
						AREG ^= 0xFF; // faster than (~AREG) & 0xFF
					break;
					
					case 0x30: // JR NC,n
						if ((FREG & CARRY) == 0)
						{
							numCycles+=3;
							PC += (byte)readMem(mem, PC) + 1; // signed immediate
						}
						else
						{
							numCycles+=2;
							PC++;
						}
					break;
					
					case 0x31: //LD SP,nn
						numCycles+=3;
						SP = readMem(mem, PC++) | (readMem(mem, PC++) << 8);
					break;
					
					case 0x32: //LDD (HL),A
						numCycles+=2;
						index = (HREG << 8) | LREG;
						writeMem(mem, index, AREG);
						index = (index-1) & 0xFFFF;
						HREG = index >> 8;
						LREG = index & 0x00FF;
					break;
					
					case 0x33: // INC SP
						numCycles+=2;
						SP = (SP+1) & 0xFFFF;
					break;
					
					case 0x34: // INC (HL)
						numCycles+=3;
						index = (HREG << 8) | LREG;
						memval = readMem(mem, index);
						FREG = FLAG_INC[memval] | (FREG & CARRY);
						writeMem(mem, index, (memval + 1) & 0xFF);
					break;
					
					case 0x35: // DEC (HL)
						numCycles+=3;
						index = (HREG << 8) | LREG;
						memval = readMem(mem, index);
						FREG = FLAG_DEC[memval] | (FREG & CARRY);
						writeMem(mem, index, (memval - 1) & 0xFF);
					break;
					
					case 0x36: //LD (HL),n
						numCycles+=3;
						writeMem(mem,  (HREG << 8) | LREG, readMem(mem, PC++));
					break;
					
					case 0x37: // SCF
						numCycles++;
						FREG &= ~(SUBTRACT | HALF_CARRY);
						FREG |= CARRY;
					break;
					
					case 0x38: // JR C,n
						if ((FREG & CARRY) != 0)
						{
							numCycles+=3;
							PC += (byte)readMem(mem, PC) + 1; // signed immediate
						}
						else
						{
							numCycles+=2;
							PC++;
						}
					break;
					
					case 0x39: // ADD HL,SP
						numCycles+=2;
						FREG &= ZERO;
						val = ((HREG << 8) | LREG) + SP;
						if (val > 0xFFFF)
						{
							val &= 0xFFFF;
							FREG |= CARRY;
						}
						HREG = val >> 8;
						LREG = val & 0xFF;
					break;
						
					case 0x3A: //LDD A,(HL)
						numCycles+=2;
						index = (HREG << 8) | LREG;
						AREG = readMem(mem, index);
						index = (index-1) & 0xFFFF;
						HREG = index >> 8;
						LREG = index & 0x00FF;
					break;
					
					case 0x3B: // DEC SP
						numCycles+=2;
						SP = (SP-1) & 0xFFFF;
					break;
					
					case 0x3C: // INC A
						numCycles++;
						FREG = FLAG_INC[AREG] | (FREG & CARRY);
						AREG = (AREG+1) & 0xFF;
					break;
					
					case 0x3D: // DEC A
						numCycles++;
						FREG = FLAG_DEC[AREG] | (FREG & CARRY);
						AREG = (AREG-1) & 0xFF;
					break;
					
					case 0x3E: //LD A,n
						numCycles+=2;
						AREG = readMem(mem, PC++);
					break;
					
					case 0x3F: // CCF
						numCycles++;
						FREG &= ~(SUBTRACT | HALF_CARRY);
						FREG ^= CARRY;
					break;
								
					case 0x40: //LD B,B
						numCycles++;
					break;
					
					case 0x41: //LD B,C
						numCycles++;
						BREG=CREG;
					break;
					
					case 0x42: //LD B,D
						numCycles++;
						BREG=DREG;
					break;
					
					case 0x43: //LD B,E
						numCycles++;
						BREG=EREG;
					break;
					
					case 0x44: //LD B,H
						numCycles++;
						BREG=HREG;
					break;
					
					case 0x45: //LD B,L
						numCycles++;
						BREG=LREG;
					break;
					
					case 0x46: //LD B,(HL)
						numCycles+=2;
						BREG=readMem(mem,  ( HREG << 8) | LREG );
					break;
					
					case 0x47: //LD B,A
						numCycles++;
						BREG=AREG;
					break;
					
					case 0x48: //LD C,B
						numCycles++;
						CREG=BREG;
					break;
					
					case 0x49: //LD C,C
						numCycles++;
					break;
					
					case 0x4A: //LD C,D
						numCycles++;
						CREG=DREG;
					break;
					
					case 0x4B: //LD C,E
						numCycles++;
						CREG=EREG;
					break;
					
					case 0x4C: //LD C,H
						numCycles++;
						CREG=HREG;
					break;
					
					case 0x4D: //LD C,L
						numCycles++;
						CREG=LREG;
					break;
					
					case 0x4E: //LD C,(HL)
						numCycles+=2;
						CREG=readMem(mem,  ( HREG << 8) | LREG );
					break;
					
					case 0x4F: //LD C,A
						numCycles++;
						CREG=AREG;
					break;
					
					case 0x50: //LD D,B
						numCycles++;
						DREG=BREG;
					break;
					
					case 0x51: //LD D,C
						numCycles++;
						DREG=CREG;
					break;
						
					case 0x52: //LD D,D
						numCycles++;
					break;
					
					case 0x53: //LD D,E
						numCycles++;
						DREG=EREG;
					break;
					
					case 0x54: //LD D,H
						numCycles++;
						DREG=HREG;
					break;
					
					case 0x55: //LD D,L
						numCycles++;
						DREG=LREG;
					break;
					
					case 0x56: //LD D,(HL)
						numCycles+=2;
						DREG=readMem(mem,  ( HREG << 8) | LREG );
					break;
					
					case 0x57: //LD D,A
						numCycles++;
						DREG=AREG;
					break;
					
					case 0x58: //LD E,B
						numCycles++;
						EREG=BREG;
					break;
					
					case 0x59: //LD E,C
						numCycles++;
						EREG=CREG;
					break;
					
					case 0x5A: //LD E,D
						numCycles++;
						EREG=DREG;
					break;
					
					case 0x5B: //LD E,E
						numCycles++;
					break;
					
					case 0x5C: //LD E,H
						numCycles++;
						EREG=HREG;
					break;
					
					case 0x5D: //LD E,L
						numCycles++;
						EREG=LREG;
					break;
					
					case 0x5E: //LD E,(HL)
						numCycles+=2;
						EREG=readMem(mem,  (HREG << 8) | LREG );
					break;
					
					case 0x5F: //LD E,A
						numCycles++;
						EREG=AREG;
					break;
					
					case 0x60: //LD H,B
						numCycles++;
						HREG=BREG;
					break;
					
					case 0x61: //LD H,C
						numCycles++;
						HREG=CREG;
					break;
					
					case 0x62: //LD H,D
						numCycles++;
						HREG=DREG;
					break;
					
					case 0x63: //LD H,E
						numCycles++;
						HREG=EREG;
					break;
					
					case 0x64: //LD H,H
						numCycles++;
					break;
					
					case 0x65: //LD H,L
						numCycles++;
						HREG=LREG;
					break;
					
					case 0x66: //LD H,(HL)
						numCycles+=2;
						HREG=readMem(mem,  (HREG << 8 ) | LREG );
					break;
					
					case 0x67: //LD H,A
						numCycles++;
						HREG=AREG;
					break;
					
					case 0x68: //LD L,B
						numCycles++;
						LREG=BREG;
					break;
					
					case 0x69: //LD L,C
						numCycles++;
						LREG=CREG;
					break;
					
					case 0x6A: //LD L,D
						numCycles++;
						LREG=DREG;
					break;
					
					case 0x6B: //LD L,E
						numCycles++;
						LREG=EREG;
					break;
					
					case 0x6C: //LD L,H
						numCycles++;
						LREG=HREG;
					break;
						
					case 0x6D: //LD L,L
						numCycles++;
					break;
					
					case 0x6E: //LD L,(HL)
						numCycles+=2;
						LREG=readMem(mem,  (HREG << 8 ) | LREG );
					break;
					
					case 0x6F: //LD L,A
						numCycles++;
						LREG=AREG;
					break;
					
					case 0x70: //LD (HL),B
						numCycles+=2;
						writeMem(mem, (HREG << 8) | LREG, BREG);
					break;
					
					case 0x71: //LD (HL),C
						numCycles+=2;
						writeMem(mem, (HREG << 8) | LREG, CREG);
					break;
					
					case 0x72: //LD (HL),D
						numCycles+=2;
						writeMem(mem, (HREG << 8) | LREG, DREG);
					break;
					
					case 0x73: //LD (HL),E
						numCycles+=2;
						writeMem(mem, (HREG << 8) | LREG, EREG);
					break;
					
					case 0x74: //LD (HL),H
						numCycles+=2;
						writeMem(mem, (HREG << 8) | LREG, HREG);
					break;
					
					case 0x75: //LD (HL),L
						numCycles+=2;
						writeMem(mem, (HREG << 8) | LREG, LREG);
					break;
					
					case 0x76: // HALT
						if (IME)
							numCycles = nextHBlank;
						else
							numCycles++;
						//if (IME)
							//*give control to interrupt handler*
						//else
							//PC++; (skip next instruction in GB mode)
					break;
				
					case 0x77: // LD (HL),A
						numCycles+=2;
						writeMem(mem, (HREG << 8) | LREG, AREG);
					break;
					
					case 0x78: //LD A,B
						numCycles++;
						AREG=BREG;
					break;
				
					case 0x79: //LD A,C
						numCycles++;
						AREG=CREG;
					break;
				
					case 0x7A: //LD A,D
						numCycles++;
						AREG=DREG;
					break;
				
					case 0x7B: //LD A,E
						numCycles++;
						AREG=EREG;
					break;
				
					case 0x7C: //LD A,H
						numCycles++;
						AREG=HREG;
					break;
				
					case 0x7D: //LD A,L
						numCycles++;
						AREG=LREG;
					break;
				
					case 0x7E: //LD A,(HL)
						numCycles+=2;
						AREG=readMem(mem, (HREG << 8) | LREG);
					break;
					
					case 0x7F: //LD A,A
						numCycles++;
					break;
					
					case 0x80: //ADD A,B
						numCycles++;
						FREG = FLAG_ADD[(BREG << 8) | AREG];
						AREG = (AREG+BREG) & 0xFF;
					break;
					
					case 0x81: //ADD A,C
						numCycles++;
						FREG = FLAG_ADD[(CREG << 8 ) | AREG];
						AREG = (AREG+CREG) & 0xFF;
					break;
					
					case 0x82: //ADD A,D
						numCycles++;
						FREG = FLAG_ADD[(DREG << 8) | AREG];
						AREG = (AREG+DREG) & 0xFF;
					break;
					
					case 0x83: //ADD A,E
						numCycles++;
						FREG = FLAG_ADD[(EREG << 8) | AREG];
						AREG = (AREG+EREG) & 0xFF;
					break;
					
					case 0x84: //ADD A,H
						numCycles++;
						FREG = FLAG_ADD[(HREG << 8) | AREG];
						AREG = (AREG+HREG) & 0xFF;
					break;
					
					case 0x85: //ADD A,L
						numCycles++;
						FREG = FLAG_ADD[(LREG << 8) | AREG];
						AREG = (AREG+LREG) & 0xFF;
					break;
					
					case 0x86: // ADD A,(HL)
						numCycles+=2;
						memval = readMem(mem, (HREG << 8) | LREG);
						FREG = FLAG_ADD[(memval << 8) | AREG];
						AREG = (AREG+memval) & 0xFF;
					break;
					
					case 0x87: //ADD A,A
						numCycles++;
						FREG = FLAG_ADD[(AREG << 8) | AREG];
						AREG = (AREG+AREG) & 0xFF;
					break;
					
					case 0x88: //ADC A,B
						numCycles++;
						val = BREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[(val << 8) | AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x89: //ADC A,C
						numCycles++;
						val = CREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[(val << 8) | AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8A: //ADC A,D
						numCycles++;
						val = DREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[(val << 8) | AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8B: //ADC A,E
						numCycles++;
						val = EREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[(val << 8) | AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8C: //ADC A,H
						numCycles++;
						val = HREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[(val << 8) | AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8D: //ADC A,L
						numCycles++;
						val = LREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[(val << 8) | AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8E: //ADC A,(HL)
						numCycles+=2;
						memval = readMem(mem, (HREG << 8) | LREG) + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[(memval << 8) | AREG];
						AREG = (AREG+memval) & 0xFF;
					break;
					
					case 0x8F: //ADC A,A
						numCycles++;
						val = AREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[(val << 8) | AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x90: // SUB A,B
						numCycles++;
						FREG = FLAG_SUB[(BREG << 8) | AREG];
						AREG = (AREG-BREG) &  0xFF;
					break;
					
					case 0x91: // SUB A,C
						numCycles++;
						FREG = FLAG_SUB[(CREG << 8) | AREG];
						AREG = (AREG-CREG) &  0xFF;
					break;
					
					case 0x92: // SUB A,D
						numCycles++;
						FREG = FLAG_SUB[(DREG << 8) | AREG];
						AREG = (AREG-DREG) &  0xFF;
					break;
					
					case 0x93: // SUB A,E
						numCycles++;
						FREG = FLAG_SUB[(EREG << 8) | AREG];
						AREG = (AREG-EREG) &  0xFF;
					break;
					
					case 0x94: // SUB A,H
						numCycles++;
						FREG = FLAG_SUB[(HREG << 8) | AREG];
						AREG = (AREG-HREG) &  0xFF;
					break;
					
					case 0x95: // SUB A,L
						numCycles++;
						FREG = FLAG_SUB[(LREG << 8) | AREG];
						AREG = (AREG-LREG) &  0xFF;
					break;
					
					case 0x96: // SUB A,(HL)
						numCycles+=2;
						memval = readMem(mem, (HREG << 8) | LREG);
						FREG = FLAG_SUB[(memval << 8) | AREG];
						AREG = (AREG-memval) &  0xFF;
					break;
					
					case 0x97: // SUB A,A
						numCycles++;
						FREG = SUBTRACT | ZERO;
						AREG = 0;
					break;
					
					case 0x98: // SBC A,B
						numCycles++;
						val = BREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[(val << 8) | AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x99: // SBC A,C
						numCycles++;
						val = CREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[(val << 8) | AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x9A: // SBC A,D
						numCycles++;
						val = DREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[(val << 8) | AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x9B: // SBC A,E
						numCycles++;
						val = EREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[(val << 8) | AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x9C: // SBC A,H
						numCycles++;
						val = HREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[(val << 8) | AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x9D: // SBC A,L
						numCycles++;
						val = LREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[(val << 8) | AREG];
						AREG = (AREG - val) & 0xFF;
						
					break;
					
					case 0x9E: // SBC A,(HL)
						numCycles+=2;
						memval = readMem(mem, (HREG << 8) | LREG) + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[(memval << 8) | AREG];
						AREG = (AREG - memval) & 0xFF;
					break;
					
					case 0x9F: // SBC A,A
						numCycles++;
						val = AREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[(val << 8) | AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0xA0: // AND B
						numCycles++;
						AREG &= BREG;
						FREG = HALF_CARRY | (((AREG-1) >> 24) & ZERO);
					break;
					
					case 0xA1: // AND C
						numCycles++;
						AREG &= CREG;
						FREG = HALF_CARRY | (((AREG-1) >> 24) & ZERO);
					break;
					
					case 0xA2: // AND D
						numCycles++;
						AREG &= DREG;
						FREG = HALF_CARRY | (((AREG-1) >> 24) & ZERO);
					break;
					
					case 0xA3: // AND E
						numCycles++;
						AREG &= EREG;
						FREG = HALF_CARRY | (((AREG-1) >> 24) & ZERO);
					break;
					
					case 0xA4: // AND H
						numCycles++;
						AREG &= HREG;
						FREG = HALF_CARRY | (((AREG-1) >> 24) & ZERO);
					break;
					
					case 0xA5: // AND L
						numCycles++;
						AREG &= LREG;
						FREG = HALF_CARRY | (((AREG-1) >> 24) & ZERO);
					break;
					
					case 0xA6: // AND (HL)
						numCycles+=2;
						AREG &= readMem(mem, (HREG << 8) | LREG);
						FREG = HALF_CARRY | (((AREG-1) >> 24) & ZERO);
					break;
					
					case 0xA7: // AND A
						// A&A = A, no change
						numCycles++;
						FREG = HALF_CARRY | (((AREG-1) >> 24) & ZERO);
					break;
					
					case 0xA8: // XOR B
						numCycles++;
						AREG ^= BREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xA9: // XOR C
						numCycles++;
						AREG ^= CREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xAA: // XOR D
						numCycles++;
						AREG ^= DREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xAB: // XOR E
						numCycles++;
						AREG ^= EREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xAC: // XOR H
						numCycles++;
						AREG ^= HREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xAD: // XOR L
						numCycles++;
						AREG ^= LREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xAE: // XOR (HL)
						numCycles+=2;
						AREG ^= readMem(mem, (HREG << 8) | LREG);
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xAF: // XOR A
						// A^A = 0
						numCycles++;
						AREG = 0;
						FREG = ZERO;
					break;
					
					case 0xB0: // OR B
						numCycles++;
						AREG |= BREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xB1: // OR C
						numCycles++;
						AREG |= CREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xB2: // OR D
						numCycles++;
						AREG |= DREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xB3: // OR E
						numCycles++;
						AREG |= EREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xB4: // OR H
						numCycles++;
						AREG |= HREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xB5: // OR L
						numCycles++;
						AREG |= LREG;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xB6: // OR (HL)
						numCycles+=2;
						AREG |= readMem(mem, (HREG << 8) | LREG);
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xB7: // OR A
						// A|A = A, no change
						numCycles++;
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xB8: // CP B
						numCycles++;
						FREG = FLAG_SUB[(BREG << 8) | AREG];
					break;
					
					case 0xB9: // CP C
						numCycles++;
						FREG = FLAG_SUB[(CREG << 8) | AREG];
					break;
					
					case 0xBA: // CP D
						numCycles++;
						FREG = FLAG_SUB[(DREG << 8) | AREG];
					break;
					
					case 0xBB: // CP E
						numCycles++;
						FREG = FLAG_SUB[(EREG << 8) | AREG];
					break;
					
					case 0xBC: // CP H
						numCycles++;
						FREG = FLAG_SUB[(HREG << 8) | AREG];
					break;
					
					case 0xBD: // CP L
						numCycles++;
						FREG = FLAG_SUB[(LREG << 8) | AREG];
					break;
					
					case 0xBE: // CP (HL)
						numCycles+=2;
						FREG = FLAG_SUB[(readMem(mem, (HREG << 8) | LREG) << 8) | AREG];
					break;
					
					case 0xBF: // CP A
						numCycles++;
						FREG = SUBTRACT | ZERO;
					break;
					
					case 0xC0: // RET NZ
						if ((FREG & ZERO) == 0)
						{
							numCycles+=5;
							PC = readMem(mem, SP++) | (readMem(mem, SP++) << 8);
						}
						else
							numCycles+=2;
					break;
					
					case 0xC1: // POP BC
						numCycles+=3;
						CREG = readMem(mem, SP++);
						BREG = readMem(mem, SP++);
					break;
					
					case 0xC2: // JP NZ,nn
						if ((FREG & ZERO) == 0)
						{
							numCycles+=4;
							PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xC3: // JP nn
						numCycles+=4;
						PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
					break;
					
					case 0xC4: // CALL NZ,nn
						if ((FREG & ZERO) == 0)
						{
							numCycles+=6;
							writeMem(mem, --SP, (PC+2) >> 8);
							writeMem(mem, --SP, (PC+2) & 0x00FF);
							PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xC5: //PUSH BC
						numCycles+=4;
						writeMem(mem, --SP, BREG);
						writeMem(mem, --SP, CREG);
					break;
					
					case 0xC6: // ADD A,n
						numCycles+=2;
						memval = readMem(mem, PC++);
						FREG = FLAG_ADD[(memval << 8) | AREG];
						AREG = (AREG+memval) & 0xFF;
					break;
					
					case 0xC7: // RST 00H
						numCycles += 4;
						writeMem(mem, --SP, PC >> 8);
						writeMem(mem, --SP, PC & 0x00FF);
						PC = 0x0000;
					break;
					
					case 0xC8: // RET Z
						if ((FREG & ZERO) != 0)
						{
							numCycles+=5;
							PC = readMem(mem, SP++) | (readMem(mem, SP++) << 8);
						}
						else
							numCycles+=2;
					break;
					
					case 0xC9: // RET
						numCycles+=4;
						PC = readMem(mem, SP++) | (readMem(mem, SP++) << 8);
					break;
					
					case 0xCA: // JP Z,nn
						if ((FREG & ZERO) != 0)
						{
							numCycles+=4;
							PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xCB: // 2-byte opcodes
						switch (readMem(mem, PC++))
						{
							case 0x00: // RLC B
								numCycles+=2;
								FREG = (BREG & BIT7) >> 3;
								BREG = ((BREG << 1) | (BREG >> 7)) & 0xFF;
								if (BREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x01: // RLC C
								numCycles+=2;
								FREG = (CREG & BIT7) >> 3;
								CREG = ((CREG << 1) | (CREG >> 7)) & 0xFF;
								if (CREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x02: // RLC D
								numCycles+=2;
								FREG = (DREG & BIT7) >> 3;
								DREG = ((DREG << 1) | (DREG >> 7)) & 0xFF;
								if (DREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x03: // RLC E
								numCycles+=2;
								FREG = (EREG & BIT7) >> 3;
								EREG = ((EREG << 1) | (EREG >> 7)) & 0xFF;
								if (EREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x04: // RLC H
								numCycles+=2;
								FREG = (HREG & BIT7) >> 3;
								HREG = ((HREG << 1) | (HREG >> 7)) & 0xFF;
								if (HREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x05: // RLC L
								numCycles+=2;
								FREG = (LREG & BIT7) >> 3;
								LREG = ((LREG << 1) | (LREG >> 7)) & 0xFF;
								if (LREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x06: // RLC (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								memval = readMem(mem, index);
								FREG = (memval & BIT7) >> 3;
								if (writeMem(mem, index, ((memval << 1) | (memval >> 7)) & 0xFF) == 0)
									FREG |= ZERO;
							break;
							
							case 0x07: // RLC A
								numCycles+=2;
								FREG = (AREG & BIT7) >> 3;
								AREG = ((AREG << 1) | (AREG >> 7)) & 0xFF;
								if (AREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x08: // RRC B
								numCycles+=2;
								FREG = (BREG & BIT0) << 4;
								BREG = ((BREG >> 1) | (BREG << 7)) & 0xFF;
								if (BREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x09: // RRC C
								numCycles+=2;
								FREG = (CREG & BIT0) << 4;
								CREG = ((CREG >> 1) | (CREG << 7)) & 0xFF;
								if (CREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x0A: // RRC D
								numCycles+=2;
								FREG = (DREG & BIT0) << 4;
								DREG = ((DREG >> 1) | (DREG << 7)) & 0xFF;
								if (DREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x0B: // RRC E
								numCycles+=2;
								FREG = (EREG & BIT0) << 4;
								EREG = ((EREG >> 1) | (EREG << 7)) & 0xFF;
								if (EREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x0C: // RRC H
								numCycles+=2;
								FREG = (HREG & BIT0) << 4;
								HREG = ((HREG >> 1) | (HREG << 7)) & 0xFF;
								if (HREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x0D: // RRC L
								numCycles+=2;
								FREG = (LREG & BIT0) << 4;
								LREG = ((LREG >> 1) | (LREG << 7)) & 0xFF;
								if (LREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x0E: // RRC (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								memval = readMem(mem, index);
								FREG = (memval & BIT0) << 4;
								if (writeMem(mem, index, ((memval >> 1) | (memval << 7)) & 0xFF) == 0)
									FREG |= ZERO;
							break;
							
							case 0x0F: // RRC A
								numCycles+=2;
								FREG = (AREG & BIT0) << 4;
								AREG = ((AREG >> 1) | (AREG << 7)) & 0xFF;
								if (AREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x10: // RL B
								numCycles+=2;
								val = (FREG & CARRY) >> 4;
								FREG = (BREG & BIT7) >> 3;
								BREG = ((BREG << 1) | val) & 0xFF;
								if (BREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x11: // RL C
								numCycles+=2;
								val = (FREG & CARRY) >> 4;
								FREG = (CREG & BIT7) >> 3;
								CREG = ((CREG << 1) | val) & 0xFF;
								if (CREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x12: // RL D
								numCycles+=2;
								val = (FREG & CARRY) >> 4;
								FREG = (DREG & BIT7) >> 3;
								DREG = ((DREG << 1) | val) & 0xFF;
								if (DREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x13: // RL E
								numCycles+=2;
								val = (FREG & CARRY) >> 4;
								FREG = (EREG & BIT7) >> 3;
								EREG = ((EREG << 1) | val) & 0xFF;
								if (EREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x14: // RL H
								numCycles+=2;
								val = (FREG & CARRY) >> 4;
								FREG = (HREG & BIT7) >> 3;
								HREG = ((HREG << 1) | val) & 0xFF;
								if (HREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x15: // RL L
								numCycles+=2;
								val = (FREG & CARRY) >> 4;
								FREG = (LREG & BIT7) >> 3;
								LREG = ((LREG << 1) | val) & 0xFF;
								if (LREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x16: // RL (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								val = (FREG & CARRY) >> 4;
								memval = readMem(mem, index);
								FREG = (memval & BIT7) >> 3;
								if (writeMem(mem, index, ((memval << 1) | val) & 0xFF) == 0)
									FREG |= ZERO;
							break;
							
							case 0x17: // RL A
								numCycles+=2;
								val = (FREG & CARRY) >> 4;
								FREG = (AREG & BIT7) >> 3;
								AREG = ((AREG << 1) | val) & 0xFF;
								if (AREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x18: // RR B
								numCycles+=2;
								val = (FREG & CARRY) << 3;
								FREG = (BREG & BIT0) << 4;
								BREG = (BREG >> 1) | val;
								if (BREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x19: // RR C
								numCycles+=2;
								val = (FREG & CARRY) << 3;
								FREG = (CREG & BIT0) << 4;
								CREG = (CREG >> 1) | val;
								if (CREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x1A: // RR D
								numCycles+=2;
								val = (FREG & CARRY) << 3;
								FREG = (DREG & BIT0) << 4;
								DREG = (DREG >> 1) | val;
								if (DREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x1B: // RR E
								numCycles+=2;
								val = (FREG & CARRY) << 3;
								FREG = (EREG & BIT0) << 4;
								EREG = (EREG >> 1) | val;
								if (EREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x1C: // RR H
								numCycles+=2;
								val = (FREG & CARRY) << 3;
								FREG = (HREG & BIT0) << 4;
								HREG = (HREG >> 1) | val;
								if (HREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x1D: // RR L
								numCycles+=2;
								val = (FREG & CARRY) << 3;
								FREG = (LREG & BIT0) << 4;
								LREG = (LREG >> 1) | val;
								if (LREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x1E: // RR (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								val = (FREG & CARRY) << 3;
								memval = readMem(mem, index);
								FREG = (memval & BIT0) << 4;
								if (writeMem(mem, index, (memval >> 1) | val) == 0)
									FREG |= ZERO;
							break;
							
							case 0x1F: // RR A
								numCycles+=2;
								val = (FREG & CARRY) << 3;
								FREG = (AREG & BIT0) << 4;
								AREG = (AREG >> 1) | val;
								if (AREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x20: // SLA B
								numCycles+=2;
								FREG = (BREG & BIT7) >> 3;
								BREG = (BREG << 1) & 0xFF;
								if (BREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x21: // SLA C
								numCycles+=2;
								FREG = (CREG & BIT7) >> 3;
								CREG = (CREG << 1) & 0xFF;
								if (CREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x22: // SLA D
								numCycles+=2;
								FREG = (DREG & BIT7) >> 3;
								DREG = (DREG << 1) & 0xFF;
								if (DREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x23: // SLA E
								numCycles+=2;
								FREG = (EREG & BIT7) >> 3;
								EREG = (EREG << 1) & 0xFF;
								if (EREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x24: // SLA H
								numCycles+=2;
								FREG = (HREG & BIT7) >> 3;
								HREG = (HREG << 1) & 0xFF;
								if (HREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x25: // SLA L
								numCycles+=2;
								FREG = (LREG & BIT7) >> 3;
								LREG = (LREG << 1) & 0xFF;
								if (LREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x26: // SLA (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								memval = readMem(mem, index);
								FREG = (memval & BIT7) >> 3;
								if (writeMem(mem, index, (memval << 1) & 0xFF) == 0)
									FREG |= 0;
							break;
							
							case 0x27: // SLA A
								numCycles+=2;
								FREG = (AREG & BIT7) >> 3;
								AREG = (AREG << 1) & 0xFF;
								if (AREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x28: // SRA B
								numCycles+=2;
								FREG = (BREG & BIT0) << 4;
								BREG = (BREG & BIT7) | (BREG >> 1);
								if (BREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x29: // SRA C
								numCycles+=2;
								FREG = (CREG & BIT0) << 4;
								CREG = (CREG & BIT7) | (CREG >> 1);
								if (CREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x2A: // SRA D
								numCycles+=2;
								FREG = (DREG & BIT0) << 4;
								DREG = (DREG & BIT7) | (DREG >> 1);
								if (DREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x2B: // SRA E
								numCycles+=2;
								FREG = (EREG & BIT0) << 4;
								EREG = (EREG & BIT7) | (EREG >> 1);
								if (EREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x2C: // SRA H
								numCycles+=2;
								FREG = (HREG & BIT0) << 4;
								HREG = (HREG & BIT7) | (HREG >> 1);
								if (HREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x2D: // SRA L
								numCycles+=2;
								FREG = (LREG & BIT0) << 4;
								LREG = (LREG & BIT7) | (LREG >> 1);
								if (LREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x2E: // SRA (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								memval = readMem(mem, index);
								FREG = (memval & BIT0) << 4;
								if (writeMem(mem, index, (memval & BIT7) | (memval >> 1)) == 0)
									FREG |= ZERO;
							break;
							
							case 0x2F: // SRA A
								numCycles+=2;
								FREG = (AREG & BIT0) << 4;
								AREG = (AREG & BIT7) | (AREG >> 1);
								if (AREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x30: // SWAP B
								numCycles+=2;
								val = BREG & 0x0F;
								BREG >>= 4;
								BREG |= (val << 4);
								FREG = ((BREG-1) >> 24) & ZERO;
							break;
							
							case 0x31: // SWAP C
								numCycles+=2;
								val = CREG & 0x0F;
								CREG >>= 4;
								CREG |= (val << 4);
								FREG = ((CREG-1) >> 24) & ZERO;
							break;
							
							case 0x32: // SWAP D
								numCycles+=2;
								val = DREG & 0x0F;
								DREG >>= 4;
								DREG |= (val << 4);
								FREG = ((DREG-1) >> 24) & ZERO;
							break;
							
							case 0x33: // SWAP E
								numCycles+=2;
								val = EREG & 0x0F;
								EREG >>= 4;
								EREG |= (val << 4);
								FREG = ((EREG-1) >> 24) & ZERO;
							break;
							
							case 0x34: // SWAP H
								numCycles+=2;
								val = HREG & 0x0F;
								HREG >>= 4;
								HREG |= (val << 4);
								FREG = ((HREG-1) >> 24) & ZERO;
							break;
							
							case 0x35: // SWAP L
								numCycles+=2;
								val = LREG & 0x0F;
								LREG >>= 4;
								LREG |= (val << 4);
								FREG = ((LREG-1) >> 24) & ZERO;
							break;
							
							case 0x36: // SWAP (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								memval = readMem(mem, index);
								val = memval >> 4;
								val |= ((memval & 0x0F) << 4);
								writeMem(mem, index, val);
								FREG = ((val-1) >> 24) & ZERO;
							break;
							
							case 0x37: // SWAP A
								numCycles+=2;
								val = AREG & 0x0F;
								AREG >>= 4;
								AREG |= (val << 4);
								FREG = ((AREG-1) >> 24) & ZERO;
							break;
							
							case 0x38: // SRL B
								numCycles+=2;
								FREG = (BREG & BIT0) << 4;
								BREG >>= 1;
								if (BREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x39: // SRL C
								numCycles+=2;
								FREG = (CREG & BIT0) << 4;
								CREG >>= 1;
								if (CREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x3A: // SRL D
								numCycles+=2;
								FREG = (DREG & BIT0) << 4;
								DREG >>= 1;
								if (DREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x3B: // SRL E
								numCycles+=2;
								FREG = (EREG & BIT0) << 4;
								EREG >>= 1;
								if (EREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x3C: // SRL H
								numCycles+=2;
								FREG = (HREG & BIT0) << 4;
								HREG >>= 1;
								if (HREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x3D: // SRL L
								numCycles+=2;
								FREG = (LREG & BIT0) << 4;
								LREG >>= 1;
								if (LREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x3E: // SRL (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								memval = readMem(mem, index);
								FREG = (memval & BIT0) << 4;
								if (writeMem(mem, index, memval >> 1) == 0)
									FREG |= ZERO;
							break;
							
							case 0x3F: // SRL A
								numCycles+=2;
								FREG = (AREG & BIT0) << 4;
								AREG >>= 1;
								if (AREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x40: // BIT 0,B
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~BREG & BIT0) << 7);
							break;
							
							case 0x41: // BIT 0,C
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~CREG & BIT0) << 7);
							break;
							
							case 0x42: // BIT 0,D
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~DREG & BIT0) << 7);
							break;
							
							case 0x43: // BIT 0,E
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~EREG & BIT0) << 7);
							break;
							
							case 0x44: // BIT 0,H
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~HREG & BIT0) << 7);
							break;
							
							case 0x45: // BIT 0,L
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~LREG & BIT0) << 7);
							break;
							
							case 0x46: // BIT 0,(HL)
								numCycles+=4;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~readMem(mem, (HREG << 8) | LREG) & BIT0) << 7);
							break;
							
							case 0x47: // BIT 0,A
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~AREG & BIT0) << 7);
							break;
							
							case 0x48: // BIT 1,B
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~BREG & BIT1) << 6);
							break;
							
							case 0x49: // BIT 1,C
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~CREG & BIT1) << 6);
							break;
							
							case 0x4A: // BIT 1,D
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~DREG & BIT1) << 6);
							break;
							
							case 0x4B: // BIT 1,E
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~EREG & BIT1) << 6);
							break;
							
							case 0x4C: // BIT 1,H
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~HREG & BIT1) << 6);
							break;
							
							case 0x4D: // BIT 1,L
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~LREG & BIT1) << 6);
							break;
							
							case 0x4E: // BIT 1,(HL)
								numCycles+=4;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~readMem(mem, (HREG << 8) | LREG) & BIT1) << 6);
							break;
							
							case 0x4F: // BIT 1,A
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~AREG & BIT1) << 6);
							break;
							
							case 0x50: // BIT 2,B
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~BREG & BIT2) << 5);
							break;
							
							case 0x51: // BIT 2,C
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~CREG & BIT2) << 5);
							break;
							
							case 0x52: // BIT 2,D
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~DREG & BIT2) << 5);
							break;
							
							case 0x53: // BIT 2,E
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~EREG & BIT2) << 5);
							break;
							
							case 0x54: // BIT 2,H
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~HREG & BIT2) << 5);
							break;
							
							case 0x55: // BIT 2,L
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~LREG & BIT2) << 5);
							break;
							
							case 0x56: // BIT 2,(HL)
								numCycles+=4;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~readMem(mem, (HREG << 8) | LREG) & BIT2) << 5);
							break;
							
							case 0x57: // BIT 2,A
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~AREG & BIT2) << 5);
							break;
							
							case 0x58: // BIT 3,B
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~BREG & BIT3) << 4);
							break;
							
							case 0x59: // BIT 3,C
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~CREG & BIT3) << 4);
							break;
							
							case 0x5A: // BIT 3,D
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~DREG & BIT3) << 4);
							break;
							
							case 0x5B: // BIT 3,E
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~EREG & BIT3) << 4);
							break;
							
							case 0x5C: // BIT 3,H
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~HREG & BIT3) << 4);
							break;
							
							case 0x5D: // BIT 3,L
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~LREG & BIT3) << 4);
							break;
							
							case 0x5E: // BIT 3,(HL)
								numCycles+=4;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~readMem(mem, (HREG << 8) | LREG) & BIT3) << 4);
							break;
							
							case 0x5F: // BIT 3,A
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~AREG & BIT3) << 4);
							break;
							
							case 0x60: // BIT 4,B
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~BREG & BIT4) << 3);
							break;
							
							case 0x61: // BIT 4,C
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~CREG & BIT4) << 3);
							break;
							
							case 0x62: // BIT 4,D
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~DREG & BIT4) << 3);
							break;
							
							case 0x63: // BIT 4,E
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~EREG & BIT4) << 3);
							break;
							
							case 0x64: // BIT 4,H
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~HREG & BIT4) << 3);
							break;
							
							case 0x65: // BIT 4,L
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~LREG & BIT4) << 3);
							break;
							
							case 0x66: // BIT 4,(HL)
								numCycles+=4;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~readMem(mem, (HREG << 8) | LREG) & BIT4) << 3);
							break;
							
							case 0x67: // BIT 4,A
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~AREG & BIT4) << 3);
							break;
							
							case 0x68: // BIT 5,B
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~BREG & BIT5) << 2);
							break;
							
							case 0x69: // BIT 5,C
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~CREG & BIT5) << 2);
							break;
							
							case 0x6A: // BIT 5,D
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~DREG & BIT5) << 2);
							break;
							
							case 0x6B: // BIT 5,E
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~EREG & BIT5) << 2);
							break;
							
							case 0x6C: // BIT 5,H
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~HREG & BIT5) << 2);
							break;
							
							case 0x6D: // BIT 5,L
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~LREG & BIT5) << 2);
							break;
							
							case 0x6E: // BIT 5,(HL)
								numCycles+=4;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~readMem(mem, (HREG << 8) | LREG) & BIT5) << 2);
							break;
							
							case 0x6F: // BIT 5,A
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~AREG & BIT5) << 2);
							break;
							
							case 0x70: // BIT 6,B
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~BREG & BIT6) << 1);
							break;
							
							case 0x71: // BIT 6,C
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~CREG & BIT6) << 1);
							break;
							
							case 0x72: // BIT 6,D
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~DREG & BIT6) << 1);
							break;
							
							case 0x73: // BIT 6,E
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~EREG & BIT6) << 1);
							break;
							
							case 0x74: // BIT 6,H
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~HREG & BIT6) << 1);
							break;
							
							case 0x75: // BIT 6,L
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~LREG & BIT6) << 1);
							break;
							
							case 0x76: // BIT 6,(HL)
								numCycles+=4;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~readMem(mem, (HREG << 8) | LREG) & BIT6) << 1);
							break;
							
							case 0x77: // BIT 6,A
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | ((~AREG & BIT6) << 1);
							break;
							
							case 0x78: // BIT 7,B
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | (~BREG & BIT7);
							break;
							
							case 0x79: // BIT 7,C
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | (~CREG & BIT7);
							break;
							
							case 0x7A: // BIT 7,D
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | (~DREG & BIT7);
							break;
							
							case 0x7B: // BIT 7,E
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | (~EREG & BIT7);
							break;
							
							case 0x7C: // BIT 7,H
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | (~HREG & BIT7);
							break;
							
							case 0x7D: // BIT 7,L
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | (~LREG & BIT7);
							break;
							
							case 0x7E: // BIT 7,(HL)
								numCycles+=4;
								FREG = (FREG & CARRY) | HALF_CARRY | (~readMem(mem, (HREG << 8) | LREG) & BIT7);
							break;
							
							case 0x7F: // BIT 7,A
								numCycles+=2;
								FREG = (FREG & CARRY) | HALF_CARRY | (~AREG & BIT7);
							break;
							
							case 0x80: // RES 0,B
								numCycles+=2;
								BREG &= ~BIT0;
							break;
							
							case 0x81: // RES 0,C
								numCycles+=2;
								CREG &= ~BIT0;
							break;
							
							case 0x82: // RES 0,D
								numCycles+=2;
								DREG &= ~BIT0;
							break;
							
							case 0x83: // RES 0,E
								numCycles+=2;
								EREG &= ~BIT0;
							break;
							
							case 0x84: // RES 0,H
								numCycles+=2;
								HREG &= ~BIT0;
							break;
							
							case 0x85: // RES 0,L
								numCycles+=2;
								LREG &= ~BIT0;
							break;
							
							case 0x86: // RES 0,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) & ~BIT0);
								// may create a method and change to: andMem((HREG << 8) | LREG, ~BIT0);
							break;
							
							case 0x87: // RES 0,A
								numCycles+=2;
								AREG &= ~BIT0;
							break;
							
							case 0x88: // RES 1,B
								numCycles+=2;
								BREG &= ~BIT1;
							break;
							
							case 0x89: // RES 1,C
								numCycles+=2;
								CREG &= ~BIT1;
							break;
							
							case 0x8A: // RES 1,D
								numCycles+=2;
								DREG &= ~BIT1;
							break;
							
							case 0x8B: // RES 1,E
								numCycles+=2;
								EREG &= ~BIT1;
							break;
							
							case 0x8C: // RES 1,H
								numCycles+=2;
								HREG &= ~BIT1;
							break;
							
							case 0x8D: // RES 1,L
								numCycles+=2;
								LREG &= ~BIT1;
							break;
							
							case 0x8E: // RES 1,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) & ~BIT1);
							break;
							
							case 0x8F: // RES 1,A
								numCycles+=2;
								AREG &= ~BIT1;
							break;
							
							case 0x90: // RES 2,B
								numCycles+=2;
								BREG &= ~BIT2;
							break;
							
							case 0x91: // RES 2,C
								numCycles+=2;
								CREG &= ~BIT2;
							break;
							
							case 0x92: // RES 2,D
								numCycles+=2;
								DREG &= ~BIT2;
							break;
							
							case 0x93: // RES 2,E
								numCycles+=2;
								EREG &= ~BIT2;
							break;
							
							case 0x94: // RES 2,H
								numCycles+=2;
								HREG &= ~BIT2;
							break;
							
							case 0x95: // RES 2,L
								numCycles+=2;
								LREG &= ~BIT2;
							break;
							
							case 0x96: // RES 2,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) & ~BIT2);
							break;
							
							case 0x97: // RES 2,A
								numCycles+=2;
								AREG &= ~BIT2;
							break;
							
							case 0x98: // RES 3,B
								numCycles+=2;
								BREG &= ~BIT3;
							break;
							
							case 0x99: // RES 3,C
								numCycles+=2;
								CREG &= ~BIT3;
							break;
							
							case 0x9A: // RES 3,D
								numCycles+=2;
								DREG &= ~BIT3;
							break;
							
							case 0x9B: // RES 3,E
								numCycles+=2;
								EREG &= ~BIT3;
							break;
							
							case 0x9C: // RES 3,H
								numCycles+=2;
								HREG &= ~BIT3;
							break;
							
							case 0x9D: // RES 3,L
								numCycles+=2;
								LREG &= ~BIT3;
							break;
							
							case 0x9E: // RES 3,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) & ~BIT3);
							break;
							
							case 0x9F: // RES 3,A
								numCycles+=2;
								AREG &= ~BIT3;
							break;
							
							case 0xA0: // RES 4,B
								numCycles+=2;
								BREG &= ~BIT4;
							break;
							
							case 0xA1: // RES 4,C
								numCycles+=2;
								CREG &= ~BIT4;
							break;
							
							case 0xA2: // RES 4,D
								numCycles+=2;
								DREG &= ~BIT4;
							break;
							
							case 0xA3: // RES 4,E
								numCycles+=2;
								EREG &= ~BIT4;
							break;
							
							case 0xA4: // RES 4,H
								numCycles+=2;
								HREG &= ~BIT4;
							break;
							
							case 0xA5: // RES 4,L
								numCycles+=2;
								LREG &= ~BIT4;
							break;
							
							case 0xA6: // RES 4,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) & ~BIT4);
							break;
							
							case 0xA7: // RES 4,A
								numCycles+=2;
								AREG &= ~BIT4;
							break;
							
							case 0xA8: // RES 5,B
								numCycles+=2;
								BREG &= ~BIT5;
							break;
							
							case 0xA9: // RES 5,C
								numCycles+=2;
								CREG &= ~BIT5;
							break;
							
							case 0xAA: // RES 5,D
								numCycles+=2;
								DREG &= ~BIT5;
							break;
							
							case 0xAB: // RES 5,E
								numCycles+=2;
								EREG &= ~BIT5;
							break;
							
							case 0xAC: // RES 5,H
								numCycles+=2;
								HREG &= ~BIT5;
							break;
							
							case 0xAD: // RES 5,L
								numCycles+=2;
								LREG &= ~BIT5;
							break;
							
							case 0xAE: // RES 5,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) & ~BIT5);
							break;
							
							case 0xAF: // RES 5,A
								numCycles+=2;
								AREG &= ~BIT5;
							break;
							
							case 0xB0: // RES 6,B
								numCycles+=2;
								BREG &= ~BIT6;
							break;
							
							case 0xB1: // RES 6,C
								numCycles+=2;
								CREG &= ~BIT6;
							break;
							
							case 0xB2: // RES 6,D
								numCycles+=2;
								DREG &= ~BIT6;
							break;
							
							case 0xB3: // RES 6,E
								numCycles+=2;
								EREG &= ~BIT6;
							break;
							
							case 0xB4: // RES 6,H
								numCycles+=2;
								HREG &= ~BIT6;
							break;
							
							case 0xB5: // RES 6,L
								numCycles+=2;
								LREG &= ~BIT6;
							break;
							
							case 0xB6: // RES 6,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) & ~BIT6);
							break;
							
							case 0xB7: // RES 6,A
								numCycles+=2;
								AREG &= ~BIT6;
							break;
							
							case 0xB8: // RES 7,B
								numCycles+=2;
								BREG &= ~BIT7;
							break;
							
							case 0xB9: // RES 7,C
								numCycles+=2;
								CREG &= ~BIT7;
							break;
							
							case 0xBA: // RES 7,D
								numCycles+=2;
								DREG &= ~BIT7;
							break;
							
							case 0xBB: // RES 7,E
								numCycles+=2;
								EREG &= ~BIT7;
							break;
							
							case 0xBC: // RES 7,H
								numCycles+=2;
								HREG &= ~BIT7;
							break;
							
							case 0xBD: // RES 7,L
								numCycles+=2;
								LREG &= ~BIT7;
							break;
							
							case 0xBE: // RES 7,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) & ~BIT0);
							break;
							
							case 0xBF: // RES 7,A
								numCycles+=2;
								AREG &= ~BIT7;
							break;
							
							case 0xC0: // SET 0,B
								numCycles+=2;
								BREG |= BIT0;
							break;
							
							case 0xC1: // SET 0,C
								numCycles+=2;
								CREG |= BIT0;
							break;
							
							case 0xC2: // SET 0,D
								numCycles+=2;
								DREG |= BIT0;
							break;
							
							case 0xC3: // SET 0,E
								numCycles+=2;
								EREG |= BIT0;
							break;
							
							case 0xC4: // SET 0,H
								numCycles+=2;
								HREG |= BIT0;
							break;
							
							case 0xC5: // SET 0,L
								numCycles+=2;
								LREG |= BIT0;
							break;
							
							case 0xC6: // SET 0,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) | BIT0);
								// may create a method and change to: orMem((HREG << 8) | LREG, BIT0);
							break;
							
							case 0xC7: // SET 0,A
								numCycles+=2;
								AREG |= BIT0;
							break;
							
							case 0xC8: // SET 1,B
								numCycles+=2;
								BREG |= BIT1;
							break;
							
							case 0xC9: // SET 1,C
								numCycles+=2;
								CREG |= BIT1;
							break;
							
							case 0xCA: // SET 1,D
								numCycles+=2;
								DREG |= BIT1;
							break;
							
							case 0xCB: // SET 1,E
								numCycles+=2;
								EREG |= BIT1;
							break;
							
							case 0xCC: // SET 1,H
								numCycles+=2;
								HREG |= BIT1;
							break;
							
							case 0xCD: // SET 1,L
								numCycles+=2;
								LREG |= BIT1;
							break;
							
							case 0xCE: // SET 1,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) | BIT1);
							break;
							
							case 0xCF: // SET 1,A
								numCycles+=2;
								AREG |= BIT1;
							break;
							
							case 0xD0: // SET 2,B
								numCycles+=2;
								BREG |= BIT2;
							break;
							
							case 0xD1: // SET 2,C
								numCycles+=2;
								CREG |= BIT2;
							break;
							
							case 0xD2: // SET 2,D
								numCycles+=2;
								DREG |= BIT2;
							break;
							
							case 0xD3: // SET 2,E
								numCycles+=2;
								EREG |= BIT2;
							break;
							
							case 0xD4: // SET 2,H
								numCycles+=2;
								HREG |= BIT2;
							break;
							
							case 0xD5: // SET 2,L
								numCycles+=2;
								LREG |= BIT2;
							break;
							
							case 0xD6: // SET 2,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) | BIT2);
							break;
							
							case 0xD7: // SET 2,A
								numCycles+=2;
								AREG |= BIT2;
							break;
							
							case 0xD8: // SET 3,B
								numCycles+=2;
								BREG |= BIT3;
							break;
							
							case 0xD9: // SET 3,C
								numCycles+=2;
								CREG |= BIT3;
							break;
							
							case 0xDA: // SET 3,D
								numCycles+=2;
								DREG |= BIT3;
							break;
							
							case 0xDB: // SET 3,E
								numCycles+=2;
								EREG |= BIT3;
							break;
							
							case 0xDC: // SET 3,H
								numCycles+=2;
								HREG |= BIT3;
							break;
							
							case 0xDD: // SET 3,L
								numCycles+=2;
								LREG |= BIT3;
							break;
							
							case 0xDE: // SET 3,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) | BIT3);
							break;
							
							case 0xDF: // SET 3,A
								numCycles+=2;
								AREG |= BIT3;
							break;
							
							case 0xE0: // SET 4,B
								numCycles+=2;
								BREG |= BIT4;
							break;
							
							case 0xE1: // SET 4,C
								numCycles+=2;
								CREG |= BIT4;
							break;
							
							case 0xE2: // SET 4,D
								numCycles+=2;
								DREG |= BIT4;
							break;
							
							case 0xE3: // SET 4,E
								numCycles+=2;
								EREG |= BIT4;
							break;
							
							case 0xE4: // SET 4,H
								numCycles+=2;
								HREG |= BIT4;
							break;
							
							case 0xE5: // SET 4,L
								numCycles+=2;
								LREG |= BIT4;
							break;
							
							case 0xE6: // SET 4,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) | BIT4);
							break;
							
							case 0xE7: // SET 4,A
								numCycles+=2;
								AREG |= BIT4;
							break;
							
							case 0xE8: // SET 5,B
								numCycles+=2;
								BREG |= BIT5;
							break;
							
							case 0xE9: // SET 5,C
								numCycles+=2;
								CREG |= BIT5;
							break;
							
							case 0xEA: // SET 5,D
								numCycles+=2;
								DREG |= BIT5;
							break;
							
							case 0xEB: // SET 5,E
								numCycles+=2;
								EREG |= BIT5;
							break;
							
							case 0xEC: // SET 5,H
								numCycles+=2;
								HREG |= BIT5;
							break;
							
							case 0xED: // SET 5,L
								numCycles+=2;
								LREG |= BIT5;
							break;
							
							case 0xEE: // SET 5,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) | BIT5);
							break;
							
							case 0xEF: // SET 5,A
								numCycles+=2;
								AREG |= BIT5;
							break;
							
							case 0xF0: // SET 6,B
								numCycles+=2;
								BREG |= BIT6;
							break;
							
							case 0xF1: // SET 6,C
								numCycles+=2;
								CREG |= BIT6;
							break;
							
							case 0xF2: // SET 6,D
								numCycles+=2;
								DREG |= BIT6;
							break;
							
							case 0xF3: // SET 6,E
								numCycles+=2;
								EREG |= BIT6;
							break;
							
							case 0xF4: // SET 6,H
								numCycles+=2;
								HREG |= BIT6;
							break;
							
							case 0xF5: // SET 6,L
								numCycles+=2;
								LREG |= BIT6;
							break;
							
							case 0xF6: // SET 6,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) | BIT6);
							break;
							
							case 0xF7: // SET 6,A
								numCycles+=2;
								AREG |= BIT6;
							break;
							
							case 0xF8: // SET 7,B
								numCycles+=2;
								BREG |= BIT7;
							break;
							
							case 0xF9: // SET 7,C
								numCycles+=2;
								CREG |= BIT7;
							break;
							
							case 0xFA: // SET 7,D
								numCycles+=2;
								DREG |= BIT7;
							break;
							
							case 0xFB: // SET 7,E
								numCycles+=2;
								EREG |= BIT7;
							break;
							
							case 0xFC: // SET 7,H
								numCycles+=2;
								HREG |= BIT7;
							break;
							
							case 0xFD: // SET 7,L
								numCycles+=2;
								LREG |= BIT7;
							break;
							
							case 0xFE: // SET 7,(HL)
								numCycles+=4;
								index = (HREG << 8) | LREG; 
								writeMem(mem, index, readMem(mem, index) | BIT7);
							break;
							
							case 0xFF: // SET 7,A
								numCycles+=2;
								AREG |= BIT7;
							break;
							
							default: throw new AssertionError("Invalid byte following CB opcode");
						}
					break;
					
					case 0xCC: // CALL Z,nn
						if ((FREG & ZERO) != 0)
						{
							numCycles+=6;
							writeMem(mem, --SP, (PC+2) >> 8);
							writeMem(mem, --SP, (PC+2) & 0x00FF);
							PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xCD: // CALL nn
						numCycles+=6;
						writeMem(mem, --SP, (PC+2) >> 8);
						writeMem(mem, --SP, (PC+2) & 0x00FF);
						PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
					break;
					
					case 0xCE: //ADC A,n
						numCycles+=2;
						memval = readMem(mem, PC++) + ((FREG & CARRY) >> 4);	
						FREG = FLAG_ADD[(memval << 8) | AREG];
						AREG = (AREG+memval) & 0xFF;
					break;
					
					case 0xCF: // RST 08H
						numCycles += 4;
						writeMem(mem, --SP, PC >> 8);
						writeMem(mem, --SP, PC & 0x00FF);
						PC = 0x0008;
					break;
					
					case 0xD0: // RET NC
						if ((FREG & CARRY) == 0)
						{
							numCycles+=5;
							PC = readMem(mem, SP++) | (readMem(mem, SP++) << 8);
						}
						else
							numCycles+=2;
					break;
					
					case 0xD1: // POP DE
						numCycles+=3;
						EREG = readMem(mem, SP++);
						DREG = readMem(mem, SP++);
					break;
					
					case 0xD2: // JP NC,nn
						if ((FREG & CARRY) == 0)
						{
							numCycles+=4;
							PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xD4: // CALL NC,nn
						if ((FREG & CARRY) == 0)
						{
							numCycles+=6;
							writeMem(mem, --SP, (PC+2) >> 8);
							writeMem(mem, --SP, (PC+2) & 0x00FF);
							PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
						
					case 0xD5: //PUSH DE
						numCycles+=4;
						writeMem(mem, --SP, DREG);
						writeMem(mem, --SP, EREG);
					break;
					
					case 0xD6: // SUB A,n
						numCycles+=2;
						memval = readMem(mem, PC++);
						FREG = FLAG_SUB[(memval << 8) | AREG];
						AREG = (AREG-memval) &  0xFF;
					break;
					
					case 0xD7: // RST 10H
						numCycles += 4;
						writeMem(mem, --SP, PC >> 8);
						writeMem(mem, --SP, PC & 0x00FF);
						PC = 0x0010;
					break;
					
					case 0xD8: // RET C
						if ((FREG & CARRY) != 0)
						{
							numCycles+=5;
							PC = readMem(mem, SP++) | (readMem(mem, SP++) << 8);
						}
						else
							numCycles+=2;
					break;
					
					case 0xD9: // RETI
						numCycles+=4;
						PC = readMem(mem, SP++) | (readMem(mem, SP++) << 8);
						IME = true;
					break;
					
					case 0xDA: // JP C,nn
						if ((FREG & CARRY) != 0)
						{
							numCycles+=4;
							PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xDC: // CALL C,nn
						if ((FREG & CARRY) != 0)
						{
							numCycles+=6;
							writeMem(mem, --SP, (PC+2) >> 8);
							writeMem(mem, --SP, (PC+2) & 0x00FF);
							PC = readMem(mem, PC) | (readMem(mem, PC+1) << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xDE: // SBC A,n
						numCycles+=2;
						memval = readMem(mem, PC++) + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[(memval << 8) | AREG];
						AREG = (AREG - memval) & 0xFF;
					break;
					
					case 0xDF: // RST 18H
						numCycles += 4;
						writeMem(mem, --SP, PC >> 8);
						writeMem(mem, --SP, PC & 0x00FF);
						PC = 0x0018;
					break;
					
					case 0xE0: //LDH (n),A **WRITE TO ADDRESS N**
						numCycles+=3;
						writeMem(mem, 0xFF00 + readMem(mem, PC++), AREG);
					break;
					
					case 0xE1: // POP HL
						numCycles+=3;
						LREG = readMem(mem, SP++);
						HREG = readMem(mem, SP++);
					break;
						
					case 0xE2: //LD (C),A **WRITE TO IO C**
						numCycles+=2;
						writeMem(mem, 0xFF00 + CREG, AREG);
					break;
					
					case 0xE5: // PUSH HL
						numCycles+=4;
						writeMem(mem, --SP, HREG);
						writeMem(mem, --SP, LREG);
					break;
					
					case 0xE6: // AND n
						numCycles+=2;
						AREG &= readMem(mem, PC++);
						FREG = HALF_CARRY | (((AREG-1) >> 24) & ZERO);
					break;
					
					case 0xE7: // RST 20H
						numCycles += 4;
						writeMem(mem, --SP, PC >> 8);
						writeMem(mem, --SP, PC & 0x00FF);
						PC = 0x0020;
					break;
					
					case 0xE8: //ADD SP,n **ignores half-carry**
						numCycles+=4;
						SP += (byte)readMem(mem, PC++); // signed immediate
						if (SP > 0xFFFF)
						{
							SP &= 0xFFFF;
							FREG = CARRY;
						}
						else
							FREG = 0;
					break;
					
					case 0xE9: // JP (HL)
						numCycles++;
						PC = (HREG << 8) | LREG;
					break;
					
					case 0xEA: //LD (nn),A
						numCycles+=4;
						writeMem(mem, readMem(mem, PC++) | (readMem(mem, PC++) << 8), AREG);
					break;
					
					case 0xEE: // XOR n
						numCycles+=2;
						AREG ^= readMem(mem, PC++);
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xEF: // RST 28H
						numCycles += 4;
						writeMem(mem, --SP, PC >> 8);
						writeMem(mem, --SP, PC & 0x00FF);
						PC = 0x0028;
					break;
					
					case 0xF0: //LDH (n),A **READ FROM ADDRESS N**
						numCycles+=3;
						AREG = readMem(mem, 0xFF00 + readMem(mem, PC++));
					break;
					
					case 0xF1: // POP AF
						numCycles+=3;
						FREG = readMem(mem, SP++);
						AREG = readMem(mem, SP++);
					break;
					
					case 0xF2: //LD A,(C) **READ FROM IO C**
						numCycles += 2;
						AREG = readMem(mem, 0xFF00 + CREG);
					break;
					
					case 0xF3: // DI
						numCycles++;
						IME = false; 
						// *Officially should occur 1 instruction later. We'll see how it works...*
					break;
					
					case 0xF5: // PUSH AF
						numCycles+=4;
						writeMem(mem, --SP, AREG);
						writeMem(mem, --SP, FREG);
					break;
					
					case 0xF6: // OR n
						numCycles+=2;
						AREG |= readMem(mem, PC++);
						FREG = ((AREG-1) >> 24) & ZERO;
					break;
					
					case 0xF7: // RST 30H
						numCycles += 4;
						writeMem(mem, --SP, PC >> 8);
						writeMem(mem, --SP, PC & 0x00FF);
						PC = 0x0030;
					break;
					
					case 0xF8: //LDHL SP,n **ignores half-carry**
						numCycles+=3;
						val = SP + (byte)readMem(mem, PC++); // signed immediate
						if (val > 0xFFFF)
						{
							val &= 0xFFFF;
							FREG = CARRY;
						}
						else
							FREG = 0;
						HREG = (val >> 8);
						LREG = (val & 0x00FF);
					break;
					
					case 0xF9: //LD SP,HL
						numCycles+=2;
						SP = ( (HREG << 8 ) | LREG );
					break;
					 
					case 0xFA: //LD A,(nn)
						numCycles+=4;
						AREG = readMem(mem, readMem(mem, PC++) | (readMem(mem, PC++) << 8));
					break;
					
					case 0xFB: // EI
						numCycles++;
						IME = true; 
						// *Officially should occur 1 instruction later. We'll see how it works...*
					break;
					
					case 0xFE: // CP n
						numCycles+=2;
						FREG = FLAG_SUB[(readMem(mem, PC++) << 8) | AREG];
					break;
					
					case 0xFF: // RST 38H
						numCycles += 4;
						writeMem(mem, --SP, PC >> 8);
						writeMem(mem, --SP, PC & 0x00FF);
						PC = 0x0038;
					break;
					
					default: throw new AssertionError("Unsupported opcode");
				}
				
				//int cyclesUntilScan = ;
				
				//if (cyclesUntilScan < 64)
				//{
					//if (cyclesUntilScan > 0)
					//	HRAM[0x1F41] = (HRAM[0x1F41] & 0xFC) | (3 - (HRAM[0x1F41] >> 5));
					
					
					/*if (cyclesUntilScan > 43)
					{
						HRAM[0x1F41] |= BIT1;
						HRAM[0x1F41] &= ~BIT0;
					}
					else if (cyclesUntilScan > 0)
					{
						HRAM[0x1F41] |= BIT0;
					}*/
				
				switch (nextHBlank-numCycles)
				{
					case 114: case 113: case 112: case 111: case 110: case 109: case 108: case 107:
					case 106: case 105: case 104: case 103: case 102: case 101: case 100: case 99:
					case 98: case 97: case 96: case 95: case 94: case 93: case 92: case 91:
					case 90: case 89: case 88: case 87: case 86: case 85: case 84: case 83:
					case 82: case 81: case 80: case 79: case 78: case 77: case 76: case 75:
					case 74: case 73: case 72: case 71: case 70: case 69: case 68: case 67:
					case 66: case 65: case 64:
					
					case 57: case 56: case 55: case 54: case 53: case 52: case 51: case 50:
					case 49: case 48: case 47: case 46: case 45: case 44:
					
					case 37: case 36: case 35: case 34: case 33: case 32: case 31: case 30:
					case 29: case 28: case 27: case 26: case 25: case 24: case 23: case 22:
					case 21: case 20: case 19: case 18: case 17: case 16: case 15: case 14:
					case 13: case 12: case 11: case 10: case 9: case 8: case 7: case 6:
					case 5: case 4: case 3: case 2: case 1:
						break;
					
					case 63: case 62: case 61: case 60: case 59: case 58: 
						if (scanline < GUI.screenHeight)
							HRAM[0x1F41] = (HRAM[0x1F41] & 0xFC) | 2;
						break;
					case 43: case 42: case 41: case 40: case 39: case 38:
						if (scanline < GUI.screenHeight)
							HRAM[0x1F41] |= 3;
						break;
					default:
						if (newSerialInt > 0)
						{
							newSerialInt--;
							if (newSerialInt == 0)
							{
								HRAM[0x1F01] = 0xFF;
								HRAM[0x1F02] &= ~BIT7;
								HRAM[0x1F0F] |= BIT3;
							}
						}
						
						//	if(snd.soundEnabled)
						//		snd.outputSound();
						
						// HANDLE INTERRUPTS HERE
						
						if (IME)
						{
							if ((HRAM[0x1FFF] & BIT0) != 0 && (HRAM[0x1F0F] & BIT0) != 0)
							{
								//System.out.printf("Launching VBLANK interrupt, current address %4X\n", PC);
								HRAM[0x1F0F] &= ~BIT0;
								IME = false;
								writeMem(mem, --SP, PC >> 8);
								writeMem(mem, --SP, PC & 0x00FF);
								PC = 0x0040;
							}
							else if ((HRAM[0x1FFF] & BIT1) != 0 && (HRAM[0x1F0F] & BIT1) != 0)
							{
								//System.out.println("Launching LCDC interrupt");
								HRAM[0x1F0F] &= ~BIT1;
								IME = false;
								writeMem(mem, --SP, PC >> 8);
								writeMem(mem, --SP, PC & 0x00FF);
								PC = 0x0048;
							}
							else if ((HRAM[0x1FFF] & BIT2) != 0 && (HRAM[0x1F0F] & BIT2) != 0)
							{
								//System.out.println("Launching TIMER interrupt");
								HRAM[0x1F0F] &= ~BIT2;
								IME = false;
								writeMem(mem, --SP, PC >> 8);
								writeMem(mem, --SP, PC & 0x00FF);
								PC = 0x0050;
							}
							else if ((HRAM[0x1FFF] & BIT3) != 0 && (HRAM[0x1F0F] & BIT3) != 0)
							{
								//System.out.println("Launching SERIAL interrupt");
								HRAM[0x1F0F] &= ~BIT3;
								IME = false;
								writeMem(mem, --SP, PC >> 8);
								writeMem(mem, --SP, PC & 0x00FF);
								PC = 0x0058;
							}
							else if ((HRAM[0x1FFF] & BIT4) != 0 && (HRAM[0x1F0F] & BIT4) != 0)
							{
								//System.out.println("Launching JOYPAD interrupt");
								HRAM[0x1F0F] &= ~BIT4;
								IME = false;
								writeMem(mem, --SP, PC >> 8);
								writeMem(mem, --SP, PC & 0x00FF);
								PC = 0x0060;
							}
						}
						
						// STOP HANDLING INTERRUPTS
						
						// Draw current scanline
						if (scanline < GUI.screenHeight)
						{
							HRAM[0x1F41] &= ~0x03;
							if ((HRAM[0x1F41] & BIT3) != 0) // H-Blank interrupt
								HRAM[0x1F0F] |= BIT1;
							/*
							// Handle BG/Window
							if ((HRAM[0x1F40] & BIT0) != 0)
							{
								myColor = colorBG;
								
								int xPix = 0;
								// x = HRAM[0x1F43] % 8
								int x = HRAM[0x1F43] & 0x7;
								// y = (HRAM[0x1F42] + scanline) % 8
								int y = (HRAM[0x1F42] + scanline) & 0x7;
								// crntVal = ((((HRAM[0x1F42] + scanline) % 256) / 8) * 32) * (HRAM[0x1F43] / 8)
								int crntVal = ((((HRAM[0x1F42] + scanline) & 0xFF) >> 3) << 5) + (HRAM[0x1F43] >> 3);
								// maxVal = ((crntVal + 32) / 32) * 32
								int maxVal = (crntVal + 32) & ~0x1F;
						outer:	for(;;)
								{
									//int tileNum;
									//if ((HRAM[0x1F40] & BIT3) != 0)
									//	tileNum = VRAM[0x1C00 + crntVal];
									//else
									int tileNum = VRAM[0x1800 + ((HRAM[0x1F40] & BIT3) << 7) + crntVal];
									
									int tileIndex;
									
									if ((HRAM[0x1F40] & BIT4) != 0)
									{
									//System.out.println("**1**");
										tileIndex = tileNum << 4; // tileNum * 16
									}
									else
									{
									//System.out.println("**2**");	
										tileIndex = 0x1000 + (((byte)tileNum) << 4); // $1000 + signed tileNum * 16
									}
									
									while (x < 8)
									{
										int bitPos = (y << 4) + x; // 16*y + x
										int byteIndex = tileIndex + (bitPos >> 3); // tileIndex + (bitPos/8)
									//System.out.println("Grabbing bit " + (bitPos%8));
										int bitSet = 1 << (7-(bitPos & 0x7)); // 1 << (7-(bitPos%8))
									//System.out.println("** " + tileNum);
									//System.out.println(Integer.toHexString(tileIndex) + " out of " + Integer.toHexString(VRAM.length));
										int colorVal = ((VRAM[byteIndex] & bitSet) != 0 ? BIT0 : 0) |  // LSB
													   ((VRAM[byteIndex+1] & bitSet) != 0 ? BIT1 : 0); // MSB
										
										screen[scanline*GUI.screenWidth + xPix] = myColor[colorVal];
										xPix++;
										if (xPix >= GUI.screenWidth)
											break outer;
										x++;
									}
									
									x = 0;
									crntVal++;
									if (crntVal >= maxVal)
										crntVal -= 32;
								}
								
								// To-do: Now handle Window
								if ((HRAM[0x1F40] & BIT5) != 0)
								{
									//...
								}
							}
							// Done with BG/Window
							*/

							// Handle BG/Window (fast)
							if ((HRAM[0x1F40] & BIT0) != 0)
							{
								int SCY = HRAM[0x1F42];
								int SCX = HRAM[0x1F43];
								
								int upper = ((scanline+SCY) & 0xFF) << 8;
								int mult = scanline*GUI.screenWidth;
								
								for (int xPix = 0; xPix < GUI.screenWidth; xPix++)
								{
									// Draw 16 pixels						
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
									xPix++;
									screen[mult + xPix] = background[upper | ((xPix+SCX) & 0xFF)];
								}		
								
								// Now handle Window (slow, update later)
								if ((HRAM[0x1F40] & BIT5) != 0)
								{
									myColor = colorBG;
									
									int WY = HRAM[0x1F4A];
									int WX = HRAM[0x1F4B];
									
									if (WX <= 166 && scanline >= WY)
									{
										int windowX = 0;									
										for (int xPix = WX-7; xPix < GUI.screenWidth; xPix++)
										{
											int crntTile = ((windowOffset >> 3) << 5) + (windowX >> 3);
											
											int tileNum = VRAM[0x1800 + ((HRAM[0x1F40] & BIT6) << 4) + crntTile];
											//System.out.println(Integer.toHexString(tileNum));
						
											int tileIndex;
											if ((HRAM[0x1F40] & BIT4) != 0)
												tileIndex = tileNum << 4; // tileNum * 16
											else
												tileIndex = 0x800 + ((((byte)(tileNum)) + 128) << 4);
											
											//System.out.println(Integer.toHexString(tileIndex));
											
											int bitPos = ((windowOffset & 7) << 4) + (windowX & 7); // 16*y + x
											int byteIndex = tileIndex + (bitPos >> 3); // tileIndex + (bitPos/8)
											int bitSet = 1 << (7-(bitPos & 0x7)); // 1 << (7-(bitPos%8))

											int colorVal = ((VRAM[byteIndex] & bitSet) != 0 ? BIT0 : 0) |  // LSB
											               ((VRAM[byteIndex+1] & bitSet) != 0 ? BIT1 : 0); // MSB
										
											
											//System.out.printf("writing window (%d, %d) to screen (%d, %d)\n", windowX, windowOffset, xPix, scanline);
											
											
											screen[mult + xPix] = myColor[colorVal];
											
											windowX++;
										}
										
										windowOffset++;
									}
								}
							}
							// Done with BG/Window
							
							// Handle sprites
							if ((HRAM[0x1F40] & BIT1) != 0)
							{
								if ((HRAM[0x1F40] & BIT2) != 0) // 8*16
								{
									for (int sIndex = 0; sIndex < 0xA0; sIndex+=4)
									{
										int spriteY = HRAM[0x1E00 | sIndex];
										if (spriteY > (scanline+16) || spriteY <= (scanline))
											continue;
										
										int spriteX = HRAM[0x1E00 | (sIndex+1)];
										int patternNum = HRAM[0x1E00 | (sIndex+2)] & ~0x01;
										int flags = HRAM[0x1E00 | (sIndex+3)];
										
										if ((flags & BIT4) != 0)
											myColor = colorSP1;
										else
											myColor = colorSP0;
										
										int y = 16 - (spriteY-scanline);
										if ((flags & BIT6) != 0)
											y = 15 - y;
										
										int deltaX;
										int xPix;
										if ((flags & BIT5) != 0)
										{
											xPix = spriteX-1;
											deltaX = -1;
										}
										else
										{
											xPix = spriteX-8;
											deltaX = 1;
										}
										
										for (int x = 0; x < 8; x++)
										{
											if (xPix < 0 || xPix >= GUI.screenWidth)
											{
												xPix += deltaX;
												continue;
											}
											int bitPos = (y << 4) + x; // 16*y + x
											int byteIndex = (patternNum << 4) + (bitPos >> 3); // patternIndex + (bitPos/8)
											int bitSet = 1 << (7-(bitPos & 0x7)); // 1 << (7-(bitPos%8))
											int colorVal = ((VRAM[byteIndex] & bitSet) != 0 ? BIT0 : 0) |  // LSB
														   ((VRAM[byteIndex+1] & bitSet) != 0 ? BIT1 : 0); // MSB
											
											if (colorVal != 0)
											{
												//if ((flags & BIT7) != 0 || screen[scanline*GUI.screenWidth + xPix] == colorBG[0])
													screen[scanline*GUI.screenWidth + xPix] = myColor[colorVal];
											}
											xPix += deltaX;
										}
									}
								}
								else // 8*8
								{
									for (int sIndex = 0; sIndex < 0xA0; sIndex+=4)
									{
										int spriteY = HRAM[0x1E00 | sIndex];
										if (spriteY > (scanline+16) || spriteY <= (scanline+8))
											continue;
										
										int spriteX = HRAM[0x1E00 | (sIndex+1)];
										int patternNum = HRAM[0x1E00 | (sIndex+2)];
										int flags = HRAM[0x1E00 | (sIndex+3)];
										
										if ((flags & BIT4) != 0)
											myColor = colorSP1;
										else
											myColor = colorSP0;
										
										int y = 16 - (spriteY-scanline);
										if ((flags & BIT6) != 0)
											y = 7 - y;
										
										int deltaX;
										int xPix;
										if ((flags & BIT5) != 0)
										{
											xPix = spriteX-1;
											deltaX = -1;
										}
										else
										{
											xPix = spriteX-8;
											deltaX = 1;
										}
										
										for (int x = 0; x < 8; x++)
										{
											if (xPix < 0 || xPix >= GUI.screenWidth)
											{
												xPix += deltaX;
												continue;
											}
											int bitPos = (y << 4) + x; // 16*y + x
											int byteIndex = (patternNum << 4) + (bitPos >> 3); // patternIndex + (bitPos/8)
											int bitSet = 1 << (7-(bitPos & 0x7)); // 1 << (7-(bitPos%8))
											int colorVal = ((VRAM[byteIndex] & bitSet) != 0 ? BIT0 : 0) |  // LSB
														   ((VRAM[byteIndex+1] & bitSet) != 0 ? BIT1 : 0); // MSB
											
											if (colorVal != 0)
											{
												//if ((flags & BIT7) != 0 || screen[scanline*GUI.screenWidth + xPix] == colorBG[0])
													screen[scanline*GUI.screenWidth + xPix] = myColor[colorVal];
											}
											xPix += deltaX;
										}
									}
								}
							}
							// Done with sprites
						}
						// Finished drawing current scanline
						
						if (scanline == HRAM[0x1F45])
						{
							HRAM[0x1F41] |= BIT2;
							//System.out.printf("%d scanline, LYC interrupt\n", scanline);
							if ((HRAM[0x1F41] & BIT6) != 0)
								HRAM[0x1F0F] |= BIT1;
						}
						else
							HRAM[0x1F41] &= ~BIT2;
						
						//System.out.println(PC);
						
						if (scanline == GUI.screenHeight)
						{
							// TODO: DRAW SPRITES HERE
							
							HRAM[0x1F0F] |= BIT0; // Request VBLANK
							HRAM[0x1F41] |= BIT0;
							if ((HRAM[0x1F41] & BIT4) != 0) // LCDC V-Blank
								HRAM[0x1F0F] |= BIT1;
							
							//nextVBlank += CYCLES_PER_LINE*154;
						}
						
						HRAM[0x1F44] = ++scanline;
						nextHBlank += CYCLES_PER_LINE;
						
						if (numCycles >= 0x100000)
						{
							numCycles &= 0xFFFFF;
							nextHBlank &= 0xFFFFF;
							//nextVBlank &= 0xFFFFF;
						}
						
						HRAM[0x1F04] = (numCycles >> 6) & 0xFF;
						
						if ((HRAM[0x1F07] & BIT2) != 0)
						{
							int div = (((HRAM[0x1F07]-1) & 0x03) + 1) << 1;
							
							if (HRAM[0x1F05] > (HRAM[0x1F05] = HRAM[0x1F06] + ((numCycles >> div) % (256-HRAM[0x1F06]))))
								HRAM[0x1F0F] |= BIT2;
						}
					break;
				}
			}
			
			/*Thread.yield();
			System.gc();
			Thread.yield();
			System.gc();*/
			
			// Inform GUI class to render Gameboy's VRAM to screen
			if ((HRAM[0x1F40] & BIT7) != 0)
			{
				//sem.release();
				gui.newFrame(screen);
			}
			
			if (joypadFlag)
			{
				HRAM[0x1F0F] |= BIT4;
				joypadFlag = false;
			}
			
			HRAM[0x1F44] = scanline = 0; // new frame
			windowOffset = 0;
			frameCount++;
			
			if (throttle)
			{
				try
				{
					long waitNano = (prevFrame + nsPerFrame) - System.nanoTime() + 500000; // 500k for rounding to nearest ms
					if (waitNano >= 1000000)
						Thread.sleep(waitNano / 1000000);
				}
				catch (InterruptedException e)
				{
				}
				//while ((System.nanoTime() - prevFrame) < (1000000000/59.73))
					//Thread.yield();
			}
			else
				Thread.yield();
			
			prevFrame = System.nanoTime();
			
			if (frameCount == 100)
			{
				double secPer100 = (System.nanoTime()-startT) / 1000000000.0;
				System.out.println((secPer100 * 10) + " ms per frame (" + (167.42/secPer100) + "% full speed)");
				frameCount = 0;
				//System.out.format("total: %d hblank: %d vblank: %d\n", numCycles, nextHBlank, nextVBlank);
				startT = System.nanoTime();
			}
		}
		// cannot end main loop while CPU is still running!
	}
}
