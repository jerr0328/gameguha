/*

JavaBoy
                                  
COPYRIGHT (C) 2001 Neil Millstone and The Victoria University of Manchester
                                                                         ;;;
This program is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your option)
any later version.        

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
more details.


You should have received a copy of the GNU General Public License along with
this program; if not, write to the Free Software Foundation, Inc., 59 Temple
Place - Suite 330, Boston, MA 02111-1307, USA.

*/

import java.awt.*;
import java.awt.image.*;
import java.lang.*;
import java.io.*;
import java.applet.*;
import java.net.*;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.util.StringTokenizer;
import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.*;


/** This class can mix a square wave signal with a sound buffer.
 *  It supports all features of the Gameboys sound channels 1 and 2.
 */


class NoiseGenerator {
 /** Indicates sound is to be played on the left channel of a stereo sound */
 public static final int CHAN_LEFT = 1;

 /** Indictaes sound is to be played on the right channel of a stereo sound */
 public static final int CHAN_RIGHT = 2;

 /** Indicates that sound is mono */
 public static final int CHAN_MONO = 4;

 /** Indicates the length of the sound in frames */
 int totalLength;
 int cyclePos;

 /** The length of one cycle, in samples */
 int cycleLength;

 /** Amplitude of the wave function */
 int amplitude;

 /** Channel being played on.  Combination of CHAN_LEFT and CHAN_RIGHT, or CHAN_MONO */
 int channel;

 /** Sampling rate of the output channel */
 int sampleRate;

 /** Initial value of the envelope */
 int initialEnvelope;

 int numStepsEnvelope;

 /** Whether the envelope is an increase/decrease in amplitude */
 boolean increaseEnvelope;

 int counterEnvelope;

 /** Stores the random values emulating the polynomial generator (badly!) */
 boolean randomValues[];

 int dividingRatio;
 int polynomialSteps;
 int shiftClockFreq;
 int finalFreq;
 int cycleOffset;

 /** Creates a white noise generator with the specified wavelength, amplitude, channel, and sample rate */
 public NoiseGenerator(int waveLength, int ampl, int chan, int rate) {
  cycleLength = waveLength;
  amplitude = ampl;
  cyclePos = 0;
  channel = chan;
  sampleRate = rate;
  cycleOffset = 0;

   randomValues = new boolean[32767];

  Random rand = new java.util.Random();


  for (int r = 0; r < 32767; r++) {
	randomValues[r] = rand.nextBoolean();
  }

  cycleOffset = 0;
}

 /** Creates a white noise generator with the specified sample rate */
 public NoiseGenerator(int rate) {
  cyclePos = 0;
  channel = CHAN_LEFT | CHAN_RIGHT;
  cycleLength = 2;
  totalLength = 0;
  sampleRate = rate;
  amplitude = 32;

  randomValues = new boolean[32767];

  Random rand = new java.util.Random();


  for (int r = 0; r < 32767; r++) {
	randomValues[r] = rand.nextBoolean();
  }

  cycleOffset = 0;
 }


 public void setSampleRate(int sr) {
  sampleRate = sr;
 }

 /** Set the channel that the white noise is playing on */
 public void setChannel(int chan) {
  channel = chan;
 }

 /** Setup the envelope, and restart it from the beginning */
 public void setEnvelope(int initialValue, int numSteps, boolean increase) {
  initialEnvelope = initialValue;
  numStepsEnvelope = numSteps;
  increaseEnvelope = increase;
  amplitude = initialValue * 2;
 }

 /** Set the length of the sound */
 public void setLength(int gbLength) {
  if (gbLength == -1) {
   totalLength = -1;
  } else {
   totalLength = (64 - gbLength) / 4;
  }
 }

 public void setParameters(float dividingRatio, boolean polynomialSteps, int shiftClockFreq) {
  this.dividingRatio = (int) dividingRatio;
  if (!polynomialSteps) {
   this.polynomialSteps = 32767;
   cycleLength = 32767 << 8;
   cycleOffset = 0;
  } else {
   this.polynomialSteps = 63;
   cycleLength = 63 << 8;

   java.util.Random rand = new java.util.Random();

   cycleOffset = (int) (rand.nextFloat() * 1000);
  }
  this.shiftClockFreq = shiftClockFreq;

  if (dividingRatio == 0) dividingRatio = 0.5f;

  finalFreq = ((int) (4194304 / 8 / dividingRatio)) >> (shiftClockFreq + 1);
//  System.out.println("dr:" + dividingRatio + "  steps: " + this.polynomialSteps + "  shift:" + shiftClockFreq + "  = Freq:" + finalFreq);
 }

