// Importa los módulos necesarios
const http = require('http');
const socketIo = require('socket.io');
const mysql = require('mysql');
const dgram = require('dgram'); // Importa el módulo dgram para UDP
const express = require('express'); // Importa Express
const fs = require('fs');
const path = require('path'); // Importa el módulo path

let mostrarDatosNuevos = true; // Variable para rastrear el estado del interruptor
let fechaInicial = 0;
let fechaFinal = 0;

// Crea una instancia de Express
const app = express();

// Configura Express para servir archivos estáticos desde la carpeta actual (__dirname)
app.use(express.static(__dirname));

// Crea un servidor HTTP usando Express
const server = http.createServer(app);
const io = socketIo(server);

// Lee el archivo de configuración localmente
const configData = fs.readFileSync('./var.json', 'utf8');
const config = JSON.parse(configData);

// Accede a las variables de entorno desde el archivo local
const host = config.HOST.replace(/"/g, "'");
const user = config.USER.replace(/"/g, "'");
const password = config.PASSWORD.replace(/"/g, "'");
const database = config.DATABASE.replace(/"/g, "'");
const name = config.NAME.replace(/"/g, "'");
console.log(`El valor de host es ${host}`);

// Configura una ruta para servir tu página HTML
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html')); // Utiliza path.join para construir la ruta del archivo HTML
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
  
  const hora = received_data.split('Hora: ')[1].trim().replace(/[^0-9]/g, '');

  // Obtén el año, mes y día de la cadena original
  const anio = "20" + hora.slice(4, 6);
  const mes = hora.slice(2, 4);
  const dia = hora.slice(0, 2);

  // Crea la nueva cadena con el formato deseado
  const nueva_fecha_hora = anio + mes + dia + hora.slice(6);


  // Ejemplo de inserción en la base de datos
  const consulta = 'INSERT INTO datos (Latitud, Longitud, Altitud, Timestamp) VALUES (?, ?, ?, ?)';
  const valores = [latitud, longitud, altitud, nueva_fecha_hora];

  db.query(consulta, valores, (err, results) => {
    if (err) {
      console.error('Error al insertar datos en la base de datos:', err);
      return;
    }

    console.log('Datos insertados en la base de datos correctamente.',nueva_fecha_hora);
    console.log('mostrar datos nuevos: ',mostrarDatosNuevos);
    if (mostrarDatosNuevos){
      // Emitir un evento a todos los clientes cuando haya nuevos datos
      io.emit('update_data', valores); 
    }
    
    
  });
});

udpServer.bind(UDP_PORT);

// Resto de código de Socket.IO
io.on('connection', (socket) => {
  console.log('Un cliente se ha conectado');
  

  socket.on('Pagina1', () => {
      mostrarDatosNuevos = true;
      console.log('pag1');
      console.log('Mostrar datos nuevos activado');
  });

  socket.on('Pagina2', () => {
    mostrarDatosNuevos = false;
    console.log('pag2');
    console.log('Mostrar datos nuevos desactivado');
  });

  socket.on('desactivar_switch',(fechaInicial, fechaFinal,lat,long,RadioKm) => {
      mostrarDatosNuevos = false;
      fechaInicial = fechaInicial;
      fechaFinal = fechaFinal;
      lat = lat;
      long = long;
      RadioKm = RadioKm;
      console.log('Mostrar datos nuevos desactivado');
      console.log('Consulta rango de tiempo');
      // Obtener datos dentro del rango de fechas especificado
      obtenerDatosEnRangoDesdeDB(fechaInicial,fechaFinal,lat,long,RadioKm, (err, data) => {
        if (err) {
          console.error('Error al obtener datos desde la base de datos:', err);
          return;
        }
    
        // Enviar datos al cliente
        io.emit('update_table', data);
        console.log('Tabla enviada');
      });  
  });

  console.log(mostrarDatosNuevos);



  if (mostrarDatosNuevos){
    // Obtén los datos actualizados y envíalos cuando un cliente se conecta
    obtenerDatosActualizadosDesdeDB((err, data) => {
      if (err) {
        console.error('Error al obtener datos desde la base de datos:', err);
        return;
      }
      io.emit('update_data', data);
    });

  }

  socket.on('location',(latitud, longitud, RadioKm) => {
    latitud = latitud;
    longitud = longitud;
    RadioKm = RadioKm;
    console.log('Consulta por localizacion');
    buscarLocalizacionesEnArea(latitud,longitud,RadioKm, (err, formattedResults) => {
      if (err) {
        console.error('Error al obtener datos desde la base de datos:', err);
        return;
      }
  
      // Enviar datos al cliente
      io.emit('places', formattedResults);
      console.log('localizaciones enviadas');
    });  


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


// Función para buscar localizaciones en un área específica
function buscarLocalizacionesEnArea(latitud, longitud, radioKm, callback) {
  // Convertir el radio de kilómetros a grados aproximados (1 grado de latitud ~ 111.32 km)
  const radioGrados = radioKm / 111.32;

  // Calcular los límites del área de búsqueda
  const latitudMin = latitud - radioGrados;
  const latitudMax = latitud + radioGrados;
  const longitudMin = longitud - (radioGrados / Math.cos(latitud * (Math.PI / 180)));
  const longitudMax = longitud + (radioGrados / Math.cos(latitud * (Math.PI / 180)));

  // Consulta SQL para buscar localizaciones dentro del área
  const query = `
    SELECT Latitud, Longitud, Altitud, Timestamp FROM datos
    WHERE Latitud BETWEEN ? AND ?
    AND Longitud BETWEEN ? AND ?
  `;

  // Ejecutar la consulta SQL con los límites del área
  db.query(query, [latitudMin, latitudMax, longitudMin, longitudMax], function (error, results, fields) {
    if (error) {
      console.error('Error al ejecutar la consulta:', error);
      return callback(error, null);
    }

    // Los resultados de la consulta se encuentran en la variable "results"
    console.log('Resultados de la consulta:', results);

    // Formatear los resultados si es necesario
    const formattedResults = results.map((row) => ({
      latitud: row.Latitud,
      longitud: row.Longitud,
      altitud: row.Altitud,
      timestamp: row.Timestamp,
    }));

    callback(null, formattedResults);
  });
}




function obtenerDatosEnRangoDesdeDB(fechaInicial,fechaFinal,lat,long,RadioKm,callback){
  // Si mostrarDatosNuevos está desactivado, obtener datos dentro del rango de fechas
  // Convertir el radio de kilómetros a grados aproximados (1 grado de latitud ~ 111.32 km)
  const radioGrados = RadioKm / 111.32;

  // Calcular los límites del área de búsqueda
  const latitudMin = lat - radioGrados;
  const latitudMax = lat + radioGrados;
  const longitudMin = long - (radioGrados / Math.cos(lat * (Math.PI / 180)));
  const longitudMax = long + (radioGrados / Math.cos(lat * (Math.PI / 180)));

  let consulta = `SELECT Latitud, Longitud, Altitud, Timestamp 
  FROM datos 
  WHERE Timestamp BETWEEN ? AND ?`;

  const valores = [fechaInicial, fechaFinal];

  if (RadioKm > 0) {
    consulta += ` AND Latitud BETWEEN ? AND ? AND Longitud BETWEEN ? AND ?`;
    valores.push(latitudMin, latitudMax, longitudMin, longitudMax);
  }

  consulta += ` ORDER BY Timestamp;`;

  console.log('Fecha inicial: ', fechaInicial);
  console.log('Fecha final: ', fechaFinal);
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
