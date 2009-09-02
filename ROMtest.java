public class ROMtest {

    public static void main(String[] args) {
        ROM rom = new ROM(args[0]);
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
    }

}
