import socket
import mysql.connector
from datetime import datetime
#from app import socketio  #Importar el objeto socketio del archivo app.py
from flask_socketio import emit
import socketio

sio = socketio.Client()

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

#@socketio.on('update_data')
#def update_data(data):
#    # Actualizar los datos en la página cuando llega una actualización
#    emit('update_data', data, broadcast=True)
if not sio.connected:
    try:
        sio.connect('http://127.0.0.1:5000')
        print("Conectado al servidor")
    except Exception as e:
        print("Error de conexión:", e)

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

    # Formatear la fecha y hora al formato deseado
    fecha_formateada = fecha_obj.strftime('%Y%m%d%H%M%S')

    # Insertar datos en la base de datos
    query = "INSERT INTO datos (Latitud, Longitud, Altitud, Timestamp) VALUES (%s, %s, %s, %s)"
    values = (latitud, longitud, altitud, fecha_formateada)
    db_cursor.execute(query, values)
    db_connection.commit()
    print(f"Datos insertados: Latitud={latitud}, Longitud={longitud}, Altitud={altitud}, Timestamp={fecha_formateada}")

    # Emitir el evento de actualización a través de WebSocket
    
    #socketio.emit('update_data', (latitud, longitud, altitud, fecha_formateada))
    sio.emit('update_data',[latitud, longitud, altitud, fecha_formateada])
    #socketio.send('update_data')
    print('Datos emitidos')