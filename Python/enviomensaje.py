import socket
import time

def enviar_mensaje_tcp(ip, puerto, mensaje):
    try:
        # Crear un socket TCP
        cliente_tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        
        # Conectar al servidor
        cliente_tcp.connect((ip, puerto))
        
        # Enviar el mensaje codificado en bytes
        cliente_tcp.send(mensaje.encode())
        
        # Cerrar el socket TCP
        cliente_tcp.close()
        
        print("Mensaje enviado exitosamente por TCP.")
    except Exception as e:
        print("Error TCP:", str(e))

def enviar_mensaje_udp(ip, puerto, mensaje):
    try:
        # Crear un socket UDP
        cliente_udp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        
        # Enviar el mensaje codificado en bytes
        cliente_udp.sendto(mensaje.encode(), (ip, puerto))
        
        print("Mensaje enviado exitosamente por UDP.")
    except Exception as e:
        print("Error UDP:", str(e))

# Solicitar el protocolo al usuario
protocolo = "UDP"

if protocolo == "TCP" or protocolo == "UDP":
    # IP, puerto y mensaje
    ip_destino = "ipserver.ddns.net"
    puerto_destino = 15000
    mensaje_a_enviar = "Ubicación: Latitud 10.93441182, Longitud -74.80862449, Altitud 58.44427490234375, Hora: 26/08/2023 18:55:59"

    while True:
        # Llamar a la función correspondiente según el protocolo elegido
        if protocolo == "TCP":
            enviar_mensaje_tcp(ip_destino, puerto_destino, mensaje_a_enviar)
        else:
            enviar_mensaje_udp(ip_destino, puerto_destino, mensaje_a_enviar)
        
        # Esperar 10 segundos antes de enviar el siguiente mensaje
        time.sleep(10)
else:
    print("Protocolo no válido. Debes elegir TCP o UDP.")