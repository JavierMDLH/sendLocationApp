from flask import Flask
from flask_socketio import SocketIO, emit

app = Flask(__name__)
socketio = SocketIO(app)

@socketio.on('update_data')
def update_data(data):
    emit('update_data', data, broadcast=True)

if __name__ == '__main__':
    socketio.run(app, debug=True, allow_unsafe_werkzeug=True)