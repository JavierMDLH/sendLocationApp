import socket   

mi_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
mi_socket.connect(('127.0.0.1',15001))

respuesta = mi_socket.recv(1024)
print(respuesta)
mi_socket.close()

