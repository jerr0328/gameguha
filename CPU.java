import java.io.*;

public class CPU implements Runnable
{
	private static final int BIT7 = 1<<7;
	private static final int BIT6 = 1<<6;
	private static final int BIT5 = 1<<5;
	private static final int BIT4 = 1<<4;
	private static final int BIT3 = 1<<3;
	private static final int BIT2 = 1<<2;
	private static final int BIT1 = 1<<1;
	private static final int BIT0 = 1<<0;
	
	private static final int ZERO       = BIT7;
	private static final int SUBTRACT   = BIT6;
	private static final int HALF_CARRY = BIT5;
	private static final int CARRY      = BIT4;
	
	private static final int CYCLES_PER_LINE = 114; // (1048576 Hz/ 9198 Hz)
	
	private ROM rom;
	
	public CPU(ROM rom)
	{
		this.rom = rom;
	}
	
	private static void genFlagTable(int[][] FLAG_ADD, int[][] FLAG_SUB, int[] FLAG_INC, int[] FLAG_DEC)
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
				FLAG_ADD[a][b] = flag;
				
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
				FLAG_SUB[a][b] = flag;
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
	
	public void run()
	{
		int AREG=0x01;
		int FREG=0xB0;
		int BREG=0x00;
		int CREG=0x13;
		int DREG=0x00;
		int EREG=0xD8;
		int HREG=0x01;
		int LREG=0x4D;
		int SP=0xFFFE;
		int PC=0x0100;
		boolean IME = true;
		int[] MEM = new int[0x10000]; // (16 bit addressing)
		
		for(int i=0;i<0x4000;i++)
			MEM[i]=rom.getMem(i);
			
		int[][] FLAG_ADD = new int[257][256]; // max 255 + 1 (carry) = 256;
		int[][] FLAG_SUB = new int[257][256];
		int[] FLAG_INC = new int[256];
		int[] FLAG_DEC = new int[256];
		
		int numCycles = 0;
		int scanline = 0;
		int val;
		int index;
		
	
		genFlagTable(FLAG_ADD, FLAG_SUB, FLAG_INC, FLAG_DEC);
		
		for(;;) // loop until thread stops
		{
				
	 		while (scanline <= 153) // from 144 to 153 is v-blank period
			{
				if(MEM[PC]!=0xCB)
					System.out.format("Operation 0x%02X at %X\n",MEM[PC],PC);
				else
					System.out.format("Operation 0x%02X 0x%02X at %X\n",MEM[PC],MEM[PC+1],PC);
						
				switch(MEM[PC++])
				{
					case 0x00: //NOP
						numCycles++;
					break;
					
					case 0x01: //LD BC,nn
						numCycles+=3;
						BREG = MEM[PC++];
						CREG = MEM[PC++];
					break;
					
					case 0x02: //LD (BC),A
						numCycles+=2;
						MEM[ (BREG << 8) | CREG ] = AREG;
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
						BREG = MEM[PC++];
					break;
					
					case 0x07: // RLCA
						numCycles++;
						FREG = (AREG & BIT7) >> 3;
						AREG = ((AREG << 1) | (AREG >> 7)) & 0xFF;
					break;
					
					case 0x08: //LD (nn),SP
						numCycles+=5;
						index = ( MEM[PC++] << 8 ) | MEM[PC++];
						MEM[index] = SP >> 8;
						MEM[index+1] = SP & 0x00FF;
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
						AREG = MEM[ ( BREG << 8) | CREG ];
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
						CREG=MEM[PC++];
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
						DREG = MEM[PC++];
						EREG = MEM[PC++];
					break;
					
					case 0x12: //LD (DE),A
						numCycles+=2;
						MEM[ (DREG << 8) | EREG ] = AREG;
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
						DREG=MEM[PC++];
					break;
					
					case 0x17: // RLA
						numCycles++;
						val = (FREG & CARRY) >> 4;
						FREG = (AREG & BIT7) >> 3;
						AREG = ((AREG << 1) | val) & 0xFF;
					break;
					
					case 0x18: // JR n
						numCycles+=3;
						PC += (byte)MEM[PC++]; // signed immediate
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
						AREG = MEM[ ( DREG << 8 ) | EREG ];
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
						EREG=MEM[PC++];
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
							PC += (byte)MEM[PC++]; // signed immediate
						}
						else
						{
							numCycles+=2;
							PC++;
						}
					break;
					
					case 0x21: //LD HL,nn
						numCycles+=3;
						HREG = MEM[PC++];
						LREG = MEM[PC++];
					break;
					
					case 0x22: //LDI (HL),A
						numCycles+=2;
						index = (HREG << 8) | LREG;
						MEM[index] = AREG;
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
						HREG=MEM[PC++];
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
							PC += (byte)MEM[PC++]; // signed immediate
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
						AREG = MEM[index];
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
						LREG=MEM[PC++];
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
							PC += (byte)MEM[PC++]; // signed immediate
						}
						else
						{
							numCycles+=2;
							PC++;
						}
					break;
					
					case 0x31: //LD SP,nn
						numCycles+=3;
						SP = ( ( MEM[PC++] << 8 ) | MEM[PC++] );
					break;
					
					case 0x32: //LDD (HL),A
						numCycles+=2;
						index = (HREG << 8) | LREG;
						MEM[index] = AREG;
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
						FREG = FLAG_INC[MEM[index]] | (FREG & CARRY);
						MEM[index] = (MEM[index] + 1) & 0xFF;
					break;
					
					case 0x35: // DEC (HL)
						numCycles+=3;
						index = (HREG << 8) | LREG;
						FREG = FLAG_DEC[MEM[index]] | (FREG & CARRY);
						MEM[index] = (MEM[index] - 1) & 0xFF;
					break;
					
					case 0x36: //LD (HL),n
						numCycles+=3;
						MEM[ ( HREG << 8) | LREG ] = MEM[PC++];
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
							PC += (byte)MEM[PC++]; // signed immediate
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
						AREG = MEM[index];
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
						AREG = MEM[PC++];
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
						BREG=MEM[ ( HREG << 8) | LREG ];
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
						CREG=MEM[ ( HREG << 8) | LREG ];
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
						DREG=MEM[ ( HREG << 8) | LREG ];
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
						EREG=MEM[ ( HREG << 8) | LREG ];
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
						HREG=MEM[ ( HREG << 8 ) | LREG ];
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
						LREG=MEM[ ( HREG << 8 ) | LREG ];
					break;
					
					case 0x6F: //LD L,A
						numCycles++;
						LREG=AREG;
					break;
					
					case 0x70: //LD (HL),B
						numCycles+=2;
						MEM[ ( HREG << 8 ) | LREG ] = BREG;
					break;
					
					case 0x71: //LD (HL),C
						numCycles+=2;
						MEM[ ( HREG << 8 ) | LREG ] = CREG;
					break;
					
					case 0x72: //LD (HL),D
						numCycles+=2;
						MEM[ ( HREG << 8 ) | LREG ] = DREG;
					break;
					
					case 0x73: //LD (HL),E
						numCycles+=2;
						MEM[ ( HREG << 8 ) | LREG ] = EREG;
					break;
					
					case 0x74: //LD (HL),H
						numCycles+=2;
						MEM[ ( HREG << 8 ) | LREG ] = HREG;
					break;
					
					case 0x75: //LD (HL),L
						numCycles+=2;
						MEM[ ( HREG << 8 ) | LREG ] = LREG;
					break;
					
					case 0x76: // HALT
						numCycles++;
						//if (IME)
							//*give control to interrupt handler*
						//else
							//PC++; (skip next instruction in GB mode)
					break;
				
					case 0x77: // LD (HL),A
						numCycles+=2;
						MEM[ ( HREG << 8 ) | LREG ] = AREG;
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
						AREG=MEM[ ( HREG << 8 ) | LREG ];
					break;
					
					case 0x7F: //LD A,A
						numCycles++;
					break;
					
					case 0x80: //ADD A,B
						numCycles++;
						FREG = FLAG_ADD[BREG][AREG];
						AREG = (AREG+BREG) & 0xFF;
					break;
					
					case 0x81: //ADD A,C
						numCycles++;
						FREG = FLAG_ADD[CREG][AREG];
						AREG = (AREG+CREG) & 0xFF;
					break;
					
					case 0x82: //ADD A,D
						numCycles++;
						FREG = FLAG_ADD[DREG][AREG];
						AREG = (AREG+DREG) & 0xFF;
					break;
					
					case 0x83: //ADD A,E
						numCycles++;
						FREG = FLAG_ADD[EREG][AREG];
						AREG = (AREG+EREG) & 0xFF;
					break;
					
					case 0x84: //ADD A,H
						numCycles++;
						FREG = FLAG_ADD[HREG][AREG];
						AREG = (AREG+LREG) & 0xFF;
					break;
					
					case 0x85: //ADD A,L
						numCycles++;
						FREG = FLAG_ADD[LREG][AREG];
						AREG = (AREG+LREG) & 0xFF;
					break;
					
					case 0x86: // ADD A,(HL)
						numCycles+=2;
						val = MEM[(HREG << 8) | LREG];
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG+val) & 0xFF;
					break;
					
					case 0x87: //ADD A,A
						numCycles++;
						FREG = FLAG_ADD[AREG][AREG];
						AREG = (AREG+AREG) & 0xFF;
					break;
					
					case 0x88: //ADC A,B
						numCycles++;
						val = BREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x89: //ADC A,C
						numCycles++;
						val = CREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8A: //ADC A,D
						numCycles++;
						val = DREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8B: //ADC A,E
						numCycles++;
						val = EREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8C: //ADC A,H
						numCycles++;
						val = HREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8D: //ADC A,L
						numCycles++;
						val = LREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x8E: //ADC A,(HL)
						numCycles+=2;
						val = MEM[(HREG << 8) | LREG] + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG+val) & 0xFF;
					break;
					
