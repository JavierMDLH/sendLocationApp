const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const mysql = require('mysql');

const app = express();
const server = http.createServer(app);
const io = socketIo(server);

// Configura una ruta para servir tu página HTML
app.get('/', (req, res) => {
  res.sendFile(__dirname + '/index.html');
});

// Conéctate a la base de datos
const db = mysql.createConnection({
    host: 'database.cydkk4u9vzj2.us-east-1.rds.amazonaws.com', // Cambia esto con la dirección de tu servidor de base de datos
    user: 'admin', // Cambia esto con tu nombre de usuario de MySQL
    password: 'javier3021194', // Cambia esto con tu contraseña de MySQL
    database: 'proyecto2', // Cambia esto con el nombre de tu base de datos
  });

// Conéctate a la base de datos
db.connect((err) => {
    if (err) {
      console.error('Error al conectar a la base de datos:', err);
      return;
    }
    console.log('Conexión a la base de datos establecida');
  });
  
  // Maneja conexiones de clientes Socket.IO
  io.on('connection', (socket) => {
    console.log('Un cliente se ha conectado');
  
    // Ejemplo: Obtiene datos actualizados de la base de datos cada 5 segundos
    setInterval(() => {
      obtenerDatosActualizadosDesdeDB((err, data) => {
        if (err) {
          console.error('Error al obtener datos desde la base de datos:', err);
          return;
        }
        socket.emit('update_data', data);
      });
    }, 5000);
  
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