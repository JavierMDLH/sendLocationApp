package com.devapp.gps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.lights.Light;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.Manifest;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.devapp.gps.LocationService;


public class MainActivity extends AppCompatActivity {

    private EditText ipEditText;
    private EditText portEditText;
    private EditText ip2EditText;
    private EditText port2EditText;
    private EditText ip3EditText;
    private EditText port3EditText;

    private Switch socketSwitch;
    private TextView statusTextView; // Agregado: TextView para mostrar el estado
    private boolean sendingMessages = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Agregado: LocationManager y LocationListener
    private LocationManager locationManager;
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // Código para enviar la ubicación
            if (sendingMessages) {
                sendDataToWorker(location);
            }
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

    public static final long LOCATION_UPDATE_INTERVAL = 5000; // Intervalo de actualización en milisegundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipEditText = findViewById(R.id.ipNumber);
        portEditText = findViewById(R.id.portNumber);
        ip2EditText = findViewById(R.id.ipNumber2);
        port2EditText = findViewById(R.id.portNumber2);
        ip3EditText = findViewById(R.id.ipNumber3);
        port3EditText = findViewById(R.id.portNumber3);
        socketSwitch = findViewById(R.id.switchSend);
        statusTextView = findViewById(R.id.statusTextView); // Agregado: Referencia al TextView

        // Solicitar permiso de ubicación
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Inicializar LocationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        socketSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startService(new Intent(MainActivity.this,LocationService.class));
                    startSendingMessages();
                } else {
                    stopService(new Intent(MainActivity.this, LocationService.class));
                    stopSendingMessages();
                }
            }
        });


        // Solicitar actualizaciones de ubicación cada 10 segundos
        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, 0, locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private ScheduledExecutorService executor;
    private Runnable sendMessagesTask = new Runnable() {
        @Override
        public void run() {
            // Código para enviar el mensaje
            sendDataToWorker(null); // Enviar sin la ubicación actual para evitar problemas de concurrencia
            updateStatus("Enviando mensajes...");
        }
    };



    private void startSendingMessages() {
        final String ip = ipEditText.getText().toString();
        final int port = Integer.parseInt(portEditText.getText().toString());

        final String ip2 = ip2EditText.getText().toString();
        final int port2 = Integer.parseInt(port2EditText.getText().toString());

        final String ip3 = ip3EditText.getText().toString();
        final int port3 = Integer.parseInt(port3EditText.getText().toString());

        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // Código para enviar el mensaje
                sendDataToWorker(null); // Enviar sin la ubicación actual para evitar problemas de concurrencia
                updateStatus("Enviando mensajes...");

            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void stopSendingMessages() {
        sendingMessages = false;

        // Detener la tarea programada y las actualizaciones de ubicación
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }

        updateStatus("Mensajes detenidos");
    }

    private void updateStatus(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusTextView.setText(message);
            }
        });
    }



    private void sendDataToWorker(Location location) {
        final String ip = ipEditText.getText().toString();
        final int port = Integer.parseInt(portEditText.getText().toString());

        final String ip2 = ip2EditText.getText().toString();
        final int port2 = Integer.parseInt(port2EditText.getText().toString());

        final String ip3 = ip3EditText.getText().toString();
        final int port3 = Integer.parseInt(port3EditText.getText().toString());

        Data inputData = new Data.Builder()
                .putString("IP", ip)
                .putInt("Port", port)

                .putString("IP2", ip2)
                .putInt("Port2", port2)

                .putString("IP3", ip3)
                .putInt("Port3", port3)

                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class)
                .setInputData(inputData)
                .setInitialDelay(5, TimeUnit.SECONDS) // Actualizar después de 5 segundos
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);
    }

}

