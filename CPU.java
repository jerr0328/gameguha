public static int AREG;
public static int FREG;
public static int BREG;
public static int CREG;
public static int DREG;
public static int EREG;
public static int HREG;
public static int LREG;
public static int SP=0xFFFE; // GameBoy inits to 0xFFFE
public static int PC=0x0100; // will be 0x0100 by ROM
public static int[] MEM = new int[0x10000]; // 0xFFFF+1


public static void execute(int opcode)
{
	PC++;
	switch(opcode)
	{
		case 0x02: //LD (BC),A
			MEM[ (BREG << 8) | CREG ] = AREG;
		break;
		
		case 0x06: //LD B,n
			BREG = MEM[PC++];
		break;
		
		case 0x0A: //LD A,(BC)
			AREG = MEM[ ( BREG << 8) | CREG ];
		break;
			
		case 0x0E: //LD C,n
			CREG=MEM[PC++];
		break;
		
		case 0x12: //LD (DE),A
			MEM[ (DREG << 8) | EREG ] = AREG;
		break;
		
		case 0x16: //LD D,n
			DREG=MEM[PC++];
		break;
			
		case 0x1A: //LD A,(DE)
			AREG = MEM[ ( DREG << 8 ) | EREG ];
		break;
	
		case 0x1E: //LD E,n
			EREG=MEM[PC++];
		break;
		
		case 0x26: //LD H,n
			HREG=MEM[PC++];
		break;
	
		case 0x2E: //LD L,n
			LREG=MEM[PC++];
		break;
	
		case 0x36: //LD (HL),n
			MEM[ ( HREG << 8) | LREG ] = MEM[PC++];
		break;
				
		case 0x3E: //LD A,(0x00nn)
			AREG = MEM[ MEM[PC++] ];
		break;
					
		case 0x40: //LD B,B
		break;
		
		case 0x41: //LD B,C
			BREG=CREG;
		break;
		
		case 0x42: //LD B,D
			BREG=DREG;
		break;
		
		case 0x43: //LD B,E
			BREG=EREG;
		break;
		
		case 0x44: //LD B,H
			BREG=HREG;
		break;
		
		case 0x45: //LD B,L
			BREG=LREG;
		break;
		
		case 0x46: //LD B,(HL)
			BREG=MEM[ ( HREG << 8) | LREG ];
		break;
		
		case 0x47: //LD B,A
			BREG=AREG;
		break;
		
		case 0x48: //LD C,B
			CREG=BREG;
		break;
		
		case 0x49: //LD C,C
		break;
		
		case 0x4A: //LD C,D
			CREG=DREG;
		break;
		
		case 0x4B: //LD C,E
			CREG=EREG;
		break;
		
		case 0x4C: //LD C,H
			CREG=HREG;
		break
		
		case 0x4D: //LD C,L
			CREG=LREG;
		break;
		
		case 0x4E: //LD C,(HL)
			CREG=MEM[ ( HREG << 8) | LREG ];
		break;
		
		case 0x4F: //LD C,A
			CREG=AREG;
		break;
		
		case 0x50: //LD D,B
			DREG=BREG;
		
		case 0x51: //LD D,C
			DREG=CREG;
			
		case 0x52: //LD D,D
		break;
		
		case 0x53: //LD D,E
			DREG=EREG;
		break;
		
		case 0x54: //LD D,H
			DREG=HREG;
		break;
		
		case 0x55: //LD D,L
			DREG=LREG;
		break;
		
		case 0x56: //LD D,(HL)
			DREG=MEM[HL];
		break;
		
		case 0x57: //LD D,A
			DREG=AREG;
		break;
		
		case 0x58: //LD E,B
			EREG=BREG;
		break;
		
		case 0x59: //LD E,C
			EREG=CREG;
		break;
		
		case 0x5A: //LD E,D
			EREG=DREG;
		break;
		
		case 0x5B: //LD E,E
		break;
		
		case 0x5C: //LD E,H
			EREG=HREG;
		break;
		
		case 0x5D: //LD E,L
			EREG=LREG;
		break;
		
		case 0x5E: //LD E,(HL)
			EREG=MEM[ ( HREG << 8) | LREG ];
		break;
		
		case 0x5F: //LD E,A
			EREG=AREG;
		break;
		
		case 0x60: //LD H,B
			HREG=BREG;
		break;
		
		case 0x61: //LD H,C
			HREG=CREG;
		break;
		
		case 0x62: //LD H,D
			HREG=DREG;
		break;
		
		case 0x63: //LD H,E
			HREG=EREG;
		break;
		
		case 0x64: //LD H,H
		break;
		
		case 0x65: //LD H,L
			HREG=LREG;
		break;
		
		case 0x66: //LD H,(HL)
			HREG=MEM[ ( HREG << 8 ) | LREG ];
		break;
		
		case 0x67: //LD H,A
			HREG=AREG;
		break;
		
		case 0x68: //LD L,B
			LREG=BREG;
		break;
		
		case 0x69: //LD L,C
			LREG=CREG;
		break;
		
		case 0x6A: //LD L,D
			LREG=DREG;
		break;
		
		case 0x6B: //LD L,E
			LREG=EREG;
		break;
		
		case 0x6C: //LD L,H
			LREG=HREG;
			
		case 0x6D: //LD L,L
		break;
		
		case 0x6E: //LD L,(HL)
			LREG=MEM[ ( HREG << 8 ) | LREG ];
		break;
		
		case 0x6F: //LD L,A
			LREG=AREG;
		break;
		
		case 0x70: //LD (HL),B
			MEM[ ( HREG << 8 ) | LREG ] = BREG;
		break;
		
		case 0x71: //LD (HL),C
			MEM[ ( HREG << 8 ) | LREG ] = CREG;
		break;
		
		case 0x72: //LD (HL),D
			MEM[ ( HREG << 8 ) | LREG ] = DREG;
		break;
		
		case 0x73: //LD (HL),E
			MEM[ ( HREG << 8 ) | LREG ] = EREG;
		break;
		
		case 0x74: //LD (HL),H
			MEM[ ( HREG << 8 ) | LREG ] = HREG;
		break;
		
		case 0x75: //LD (HL),L
			MEM[ ( HREG << 8 ) | LREG ] = LREG;
		break;
	
		case 0x77: // LD (HL),A
			MEM[ ( HREG << 8 ) | LREG ] = AREG:
		break;
		
		case 0x78: //LD A,B
			AREG=BREG;
		break;
	
		case 0x79: //LD A,C
			AREG=CREG;
		break;
	
		case 0x7A: //LD A,D
			AREG=DREG;
		break;
	
		case 0x7B: //LD A,E
			AREG=EREG;
		break;
	
		case 0x7C: //LD A,H
			AREG=HREG;
		break;
	
		case 0x7D: //LD A,L
			AREG=LREG;
		break;
	
		case 0x7E: //LD A,(HL)
			AREG=MEM[ ( HREG << 8 ) | LREG ];
		break;
		
		case 0x7F: //LD A,A
		break;
		
		case 0xEA: //LD (nn),A
			MEM[ MEM[PC] | (MEM[PC+1] << 8) ] = AREG;
			PC += 2;
		break;
		
		case 0xFA: //LD A,(nn)
			AREG = MEM[ MEM[PC] | (MEM[PC+1] << 8) ];
			PC += 2;
			// Try: AREG = MEM[ MEM[PC++] | (MEM[PC++] << 8) ];
		break;
			
	}
}