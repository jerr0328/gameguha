//Sound class
import java.io.*;
import javax.sound.sampled.*;

class SquareWaveGen{
	int sweepTime;
	boolean sweepDec;
	int sweepNum;
	int sampleRate;
	int dutyCycle;
	int sndLen;
	int volInitial;
	boolean volDec;
	int sweepEnv;
	int frequency;
	int counterSweep;
	boolean counterExpire;
	
	
	public SquareWaveGen(int rate)
	{
		rate=sampleRate;
	}
	
	public void setSweep(int val)
	{
		sweepTime = val & (CPU.BIT6 | CPU.BIT5 | CPU.BIT4);
		sweepDec = (val & CPU.BIT3) != 0;
		sweepNum = (val & CPU.BIT2 | CPU.BIT1 | CPU.BIT0);
	}
	
	public void setDutyCycle(int val) 
	{
		val&= (CPU.BIT6 | CPU.BIT7);
		switch(val) //these are in 8ths 
		{
			case 0: dutyCycle = 1; //12.5%
				break;
			case 1: dutyCycle = 2; //25%
				break;
			case 2: dutyCycle = 4; //50%
				break;
			case 3: dutyCycle = 6; //75%
		}
	}
	
	public void setLength(int val)
	{
		val&= (CPU.BIT0 | CPU.BIT1 | CPU.BIT2 | CPU.BIT3 | CPU.BIT4 | CPU.BIT5);
		if(val==-1)
			sndLen=1;
		else
			sndLen = (64-val) * (1/256);
	}
	
	public void setEnvelope(int val)
	{
		volInitial = val & (CPU.BIT7 | CPU.BIT6 | CPU.BIT5 | CPU.BIT4);
		volDec = (val & CPU.BIT3) != 0;
		sweepEnv = val & (CPU.BIT2 | CPU.BIT1 | CPU.BIT0);
		
		if(sweepEnv == 0)
			volInitial = 0;
	}
	
	public void setFrequencyLo(int val)
	{
		frequency = val << 8; // Low 8
		
	}
	
	public void setFrequencyHi(int val)
	{
		frequency = val & (CPU.BIT2 | CPU.BIT1 | CPU.BIT0);
		counterSweep = val & CPU.BIT6;
		if(counterSweep == 1)
			counterExpire = true;
		if((val & CPU.BIT7)==1)
			setLength(1);
		
		
	}
	
	public void setFrequency()
	{
		
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
			
	public Sound()
	{
		channel1 = new SquareWaveGen(defaultSampleRate);
	}
			
}