package com.devapp.gps;

//import static com.devapp.gps.MainActivity.LOCATION_UPDATE_INTERVAL;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationService extends Service {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Handler handler;
    private static final long LOCATION_UPDATE_INTERVAL = 1000; // Intervalo de actualización en milisegundos

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Enviar la ubicación al MyWorker
                sendDataToWorker(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }


        };

        // Iniciar el Runnable para solicitar ubicación cada 10 segundos
        handler.postDelayed(updateLocationRunnable, LOCATION_UPDATE_INTERVAL);




        // Mostrar notificación en primer plano
        createNotification();
    }

    private void createNotification() {
        // Crea un canal de notificación (necesario en versiones recientes de Android)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel_id", "Nombre del canal", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Crea la notificación
        Notification notification = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("Enviando Mensajes")
                .setContentText("Los mensajes estan siendo enviados.")
                .setSmallIcon(R.drawable.notification_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build();

        // Inicia el servicio en primer plano con la notificación
        startForeground(1, notification);
    }



    private Runnable updateLocationRunnable = new Runnable() {
        @Override
        public void run() {
            // Solicitar actualizaciones de ubicación en tiempo real
            if (locationManager != null) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            handler.postDelayed(this, LOCATION_UPDATE_INTERVAL); // Ejecutar el Runnable nuevamente después de 10 segundos
        }
    };

    private void sendDataToWorker(Location location) {
        String locationMessage = createLocationMessage(location);

        Data inputData = new Data.Builder()
                .putString("LocationMessage", locationMessage)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);
    }

    private String createLocationMessage(Location location) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            double altitude = location.getAltitude();
            long timestamp = location.getTime();

            return "Ubicacion: Latitud " + latitude + ", Longitud " + longitude + ", Altitud " + altitude +
                    ", Hora: " + getFormattedTime(timestamp);
        } else {
            return null;
        }
    }

    private String getFormattedTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //@Override
    //public void onDestroy() {
    //    super.onDestroy();
    //    // Detener las actualizaciones de ubicación
    //    if (locationManager != null && locationListener != null) {
    //        locationManager.removeUpdates(locationListener);
    //    }
    // Detener el Runnable
    //    handler.removeCallbacks(updateLocationRunnable);
    //}
}
