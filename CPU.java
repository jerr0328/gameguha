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

public static void execute(int opcode)
{
	PC++;
	switch(opcode)
	{
		case 0x06: //inc pc
		case 0x
	}
}