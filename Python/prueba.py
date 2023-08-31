import socket
import mysql.connector
from datetime import datetime
from app import socketio  # Asegúrate de importar el objeto socketio del archivo app.py

UDP_IP = "0.0.0.0"
UDP_PORT = 15000

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))

# Establecer conexión con MySQL
db_connection = mysql.connector.connect(
    host="localhost",
    user="root",
    password="Javier3021194",
    database="proyecto1"
)
db_cursor = db_connection.cursor()

while True:
    data, addr = sock.recvfrom(1024)
    received_data = data.decode('utf-8')
    print(f"Datos recibidos de {addr}: {received_data}")

    # Separar los datos en latitud, longitud, altitud y hora
    latitud = received_data.split('Latitud ')[1].split(',')[0]
    longitud = received_data.split('Longitud ')[1].split(',')[0]
    altitud = received_data.split('Altitud ')[1].split(',')[0]
    fecha_hora = received_data.split('Hora: ')[1].strip()

    # Convertir la fecha y hora a un objeto datetime
    fecha_obj = datetime.strptime(fecha_hora, '%d/%m/%Y %H:%M:%S')

    # Insertar datos en la base de datos
    query = "INSERT INTO datos (Latitud, Longitud, Altitud, Timestamp) VALUES (%s, %s, %s, %s)"
    values = (latitud, longitud, altitud, fecha_obj)
    db_cursor.execute(query, values)
    db_connection.commit()
    print(f"Datos insertados: Latitud={latitud}, Longitud={longitud}, Altitud={altitud}, Timestamp={fecha_obj}")

    # Emitir el evento de actualización a través de WebSocket
    socketio.emit('update_data', (latitud, longitud, altitud, fecha_obj))




from flask import Flask, render_template
from flask_socketio import SocketIO,emit
import mysql.connector

app = Flask(__name__)
socketio = SocketIO(app)

@app.route('/')
def index():
    # Conectarse a la base de datos
    db_connection = mysql.connector.connect(
        host="localhost",
        user="root",
        password="Javier3021194",
        database="proyecto1"
    )
    db_cursor = db_connection.cursor()

    # Consultar los últimos datos insertados
    query = "SELECT Latitud, Longitud, Altitud, Timestamp FROM datos ORDER BY iddatos DESC LIMIT 1"
    db_cursor.execute(query)
    last_data = db_cursor.fetchone()

    return render_template('index.html', data=last_data)

@socketio.on('update_data')
def update_data(data):
    # Actualizar los datos en la página cuando llega una actualización
    emit('update_data', data, broadcast=True)

if __name__ == '__main__':
    socketio.run(app, debug=True, allow_unsafe_werkzeug=True)



<!DOCTYPE html>
<html>
<head>
    <title>Últimos datos recibidos</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/socket.io/4.1.2/socket.io.js"></script>
</head>
<body>
    <h1>Últimos datos recibidos:</h1>
    <p>Latitud: <span id="latitud">{{ data[0] }}</span></p>
    <p>Longitud: <span id="longitud">{{ data[1] }}</span></p>
    <p>Altitud: <span id="altitud">{{ data[2] }}</span></p>
    <p>Timestamp: <span id="timestamp">{{ data[3] }}</span></p>

    <script>
        const socket = io.connect('http://' + document.domain + ':' + location.port);

        socket.on('connect', function() {
            console.log('Conectado al servidor WebSocket');
        });

        socket.on('disconnect', function() {
            console.log('Desconectado del servidor WebSocket');
        });

        socket.on('update_data', function(data) {
            document.getElementById('latitud').innerText = data[0];
            document.getElementById('longitud').innerText = data[1];
            document.getElementById('altitud').innerText = data[2];
            document.getElementById('timestamp').innerText = data[3];
        });
    </script>
</body>
</html>
