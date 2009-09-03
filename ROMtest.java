public class ROMtest {

    public static void main(String[] args) {
        ROM rom = new ROM(args[0]);
		CPU cpu = new CPU();
        rom.printTitle();
        rom.printCartType();
		rom.printROMSize();
		rom.printRAMSize();
        System.out.print("Color: ");
        if(rom.isCGB())
            System.out.println("Yes");
        else
            System.out.println("No");
		rom.verifyChecksum();
		cpu.genFlagTable();
		for(int i=0x100;i<0x4000;i++)
		{
			System.out.format("Executing: 0x%02X @ 0x%02X\n",rom.getMem(i),i);
			cpu.execute(rom.getMem(i));
			System.out.println("OK!");
		}
    }

}
