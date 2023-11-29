package com.devapp.gps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationService extends Service {
    private static final String TODO = "0";
    private Handler handler;
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // 5 segundos
    private static final long SEND_INTERVAL = 5000; // 5 segundos
    private DatagramSocket socket1, socket2, socket3;
    private InetAddress address1, address2, address3;
    private int port1, port2, port3;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean sendingMessages = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();

        try {
            socket1 = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Configurar el LocationManager y el LocationListener para obtener la ubicación
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                // Enviar la ubicación a través de los sockets UDP
                sendToSocket1();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();
            // Obtener las direcciones IP y los puertos de los extras
            String ip1 = extras.getString("IP1");
            int port1 = extras.getInt("Port1");
            String ip2 = extras.getString("IP2");
            int port2 = extras.getInt("Port2");
            String ip3 = extras.getString("IP3");
            int port3 = extras.getInt("Port3");

            try {
                address1 = InetAddress.getByName(ip1);
                address2 = InetAddress.getByName(ip2);
                address3 = InetAddress.getByName(ip3);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            this.port1 = port1;
            this.port2 = port2;
            this.port3 = port3;

            // Iniciar el Runnable para solicitar ubicación cada 5 segundos
            handler.post(updateLocationRunnable);
            // Iniciar el envío de mensajes periódicamente cada 5 segundos
            startSendingMessagesPeriodically();
        }

        return START_STICKY;
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
            handler.postDelayed(this, LOCATION_UPDATE_INTERVAL); // Ejecutar el Runnable nuevamente después de 5 segundos
        }
    };

    private void startSendingMessagesPeriodically() {
        // Verificar si ya se está enviando mensajes periódicamente
        if (!sendingMessages) {
            sendingMessages = true;
            handler.postDelayed(sendMessagesRunnable, SEND_INTERVAL);
        }
    }

    private void stopSendingMessages() {
        // Detener el envío de mensajes y actualizar el estado
        sendingMessages = false;
        // Remover el Runnable para detener el envío periódico
        handler.removeCallbacks(sendMessagesRunnable);
    }

    private Runnable sendMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            // El LocationListener ya está actualizando la ubicación en tiempo real,
            // por lo que no es necesario obtener la ubicación aquí.
            // Simplemente llamamos a los métodos para enviar mensajes a través de sockets
            sendToSocket1();
            sendToSocket2();
            sendToSocket3();

            // Volver a programar el Runnable para el próximo envío después de 5 segundos
            handler.postDelayed(this, SEND_INTERVAL);
        }
    };

    private void sendToSocket1() {
        new SendLocationDataTask(socket1, address1, port1).execute();
    }

    private void sendToSocket2() {
        new SendLocationDataTask(socket2, address2, port2).execute();
    }

    private void sendToSocket3() {
        new SendLocationDataTask(socket3, address3, port3).execute();
    }

    private class SendLocationDataTask extends AsyncTask<Void, Void, Void> {
        private DatagramSocket socket;
        private InetAddress address;
        private int port;

        SendLocationDataTask(DatagramSocket socket, InetAddress address, int port) {
            this.socket = socket;
            this.address = address;
            this.port = port;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String locationMessage = createLocationMessage();
            byte[] data = locationMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private String createLocationMessage() {
        // Aquí puedes personalizar el formato del mensaje de ubicación
        // Puedes obtener datos como latitud, longitud, altitud y hora de la ubicación
        // y formatearlos como desees.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return TODO;
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            double altitude = location.getAltitude();
            long timestamp = location.getTime();

            return "Ubicacion: Latitud " + latitude + ", Longitud " + longitude + ", Altitud " + altitude +
                    ", Hora: " + getFormattedTime(timestamp);
        } else {
            return "Ubicacion: Latitud " + "0.0000" + ", Longitud " + "0.0000" + ", Altitud " + "0.0000" +
                    ", Hora: " + "28/08/2023 00:00:00";
        }
    }

    private String getFormattedTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Detener el envío de mensajes y eliminar callbacks
        stopSendingMessages();
        handler.removeCallbacks(updateLocationRunnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
