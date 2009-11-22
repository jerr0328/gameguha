//Sound class
import java.io.*;
import javax.sound.sampled.*;
import java.util.concurrent.*; 

class SquareWaveGen
{
	
	final static int CHAN_LEFT = 1;
	final static int CHAN_RIGHT = 2;
	final static int CHAN_MONO = 4;
	
	int sampleRate; // starting sampleRate
	final double lenDiv = 1/256.0;
	int cyclePos; //Position of waveform in samples
	int dutyCycle; // amount of time sample stays high, in eights
	double soundLength; // length of sound in frames
	int initialVolume; // initial amplitude
	int increaseEnvelope; // 1 = increasing amplitude, 0 = decreasing
	int numStepsEnvelope; // number of envelope steps 
	int amplitude; // amplitude of waveform
	int frequency; // frequency that is stored in gameboy format
	int counterSelect;
	int counterEnvelope; // position in envelope
	int cycleLength; // length of waveform in samples
	int channel; // channel to be played from .. L/R/Mono
	int timeSweep; // amount of time between a sweep step
	int numSweep; // number of sweep steps
	int decreaseSweep; // 1 = decrease frequency, otherwise increase
	int counterSweep; // position in the sweep


		public SquareWaveGen(int waveLen, int amp, int duty, int chan, int rate)
		{
			cycleLength = waveLen;
			amplitude = amp;
			cyclePos = 0;
			dutyCycle = duty;
			channel = chan;
			sampleRate = rate;
		}
	
		public SquareWaveGen(int sampleRate)
		{
			cyclePos = 0;
			this.sampleRate = sampleRate;
			channel = CHAN_LEFT | CHAN_RIGHT;
			dutyCycle = 4;
			soundLength = 0;
			amplitude = 32;
			cycleLength = 2;
			counterSweep = 0;
		
		
		}
	
		public void setSweep(int time, int num, int decrease)
		{
			timeSweep = (time + 1) >> 2;
			numSweep = num;
			decreaseSweep = decrease;
			counterSweep = 0;
		}

		public void setSoundLength(int len)
		{
			if(len == -1)
			soundLength = -1;
		
			soundLength = (64 - len) * lenDiv;
		}

		public void setWavePatternDuty(int duty)
		{
			switch(duty)
			{
				case 0: dutyCycle = 1;
					break;
				case 1: dutyCycle = 2;
					break;
				case 2: dutyCycle = 4;
					break;
				case 3: dutyCycle = 6;
					break;
			}
		}

		public void setVolumeEnvelope(int initVol, int numSteps, int increase)
		{
			initialVolume = initVol;
			numStepsEnvelope = numSteps;
			if(increase == 8)
				increaseEnvelope = 1;
			else
				increaseEnvelope = 0;
		
			amplitude = initVol * 2;
		}
	
		public void setCounter(int val)
		{
			counterSelect = val;
		}

		public void setFrequencyLo(int freq)
		{
			frequency = (frequency & 0xFF) | freq;
			setFrequency(frequency);
		}
	
		public void setFrequencyHi(int freq)
		{
			frequency = (frequency & ~0x700) | ((freq & 0x07)<<8);
			setFrequency(frequency);
		}
	
		public void setFrequency(int frequency)
		{
			try{
				float gbFrequency = 131072 >> 11;

				if(frequency != 2048){
					gbFrequency = ((float) 131072 / (float) (2048 - frequency));
				}
				this.frequency = frequency;
				if(frequency != 0)
				{
					cycleLength = (256 * sampleRate) / (int) gbFrequency;
				}
				else
				{
				cycleLength = 65535;
			}
			if(cycleLength == 0) cycleLength =1;
		} catch(ArithmeticException e){}
	}

	public void play(byte[] b, int len, int offset)
	{
		int val = 0;
		if(soundLength != 0)
		{
			soundLength--;
			
			if(timeSweep != 0)
			{
				counterSweep++;
				if(counterSweep > timeSweep)
				{
					if(decreaseSweep == 1)
					{
						setFrequency(frequency - (frequency >> numSweep));
					}
					else
						setFrequency(frequency + (frequency >> numSweep));
					counterSweep = 0;
				}
			}
			
			counterEnvelope++;
			if(numStepsEnvelope != 0)
			{
				if(((counterEnvelope % numStepsEnvelope) == 0) && (amplitude > 0))
				{
					if(increaseEnvelope != 1)
					{
						if(amplitude > 0) 
							amplitude-=2;
						else
							if(amplitude < 16)
								amplitude+=2;
					}
				}
			}
			
			for(int r=offset; r<offset+len; r++)
			{
				if(cycleLength != 0)
				{
					if(((8 * cyclePos) / cycleLength ) >= dutyCycle)
						val = amplitude;
					else
						val = -amplitude;
				}
				
				if((channel & CHAN_LEFT) != 0)
					b[r*2] += val;
				if((channel & CHAN_RIGHT) !=0)
					b[r*2 +1] +=val;
				if((channel & CHAN_MONO) !=0)
					b[r] += val;
			}
			cyclePos = (cyclePos+256) % cycleLength;
			
		}
		
	}

}

