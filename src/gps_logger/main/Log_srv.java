package gps_logger.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.location.*;
import android.content.Context;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.*;

public class Log_srv extends Service {
	public double alt, lng, lat;
	public float speed;
	public long time;
	public String status;
	public boolean run_as_daemon;

	private LocationManager loc_mgr;
	private myLocationListener loc_lsn;
	private String buffer="";
	private FileOutputStream ftmp_out;
	private IBinder binder;
	private File my_path = new File(Environment.getExternalStorageDirectory(), "gps_logger");
	private File ftmp    = new File(my_path, "gps.tmp");


	public class local_binder extends Binder {
		Service getService() {
			return Log_srv.this;
		}
	}

	public IBinder onBind(Intent intent) {
		Log.d("LogSrv", "on bind");
		return binder;
	}

	public void onRebind(Intent intent) {
		Log.d("LogSrv", "on rebind");
		return;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d("LogSrv", "on unbind");
		super.onUnbind(intent);
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		status = "Waiting";
		Log.d("LogSrv", "on create");

		// GPS initial
		loc_mgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		loc_lsn = new myLocationListener();
		loc_mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, loc_lsn);
		binder = (IBinder) new local_binder();
		
		if (false == my_path.exists()) {
			my_path.mkdir();
		}
	}

	public void onStartCommand() {
		Log.d("LogSrv", "start command");
		return;
	}

	public void onDestroy() {
		super.onDestroy();
		Log.d("LogSrv", "on destroy");
		ftmp.delete();
		loc_mgr.removeUpdates(loc_lsn);

		if (ftmp_out != null) {
			try {
				ftmp_out.close();
			} catch (IOException e) {
				Log.e("LogSrv", "fail to open temp file");
			}
		}
	}

	class myLocationListener implements LocationListener {
		public void onProviderDisabled(String provider) {
			status = "GPS is disabled";
		}

		public void onProviderEnabled(String provider) {
			status = "GPS is enabled";
		}

		public void onLocationChanged(Location location) {
			lat = location.getLongitude();
			lng = location.getLatitude();
			alt = location.getAltitude();
			time = location.getTime();
			speed = location.getSpeed();
			if (false == run_as_daemon) {
				return;
			}
			buffer += lat + "," + lng + "," + alt + " \n";

			// write to file while buffer more than 1KB
			if (buffer.length() > 200) {
				try {
					ftmp_out.write(buffer.getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
				buffer = "";
			}
		}

		public void onStatusChanged(String provider, int stat_code, Bundle ex) {
			switch (stat_code) {
			case LocationProvider.OUT_OF_SERVICE:
				status = "No signal";
				break;
			case LocationProvider.AVAILABLE:
				status = "Fixed";
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				status = "calculating";
				break;
			default:
				status = "unknown";
				break;
			}
		}
	}

	public boolean enter_daemon() {
		if (my_path.canWrite() == false) {
			Log.e("LogSrv", "extern storage removable");
			return false;
		}
		
		if (ftmp.exists()) {
			flush_to_kml("gps.kml." + ftmp.lastModified());
		}

		try {
			ftmp_out = new FileOutputStream(ftmp, true);
			run_as_daemon = true;
		} catch (IOException e1) {
			Log.e("LogSrv", "fail to open temp file");
		}
		return true;
	}

	public void exit_deamon(String filename) {
		run_as_daemon = false;
		flush_to_kml(filename);
		try {
			ftmp_out.close();
		} catch (IOException e) {}
	}

	private void flush_to_kml(String filename) {
		int len;
		FileOutputStream fout;
		FileInputStream fin;
		String xml_header, xml_footer;

		xml_header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		xml_header += "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n";
		xml_header += "   <Placemark>\n";
		xml_header += "      <name>GPS Logger</name>\n";
		xml_header += "      <description>Record By GPS Logger</description>\n";
		xml_header += "      <LineString>\n";
		xml_header += "         <coordinates>";
		xml_footer = "         </coordinates>\n";
		xml_footer += "      </LineString>\n";
		xml_footer += "   </Placemark>\n";
		xml_footer += "</kml>\n";

		try {
			fin = new FileInputStream(ftmp);
			fout = new FileOutputStream(new File(my_path, filename));

			byte[] buff = new byte[1024];
			fout.write(xml_header.getBytes());

			while (true) {
				len = fin.read(buff, 0, 1024);
				if (len < 0) {
					break;
				}
				fout.write(buff, 0, len);
			}

			fout.write(buffer.getBytes());
			buffer="";

			fout.write(xml_footer.getBytes());
			fout.close();
			fin.close();
		} catch (IOException e) {
			Log.e("LogSrv", "flush to file error");
		}
	}

}