//Sound class
import java.io.*;
import javax.sound.sampled.*;

class SquareWaveGen{
	int sweepTime;
	boolean sweepDec;
	int sweepNum;
	
	public void setSweep(int val)
	{
		sweepTime = val & (CPU.BIT6 | CPU.BIT5 | CPU.BIT4);
		sweepDec = (val & CPU.BIT3) != 0;
		sweepNum = (val & CPU.BIT2 | CPU.BIT1 | CPU.BIT0);
	}
}

class VoluntaryWaveGen{
	
}

class NoiseGen{
	
}


public class Sound {
	
	SourceDataLine soundLine;
	int defaultSampleRate = 44100;
	int defaultBufferLength = 200;
	SquareWaveGen channel1;
	SquareWaveGen channel2;
	VoluntaryWaveGen channel3;
	NoiseGen channel4;
	boolean soundEnabled = false;
	boolean channel1Enable = true, channel2Enable = true,
			channel3Enable = true, channel4Enable = true;
			
}