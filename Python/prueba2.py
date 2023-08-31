import socket
import mysql.connector
from datetime import datetime
from flask import Flask, render_template
from flask_socketio import SocketIO, emit

app = Flask(__name__)
socketio = SocketIO(app)

UDP_IP = "127.0.0.1"
UDP_PORT = 15000

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))

db_connection = mysql.connector.connect(
    host="localhost",
    user="root",
    password="Javier3021194",
    database="proyecto1"
)
db_cursor = db_connection.cursor()

# Función para convertir el objeto datetime a una cadena formateada
def format_datetime_to_string(dt):
    return dt.strftime('%Y%m%d%H%M%S')

@app.route('/')
def index():
    # Consultar los últimos datos insertados
    query = "SELECT Latitud, Longitud, Altitud, Timestamp FROM datos ORDER BY iddatos DESC LIMIT 1"
    db_cursor.execute(query)
    last_data = db_cursor.fetchone()

    return render_template('index.html', data=last_data)

@socketio.on('update_data')
def handle_update(data):
    # Obtener los últimos datos de la base de datos
    db_cursor.execute(query)
    last_data = db_cursor.fetchone()

    # Convertir el objeto datetime a una cadena formateada
    timestamp_str = format_datetime_to_string(last_data[3])

    # Crear una nueva tupla con la cadena formateada en lugar del objeto datetime
    updated_data = (last_data[0], last_data[1], last_data[2], timestamp_str)

    # Emitir el evento de actualización con los datos formateados
    emit('update_data', updated_data, broadcast=True)
    print("Datos actualizados emitidos")

if __name__ == '__main__':
    socketio.run(app, host='127.0.0.1', port=5000, debug=True, allow_unsafe_werkzeug=True)
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
        socketio.emit('update_data', (latitud, longitud, altitud, fecha_formateada))
        
