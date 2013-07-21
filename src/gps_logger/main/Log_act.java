package gps_logger.main;

import android.os.*;
import android.view.*;
import android.widget.*;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;

import java.util.*;
import java.text.SimpleDateFormat;

import gps_logger.main.R;

public class Log_act extends Activity {

	private EditText edit_lat, edit_lng, edit_alt, edit_speed, edit_time;
	private TextView text_msg;
	private Button btn_log;
	private Log_srv logger_srv = null;
	private Timer timer;
	private myhandler timer_hdlr;
	private mytimertask task;
	private myServiceConnection mConnection = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		init_view();

		// Start a timer task to update GPS status
		timer_hdlr = new myhandler();
		task = new mytimertask();
		timer = new Timer();
		timer.scheduleAtFixedRate(task, 1000, 2000);

		// (create and then ) Connect to LogSrv
		mConnection = new myServiceConnection();
		startService(new Intent(this, Log_srv.class));
		bindService(new Intent(this, Log_srv.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		timer.cancel();
		unbindService(mConnection);

		if (logger_srv != null && logger_srv.run_as_daemon == false) {
			stopService(new Intent(this, Log_srv.class));
		}
	}

	private void init_view() {
		edit_lat = (EditText) findViewById(R.id.edit_lat);
		edit_lng = (EditText) findViewById(R.id.edit_lng);
		edit_alt = (EditText) findViewById(R.id.edit_alt);
		edit_speed = (EditText) findViewById(R.id.edit_speed);
		edit_time = (EditText) findViewById(R.id.edit_time);
		text_msg = (TextView) findViewById(R.id.text_msg);
		btn_log = (Button) findViewById(R.id.btn_log);
		

		btn_log.setText("Start");
		btn_log.setOnClickListener(new btn_log_onclick_listener());
		// TODO: add mark function here
		//btn_stop = (Button) findViewById(R.id.btn_stop);
		//btn_stop.setOnClickListener(new btn_onclick_listener());
	}

	class myhandler extends Handler {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			text_msg.setText(logger_srv.status);
			edit_lat.setText(String.valueOf(logger_srv.lat));
			edit_lng.setText(String.valueOf(logger_srv.lng));
			edit_alt.setText(String.valueOf(logger_srv.alt));
			edit_time.setText(sdf.format(logger_srv.time));
			edit_speed.setText(String.valueOf(logger_srv.speed));
		}
	}

	class mytimertask extends TimerTask {
		public void run() {
			// Sending message util logger_srv is valid
			if (logger_srv != null) {
				timer_hdlr.sendMessage(new Message());
			}
		}
	}

	class myServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName name, IBinder service) {
			logger_srv = (Log_srv) ((Log_srv.local_binder) service).getService();

			// Auto set the label of button
			if (logger_srv.run_as_daemon == true) {
				btn_log.setText("Stop");
			} else {
				btn_log.setText("Start");
			}
		}

		public void onServiceDisconnected(ComponentName arg0) {
			logger_srv = null;
		}

	}

	class btn_log_onclick_listener implements View.OnClickListener {
		public void onClick(View v) {
			Button btn = (Button) v;
			if (btn.getText() == "Start") {
				if(logger_srv.enter_daemon()){
					btn.setText("Stop");;
				}
			} else {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

				logger_srv.exit_deamon("gps-"+ sdf.format(new Date())+".kml");
				btn.setText("Start");
			}
		}
	}
}