 /** Output a single frame of samples, of specified length.  Start at position indicated in the
  *  output array.
  */
 public void play(byte[] b, int length, int offset) {
  int val;

  if (totalLength != 0) {
   totalLength--;

   counterEnvelope++;
   if (numStepsEnvelope != 0) {
    if (((counterEnvelope % numStepsEnvelope) == 0) && (amplitude > 0)) {
     if (!increaseEnvelope) {
      if (amplitude > 0) amplitude-=2;
     } else {
      if (amplitude < 16) amplitude+=2;
     }
    }
   }


   int step = ((finalFreq) / (sampleRate >> 8));
  // System.out.println("Step=" + step);

   for (int r = offset; r < offset + length; r++) {
	boolean value = randomValues[((cycleOffset ) + (cyclePos >> 8)) & 0x7FFF];
	int v = value? (amplitude / 2): (-amplitude / 2);

    if ((channel & CHAN_LEFT) != 0) b[r * 2] += v;
    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += v;
    if ((channel & CHAN_MONO) != 0) b[r] += v;

    cyclePos = (cyclePos + step) % cycleLength;
  }

   /*
   for (int r = offset; r < offset + length; r++) {
    val = (int) ((Math.random() * amplitude * 2) - amplitude);

    if ((channel & CHAN_LEFT) != 0) b[r * 2] += val;
    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += val;
    if ((channel & CHAN_MONO) != 0) b[r] += val;

 //   System.out.print(val + " ");

    cyclePos = (cyclePos + 256) % cycleLength;

   }*/
  }
 }

}



class VoluntaryWaveGenerator {
 public static final int CHAN_LEFT = 1;
 public static final int CHAN_RIGHT = 2;
 public static final int CHAN_MONO = 4;

 int totalLength;
 int cyclePos;
 int cycleLength;
 int amplitude;
 int channel;
 int sampleRate;
 int volumeShift;

 byte[] waveform = new byte[32];

 public VoluntaryWaveGenerator(int waveLength, int ampl, int duty, int chan, int rate) {
  cycleLength = waveLength;
  amplitude = ampl;
  cyclePos = 0;
  channel = chan;
  sampleRate = rate;
 }

 public VoluntaryWaveGenerator(int rate) {
  cyclePos = 0;
  channel = CHAN_LEFT | CHAN_RIGHT;
  cycleLength = 2;
  totalLength = 0;
  sampleRate = rate;
  amplitude = 32;
 }

 public void setSampleRate(int sr) {
  sampleRate = sr;
 }

 public void setFrequency(int gbFrequency) {
//  cyclePos = 0;
  float frequency = (int) ((float) 65536 / (float) (2048 - gbFrequency));
//  System.out.println("gbFrequency: " + gbFrequency + "");
  cycleLength = (int) ((float) (256f * sampleRate) / (float) frequency);
  if (cycleLength == 0) cycleLength = 1;
//  System.out.println("Cycle length : " + cycleLength + " samples");
 }

 public void setChannel(int chan) {
  channel = chan;
 }

 public void setLength(int gbLength) {
  if (gbLength == -1) {
   totalLength = -1;
  } else {
   totalLength = (256 - gbLength) / 4;
  }
 }

 public void setSamplePair(int address, int value) {
  waveform[address * 2] = (byte) ((value & 0xF0) >> 4);
  waveform[address * 2 + 1] = (byte) ((value & 0x0F)) ;
 }

 public void setVolume(int volume) {
  switch (volume) {
   case 0 : volumeShift = 5;
            break;
   case 1 : volumeShift = 0;
            break;
   case 2 : volumeShift = 1;
            break;
   case 3 : volumeShift = 2;
            break;
  }
//  System.out.println("A:"+volume);
 }

 public void play(byte[] b, int length, int offset) {
  int val;

  if (totalLength != 0) {
   totalLength--;

   for (int r = offset; r < offset + length; r++) {

    int samplePos = (31 * cyclePos) / cycleLength;
    val = waveform[samplePos % 32] >> volumeShift << 1;
//    System.out.print(" " + val);

    if ((channel & CHAN_LEFT) != 0) b[r * 2] += val;
    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += val;
    if ((channel & CHAN_MONO) != 0) b[r] += val;

 //   System.out.print(val + " ");
    cyclePos = (cyclePos + 256) % cycleLength;
   }
  }
 }

}

class SquareWaveGenerator {
 /** Sound is to be played on the left channel of a stereo sound */
 public static final int CHAN_LEFT = 1;

 /** Sound is to be played on the right channel of a stereo sound */
 public static final int CHAN_RIGHT = 2;

 /** Sound is to be played back in mono */
 public static final int CHAN_MONO = 4;

 /** Length of the sound (in frames) */
 int totalLength;

 /** Current position in the waveform (in samples) */
 int cyclePos;

 /** Length of the waveform (in samples) */
 int cycleLength;

 /** Amplitude of the waveform */
 int amplitude;

