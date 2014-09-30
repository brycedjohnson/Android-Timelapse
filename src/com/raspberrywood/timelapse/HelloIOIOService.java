package com.raspberrywood.timelapse;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.Sequencer;
import ioio.lib.api.Sequencer.ChannelConfig;
import ioio.lib.api.Sequencer.ChannelConfigBinary;
import ioio.lib.api.Sequencer.ChannelConfigSteps;
import ioio.lib.api.Sequencer.ChannelCueBinary;
import ioio.lib.api.Sequencer.ChannelCueSteps;
import ioio.lib.api.Sequencer.Clock;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * An example IOIO service. While this service is alive, it will attempt to
 * connect to a IOIO and blink the LED. A notification will appear on the
 * notification bar, enabling the user to stop the service.
 */
public class HelloIOIOService extends IOIOService implements OnSharedPreferenceChangeListener{
	
	public static SharedPreferences prefs;
	private NotificationManager nm;
	private static final String TAG = "TimeLapseActivity";
	
	public static final float STEPS_FREQ = 62500;

	public static final Clock STEPPER_CLOCK = Clock.CLK_2M;
	public static final int STEPPER_CLOCK_HZ = 2000000;

	public static final float CUE_TIME_UNITS = (float) 16e-6; // 16 microseconds
	public static final int CUE_ONE_SEC = (int) (1 / CUE_TIME_UNITS);
	public static final int CUE_MAX = 65536;
	public static final float CUE_TIME_MAX = CUE_TIME_UNITS * CUE_MAX; // 1.048576
																		// seconds
	/* set-able variables or variables that are changed by set-able variables*/
	public float photo_time = 30;
	public float pause_before_photo_time = 1;
	public int STEPS_PER_INCH = 180;
	public int MICROSTEPS_PER_STEP = 8;
	public float INCHES_PER_PAUSE = 4;
	public int PWM_PERIOD = 400;
	public int CUE_STEPPER_AMT = 36500;
	public float speed = 1;
	public int canon_ticks = 16;
	
