package com.devapp.gps;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final int UDP_SERVER_PORT = 15000; // Puerto UDP del servidor
    private static final String SERVER_IP = "44.218.79.34/"; // Dirección IP del servidor

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient()); // Permite la navegación dentro del WebView

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Habilitar JavaScript (si es necesario)

        // Configuración para permitir contenido HTTP y HTTPS
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Carga una URL en el WebView
        webView.loadUrl("http://mapmydrive.ddns.net/index5.html"); // Reemplaza esto con la URL de tu página web

        // Verificar y solicitar permisos de ubicación si no están otorgados
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }

        // Inicializar el LocationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Obtener la última ubicación conocida
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        //if (lastKnownLocation != null) {
            // Enviar la ubicación por UDP
            //sendUDPMessage(lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude());
        //} else {
            Log.e("Location", "Última ubicación desconocida");
        //}
    }

    // Asegura que la navegación en el WebView siga dentro de la aplicación
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    private void sendUDPMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket udpSocket = new DatagramSocket();
                    InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                    byte[] buf = message.getBytes();

                    DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, UDP_SERVER_PORT);
                    udpSocket.send(packet);
                    udpSocket.close();
                } catch (Exception e) {
                    Log.e("UDP", "Error al enviar mensaje por UDP: " + e.getMessage());
                }
            }
        }).start();
    }


}