 /** Amount of time the sample stays high in a single waveform (in eighths) */
 int dutyCycle;

 /** The channel that the sound is to be played back on */
 int channel;

 /** Sample rate of the sound buffer */
 int sampleRate;

 /** Initial amplitude */
 int initialEnvelope;

 /** Number of envelope steps */
 int numStepsEnvelope;

 /** If true, envelope will increase amplitude of sound, false indicates decrease */
 boolean increaseEnvelope;

 /** Current position in the envelope */
 int counterEnvelope;

 /** Frequency of the sound in internal GB format */
 int gbFrequency;

 /** Amount of time between sweep steps. */
 int timeSweep;

 /** Number of sweep steps */
 int numSweep;

 /** If true, sweep will decrease the sound frequency, otherwise, it will increase */
 boolean decreaseSweep;

 /** Current position in the sweep */
 int counterSweep;

 /** Create a square wave generator with the supplied parameters */
 public SquareWaveGenerator(int waveLength, int ampl, int duty, int chan, int rate) {
  cycleLength = waveLength;
  amplitude = ampl;
  cyclePos = 0;
  dutyCycle = duty;
  channel = chan;
  sampleRate = rate;
 }

 /** Create a square wave generator at the specified sample rate */
 public SquareWaveGenerator(int rate) {
  dutyCycle = 4;
  cyclePos = 0;
  channel = CHAN_LEFT | CHAN_RIGHT;
  cycleLength = 2;
  totalLength = 0;
  sampleRate = rate;
  amplitude = 32;
  counterSweep = 0;
 }

 /** Set the sound buffer sample rate */
 public void setSampleRate(int sr) {
  sampleRate = sr;
 }

 /** Set the duty cycle */
 public void setDutyCycle(int duty) {
  switch (duty) {
   case 0 : dutyCycle = 1;
            break;
   case 1 : dutyCycle = 2;
            break;
   case 2 : dutyCycle = 4;
            break;
   case 3 : dutyCycle = 6;
            break;
  }
//  System.out.println(dutyCycle);
 }

 /** Set the sound frequency, in internal GB format */
 public void setFrequency(int gbFrequency) {
  try {
  float frequency = 131072 / 2048;

  if (gbFrequency != 2048) {
   frequency = ((float) 131072 / (float) (2048 - gbFrequency));
  }
//  System.out.println("gbFrequency: " + gbFrequency + "");
  this.gbFrequency = gbFrequency;
  if (frequency != 0) {
   cycleLength = (256 * sampleRate) / (int) frequency;
  } else {
   cycleLength = 65535;
  }
  if (cycleLength == 0) cycleLength = 1;
//  System.out.println("Cycle length : " + cycleLength + " samples");
  } catch (ArithmeticException e) {
   // Skip ip
  }
 }

 /** Set the channel for playback */
 public void setChannel(int chan) {
  channel = chan;
 }

 /** Set the envelope parameters */
 public void setEnvelope(int initialValue, int numSteps, boolean increase) {
  initialEnvelope = initialValue;
  numStepsEnvelope = numSteps;
  increaseEnvelope = increase;
  amplitude = initialValue * 2;
 }

 /** Set the frequency sweep parameters */
 public void setSweep(int time, int num, boolean decrease) {
  timeSweep = (time + 1) / 2;
  numSweep = num;
  decreaseSweep = decrease;
  counterSweep = 0;
//  System.out.println("Sweep: " + time + ", " + num + ", " + decrease);
 }

 public void setLength(int gbLength) {
  if (gbLength == -1) {
   totalLength = -1;
  } else {
   totalLength = (64 - gbLength) / 4;
  }
 }

 public void setLength3(int gbLength) {
  if (gbLength == -1) {
   totalLength = -1;
  } else {
   totalLength = (256 - gbLength) / 4;
  }
 }

 public void setVolume3(int volume) {
  switch (volume) {
   case 0 : amplitude = 0;
            break;
   case 1 : amplitude = 32;
            break;
   case 2 : amplitude = 16;
            break;
   case 3 : amplitude = 8;
            break;
  }
//  System.out.println("A:"+volume);
 }

