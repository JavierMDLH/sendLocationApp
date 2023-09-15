// Importa el módulo http
const http = require('http');
// Importa el módulo socket.io
const socketIo = require('socket.io');
const mysql = require('mysql');
const dgram = require('dgram'); // Importa el módulo dgram para UDP

let mostrarDatosNuevos = false; // Variable para rastrear el estado del interruptor

const app = require('express')(); // Usa require para crear la aplicación express directamente
const server = http.createServer(app);
const io = socketIo(server);

const fs = require('fs');

// Lee el archivo de configuración
const configData = fs.readFileSync('var.json', 'utf8');
const config = JSON.parse(configData);

// Accede a las variables de entorno
const host = config.HOST.replace(/"/g, "'");
const user = config.USER.replace(/"/g, "'");
const password = config.PASSWORD.replace(/"/g, "'");
const database = config.DATABASE.replace(/"/g, "'");
console.log(`El valor de host es ${host}`);


// Configura una ruta para servir tu página HTML
app.get('/', (req, res) => {
  res.sendFile(__dirname + '/index.html');
});

// Conéctate a la base de datos
const db = mysql.createConnection({
  host: host,
  user: user,
  password: password,
  database: database,
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
const UDP_PORT = 15000; // Reemplaza con el puerto UDP que deseas utilizar

udpServer.on('listening', () => {
  const address = udpServer.address();
  console.log(`Servidor UDP escuchando en ${address.address}:${address.port}`);
});


udpServer.on('message', (message, remote) => {
  // Aquí puedes procesar los datos UDP recibidos y luego insertarlos en la base de datos
  //console.log(`Datos UDP recibidos de ${remote.address}:${remote.port}: ${message}`);
  console.log(`Datos UDP recibidos de ${remote.address}:${remote.port}`);

  // Separar los datos en latitud, longitud, altitud y hora
  const received_data = message.toString();
  const latitud = received_data.split('Latitud ')[1].split(',')[0];
  const longitud = received_data.split('Longitud ')[1].split(',')[0];
  const altitud = received_data.split('Altitud ')[1].split(',')[0];
  

  const fecha_hora ="'"+ received_data.split('Hora: ')[1].trim().replace(/[^0-9]/g, '')+"'";


  // Ejemplo de inserción en la base de datos
  const consulta = 'INSERT INTO datos (Latitud, Longitud, Altitud, Timestamp) VALUES (?, ?, ?, ?)';
  const valores = [latitud, longitud, altitud, fecha_hora];

  db.query(consulta, valores, (err, results) => {
    if (err) {
      console.error('Error al insertar datos en la base de datos:', err);
      return;
    }

    console.log('Datos insertados en la base de datos correctamente.',fecha_hora);

    if (mostrarDatosNuevos){
      // Emitir un evento a todos los clientes cuando haya nuevos datos
      //io.emit('update_data', valores); 
    }
    
    
  });
});

udpServer.bind(UDP_PORT);

// Resto de tu código de Socket.IO
io.on('connection', (socket) => {
  console.log('Un cliente se ha conectado');

  socket.on('activar_switch', () => {
      mostrarDatosNuevos = true;
      console.log('Mostrar datos nuevos activado');
  });

  socket.on('desactivar_switch', () => {
      mostrarDatosNuevos = false;
      console.log('Mostrar datos nuevos desactivado');
  });
  console.log(mostrarDatosNuevos);
  if (mostrarDatosNuevos){
    // Obtén los datos actualizados y envíalos cuando un cliente se conecta
    obtenerDatosActualizadosDesdeDB((err, data) => {
      if (err) {
        console.error('Error al obtener datos desde la base de datos:', err);
        return;
      }
      socket.emit('update_data', data);
    });

  }else{

    socket.on('date_range_selected', (fechas) => {
      // Obtener datos dentro del rango de fechas especificado
      obtenerDatosEnRangoDesdeDB(fechas.fechaInicial, fechas.fechaFinal, (err, data) => {
        if (err) {
          console.error('Error al obtener datos desde la base de datos:', err);
          return;
        }
  
        // Enviar datos al cliente
        socket.emit('update_data', data);
      });
    });
    
  }
  

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
  // Realiza una consulta SQL para obtener los datos más recientes desde la base de datos
  const consulta = 'SELECT Latitud, Longitud, Altitud, Timestamp FROM datos ORDER BY iddatos DESC LIMIT 1';
  console.log('Datos nuevos');
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
    console.log(results[0].Timestamp);
  });
}  

function obtenerDatosEnRangoDesdeDB(fechaInicial,fechaFinal,callback){
  // Si mostrarDatosNuevos está desactivado, obtener datos dentro del rango de fechas
  const consulta = 'SELECT Latitud, Longitud, Altitud, Timestamp FROM datos WHERE Timestamp BETWEEN ? AND ? ORDER BY Timestamp';
  const valores = [fechaInicial, fechaFinal];

  db.query(consulta, valores, (err, results) => {
    if (err) {
      callback(err, null);
      return;
    }
    console.log('Extrayendo datos')
    // Extraer los datos de la consulta
    const data = results.map((row) => ({
      latitud: row.Latitud,
      longitud: row.Longitud,
      altitud: row.Altitud,
      timestamp: row.Timestamp,
    }));

    callback(null, data);
  });
}  

