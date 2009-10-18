//Sound class
import java.io.*;
import javax.sound.sampled.*;

class SquareWaveGen{
	
	final static int CHAN_LEFT = 1;
	final static int CHAN_RIGHT = 2;
	final static int CHAN_MONO = 4;
	
	int sampleRate; // starting sampleRate
	final double lenDiv = 1/256.0;
	
	int dutyCycle;
	double soundLength;
	int initialVolume;
	int envelopeDirection;
	int envelopeSweep;
	int amplitude;
	int frequency;
	int counterSelect;
	int cycleLength;
	int channel;

	public SquareWaveGen(int sampleRate)
	{
		this.sampleRate = sampleRate;
		channel = CHAN_LEFT | CHAN_RIGHT;
		dutyCycle = 4;
		soundLength = 0;
		amplitude = 32;
		
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

	public void setVolumeEnvelope(int initVol, int envDir, int envSweep)
	{
		initialVolume = initVol;
		envelopeDirection = envDir;
		envelopeSweep = envSweep;
		
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
}

class VoluntaryWaveGen{
	
}

class NoiseGen{
	
}


public class Sound {
	
	SourceDataLine soundLine;
	int defaultSampleRate = 44100; // 44.1Khz
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
		soundLine = initSoundHardware();
		channel2 = new SquareWaveGen(defaultSampleRate);
	}
	
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