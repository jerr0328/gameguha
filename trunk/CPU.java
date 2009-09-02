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
	
	private static final int ZERO       = 0x80;
	private static final int SUBTRACT   = 0x40;
	private static final int HALF_CARRY = 0x20;
	private static final int CARRY      = 0x10;
	
	private static int numCycles = 0;
	private static int val = 0;
	
	public static void genFlagTable()
	{
		FLAG_ADD = new int[256][257]; // max 255 + 1 (carry) = 256
		FLAG_SUB = new int[256][257];
		
		int result, flag;
		for (int a = 0; a <= 255; a++)
			for (int b = 0; b <= 256; b++)
			{
				result = a+b;
				flag = 0;
				if ((a & 0x0F) + (b & 0x0F) > 0x0F) // carry from bit[3]
					flag |= HALF_CARRY;
				if (result > 0xFF)
				{
					result &= 0xFF;
					FREG |= CARRY;
				}
				if (result == 0)
					FREG |= ZERO;
				FLAG_ADD[a][b] = flag;
				
				result = a-b;
				flag = SUBTRACT;
				if ((a & 0x0F) < (b & 0x0F)) // borrow from bit[4]
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
	}
	
	public static int execute(int opcode)
	{
		switch(opcode)
		{
			case 0x01: //LD BC,nn
				BREG = MEM[++PC];
				CREG = MEM[++PC];
				numCycles+=3;
			break;
			
			case 0x02: //LD (BC),A
				MEM[ (BREG << 8) | CREG ] = AREG;
				numCycles+=2;
			break;
			
			case 0x06: //LD B,n
				BREG = MEM[++PC];
				numCycles+=2;
			break;
			
			case 0x08: //LD (nn),SP
				MEM[ ( MEM[++PC] << 8 ) | MEM[++PC] ] = SP;
				numCycles+=5;
			break;
			
			case 0x0A: //LD A,(BC)
				AREG = MEM[ ( BREG << 8) | CREG ];
				numCycles+=2;
			break;
				
			case 0x0E: //LD C,n
				CREG=MEM[++PC];
				numCycles+=2;
			break;
			
			case 0x11: //LD DE,nn
				DREG = MEM[++PC];
				EREG = MEM[++PC];
				numCycles+=3;
			break;
			
			case 0x12: //LD (DE),A
				MEM[ (DREG << 8) | EREG ] = AREG;
				numCycles+=2;
			break;
			
			case 0x16: //LD D,n
				DREG=MEM[++PC];
				numCycles+=2;
			break;
				
			case 0x1A: //LD A,(DE)
				AREG = MEM[ ( DREG << 8 ) | EREG ];
				numCycles+=2;
			break;
		
			case 0x1E: //LD E,n
				EREG=MEM[++PC];
				numCycles+=2;
			break;
			
			case 0x26: //LD H,n
				HREG=MEM[++PC];
				numCycles+=2;
			break;
		
			case 0x21: //LD HL,nn
				HREG = MEM[++PC];
				LREG = MEM[++PC];
				numCycles+=3;
			break;
			
			case 0x22: //LDI (HL),A
				MEM[ ( HREG << 8 ) | LREG ] = AREG;
				if (LREG == 0xFF)
				{
					if(HREG == 0xFF)
						HREG = 0;
					else 
						HREG++;
					LREG = 0;
				}
				else
					LREG++;
				numCycles+=2;
			break;	
			
			case 0x2A: //LDI A,(HL)
				AREG = MEM [ (HREG << 8 ) | LREG ];
				if (LREG == 0xFF)
				{
					if(HREG == 0xFF)
						HREG = 0;
					else 
						HREG++;
					LREG = 0;
				}
				else
					LREG++;
				numCycles+=2;
			break;
				
			case 0x2E: //LD L,n
				LREG=MEM[++PC];
				numCycles+=2;
			break;
			
			case 0x31: //LD SP,nn
				SP = ( ( MEM[++PC] << 8 ) | MEM[++PC] );
				numCycles+=3;
			break;
			
			case 0x32: //LDD (HL),A
				MEM[ (HREG << 8 ) | LREG ] = AREG;
				if (LREG == 0)
				{
					if (HREG == 0)
						HREG = 0xFF;
					else
						HREG--;
					LREG=0xFF;
				}
				else
					LREG--;
				numCycles+=2;
			break;
				
			case 0x3A: //LDD A,(HL)
				AREG = MEM[ (HREG << 8 ) | LREG ];
				if (LREG == 0)
				{
					if (HREG == 0)
						HREG = 0xFF;
					else
						HREG--;
					LREG=0xFF;
				}
				else
					LREG--;
				numCycles+=2;
			break;
		
			case 0x36: //LD (HL),n
				MEM[ ( HREG << 8) | LREG ] = MEM[++PC];
				numCycles+=3;
			break;
					
			case 0x3E: //LD A,n
				AREG = MEM[++PC];
				numCycles+=2;
			break;
						
			case 0x40: //LD B,B
				numCycles++;
			break;
			
			case 0x41: //LD B,C
				BREG=CREG;
				numCycles++;
			break;
			
			case 0x42: //LD B,D
				BREG=DREG;
				numCycles++;
			break;
			
			case 0x43: //LD B,E
				BREG=EREG;
				numCycles++;
			break;
			
			case 0x44: //LD B,H
				BREG=HREG;
				numCycles++;
			break;
			
			case 0x45: //LD B,L
				BREG=LREG;
				numCycles++;
			break;
			
			case 0x46: //LD B,(HL)
				BREG=MEM[ ( HREG << 8) | LREG ];
				numCycles+=2;
			break;
			
			case 0x47: //LD B,A
				BREG=AREG;
				numCycles++;
			break;
			
			case 0x48: //LD C,B
				CREG=BREG;
				numCycles++;
			break;
			
			case 0x49: //LD C,C
				numCycles++;
			break;
			
			case 0x4A: //LD C,D
				CREG=DREG;
				numCycles++;
			break;
			
			case 0x4B: //LD C,E
				CREG=EREG;
				numCycles++;
			break;
			
			case 0x4C: //LD C,H
				CREG=HREG;
				numCycles++;
			break;
			
			case 0x4D: //LD C,L
				CREG=LREG;
				numCycles++;
			break;
			
			case 0x4E: //LD C,(HL)
				CREG=MEM[ ( HREG << 8) | LREG ];
				numCycles+=2;
			break;
			
			case 0x4F: //LD C,A
				CREG=AREG;
				numCycles++;
			break;
			
			case 0x50: //LD D,B
				DREG=BREG;
				numCycles++;
			break;
			
			case 0x51: //LD D,C
				DREG=CREG;
				numCycles++;
			break;
				
			case 0x52: //LD D,D
				numCycles++;
			break;
			
			case 0x53: //LD D,E
				DREG=EREG;
				numCycles++;
			break;
			
			case 0x54: //LD D,H
				DREG=HREG;
				numCycles++;
			break;
			
			case 0x55: //LD D,L
				DREG=LREG;
				numCycles++;
			break;
			
			case 0x56: //LD D,(HL)
				DREG=MEM[ ( HREG << 8) | LREG ];
				numCycles+=2;
			break;
			
			case 0x57: //LD D,A
				DREG=AREG;
				numCycles++;
			break;
			
			case 0x58: //LD E,B
				EREG=BREG;
				numCycles++;
			break;
			
			case 0x59: //LD E,C
				EREG=CREG;
				numCycles++;
			break;
			
			case 0x5A: //LD E,D
				EREG=DREG;
				numCycles++;
			break;
			
			case 0x5B: //LD E,E
				numCycles++;
			break;
			
			case 0x5C: //LD E,H
				EREG=HREG;
				numCycles++;
			break;
			
			case 0x5D: //LD E,L
				EREG=LREG;
				numCycles++;
			break;
			
			case 0x5E: //LD E,(HL)
				EREG=MEM[ ( HREG << 8) | LREG ];
				numCycles+=2;
			break;
			
			case 0x5F: //LD E,A
				EREG=AREG;
				numCycles++;
			break;
			
			case 0x60: //LD H,B
				HREG=BREG;
				numCycles++;
			break;
			
			case 0x61: //LD H,C
				HREG=CREG;
				numCycles++;
			break;
			
			case 0x62: //LD H,D
				HREG=DREG;
				numCycles++;
			break;
			
			case 0x63: //LD H,E
				HREG=EREG;
				numCycles++;
			break;
			
			case 0x64: //LD H,H
				numCycles++;
			break;
			
			case 0x65: //LD H,L
				HREG=LREG;
				numCycles++;
			break;
			
			case 0x66: //LD H,(HL)
				HREG=MEM[ ( HREG << 8 ) | LREG ];
				numCycles+=2;
			break;
			
			case 0x67: //LD H,A
				HREG=AREG;
				numCycles++;
			break;
			
			case 0x68: //LD L,B
				LREG=BREG;
				numCycles++;
			break;
			
			case 0x69: //LD L,C
				LREG=CREG;
				numCycles++;
			break;
			
			case 0x6A: //LD L,D
				LREG=DREG;
				numCycles++;
			break;
			
			case 0x6B: //LD L,E
				LREG=EREG;
				numCycles++;
			break;
			
			case 0x6C: //LD L,H
				LREG=HREG;
				numCycles++;
			break;
				
			case 0x6D: //LD L,L
				numCycles++;
			break;
			
			case 0x6E: //LD L,(HL)
				LREG=MEM[ ( HREG << 8 ) | LREG ];
				numCycles+=2;
			break;
			
			case 0x6F: //LD L,A
				LREG=AREG;
				numCycles++;
			break;
			
			case 0x70: //LD (HL),B
				MEM[ ( HREG << 8 ) | LREG ] = BREG;
				numCycles+=2;
			break;
			
			case 0x71: //LD (HL),C
				MEM[ ( HREG << 8 ) | LREG ] = CREG;
				numCycles+=2;
			break;
			
			case 0x72: //LD (HL),D
				MEM[ ( HREG << 8 ) | LREG ] = DREG;
				numCycles+=2;
			break;
			
			case 0x73: //LD (HL),E
				MEM[ ( HREG << 8 ) | LREG ] = EREG;
				numCycles+=2;
			break;
			
			case 0x74: //LD (HL),H
				MEM[ ( HREG << 8 ) | LREG ] = HREG;
				numCycles+=2;
			break;
			
			case 0x75: //LD (HL),L
				MEM[ ( HREG << 8 ) | LREG ] = LREG;
				numCycles+=2;
			break;
		
			case 0x77: // LD (HL),A
				MEM[ ( HREG << 8 ) | LREG ] = AREG;
				numCycles+=2;
			break;
			
			case 0x78: //LD A,B
				AREG=BREG;
				numCycles++;
			break;
		
			case 0x79: //LD A,C
				AREG=CREG;
				numCycles++;
			break;
		
			case 0x7A: //LD A,D
				AREG=DREG;
				numCycles++;
			break;
		
			case 0x7B: //LD A,E
				AREG=EREG;
				numCycles++;
			break;
		
			case 0x7C: //LD A,H
				AREG=HREG;
				numCycles++;
			break;
		
			case 0x7D: //LD A,L
				AREG=LREG;
				numCycles++;
			break;
		
			case 0x7E: //LD A,(HL)
				AREG=MEM[ ( HREG << 8 ) | LREG ];
				numCycles+=2;
			break;
			
			case 0x7F: //LD A,A
				numCycles++;
			break;
			
			case 0x80: //ADD A,B
				FREG = FLAG_ADD[AREG][BREG];
				AREG = (AREG+BREG) & 0xFF;
				numCycles++;
			break;
			
			case 0x81: //ADD A,C
				FREG = FLAG_ADD[AREG][CREG];
				AREG = (AREG+CREG) & 0xFF;
				numCycles++;
			break;
			
			case 0x82: //ADD A,D
				FREG = FLAG_ADD[AREG][DREG];
				AREG = (AREG+DREG) & 0xFF;
				numCycles++;
			break;
			
			case 0x83: //ADD A,E
				FREG = FLAG_ADD[AREG][EREG];
				AREG = (AREG+EREG) & 0xFF;
				numCycles++;
			break;
			
			case 0x84: //ADD A,H
				FREG = FLAG_ADD[AREG][HREG];
				AREG = (AREG+LREG) & 0xFF;
				numCycles++;
			break;
			
			case 0x85: //ADD A,L
				FREG = FLAG_ADD[AREG][LREG];
				AREG = (AREG+LREG) & 0xFF;
				numCycles++;
			break;
			
			case 0x86: // ADD A,(HL)
				val = MEM[(HREG << 8) | LREG];
				FREG = FLAG_ADD[AREG][val];
				AREG = (AREG+val) & 0xFF;
				numCycles+=2;
			break;
			
			case 0x87: //ADD A,A
				FREG = FLAG_ADD[AREG][AREG];
				AREG = (AREG+AREG) & 0xFF;
				numCycles++;
			break;
			
			case 0x88: //ADC A,B
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_ADD[AREG][BREG+1];
					AREG = (AREG + BREG+1) & 0xFF;
				}
				else
				{
					FREG = FLAG_ADD[AREG][BREG];
					AREG = (AREG + BREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x89: //ADC A,C
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_ADD[AREG][CREG+1];
					AREG = (AREG + CREG+1) & 0xFF;
				}
				else
				{
					FREG = FLAG_ADD[AREG][CREG];
					AREG = (AREG + CREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x8A: //ADC A,D
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_ADD[AREG][DREG+1];
					AREG = (AREG + DREG+1) & 0xFF;
				}
				else
				{
					FREG = FLAG_ADD[AREG][DREG];
					AREG = (AREG + DREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x8B: //ADC A,E
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_ADD[AREG][EREG+1];
					AREG = (AREG + EREG+1) & 0xFF;
				}
				else
				{
					FREG = FLAG_ADD[AREG][EREG];
					AREG = (AREG + EREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x8C: //ADC A,H
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_ADD[AREG][HREG+1];
					AREG = (AREG + HREG+1) & 0xFF;
				}
				else
				{
					FREG = FLAG_ADD[AREG][HREG];
					AREG = (AREG + HREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x8D: //ADC A,L
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_ADD[AREG][LREG+1];
					AREG = (AREG + LREG+1) & 0xFF;
				}
				else
				{
					FREG = FLAG_ADD[AREG][LREG];
					AREG = (AREG + LREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x8E: //ADC A,(HL)
				if ((FREG & CARRY) != 0)
					val = MEM[(HREG << 8) | LREG] + 1;
				else
					val = MEM[(HREG << 8) | LREG];	
				FREG = FLAG_ADD[AREG][val];
				AREG = (AREG+val) & 0xFF;
				numCycles+=2;
			break;
			
			case 0x8F: //ADC A,A
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_ADD[AREG][AREG+1];
					AREG = (AREG + AREG+1) & 0xFF;
				}
				else
				{
					FREG = FLAG_ADD[AREG][AREG];
					AREG = (AREG + AREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x90: // SUB A,B
				FREG = FLAG_SUB[AREG][BREG];
				AREG = (AREG-BREG) &  0xFF;
				numCycles++;
			break;
			
			case 0x91: // SUB A,C
				FREG = FLAG_SUB[AREG][CREG];
				AREG = (AREG-CREG) &  0xFF;
				numCycles++;
			break;
			
			case 0x92: // SUB A,D
				FREG = FLAG_SUB[AREG][DREG];
				AREG = (AREG-DREG) &  0xFF;
				numCycles++;
			break;
			
			case 0x93: // SUB A,E
				FREG = FLAG_SUB[AREG][EREG];
				AREG = (AREG-EREG) &  0xFF;
				numCycles++;
			break;
			
			case 0x94: // SUB A,H
				FREG = FLAG_SUB[AREG][HREG];
				AREG = (AREG-HREG) &  0xFF;
				numCycles++;
			break;
			
			case 0x95: // SUB A,L
				FREG = FLAG_SUB[AREG][LREG];
				AREG = (AREG-LREG) &  0xFF;
				numCycles++;
			break;
			
			case 0x96: // SUB A,(HL)
				val = MEM[(HREG << 8) | LREG];
				FREG = FLAG_SUB[AREG][val];
				AREG = (AREG-val) &  0xFF;
				numCycles+=2;
			break;
			
			case 0x97: // SUB A,A
				FREG = SUBTRACT | ZERO;
				AREG = 0;
				numCycles++;
			break;
			
			case 0x98: // SBC A,B
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_SUB[AREG][BREG+1];
					AREG = (AREG - (BREG+1)) & 0xFF;
				}
				else
				{
					FREG = FLAG_SUB[AREG][BREG];
					AREG = (AREG - BREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x99: // SBC A,C
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_SUB[AREG][CREG+1];
					AREG = (AREG - (CREG+1)) & 0xFF;
				}
				else
				{
					FREG = FLAG_SUB[AREG][CREG];
					AREG = (AREG - CREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x9A: // SBC A,D
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_SUB[AREG][DREG+1];
					AREG = (AREG - (DREG+1)) & 0xFF;
				}
				else
				{
					FREG = FLAG_SUB[AREG][DREG];
					AREG = (AREG - DREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x9B: // SBC A,E
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_SUB[AREG][EREG+1];
					AREG = (AREG - (EREG+1)) & 0xFF;
				}
				else
				{
					FREG = FLAG_SUB[AREG][EREG];
					AREG = (AREG - EREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x9C: // SBC A,H
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_SUB[AREG][HREG+1];
					AREG = (AREG - (HREG+1)) & 0xFF;
				}
				else
				{
					FREG = FLAG_SUB[AREG][HREG];
					AREG = (AREG - HREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x9D: // SBC A,L
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_SUB[AREG][LREG+1];
					AREG = (AREG - (LREG+1)) & 0xFF;
				}
				else
				{
					FREG = FLAG_SUB[AREG][LREG];
					AREG = (AREG - LREG) & 0xFF;
				}
				numCycles++;
			break;
			
			case 0x9E: // SBC A,(HL)
				if ((FREG & CARRY) != 0)
					val = MEM[(HREG << 8) | LREG] + 1;
				else
					val = MEM[(HREG << 8) | LREG];
				
				FREG = FLAG_SUB[AREG][val];
				AREG = (AREG - val) & 0xFF;
				numCycles+=2;
			break;
			
			case 0x9F: // SBC A,A
				if ((FREG & CARRY) != 0)
				{
					FREG = FLAG_SUB[AREG][AREG+1];
					AREG = 0xFF;
				}
				else
				{
					FREG = SUBTRACT | ZERO;
					AREG = 0;
				}
				numCycles++;
			break;
			
			case 0xA0: // AND B
				AREG &= BREG;
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
				numCycles++;
			break;
			
			case 0xA1: // AND C
				AREG &= CREG;
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
				numCycles++;
			break;
			
			case 0xA2: // AND D
				AREG &= DREG;
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
				numCycles++;
			break;
			
			case 0xA3: // AND E
				AREG &= EREG;
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
				numCycles++;
			break;
			
			case 0xA4: // AND H
				AREG &= HREG;
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
				numCycles++;
			break;
			
			case 0xA5: // AND L
				AREG &= LREG;
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
				numCycles++;
			break;
			
			case 0xA6: // AND (HL)
				AREG &= MEM[(HREG << 8) | LREG];
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
				numCycles+=2;
			break;
			
			case 0xA7: // AND A
				// A&A = A, no change
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
				numCycles++;
			break;
			
			case 0xA8: // XOR B
				AREG ^= BREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xA9: // XOR C
				AREG ^= CREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xAA: // XOR D
				AREG ^= DREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xAB: // XOR E
				AREG ^= EREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xAC: // XOR H
				AREG ^= HREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xAD: // XOR L
				AREG ^= LREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xAE: // XOR (HL)
				AREG ^= MEM[(HREG << 8) | LREG];
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles+=2;
			break;
			
			case 0xAF: // XOR A
				// A^A = 0
				AREG = 0;
				FREG = ZERO;
				numCycles++;
			break;
			
			case 0xB0: // OR B
				AREG |= BREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xB1: // OR C
				AREG |= CREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xB2: // OR D
				AREG |= DREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xB3: // OR E
				AREG |= EREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xB4: // OR H
				AREG |= HREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xB5: // OR L
				AREG |= LREG;
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xB6: // OR (HL)
				AREG |= MEM[(HREG << 8) | LREG];
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles+=2;
			break;
			
			case 0xB7: // OR A
				// A|A = A, no change
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles++;
			break;
			
			case 0xB8: // CP B
				FREG = FLAG_SUB[AREG][BREG];
				numCycles++;
			break;
			
			case 0xB9: // CP C
				FREG = FLAG_SUB[AREG][CREG];
				numCycles++;
			break;
			
			case 0xBA: // CP D
				FREG = FLAG_SUB[AREG][DREG];
				numCycles++;
			break;
			
			case 0xBB: // CP E
				FREG = FLAG_SUB[AREG][EREG];
				numCycles++;
			break;
			
			case 0xBC: // CP H
				FREG = FLAG_SUB[AREG][HREG];
				numCycles++;
			break;
			
			case 0xBD: // CP L
				FREG = FLAG_SUB[AREG][LREG];
				numCycles++;
			break;
			
			case 0xBE: // CP (HL)
				FREG = FLAG_SUB[AREG][ MEM[(HREG << 8) | LREG] ];
				numCycles+=2;
			break;
			
			case 0xBF: // CP A
				FREG = SUBTRACT | ZERO;
				numCycles++;
			break;
			
			case 0xC1: // POP C1
				BREG = MEM[SP++];
				CREG = MEM[SP++];
				numCycles+=3;
			break;
			
			case 0xC5: //PUSH BC
				MEM[--SP] = CREG;
				MEM[--SP] = BREG;
				numCycles+=4;
			break;
			
			case 0xC6: // ADD A,n
				val = MEM[++PC];
				FREG = FLAG_ADD[AREG][val];
				AREG = (AREG+val) & 0xFF;
				numCycles+=2;
			break;
			
			case 0xCE: //ADC A,n
				if ((FREG & CARRY) != 0)
					val = MEM[++PC] + 1;
				else
					val = MEM[++PC];	
				FREG = FLAG_ADD[AREG][val];
				AREG = (AREG+val) & 0xFF;
				numCycles+=2;
			break;
			
			case 0xD1: // POP DE
				DREG = MEM[SP++];
				EREG = MEM[SP++];
				numCycles+=3;
			break;
				
			case 0xD5: //PUSH DE
				MEM[--SP] = EREG;
				MEM[--SP] = DREG;
				numCycles+=4;
			break;
			
			case 0xD6: // SUB A,n
				val = MEM[++PC];
				FREG = FLAG_SUB[AREG][val];
				AREG = (AREG-val) &  0xFF;
				numCycles+=2;
			break;
			
			case 0xDE: // SBC A,n
				if ((FREG & CARRY) != 0)
					val = MEM[++PC] + 1;
				else
					val = MEM[++PC];
				
				FREG = FLAG_SUB[AREG][val];
				AREG = (AREG - val) & 0xFF;
				numCycles+=2;
			break;
			
			case 0xE0: //LDH (n),A **WRITE TO ADDRESS N**
				MEM[ 0xFF00 + MEM[++PC] ] = AREG;
				numCycles+=3;
			break;
			
			case 0xE1: // POP HL
				HREG = MEM[SP++];
				LREG = MEM[SP++];
				numCycles+=3;
			break;
				
			case 0xE2: //LD (C),A **WRITE TO IO C**
				MEM[ 0xFF00 + CREG ] = AREG;
				numCycles+=2;
			break;
			
			case 0xE5: // PUSH HL
				MEM[--SP] = LREG;
				MEM[--SP] = HREG;
				numCycles+=4;
			break;
			
			case 0xE6: // AND n
				AREG &= MEM[++PC];
				if (AREG == 0)
					FREG = ZERO | HALF_CARRY;
				else
					FREG = HALF_CARRY;
				numCycles+=2;
			break;
			
			case 0xEA: //LD (nn),A
				MEM[ MEM[++PC] | (MEM[++PC] << 8) ] = AREG;
				numCycles+=4;
			break;
			
			case 0xEE: // XOR n
				AREG ^= MEM[++PC];
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles+=2;
			break;
			
			case 0xF0: //LDH (n),A **READ FROM ADDRESS N**
				AREG = MEM[ 0xFF00 + MEM[++PC] ];
				numCycles+=3;
			break;
			
			case 0xF1: // POP AF
				AREG = MEM[SP++];
				FREG = MEM[SP++];
				numCycles+=3;
			break;
			
			case 0xF2: //LD A,(C) **READ FROM IO C**
				AREG = MEM[ 0xFF00 + CREG ];
				numCycles += 2;
			break;
			
			case 0xF5: // PUSH AF
				MEM[--SP] = FREG;
				MEM[--SP] = AREG;
				numCycles+=4;
			break;
			
			case 0xF6: // OR n
				AREG |= MEM[++PC];
				if (AREG == 0)
					FREG = ZERO;
				else
					FREG = 0;
				numCycles+=2;
			break;
			
			case 0xF8: //LDHL SP,n **ignores half-carry**
				byte offset = (byte) MEM[++PC]; // signed immediate
				
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
				numCycles+=3;
			break;
			
			case 0xF9: //LD SP,HL
				SP = ( (HREG << 8 ) | LREG );
				numCycles+=2;
			break;
			 
			case 0xFA: //LD A,(nn)
				AREG = MEM[ MEM[++PC] | (MEM[++PC] << 8) ];
				numCycles+=4;
			break;
			
			case 0xFE: // CP n
				FREG = FLAG_SUB[AREG][ MEM[++PC] ];
				numCycles+=2;
			break;
		}
		++PC;
		
		return numCycles;
	}
}
