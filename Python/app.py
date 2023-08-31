from flask import Flask, render_template
from flask_socketio import SocketIO, emit
import mysql.connector
from datetime import datetime

app = Flask(__name__)
socketio = SocketIO(app)


# Ruta de inicio para mostrar los últimos datos en la página web
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

# Función para convertir el objeto datetime a una cadena formateada
def format_datetime_to_string(dt):
    return dt.strftime('%Y%m%d%H%M%S')

# Manejar el evento de actualización de datos desde el cliente
@socketio.on('update_data')
def update_data(data):
    # Obtener los últimos datos de la base de datos
    db_connection = mysql.connector.connect(
        host="localhost",
        user="root",
        password="Javier3021194",
        database="proyecto1"
    )
    db_cursor = db_connection.cursor()

    query = "SELECT Latitud, Longitud, Altitud, Timestamp FROM datos ORDER BY iddatos DESC LIMIT 1"
    db_cursor.execute(query)
    last_data = db_cursor.fetchone()

    # Convertir el objeto datetime a una cadena formateada
    timestamp_str = format_datetime_to_string(last_data[3])

    # Crear una nueva tupla con la cadena formateada en lugar del objeto datetime
    updated_data = [last_data[0], last_data[1], last_data[2], timestamp_str]
    
    # Emitir el evento de actualización con los datos formateados
    emit('update_data', updated_data, broadcast=True)
    print("Datos actualizados emitidos")
    print(updated_data)

if __name__ == '__main__':
    socketio.run(app,host='127.0.0.1',port=5000, debug=True, allow_unsafe_werkzeug=True)