 /** Output a frame of sound data into the buffer using the supplied frame length and array offset. */
 public void play(byte[] b, int length, int offset) {
	
  int val = 0;

  if (totalLength != 0) {
   totalLength--;

   if (timeSweep != 0) {
    counterSweep++;
    if (counterSweep > timeSweep) {
     if (decreaseSweep) {
      setFrequency(gbFrequency - (gbFrequency >> numSweep));
     } else {
      setFrequency(gbFrequency + (gbFrequency >> numSweep));
     }
     counterSweep = 0;
    }
   }

   counterEnvelope++;
   if (numStepsEnvelope != 0) {
    if (((counterEnvelope % numStepsEnvelope) == 0) && (amplitude > 0)) {
     if (!increaseEnvelope) {
      if (amplitude > 0) amplitude-=2;
     } else {
      if (amplitude < 16) amplitude+=2;
     }
    }
   }
   for (int r = offset; r < offset + length; r++) {

    if (cycleLength != 0) {
     if (((8 * cyclePos) / cycleLength) >= dutyCycle) {
      val = amplitude;
     } else {
      val = -amplitude;
     }
    }

/*    if (cyclePos >= (cycleLength / 2)) {
     val = amplitude;
    } else {
     val = -amplitude;
    }*/


    if ((channel & CHAN_LEFT) != 0) b[r * 2] += val;
    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += val;
    if ((channel & CHAN_MONO) != 0) b[r] += val;

 //   System.out.print(val + " ");

    cyclePos = (cyclePos + 256) % cycleLength;
   }
  }
 }

}




/** This is the central controlling class for the sound.
 *  It interfaces with the Java Sound API, and handles the
 *  calsses for each sound channel.
 */
class Sound extends Thread {
 /** The DataLine for outputting the sound */
 SourceDataLine soundLine;

 private final Semaphore sem;
 SquareWaveGenerator channel1;
 SquareWaveGenerator channel2;
 VoluntaryWaveGenerator channel3;
 NoiseGenerator channel4;
 boolean soundEnabled = false;

 /** If true, channel is enabled */
 boolean channel1Enable = true, channel2Enable = true,
         channel3Enable = true, channel4Enable = true;

 /** Current sampling rate that sound is output at */
 int sampleRate = 44100;

 /** Amount of sound data to buffer before playback */
 int bufferLengthMsec = 200;

 /** Initialize sound emulation, and allocate sound hardware */
 public void setChan1(boolean channel1Enable)
{
	this.channel1Enable = channel1Enable;
}


public void setChan2(boolean channel2Enable)
{
	this.channel2Enable = channel2Enable;
}

public void setChan3(boolean channel3Enable)
{
	this.channel3Enable = channel3Enable;
}

public void setChan4(boolean channel4Enable)
{
	this.channel4Enable = channel4Enable;
}

public void setSoundEnable(boolean soundEnabled)
{
	this.soundEnabled = soundEnabled;
}

 public Sound(Semaphore sem) {
	this.sem = sem;
  soundLine = initSoundHardware();
  channel1 = new SquareWaveGenerator(sampleRate);
  channel2 = new SquareWaveGenerator(sampleRate);
  channel3 = new VoluntaryWaveGenerator(sampleRate);
  channel4 = new NoiseGenerator(sampleRate);
 }

 /** Initialize sound hardware if available */
 public SourceDataLine initSoundHardware() {

  try {
   AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
    sampleRate, 8, 2, 2, sampleRate, true);
   DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);

   if (!AudioSystem.isLineSupported(lineInfo)) {
    System.out.println("Error: Can't find audio output system!");
    soundEnabled = false;
   } else {
    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(lineInfo);

    int bufferLength = (sampleRate / 1000) * bufferLengthMsec;
    line.open(format, bufferLength);
    line.start();
//    System.out.println("Initialized audio successfully.");
    soundEnabled = true;
    return line;
   }
  } catch (Exception e) {
   System.out.println("Error: Audio system busy!");
   soundEnabled = false;
  }

  return null;
 }

 /** Change the sample rate of the playback */
 public void setSampleRate(int sr) {
  sampleRate = sr;

  soundLine.flush();
  soundLine.close();

  soundLine = initSoundHardware();

  channel1.setSampleRate(sr);
  channel2.setSampleRate(sr);
  channel3.setSampleRate(sr);
  channel4.setSampleRate(sr);
 }

 /** Change the sound buffer length */
 public void setBufferLength(int time) {
  bufferLengthMsec = time;

  soundLine.flush();
  soundLine.close();

  soundLine = initSoundHardware();
 }

 /** Adds a single frame of sound data to the buffer */
 public void run() {
	
	for(;;)
	{
		synchronized (this) {
			if (soundEnabled) {
		
				try
				{
					sem.acquire();
				}
				catch(Throwable e)
				{
					e.printStackTrace();
				}
		
				int numSamples;

				if (sampleRate / 28 >= soundLine.available() * 2) {
					numSamples = soundLine.available() * 2;
				} else {
					numSamples = (sampleRate / 28) & 0xFFFE;
				}

				byte[] b = new byte[numSamples];
				if (channel1Enable) channel1.play(b, numSamples / 2, 0);
				if (channel2Enable) channel2.play(b, numSamples / 2, 0);
				if (channel3Enable) channel3.play(b, numSamples / 2, 0);
				if (channel4Enable) channel4.play(b, numSamples / 2, 0);
				soundLine.write(b, 0, numSamples);
				}
			}
		}
	}
}


