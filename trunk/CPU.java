public class CPU
{
	private static int AREG;
	private static int FREG;
	private static int BREG;
	private static int CREG;
	private static int DREG;
	private static int EREG;
	private static int HREG;
	private static int LREG;
	private static int SP=0xFFFE; // GameBoy inits to 0xFFFE
	private static int PC=0x0100; // will be 0x0100 by ROM
	private static int[] MEM = new int[0x10000]; // (== 0xFFFF+1 == 1<<16)
	private static int[][] FLAG_ADD;
	private static int[][] FLAG_SUB;
	private static int[] FLAG_INC;
	private static int[] FLAG_DEC;
	
	private static final int ZERO       = 0x80;
	private static final int SUBTRACT   = 0x40;
	private static final int HALF_CARRY = 0x20;
	private static final int CARRY      = 0x10;
	
	private static int numCycles = 0;
	
	public static int getMem(int index)
	{
		return MEM[index];
	}
	
	public static void writeMem(int index, int byteVal)
	{
		MEM[index] = byteVal;
	}
	
	public static void genFlagTable()
	{
		FLAG_ADD = new int[257][256]; // max 255 + 1 (carry) = 256
		FLAG_SUB = new int[257][256];
		
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
					FREG |= CARRY;
				}
				if (result == 0)
					FREG |= ZERO;
				FLAG_ADD[a][b] = flag;
				
				result = b-a;
				flag = SUBTRACT;
				if ((b & 0x0F) < (a & 0x0F)) // borrow from bit[4]
					flag |= HALF_CARRY;
				if (result < 0)
				{
					result &= 0xFF;
					FREG |= CARRY;
				}
				if (result == 0)
					FREG |= ZERO;
				FLAG_SUB[a][b] = flag;
			}
		
		FLAG_INC = new int[256];
		FLAG_DEC = new int[256];
		
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
	
	public static int execute(int opcode)
	{
		int val;
		byte offset;
		
		switch(opcode)
		{
			case 0x00: //NOP
				numCycles++;
			break;
			
			case 0x01: //LD BC,nn
				numCycles+=3;
				BREG = MEM[++PC];
				CREG = MEM[++PC];
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
				BREG = MEM[++PC];
			break;
			
			case 0x08: //LD (nn),SP
				numCycles+=5;
				val = ( MEM[++PC] << 8 ) | MEM[++PC];
				MEM[val] = SP >> 8;
				MEM[val+1] = SP & 0x00FF;
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
				CREG=MEM[++PC];
			break;
			
			case 0x11: //LD DE,nn
				numCycles+=3;
				DREG = MEM[++PC];
				EREG = MEM[++PC];
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
				DREG=MEM[++PC];
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
				EREG=MEM[++PC];
			break;
			
			case 0x21: //LD HL,nn
				numCycles+=3;
				HREG = MEM[++PC];
				LREG = MEM[++PC];
			break;
			
			case 0x22: //LDI (HL),A
				numCycles+=2;
				val = (HREG << 8) | LREG;
				MEM[val] = AREG;
				val = (val+1) & 0xFFFF;
				HREG = val >> 8;
				LREG = val & 0x00FF;
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
				HREG=MEM[++PC];
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
				val = (HREG << 8) | LREG;
				AREG = MEM[val];
				val = (val+1) & 0xFFFF;
				HREG = val >> 8;
				LREG = val & 0x00FF;
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
				LREG=MEM[++PC];
			break;
			
			case 0x31: //LD SP,nn
				numCycles+=3;
				SP = ( ( MEM[++PC] << 8 ) | MEM[++PC] );
			break;
			
			case 0x32: //LDD (HL),A
				numCycles+=2;
				val = (HREG << 8) | LREG;
				MEM[val] = AREG;
				val = (val-1) & 0xFFFF;
				HREG = val >> 8;
				LREG = val & 0x00FF;
			break;
			
			case 0x33: // INC SP
				numCycles+=2;
				SP = (SP+1) & 0xFFFF;
			break;
			
			case 0x34: // INC (HL)
				numCycles+=3;
				val = (HREG << 8) | LREG;
				FREG = FLAG_INC[MEM[val]] | (FREG & CARRY);
				MEM[val] = (MEM[val] + 1) & 0xFF;
			break;
			
			case 0x35: // DEC (HL)
				numCycles+=3;
				val = (HREG << 8) | LREG;
				FREG = FLAG_DEC[MEM[val]] | (FREG & CARRY);
				MEM[val] = (MEM[val] - 1) & 0xFF;
			break;
			
			case 0x36: //LD (HL),n
				numCycles+=3;
				MEM[ ( HREG << 8) | LREG ] = MEM[++PC];
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
				val = (HREG << 8) | LREG;
				AREG = MEM[val];
				val = (val-1) & 0xFFFF;
				HREG = val >> 8;
				LREG = val & 0x00FF;
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
				AREG = MEM[++PC];
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
			
			case 0xC1: // POP C1
				numCycles+=3;
				BREG = MEM[SP++];
				CREG = MEM[SP++];
			break;
			
			case 0xC5: //PUSH BC
				numCycles+=4;
				MEM[--SP] = CREG;
				MEM[--SP] = BREG;
			break;
			
			case 0xC6: // ADD A,n
				numCycles+=2;
				val = MEM[++PC];
				FREG = FLAG_ADD[val][AREG];
				AREG = (AREG+val) & 0xFF;
			break;
			
			case 0xCB: // 2-byte opcodes
				switch (MEM[++PC])
				{
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
						val = MEM[(HREG << 8) | LREG];
						val >>= 4;
						val |= ((MEM[(HREG << 8) | LREG] & 0x0F) << 4);
						MEM[(HREG << 8) | LREG] = val;
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
				}
			break;
			
			case 0xCE: //ADC A,n
				numCycles+=2;
				val = MEM[++PC] + ((FREG & CARRY) >> 4);	
				FREG = FLAG_ADD[val][AREG];
				AREG = (AREG+val) & 0xFF;
			break;
			
			case 0xD1: // POP DE
				numCycles+=3;
				DREG = MEM[SP++];
				EREG = MEM[SP++];
			break;
				
			case 0xD5: //PUSH DE
				numCycles+=4;
				MEM[--SP] = EREG;
				MEM[--SP] = DREG;
			break;
			
			case 0xD6: // SUB A,n
				numCycles+=2;
				val = MEM[++PC];
				FREG = FLAG_SUB[val][AREG];
				AREG = (AREG-val) &  0xFF;
			break;
			
			case 0xDE: // SBC A,n
				numCycles+=2;
				val = MEM[++PC] + ((FREG & CARRY) >> 4);
				FREG = FLAG_SUB[val][AREG];
				AREG = (AREG - val) & 0xFF;
			break;
			
			case 0xE0: //LDH (n),A **WRITE TO ADDRESS N**
				numCycles+=3;
				MEM[ 0xFF00 + MEM[++PC] ] = AREG;
			break;
			
			case 0xE1: // POP HL
				numCycles+=3;
				HREG = MEM[SP++];
				LREG = MEM[SP++];
			break;
				
			case 0xE2: //LD (C),A **WRITE TO IO C**
				numCycles+=2;
				MEM[ 0xFF00 + CREG ] = AREG;
			break;
			
			case 0xE5: // PUSH HL
				numCycles+=4;
				MEM[--SP] = LREG;
				MEM[--SP] = HREG;
			break;
			
			case 0xE6: // AND n
				numCycles+=2;
				AREG &= MEM[++PC];
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
			break;
			
			case 0xE8: //ADD SP,n **ignores half-carry**
				numCycles+=4;
				offset = (byte) MEM[++PC]; // signed immediate
				SP += offset;
				if (SP > 0xFFFF)
				{
					SP &= 0xFFFF;
					FREG = CARRY;
				}
				else
					FREG = 0;
			break;
			
			case 0xEA: //LD (nn),A
				numCycles+=4;
				MEM[ MEM[++PC] | (MEM[++PC] << 8) ] = AREG;
			break;
			
			case 0xEE: // XOR n
				numCycles+=2;
				AREG ^= MEM[++PC];
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
			break;
			
			case 0xF0: //LDH (n),A **READ FROM ADDRESS N**
				numCycles+=3;
				AREG = MEM[ 0xFF00 + MEM[++PC] ];
			break;
			
			case 0xF1: // POP AF
				numCycles+=3;
				AREG = MEM[SP++];
				FREG = MEM[SP++];
			break;
			
			case 0xF2: //LD A,(C) **READ FROM IO C**
				numCycles += 2;
				AREG = MEM[ 0xFF00 + CREG ];
			break;
			
			case 0xF5: // PUSH AF
				numCycles+=4;
				MEM[--SP] = FREG;
				MEM[--SP] = AREG;
			break;
			
			case 0xF6: // OR n
				numCycles+=2;
				AREG |= MEM[++PC];
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
			break;
			
			case 0xF8: //LDHL SP,n **ignores half-carry**
				numCycles+=3;
				offset = (byte) MEM[++PC]; // signed immediate
				val = SP+offset;
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
				AREG = MEM[ MEM[++PC] | (MEM[++PC] << 8) ];
			break;
			
			case 0xFE: // CP n
				numCycles+=2;
				FREG = FLAG_SUB[ MEM[++PC] ][AREG];
			break;
			
			default:
				System.out.format("Not implemented: %02X\n",opcode);
			break;
		}
		++PC;
		
		return numCycles;
	}
}
