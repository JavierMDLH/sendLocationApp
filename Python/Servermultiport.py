import socket
import threading

def manejar_cliente(client_socket, client_address):
    try:
        while True:
            datos = client_socket.recv(1024)
            if not datos:
                break
            print('Paquete recibido desde', client_address, ':', datos.decode('utf-8'))
    except:
        pass
    client_socket.close()

def crear_servidor(puerto):
    direccion = '0.0.0.0'
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((direccion, puerto))
    server_socket.listen(5)
    
    
    
    while True:
        client_socket, client_address = server_socket.accept()
        #print('Conexión establecida desde:', client_address, 'en puerto:', puerto)
        client_thread = threading.Thread(target=manejar_cliente, args=(client_socket, client_address))
        client_thread.start()

puerto_inicial = 15000
puerto_final = 15100  # El puerto final debe ser mayor que el puerto inicial
print('Escuchando en puertos',puerto_inicial,' - ',puerto_final)
threads = []

try:
    for puerto in range(puerto_inicial, puerto_final + 1):
        thread = threading.Thread(target=crear_servidor, args=(puerto,))
        threads.append(thread)
        thread.start()
except KeyboardInterrupt:
    print('Cerrando servidores...')
    for thread in threads:
        thread.join()