	public boolean stepper_direction = true;
	
	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		return new BaseIOIOLooper() {
			private boolean dir_ = false;
			private int color_ = 0;
			
			private Sequencer.ChannelCueBinary led1Cue_ = new ChannelCueBinary();
			private Sequencer.ChannelCueSteps led2Cue_ = new ChannelCueSteps();
			private Sequencer.ChannelCueBinary stepperDirCue_ = new ChannelCueBinary();
			private Sequencer.ChannelCueSteps stepperStepCue_ = new ChannelCueSteps();

			private Sequencer.ChannelCue[] cue_ = new Sequencer.ChannelCue[] {
					led1Cue_, led2Cue_, stepperDirCue_, stepperStepCue_ };
			
			private Sequencer sequencer_;
			
			@Override
			protected void setup() throws ConnectionLostException,
					InterruptedException {
				final ChannelConfigBinary led1Config = new Sequencer.ChannelConfigBinary(
						true, true, new DigitalOutput.Spec(0,
								DigitalOutput.Spec.Mode.OPEN_DRAIN));
				
				final ChannelConfigSteps led2pwm = new Sequencer.ChannelConfigSteps(
						new DigitalOutput.Spec(27,
								DigitalOutput.Spec.Mode.NORMAL));
				
				final ChannelConfigBinary stepperDirConfig = new Sequencer.ChannelConfigBinary(
						false, false, new DigitalOutput.Spec(4,
								DigitalOutput.Spec.Mode.NORMAL));
				
				final ChannelConfigSteps stepperStepConfig = new ChannelConfigSteps(
						new DigitalOutput.Spec(3,
								DigitalOutput.Spec.Mode.NORMAL));
				
				final ChannelConfig[] config = new ChannelConfig[] {
						led1Config, led2pwm, stepperDirConfig, stepperStepConfig };

				
				sequencer_ = ioio_.openSequencer(config);

				checkPrefSettings();
				
				// Pre-fill.
				sequencer_.waitEventType(Sequencer.Event.Type.STOPPED);

				led2Cue_.clk = Clock.CLK_16M;
				led2Cue_.period = 65000;
				led2Cue_.pulseWidth = 0;
				stepperStepCue_.clk = STEPPER_CLOCK;
				stepperStepCue_.period = 65000;
				stepperStepCue_.pulseWidth = 0;

				//loop();


				sequencer_.start();
			}

			@Override
			public void loop() throws ConnectionLostException,	InterruptedException {
				move();
				pause(pause_before_photo_time);
				photo();
				pause(photo_time);
			}

			private void move() throws ConnectionLostException,
					InterruptedException {
				Log.i(TAG, "Queueing Move");
				led1Cue_.value = (color_ & 1) == 0;
				led2Cue_.pulseWidth = 0;
				stepperDirCue_.value = stepper_direction;
				stepperStepCue_.period = PWM_PERIOD;
				stepperStepCue_.pulseWidth = (int) Math.floor(PWM_PERIOD / 2);
				
				int ticks_cue = CUE_STEPPER_AMT;
				Log.v(TAG, "Move: ticks_cue_start: " + ticks_cue);
				while (ticks_cue > 2) {
					if (ticks_cue > 62500) {
						sequencer_.push(cue_, 62500);
						ticks_cue -= 62500;
					} else {
						sequencer_.push(cue_, ticks_cue);
						ticks_cue -= ticks_cue;
					}
				}
					
			}

			/**
			 * Input: Seconds of pause
			 * 
			 * @throws ConnectionLostException
			 * @throws InterruptedException
			 */
			private void pause(float pause_seconds)
					throws ConnectionLostException, InterruptedException {
				Log.i(TAG, "Queueing Pause: " + pause_seconds +" seconds");
				led1Cue_.value = (color_ & 1) == 0;
				led2Cue_.pulseWidth = 0;
				stepperStepCue_.pulseWidth = 0;

				int ticks_pause = (int) Math.round(pause_seconds * 62500);
				Log.v(TAG, "pause: ticks_left: " + ticks_pause);
				while (ticks_pause > 2) {
					if (ticks_pause > 62500) {
						Log.v(TAG, "pause: ticks_left: " + ticks_pause);
						sequencer_.push(cue_, 62500);
						ticks_pause -= 62500;
					} else {
						sequencer_.push(cue_, ticks_pause);
						ticks_pause -= ticks_pause;
					}
					
				}

			}

			private void photo() throws ConnectionLostException,
					InterruptedException {
				Log.i(TAG, "Queueing photo");
				stepperStepCue_.pulseWidth = 0;
				led1Cue_.value = false;

				led2Cue_.clk = Clock.CLK_16M;
				led2Cue_.period = (int) Math.round(16e6 * (2 * 15.24e-6));
				led2Cue_.pulseWidth = (int) Math.floor(led2Cue_.period / 2);
				sequencer_.push(cue_, (int) Math.round(canon_ticks * 2 * (15.24e-6 / CUE_TIME_UNITS)));

				led2Cue_.pulseWidth = 0;
				sequencer_.push(cue_, (int) Math.round(7730 / 16));

				led2Cue_.period = (int) Math.round(16e6 * (2 * 15.24e-6));
				led2Cue_.pulseWidth = (int) Math.floor(led2Cue_.period / 2);
				sequencer_.push(cue_, (int) Math.round(canon_ticks * 2 * (15.24e-6 / CUE_TIME_UNITS)));
			}

			/**
			 * Called when the IOIO is disconnected.
			 * 
			 * @see ioio.lib.util.IOIOLooper#disconnected()
			 */
			@Override
			public void disconnected() {
				toast("IOIO disconnected");
			}

			/**
			 * Called when the IOIO is connected, but has an incompatible
			 * firmware version.
			 * 
			 * @see ioio.lib.util.IOIOLooper#incompatible(IOIO)
			 */
			@Override
			public void incompatible() {
				showVersions(ioio_, "Incompatible firmware version!");
			}
		};
	}
		
	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(this,"IOIO service created ",Toast.LENGTH_SHORT).show(); 
		SharedPreferences prefs = PreferenceManager
			    .getDefaultSharedPreferences(this);
			prefs.registerOnSharedPreferenceChangeListener(this);
	
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
   	
    	Toast.makeText(this,"IOIO service onStartCommand ",Toast.LENGTH_SHORT).show();
        super.onStartCommand(intent, flags, startId);
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (intent != null && intent.getAction() != null
				&& intent.getAction().equals("stop")) {
			Toast.makeText(this,"IOIO service STOP ",Toast.LENGTH_SHORT).show();
			// User clicked the notification. Need to stop the service.
			nm.cancel(0);
			stopSelf();
					
		} else {
			// Service starting. Create a notification.
			Intent notificationIntent = new Intent(this, MainActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			Notification notification;
			
			notification = new Notification(
					R.drawable.ic_launcher, "IOIO service running",
					System.currentTimeMillis());
			notification
					.setLatestEventInfo(this, "IOIO Service", "test",
							contentIntent);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			nm.notify(0, notification);
		}
		return START_STICKY;
	}
    @Override
    public void onDestroy() {
        //NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();    	
    	super.onDestroy();
    	
    }
    
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	private void showVersions(IOIO ioio, String title) {
		toast(String.format("%s\n" + "IOIOLib: %s\n"
				+ "Application firmware: %s\n" + "Bootloader firmware: %s\n"
				+ "Hardware: %s", title,
				ioio.getImplVersion(VersionType.IOIOLIB_VER),
				ioio.getImplVersion(VersionType.APP_FIRMWARE_VER),
				ioio.getImplVersion(VersionType.BOOTLOADER_VER),
				ioio.getImplVersion(VersionType.HARDWARE_VER)));
	}

	private void toast(final String message) {
		//Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		checkPrefSettings();
		
	}
	
	public void checkPrefSettings() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		photo_time = Float.parseFloat(prefs.getString("photo_time", "@string/photo_time_default"));
		pause_before_photo_time = Float.parseFloat(prefs.getString("pause_before_photo_time", "@string/pause_before_photo_time_default"));
		
		stepper_direction = prefs.getBoolean("stepper_direction", true);
				
		STEPS_PER_INCH = Integer.parseInt(prefs.getString("steps_per_inch", "@string/steps_per_inch_default"));
		MICROSTEPS_PER_STEP = Integer.parseInt(prefs.getString("microsteps", "@string/microsteps_default"));		
		speed = Float.parseFloat(prefs.getString("speed", "@string/speed_default"));
		INCHES_PER_PAUSE = Float.parseFloat(prefs.getString("inches_per_pause", "@string/inches_per_pause_default"));
		canon_ticks = Integer.parseInt(prefs.getString("canon_ticks", "@string/canon_ticks_default"));
		
		
		PWM_PERIOD = (int) (STEPPER_CLOCK_HZ / (STEPS_PER_INCH * MICROSTEPS_PER_STEP * speed));
		CUE_STEPPER_AMT = (int) (CUE_ONE_SEC / (speed/INCHES_PER_PAUSE));
		

	}

}
