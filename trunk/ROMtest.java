public class ROMtest {

    public static void main(String[] args) {
        ROM rom = new ROM(args[0]);
        rom.printTitle();
        rom.printCartType();
		rom.printRAMSize();
        System.out.print("Color: ");
        if(rom.isCGB())
            System.out.println("Yes");
        else
            System.out.println("No");
		rom.verifyChecksum();
	
		Thread cpu = new Thread(new CPU(rom));
		cpu.start();
		
		/* This should be in CPU sometime
		for(int i=0x100;i<0x4000;i++)
		{
			System.out.println("CPU MEMORY: "+CPU.getMem(0));
			System.out.format("Executing: 0x%02X @ 0x%02X\n",CPU.getMem(i),i);
			cpu.execute(rom.getMem(i));
			System.out.println("OK!");
		}*/
    }

}