class VoluntaryWaveGen
{
	int sampleRate;
	
	public VoluntaryWaveGen(int sampleRate)
	{
		this.sampleRate = sampleRate;
	}
}

class NoiseGen
{
	int sampleRate;
	
	public NoiseGen(int sampleRate)
	{
		this.sampleRate = sampleRate;
	}
}


public class Sound extends Thread {
	
	private final Semaphore sem;
	SourceDataLine soundLine;
	int defaultSampleRate = 44100; // 44.1Khz
	int defaultBufferLength = 200;
	SquareWaveGen channel1;
	SquareWaveGen channel2;
	VoluntaryWaveGen channel3;
	NoiseGen channel4;
	boolean soundEnabled = false;
	private boolean channel1Enable = true, channel2Enable = true,
			channel3Enable = false, channel4Enable = false;
			
	public void setChannelEnable(boolean chan1, boolean chan2, boolean chan3, boolean chan4)
	{
		channel1Enable = chan1;
		channel2Enable = chan2;
		channel3Enable = chan3;
		channel4Enable = chan4;
	}
			
	public Sound(Semaphore sem)
	{
		this.sem = sem;
		soundLine = initSoundHardware();
		channel1 = new SquareWaveGen(defaultSampleRate);
		channel2 = new SquareWaveGen(defaultSampleRate);
		channel3 = new VoluntaryWaveGen(defaultSampleRate);
		channel4 = new NoiseGen(defaultSampleRate);
	}
	
	public void run()
	{
		for(;;)
		{
		
			if(soundEnabled)
			{
	
				try
				{
					sem.acquire();
				}
				catch(Throwable e)
				{
					e.printStackTrace();
				}
			
				int numSamples;
			
				if(defaultSampleRate / 28 >= soundLine.available() * 2)
					numSamples = soundLine.available() * 2;
				else
					numSamples = (defaultSampleRate / 28) & 0xFFFE;
	
		
			byte[] b = new byte[numSamples];
		//	if(channel1Enable)
		//		channel1.play(b,numSamples/2,0);
			if(channel2Enable)
				channel2.play(b,numSamples/2,0);
		//	if(channel3Enable)
		//		channel3.play(b,numSamples/2,0);
		//	if(channel4Enable)
		//		channel4.play(b,numSamples/2,0);
		
			soundLine.write(b,0,numSamples);
		
			}
			
		}
	}
/*	public void run()
	{
		
		for(;;)
		{
			try
			{
				sem.acquire();
			}
			catch(Throwable e)
			{
				e.printStackTrace();
			}
			
			if(soundEnabled)
			{
				int numSamples;
			
				if(defaultSampleRate / 28 >= soundLine.available() * 2)
					numSamples = soundLine.available() * 2;
				else
					numSamples = (defaultSampleRate / 28) & 0xFFFE;
				
			byte[] b = new byte[numSamples];
		
			for(int i=0;i<numSamples;i++)
				b[i] = (byte)((Math.random() * 256)-128);
			
			soundLine.write(b,0,numSamples);
			}
		}
	}*/
	
	public SourceDataLine initSoundHardware() 
	{
		try 
		{
			AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
			defaultSampleRate,8,2,2,defaultSampleRate, true);
			DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class,format);
			
			if(!AudioSystem.isLineSupported(lineInfo))
			{
				System.out.println("Can't find Audio Output System");
				soundEnabled = false;
			}
			else
			{
				SourceDataLine line = (SourceDataLine) AudioSystem.getLine(lineInfo);
				
				int bufferLength = (defaultSampleRate / 1000) * defaultBufferLength;
				line.open(format, bufferLength);
				line.start();
				
				soundEnabled = true;
				return line;
			}
			} catch (Exception e)
			{
				System.out.println("Error: Audio System Busy!");
				soundEnabled = false;
			}
			return null;
		}
	}			
