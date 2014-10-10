Android-Timelapse
=================

Read more here:  http://raspberrywood.com/android-timelapse-dolly/



It runs as a service in the background so the screen can be off in the background.

PrefsFragment.java – I tried to make it as configurable as possible on the fly.  The biggest down-side right now is that the movements are queue up and send down to the IOIO ahead of the movement.  If you make a configuration change it will have to execute a couple movements with the old value before it gets to the new ones.

HelloIOIOservice.java – This is based off of Ytai’s example so I could get the use of the service correct.  There is quite a bit of setting up the sequencer, but the main program is in loop().

@Override
public void loop() throws ConnectionLostException,	InterruptedException {
	move();
	pause(pause_before_photo_time);
	photo();
	pause(photo_time);
}

move() – will send the PWM of an exact length.  This will send an exact number of pulses (that trigger microsteps) down to the Stepper Motor Controller – which will send the right stuff down to the stepper motor.

pause(float pause_seconds) – will wait the number of seconds desired.  I use this during the time when a photo is being taken (for example 30 second night shot) or right after a movement (1/2 second wait to make sure there isn’t extra vibrations that could get into a shot)

photo() – This will send the command to take a picture over the IR sensor.  16 pulses at ~32.7kHz.  Wait 7.33ms.  Then send another 16 pulses at ~32.7kHz.

 

There are a ton of new features that could be added.  Bulb ramping, dolly ramping, use a shutter release, use a DC motor instead of a stepper motor, etc.  One of the best things is you just need to update the app on your phone and you are good to go with adding these extra features.

The downside is the phone always has to be in range of the IOIO.  If you walk too far away and it loses connection, it will finish its queue and then wait and wait to be reconnected and for new data to be sent down.

