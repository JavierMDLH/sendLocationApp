package com.devapp.gps;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.app.Service;
public class MyWorker extends Worker {


    private LocationManager locationManager;



    public static final String TAG = "MessageWorker";

    public MyWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

    }

    @NonNull
    @Override
    public Result doWork() {
        String ip = getInputData().getString("IP");
        int port = getInputData().getInt("Port", -1);

        String ip2 = getInputData().getString("IP2");
        int port2 = getInputData().getInt("Port2", -1);

        String ip3 = getInputData().getString("IP3");
        int port3 = getInputData().getInt("Port3", -1);

        if (ip == null || port == -1 || ip2 == null || port2 ==-1 || ip3 == null || port3 == -1) {
            return Result.failure();
        }

        boolean success = realizarConexionSocket(ip, port,ip2, port2,ip3, port3);

        if (success) {
            return Result.success();
        } else {
            return Result.failure();
        }


    }



    private String LocationMessage() {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            double altitude = location.getAltitude();
            long timestamp = location.getTime();

            return "Ubicacion: Latitud " + latitude + ", Longitud " + longitude + ", Altitud " + altitude +
                    ", Hora: " + getFormattedTime(timestamp);
        }
        return null;
    }

    private String getFormattedTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private boolean realizarConexionSocket(String ip1, int port1, String ip2, int port2, String ip3, int port3) {
        String message = LocationMessage();
        try {
            // Socket para el primer destino
            DatagramSocket socket1 = new DatagramSocket();
            InetAddress address1 = InetAddress.getByName(ip1);
            byte[] data1 = message.getBytes();
            DatagramPacket packet1 = new DatagramPacket(data1, data1.length, address1, port1);
            socket1.send(packet1);
            socket1.close();

            // Socket para el segundo destino
            DatagramSocket socket2 = new DatagramSocket();
            InetAddress address2 = InetAddress.getByName(ip2);
            byte[] data2 = message.getBytes();
            DatagramPacket packet2 = new DatagramPacket(data2, data2.length, address2, port2);
            socket2.send(packet2);
            socket2.close();

            // Socket para el tercer destino
            DatagramSocket socket3 = new DatagramSocket();
            InetAddress address3 = InetAddress.getByName(ip3);
            byte[] data3 = message.getBytes();
            DatagramPacket packet3 = new DatagramPacket(data3, data3.length, address3, port3);
            socket3.send(packet3);
            socket3.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}




