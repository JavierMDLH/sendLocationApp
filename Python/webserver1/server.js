const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const mysql = require('mysql');
const dgram = require('dgram'); // Importa el módulo dgram para UDP

const app = express();
const server = http.createServer(app);
const io = socketIo(server);

// Configura una ruta para servir tu página HTML
app.get('/', (req, res) => {
  res.sendFile(__dirname + '/index.html');
});

// Conéctate a la base de datos
const db = mysql.createConnection({
  host: 'datos.cpx6dsnj7ocs.us-east-1.rds.amazonaws.com',
  user: 'rafael',
  password: 'rafael1108',
  database: 'datos',
});

// Conéctate a la base de datos
db.connect((err) => {
  if (err) {
    console.error('Error al conectar a la base de datos:', err);
    return;
  }
  console.log('Conexión a la base de datos establecida');
});

// Configura el servidor UDP
const udpServer = dgram.createSocket('udp4');
const UDP_PORT = 12345; // Reemplaza con el puerto UDP que deseas utilizar

udpServer.on('listening', () => {
  const address = udpServer.address();
  console.log(`Servidor UDP escuchando en ${address.address}:${address.port}`);
});

// Función para formatear la fecha
function formatDate(timestamp) {
    const year = timestamp.substr(0, 4);
    const month = timestamp.substr(4, 2);
    const day = timestamp.substr(6, 2);
    const hour = timestamp.substr(8, 2);
    const minute = timestamp.substr(10, 2);
    const second = timestamp.substr(12, 2);
    return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

udpServer.on('message', (message, remote) => {
  // Aquí puedes procesar los datos UDP recibidos y luego insertarlos en la base de datos
  console.log(`Datos UDP recibidos de ${remote.address}:${remote.port}: ${message}`);

  // Separar los datos en latitud, longitud, altitud y hora
  const received_data = message.toString();
  const latitud = received_data.split('Latitud ')[1].split(',')[0];
  const longitud = received_data.split('Longitud ')[1].split(',')[0];
  const altitud = received_data.split('Altitud ')[1].split(',')[0];
  const fecha_hora = formatDate(received_data.split('Hora: ')[1].trim());

  // Ejemplo de inserción en la base de datos
  const consulta = 'INSERT INTO datos (Latitud, Longitud, Altitud, Timestamp) VALUES (?, ?, ?, ?)';
  const valores = [latitud, longitud, altitud, fecha_hora];

  db.query(consulta, valores, (err, results) => {
    if (err) {
      console.error('Error al insertar datos en la base de datos:', err);
      return;
    }

    console.log('Datos insertados en la base de datos correctamente.');

    // Obtén los datos actualizados y envíalos inmediatamente a los clientes Socket.IO
    obtenerDatosActualizadosDesdeDB((err, data) => {
      if (err) {
        console.error('Error al obtener datos desde la base de datos:', err);
        return;
      }
      io.emit('update_data', data);
    });
  });
});

udpServer.bind(UDP_PORT);

// Resto de tu código de Socket.IO
io.on('connection', (socket) => {
  console.log('Un cliente se ha conectado');

  // Obtén los datos actualizados y envíalos cuando un cliente se conecta
  obtenerDatosActualizadosDesdeDB((err, data) => {
    if (err) {
      console.error('Error al obtener datos desde la base de datos:', err);
      return;
    }
    socket.emit('update_data', data);
  });

  // Maneja la desconexión del cliente
  socket.on('disconnect', () => {
    console.log('Un cliente se ha desconectado');
  });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`Servidor Socket.IO escuchando en el puerto ${PORT}`);
});

// Función para obtener datos actualizados desde la base de datos
function obtenerDatosActualizadosDesdeDB(callback) {
  // Realiza una consulta SQL para obtener tus datos desde la base de datos
  const consulta = 'SELECT Latitud, Longitud, Altitud, Timestamp FROM datos ORDER BY iddatos DESC LIMIT 1';

  db.query(consulta, (err, results) => {
    if (err) {
      callback(err, null);
      return;
    }

    if (results.length === 0) {
      // No se encontraron datos
      callback(null, []);
      return;
    }

    // Extrae los datos de la consulta
    const data = [
      results[0].Latitud,
      results[0].Longitud,
      results[0].Altitud,
      results[0].Timestamp,
    ];

    callback(null, data);
  });
}