					case 0x8F: //ADC A,A
						numCycles++;
						val = AREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG + val) & 0xFF;
					break;
					
					case 0x90: // SUB A,B
						numCycles++;
						FREG = FLAG_SUB[BREG][AREG];
						AREG = (AREG-BREG) &  0xFF;
					break;
					
					case 0x91: // SUB A,C
						numCycles++;
						FREG = FLAG_SUB[CREG][AREG];
						AREG = (AREG-CREG) &  0xFF;
					break;
					
					case 0x92: // SUB A,D
						numCycles++;
						FREG = FLAG_SUB[DREG][AREG];
						AREG = (AREG-DREG) &  0xFF;
					break;
					
					case 0x93: // SUB A,E
						numCycles++;
						FREG = FLAG_SUB[EREG][AREG];
						AREG = (AREG-EREG) &  0xFF;
					break;
					
					case 0x94: // SUB A,H
						numCycles++;
						FREG = FLAG_SUB[HREG][AREG];
						AREG = (AREG-HREG) &  0xFF;
					break;
					
					case 0x95: // SUB A,L
						numCycles++;
						FREG = FLAG_SUB[LREG][AREG];
						AREG = (AREG-LREG) &  0xFF;
					break;
					
					case 0x96: // SUB A,(HL)
						numCycles+=2;
						val = MEM[(HREG << 8) | LREG];
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG-val) &  0xFF;
					break;
					
					case 0x97: // SUB A,A
						numCycles++;
						FREG = SUBTRACT | ZERO;
						AREG = 0;
					break;
					
					case 0x98: // SBC A,B
						numCycles++;
						val = BREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x99: // SBC A,C
						numCycles++;
						val = CREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x9A: // SBC A,D
						numCycles++;
						val = DREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x9B: // SBC A,E
						numCycles++;
						val = EREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x9C: // SBC A,H
						numCycles++;
						val = HREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x9D: // SBC A,L
						numCycles++;
						val = LREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG - val) & 0xFF;
						
					break;
					
					case 0x9E: // SBC A,(HL)
						numCycles+=2;
						val = MEM[(HREG << 8) | LREG] + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0x9F: // SBC A,A
						numCycles++;
						val = AREG + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0xA0: // AND B
						numCycles++;
						AREG &= BREG;
						if (AREG == 0)
							FREG = ZERO | HALF_CARRY;
						else
							FREG = HALF_CARRY;
					break;
					
					case 0xA1: // AND C
						numCycles++;
						AREG &= CREG;
						if (AREG == 0)
							FREG = ZERO | HALF_CARRY;
						else
							FREG = HALF_CARRY;
					break;
					
					case 0xA2: // AND D
						numCycles++;
						AREG &= DREG;
						if (AREG == 0)
							FREG = ZERO | HALF_CARRY;
						else
							FREG = HALF_CARRY;
					break;
					
					case 0xA3: // AND E
						numCycles++;
						AREG &= EREG;
						if (AREG == 0)
							FREG = ZERO | HALF_CARRY;
						else
							FREG = HALF_CARRY;
					break;
					
					case 0xA4: // AND H
						numCycles++;
						AREG &= HREG;
						if (AREG == 0)
							FREG = ZERO | HALF_CARRY;
						else
							FREG = HALF_CARRY;
					break;
					
					case 0xA5: // AND L
						numCycles++;
						AREG &= LREG;
						if (AREG == 0)
							FREG = ZERO | HALF_CARRY;
						else
							FREG = HALF_CARRY;
					break;
					
					case 0xA6: // AND (HL)
						numCycles+=2;
						AREG &= MEM[(HREG << 8) | LREG];
						if (AREG == 0)
							FREG = ZERO | HALF_CARRY;
						else
							FREG = HALF_CARRY;
					break;
					
					case 0xA7: // AND A
						// A&A = A, no change
						numCycles++;
						if (AREG == 0)
							FREG = ZERO | HALF_CARRY;
						else
							FREG = HALF_CARRY;
					break;
					
					case 0xA8: // XOR B
						numCycles++;
						AREG ^= BREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xA9: // XOR C
						numCycles++;
						AREG ^= CREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xAA: // XOR D
						numCycles++;
						AREG ^= DREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xAB: // XOR E
						numCycles++;
						AREG ^= EREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xAC: // XOR H
						numCycles++;
						AREG ^= HREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xAD: // XOR L
						numCycles++;
						AREG ^= LREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xAE: // XOR (HL)
						numCycles+=2;
						AREG ^= MEM[(HREG << 8) | LREG];
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
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
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xB1: // OR C
						numCycles++;
						AREG |= CREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xB2: // OR D
						numCycles++;
						AREG |= DREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xB3: // OR E
						numCycles++;
						AREG |= EREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xB4: // OR H
						numCycles++;
						AREG |= HREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xB5: // OR L
						numCycles++;
						AREG |= LREG;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xB6: // OR (HL)
						numCycles+=2;
						AREG |= MEM[(HREG << 8) | LREG];
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xB7: // OR A
						// A|A = A, no change
						numCycles++;
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xB8: // CP B
						numCycles++;
						FREG = FLAG_SUB[BREG][AREG];
					break;
					
					case 0xB9: // CP C
						numCycles++;
						FREG = FLAG_SUB[CREG][AREG];
					break;
					
					case 0xBA: // CP D
						numCycles++;
						FREG = FLAG_SUB[DREG][AREG];
					break;
					
					case 0xBB: // CP E
						numCycles++;
						FREG = FLAG_SUB[EREG][AREG];
					break;
					
					case 0xBC: // CP H
						numCycles++;
						FREG = FLAG_SUB[HREG][AREG];
					break;
					
					case 0xBD: // CP L
						numCycles++;
						FREG = FLAG_SUB[LREG][AREG];
					break;
					
					case 0xBE: // CP (HL)
						numCycles+=2;
						FREG = FLAG_SUB[ MEM[(HREG << 8) | LREG] ][AREG];
					break;
					
					case 0xBF: // CP A
						numCycles++;
						FREG = SUBTRACT | ZERO;
					break;
					
					case 0xC0: // RET NZ
						if ((FREG & ZERO) == 0)
						{
							numCycles+=5;
							PC = MEM[SP++] | (MEM[SP++] << 8);
						}
						else
							numCycles+=2;
					break;
					
					case 0xC1: // POP BC
						numCycles+=3;
						CREG = MEM[SP++];
						BREG = MEM[SP++];
					break;
					
					case 0xC2: // JP NZ,nn
						if ((FREG & ZERO) == 0)
						{
							numCycles+=4;
							PC = MEM[PC] | (MEM[PC+1] << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xC3: // JP nn
						numCycles+=4;
						PC = MEM[PC] | (MEM[PC+1] << 8);
					break;
					
					case 0xC4: // CALL NZ,nn
						if ((FREG & ZERO) == 0)
						{
							numCycles+=6;
							MEM[--SP] = (PC+2) >> 8;
							MEM[--SP] = (PC+2) & 0x00FF;
							PC = MEM[PC] | (MEM[PC+1] << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xC5: //PUSH BC
						numCycles+=4;
						MEM[--SP] = BREG;
						MEM[--SP] = CREG;
					break;
					
					case 0xC6: // ADD A,n
						numCycles+=2;
						val = MEM[PC++];
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG+val) & 0xFF;
					break;
					
					case 0xC7: // RST 00H
						numCycles += 4;
						MEM[--SP] = PC >> 8;
						MEM[--SP] = PC & 0x00FF;
						PC = 0x0000;
					break;
					
					case 0xC8: // RET Z
						if ((FREG & ZERO) != 0)
						{
							numCycles+=5;
							PC = MEM[SP++] | (MEM[SP++] << 8);
						}
						else
							numCycles+=2;
					break;
					
					case 0xC9: // RET
						numCycles+=4;
						PC = MEM[SP++] | (MEM[SP++] << 8);
					break;
					
					case 0xCB: // 2-byte opcodes
						switch (MEM[PC++])
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
								DREG = (DREG & BIT7) >> 3;
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
								LREG = (LREG & BIT7) >> 3;
								LREG = ((LREG << 1) | (LREG >> 7)) & 0xFF;
								if (LREG == 0)
									FREG |= ZERO;
							break;
							
							case 0x06: // RLC (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								val = MEM[index];
								FREG = (val & BIT7) >> 3;
								if ((MEM[index] = (((val << 1) | (val >> 7)) & 0xFF)) == 0)
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
								val = MEM[index];
								FREG = (val & BIT0) << 4;
								if ((MEM[index] = (((val >> 1) | (val << 7)) & 0xFF)) == 0)
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
								FREG = (MEM[index] & BIT7) >> 3;
								if ((MEM[index] = ((MEM[index] << 1) | val) & 0xFF) == 0)
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
								FREG = (MEM[index] & BIT0) << 4;
								if ((MEM[index] = (MEM[index] >> 1) | val) == 0)
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
								FREG = (MEM[index] & BIT7) >> 3;
								if ((MEM[index] = (MEM[index] << 1) & 0xFF) == 0)
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
								val = MEM[index];
								FREG = (val & BIT0) << 4;
								if ((MEM[index] = (val & BIT7) | (val >> 1)) == 0)
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
								if (BREG == 0)
									FREG = ZERO;
								else
									FREG = 0;
							break;
							
							case 0x31: // SWAP C
								numCycles+=2;
								val = CREG & 0x0F;
								CREG >>= 4;
								CREG |= (val << 4);
								if (CREG == 0)
									FREG = ZERO;
								else
									FREG = 0;
							break;
							
							case 0x32: // SWAP D
								numCycles+=2;
								val = DREG & 0x0F;
								DREG >>= 4;
								DREG |= (val << 4);
								if (DREG == 0)
									FREG = ZERO;
								else
									FREG = 0;
							break;
							
							case 0x33: // SWAP E
								numCycles+=2;
								val = EREG & 0x0F;
								EREG >>= 4;
								EREG |= (val << 4);
								if (EREG == 0)
									FREG = ZERO;
								else
									FREG = 0;
							break;
							
							case 0x34: // SWAP H
								numCycles+=2;
								val = HREG & 0x0F;
								HREG >>= 4;
								HREG |= (val << 4);
								if (HREG == 0)
									FREG = ZERO;
								else
									FREG = 0;
							break;
							
							case 0x35: // SWAP L
								numCycles+=2;
								val = LREG & 0x0F;
								LREG >>= 4;
								LREG |= (val << 4);
								if (LREG == 0)
									FREG = ZERO;
								else
									FREG = 0;
							break;
							
							case 0x36: // SWAP (HL)
								numCycles+=4;
								index = (HREG << 8) | LREG;
								val = MEM[index];
								val >>= 4;
								val |= ((MEM[index] & 0x0F) << 4);
								MEM[index] = val;
								if (val == 0)
									FREG = ZERO;
								else
									FREG = 0;
							break;
							
							case 0x37: // SWAP A
								numCycles+=2;
								val = AREG & 0x0F;
								AREG >>= 4;
								AREG |= (val << 4);
								if (AREG == 0)
									FREG = ZERO;
								else
									FREG = 0;
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
								FREG = (MEM[index] & BIT0) << 4;
								if ((MEM[index] >>= 1) == 0)
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
								FREG = (FREG & CARRY) | HALF_CARRY | ((~MEM[(HREG << 8) | LREG] & BIT0) << 7);
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
								FREG = (FREG & CARRY) | HALF_CARRY | ((~MEM[(HREG << 8) | LREG] & BIT1) << 6);
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
								FREG = (FREG & CARRY) | HALF_CARRY | ((~MEM[(HREG << 8) | LREG] & BIT2) << 5);
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
								FREG = (FREG & CARRY) | HALF_CARRY | ((~MEM[(HREG << 8) | LREG] & BIT3) << 4);
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
								FREG = (FREG & CARRY) | HALF_CARRY | ((~MEM[(HREG << 8) | LREG] & BIT4) << 3);
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
								FREG = (FREG & CARRY) | HALF_CARRY | ((~MEM[(HREG << 8) | LREG] & BIT5) << 2);
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
								FREG = (FREG & CARRY) | HALF_CARRY | ((~MEM[(HREG << 8) | LREG] & BIT6) << 1);
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
								FREG = (FREG & CARRY) | HALF_CARRY | (~MEM[(HREG << 8) | LREG] & BIT7);
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
								MEM[(HREG << 8) | LREG] &= ~BIT0;
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
								MEM[(HREG << 8) | LREG] &= ~BIT1;
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
								MEM[(HREG << 8) | LREG] &= ~BIT2;
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
								MEM[(HREG << 8) | LREG] &= ~BIT3;
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
								MEM[(HREG << 8) | LREG] &= ~BIT4;
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
								MEM[(HREG << 8) | LREG] &= ~BIT5;
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
								MEM[(HREG << 8) | LREG] &= ~BIT6;
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
								MEM[(HREG << 8) | LREG] &= ~BIT7;
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
								MEM[(HREG << 8) | LREG] |= BIT0;
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
								MEM[(HREG << 8) | LREG] |= BIT1;
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
								MEM[(HREG << 8) | LREG] |= BIT2;
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
								MEM[(HREG << 8) | LREG] |= BIT3;
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
								MEM[(HREG << 8) | LREG] |= BIT4;
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
								MEM[(HREG << 8) | LREG] |= BIT5;
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
								MEM[(HREG << 8) | LREG] |= BIT6;
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
								MEM[(HREG << 8) | LREG] |= BIT7;
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
							MEM[--SP] = (PC+2) >> 8;
							MEM[--SP] = (PC+2) & 0x00FF;
							PC = MEM[PC] | (MEM[PC+1] << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xCD: // CALL nn
						numCycles+=6;
						MEM[--SP] = (PC+2) >> 8;
						MEM[--SP] = (PC+2) & 0x00FF;
						PC = MEM[PC] | (MEM[PC+1] << 8);
					break;
					
					case 0xCE: //ADC A,n
						numCycles+=2;
						val = MEM[PC++] + ((FREG & CARRY) >> 4);	
						FREG = FLAG_ADD[val][AREG];
						AREG = (AREG+val) & 0xFF;
					break;
					
					case 0xCF: // RST 08H
						numCycles += 4;
						MEM[--SP] = PC >> 8;
						MEM[--SP] = PC & 0x00FF;
						PC = 0x0008;
					break;
					
					case 0xD0: // RET NC
						if ((FREG & CARRY) == 0)
						{
							numCycles+=5;
							PC = MEM[SP++] | (MEM[SP++] << 8);
						}
						else
							numCycles+=2;
					break;
					
					case 0xD1: // POP DE
						numCycles+=3;
						EREG = MEM[SP++];
						DREG = MEM[SP++];
					break;
					
					case 0xD2: // JP NC,nn
						if ((FREG & CARRY) == 0)
						{
							numCycles+=4;
							PC = MEM[PC] | (MEM[PC+1] << 8);
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
							MEM[--SP] = (PC+2) >> 8;
							MEM[--SP] = (PC+2) & 0x00FF;
							PC = MEM[PC] | (MEM[PC+1] << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
						
					case 0xD5: //PUSH DE
						numCycles+=4;
						MEM[--SP] = DREG;
						MEM[--SP] = EREG;
					break;
					
					case 0xD6: // SUB A,n
						numCycles+=2;
						val = MEM[PC++];
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG-val) &  0xFF;
					break;
					
					case 0xD7: // RST 10H
						numCycles += 4;
						MEM[--SP] = PC >> 8;
						MEM[--SP] = PC & 0x00FF;
						PC = 0x0010;
					break;
					
					case 0xD8: // RET C
						if ((FREG & CARRY) != 0)
						{
							numCycles+=5;
							PC = MEM[SP++] | (MEM[SP++] << 8);
						}
						else
							numCycles+=2;
					break;
					
					case 0xD9: // RETI
						numCycles+=4;
						PC = MEM[SP++] | (MEM[SP++] << 8);
						IME = true;
					break;
					
					case 0xDA: // JP C,nn
						if ((FREG & CARRY) != 0)
						{
							numCycles+=4;
							PC = MEM[PC] | (MEM[PC+1] << 8);
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
							MEM[--SP] = (PC+2) >> 8;
							MEM[--SP] = (PC+2) & 0x00FF;
							PC = MEM[PC] | (MEM[PC+1] << 8);
						}
						else
						{
							numCycles+=3;
							PC+=2;
						}
					break;
					
					case 0xDE: // SBC A,n
						numCycles+=2;
						val = MEM[PC++] + ((FREG & CARRY) >> 4);
						FREG = FLAG_SUB[val][AREG];
						AREG = (AREG - val) & 0xFF;
					break;
					
					case 0xDF: // RST 18H
						numCycles += 4;
						MEM[--SP] = PC >> 8;
						MEM[--SP] = PC & 0x00FF;
						PC = 0x0018;
					break;
					
					case 0xE0: //LDH (n),A **WRITE TO ADDRESS N**
						numCycles+=3;
						MEM[ 0xFF00 + MEM[PC++] ] = AREG;
					break;
					
					case 0xE1: // POP HL
						numCycles+=3;
						LREG = MEM[SP++];
						HREG = MEM[SP++];
					break;
						
					case 0xE2: //LD (C),A **WRITE TO IO C**
						numCycles+=2;
						MEM[ 0xFF00 + CREG ] = AREG;
					break;
					
					case 0xE5: // PUSH HL
						numCycles+=4;
						MEM[--SP] = HREG;
						MEM[--SP] = LREG;
					break;
					
					case 0xE6: // AND n
						numCycles+=2;
						AREG &= MEM[PC++];
						if (AREG == 0)
							FREG = ZERO | HALF_CARRY;
						else
							FREG = HALF_CARRY;
					break;
					
					case 0xE7: // RST 20H
						numCycles += 4;
						MEM[--SP] = PC >> 8;
						MEM[--SP] = PC & 0x00FF;
						PC = 0x0020;
					break;
					
					case 0xE8: //ADD SP,n **ignores half-carry**
						numCycles+=4;
						SP += (byte)MEM[PC++]; // signed immediate
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
						MEM[ MEM[PC++] | (MEM[PC++] << 8) ] = AREG;
					break;
					
					case 0xEE: // XOR n
						numCycles+=2;
						AREG ^= MEM[PC++];
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xEF: // RST 28H
						numCycles += 4;
						MEM[--SP] = PC >> 8;
						MEM[--SP] = PC & 0x00FF;
						PC = 0x0028;
					break;
					
					case 0xF0: //LDH (n),A **READ FROM ADDRESS N**
						numCycles+=3;
						AREG = MEM[ 0xFF00 + MEM[PC++] ];
					break;
					
					case 0xF1: // POP AF
						numCycles+=3;
						FREG = MEM[SP++];
						AREG = MEM[SP++];
					break;
					
					case 0xF2: //LD A,(C) **READ FROM IO C**
						numCycles += 2;
						AREG = MEM[ 0xFF00 + CREG ];
					break;
					
					case 0xF3: // DI
						numCycles++;
						IME = false; 
						// *Officially should occur 1 instruction later. We'll see how it works...*
					break;
					
					case 0xF5: // PUSH AF
						numCycles+=4;
						MEM[--SP] = AREG;
						MEM[--SP] = FREG;
					break;
					
					case 0xF6: // OR n
						numCycles+=2;
						AREG |= MEM[PC++];
						if (AREG == 0)
							FREG = ZERO;
						else
							FREG = 0;
					break;
					
					case 0xF7: // RST 30H
						numCycles += 4;
						MEM[--SP] = PC >> 8;
						MEM[--SP] = PC & 0x00FF;
						PC = 0x0030;
					break;
					
					case 0xF8: //LDHL SP,n **ignores half-carry**
						numCycles+=3;
						val = SP + (byte)MEM[PC++]; // signed immediate
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
						AREG = MEM[ MEM[PC++] | (MEM[PC++] << 8) ];
					break;
					
					case 0xFB: // EI
						numCycles++;
						IME = true; 
						// *Officially should occur 1 instruction later. We'll see how it works...*
					break;
					
					case 0xFE: // CP n
						numCycles+=2;
						FREG = FLAG_SUB[ MEM[PC++] ][AREG];
					break;
					
					case 0xFF: // RST 38H
						numCycles += 4;
						MEM[--SP] = PC >> 8;
						MEM[--SP] = PC & 0x00FF;
						PC = 0x0038;
					break;
					
					default: throw new AssertionError("Unsupported opcode");
				}
				
				/*
				if (numCycles >= CYCLES_PER_LINE)
				{
					numCycles -= CYCLES_PER_LINE;
					Handle h-blank interrupt
					
					if (scanline <= 143)
					{
						Draw scanline to VRAM
					}
					
					MEM[LY] = ++scanline;			
					if (scanline == 144)
					{
						Handle v-blank interrupt
					}
					
					Handle LYC register
				}
				*/
				
				/*
				val = scanline*CYCLES_PER_LINE + numCycles;
				
				if (timer is enabled)
				{
					MEM[0xFF05] = val >> #.
					if (MEM[0xFF05] > 0xFF)
					{
						Handle timer interrupt
						MEM[0xFF05] = MEM[0xFF06]; // TIMA = TMA
					}
				}
				
				MEM[0xFF04] = (val >> 6) & 0xFF;
				*/
			}
			
			// Inform GUI class to render Gameboy's VRAM to screen
			// MEM[LY] = scanline = 0; // new frame
		}
		
		// cannot end main loop while CPU is still running!
	}
}
