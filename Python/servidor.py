import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(('0.0.0.0',15000))


while True:
    data, addr = sock.recvfrom(1024)
    received_data = data.decode('utf-8')
    #conexion,addr = sock.accept()
    print('Conexion establecida',addr)
    print (received_data)
    sock.sendto(data,('127.0.0.1',15001))
