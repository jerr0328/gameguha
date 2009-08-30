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
		case 0x06: // LD B,n
			BREG = MEM[PC++];
		break;
		
		case 0x0E: // LD C,n
			CREG=MEM[PC++];
		break;
	
		case 0x16: // LD D,n
			DREG=MEM[PC++];
		break;
	
		case 0x1E: // LD E,n
			EREG=MEM[PC++];
		break;
	
		case 0x26: // LD H,n
			HREG=MEM[PC++];
		break;
	
		case 0x2E: //LD L,n
			LREG=MEM[PC++];
		break;
	
		case 0x7F: //LD A,A
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
			AREG=MEM[( HREG << 8) | LREG];
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
			BREG=MEM[( HREG << 8) | LREG];
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
			CREG=MEM[( HREG << 8) | LREG];
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
			EREG=MEM[( HREG << 8) | LREG];
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
			HREG=MEM[( HREG << 8) | LREG];
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
			LREG=MEM[( HREG << 8) | LREG];
		break;
		
		
	}
}