#!/usr/bin/env python3
"""
Servidor simple para probar la conexión con la app Android
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import json
from datetime import datetime, timedelta

app = Flask(__name__)
CORS(app)  # Permitir CORS para conexiones desde Android

# Simulación de usuarios válidos
VALID_USERS = {
    "kassandra": "S,oredt2123#",
    "admin": "admin123",
    "test": "test123"
}

# Simulación de votaciones (basadas en tu sitio web)
VOTACIONES = {
    1: {
        "id": 1,
        "pregunta": "Fecha de proxima asamblea?",
        "fecha_cierre": "2025-10-30T12:00:00",
        "activa": True,
        "esta_abierta": True,
        "opciones": [
            {"id": 1, "texto": "Lunes 3 de noviembre"},
            {"id": 2, "texto": "Martes 4 de noviembre"},
            {"id": 3, "texto": "Miércoles 5 de noviembre"}
        ],
        "votos": {1: 3, 2: 1, 3: 2}
    },
    2: {
        "id": 2,
        "pregunta": "rrr",
        "fecha_cierre": "2025-10-27T23:50:00",
        "activa": True,
        "esta_abierta": True,
        "opciones": [
            {"id": 4, "texto": "Opción A"},
            {"id": 5, "texto": "Opción B"}
        ],
        "votos": {4: 2, 5: 1}
    },
    3: {
        "id": 3,
        "pregunta": "test",
        "fecha_cierre": "2025-10-19T23:11:00",
        "activa": False,
        "esta_abierta": False,
        "opciones": [
            {"id": 6, "texto": "Sí"},
            {"id": 7, "texto": "No"}
        ],
        "votos": {6: 5, 7: 3}
    }
}

# Usuarios que ya votaron (simulación)
USUARIOS_VOTARON = {
    "kassandra": {1: 1, 2: 5}  # votacion_id: opcion_votada_id
}

@app.route('/usuarios/api/login/', methods=['POST'])
def login():
    try:
        # Verificar que el contenido sea JSON
        if not request.is_json:
            print("Error: Content-Type no es application/json")
            return jsonify({
                "success": False,
                "message": "Content-Type debe ser application/json"
            }), 400
            
        data = request.get_json()
        
        # Verificar que los datos no sean None
        if data is None:
            print("Error: Datos JSON vacíos o inválidos")
            return jsonify({
                "success": False,
                "message": "Datos JSON vacíos o inválidos"
            }), 400
            
        username = data.get('username', '')
        password = data.get('password', '')
        
        print(f"Intento de login: usuario={username}, password={'*' * len(password)}")
        print(f"Datos recibidos: {data}")
        
        # Verificar que los campos requeridos estén presentes
        if not username or not password:
            print("Error: Campos username o password faltantes")
            return jsonify({
                "success": False,
                "message": "Los campos username y password son requeridos"
            }), 400
        
        # Verificar credenciales
        if username in VALID_USERS and VALID_USERS[username] == password:
            return jsonify({
                "success": True,
                "message": "Login exitoso",
                "token": f"token_{username}_{hash(password)}",  # Token simple para autenticación
                "user": {
                    "username": username,
                    "id": 1
                }
            }), 200
        else:
            # Determinar si el usuario existe pero la contraseña es incorrecta
            if username in VALID_USERS:
                error_msg = "Contraseña incorrecta"
            else:
                error_msg = "Usuario no encontrado"
            
            return jsonify({
                "success": False,
                "message": error_msg
            }), 401
            
    except Exception as e:
        print(f"Error en login: {e}")
        return jsonify({
            "success": False,
            "message": "Error interno del servidor"
        }), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "OK", "message": "Servidor funcionando"}), 200

@app.route('/usuarios/api/test/', methods=['POST'])
def test_endpoint():
    try:
        data = request.get_json()
        print(f"Endpoint de prueba recibió: {data}")
        return jsonify({
            "success": True,
            "message": "Endpoint de prueba funcionando",
            "received_data": data
        }), 200
    except Exception as e:
        print(f"Error en endpoint de prueba: {e}")
        return jsonify({
            "success": False,
            "message": f"Error: {str(e)}"
        }), 500

def verificar_token(token):
    """Verifica si el token es válido y devuelve el usuario"""
    if not token or not token.startswith("Token "):
        return None
    # El token viene como "Token token_kassandra_123456"
    token_part = token.replace("Token ", "")
    if token_part.startswith("token_"):
        # Remover el prefijo "token_" y extraer el username
        username = token_part.split("_")[1]  # token_kassandra_123456 -> kassandra
        return username if username in VALID_USERS else None
    return None

@app.route('/votaciones/api/v1/abiertas/', methods=['GET'])
def listar_votaciones_abiertas():
    try:
        auth_header = request.headers.get('Authorization')
        if not auth_header:
            return jsonify({"error": "Token de autorización requerido"}), 401
        
        username = verificar_token(auth_header)
        if not username:
            return jsonify({"error": "Token inválido"}), 401
        
        votaciones_abiertas = []
        for votacion_id, votacion in VOTACIONES.items():
            if votacion["esta_abierta"]:
                # Verificar si el usuario ya votó
                ya_vote = username in USUARIOS_VOTARON and votacion_id in USUARIOS_VOTARON[username]
                opcion_votada_id = USUARIOS_VOTARON[username].get(votacion_id) if ya_vote else None
                
                votacion_data = {
                    "id": votacion["id"],
                    "pregunta": votacion["pregunta"],
                    "fecha_cierre": votacion["fecha_cierre"],
                    "activa": votacion["activa"],
                    "esta_abierta": votacion["esta_abierta"],
                    "opciones": votacion["opciones"],
                    "ya_vote": ya_vote,
                    "opcion_votada_id": opcion_votada_id
                }
                votaciones_abiertas.append(votacion_data)
        
        return jsonify(votaciones_abiertas), 200
        
    except Exception as e:
        print(f"Error en listar votaciones: {e}")
        return jsonify({"error": "Error interno del servidor"}), 500

@app.route('/votaciones/api/v1/cerradas/', methods=['GET'])
def listar_votaciones_cerradas():
    try:
        auth_header = request.headers.get('Authorization')
        if not auth_header:
            return jsonify({"error": "Token de autorización requerido"}), 401
        
        username = verificar_token(auth_header)
        if not username:
            return jsonify({"error": "Token inválido"}), 401
        
        votaciones_cerradas = []
        for votacion_id, votacion in VOTACIONES.items():
            if not votacion["esta_abierta"]:
                # Para votaciones cerradas, no importa si el usuario votó
                votacion_data = {
                    "id": votacion["id"],
                    "pregunta": votacion["pregunta"],
                    "fecha_cierre": votacion["fecha_cierre"],
                    "activa": votacion["activa"],
                    "esta_abierta": votacion["esta_abierta"],
                    "opciones": votacion["opciones"],
                    "ya_vote": False,  # No relevante para votaciones cerradas
                    "opcion_votada_id": None
                }
                votaciones_cerradas.append(votacion_data)
        
        return jsonify(votaciones_cerradas), 200
        
    except Exception as e:
        print(f"Error en listar votaciones cerradas: {e}")
        return jsonify({"error": "Error interno del servidor"}), 500

@app.route('/votaciones/api/v1/<int:votacion_id>/votar/', methods=['POST'])
def votar(votacion_id):
    try:
        auth_header = request.headers.get('Authorization')
        if not auth_header:
            return jsonify({"ok": False, "mensaje": "Token de autorización requerido"}), 401
        
        username = verificar_token(auth_header)
        if not username:
            return jsonify({"ok": False, "mensaje": "Token inválido"}), 401
        
        data = request.get_json()
        if not data or 'opcion_id' not in data:
            return jsonify({"ok": False, "mensaje": "ID de opción requerido"}), 400
        
        opcion_id = data['opcion_id']
        
        # Verificar que la votación existe y está abierta
        if votacion_id not in VOTACIONES:
            return jsonify({"ok": False, "mensaje": "Votación no encontrada"}), 404
        
        votacion = VOTACIONES[votacion_id]
        if not votacion["esta_abierta"]:
            return jsonify({"ok": False, "mensaje": "La votación está cerrada"}), 400
        
        # Verificar que la opción existe
        opciones_ids = [op["id"] for op in votacion["opciones"]]
        if opcion_id not in opciones_ids:
            return jsonify({"ok": False, "mensaje": "Opción no válida"}), 400
        
        # Verificar si ya votó
        if username in USUARIOS_VOTARON and votacion_id in USUARIOS_VOTARON[username]:
            return jsonify({"ok": False, "mensaje": "Ya has votado en esta votación"}), 400
        
        # Registrar el voto
        if username not in USUARIOS_VOTARON:
            USUARIOS_VOTARON[username] = {}
        USUARIOS_VOTARON[username][votacion_id] = opcion_id
        
        # Actualizar contador de votos
        votacion["votos"][opcion_id] = votacion["votos"].get(opcion_id, 0) + 1
        
        return jsonify({"ok": True, "mensaje": "Voto registrado exitosamente"}), 200
        
    except Exception as e:
        print(f"Error en votar: {e}")
        return jsonify({"ok": False, "mensaje": "Error interno del servidor"}), 500

@app.route('/votaciones/api/v1/<int:votacion_id>/resultados/', methods=['GET'])
def resultados_votacion(votacion_id):
    try:
        auth_header = request.headers.get('Authorization')
        if not auth_header:
            return jsonify({"error": "Token de autorización requerido"}), 401
        
        username = verificar_token(auth_header)
        if not username:
            return jsonify({"error": "Token inválido"}), 401
        
        # Verificar que la votación existe
        if votacion_id not in VOTACIONES:
            return jsonify({"error": "Votación no encontrada"}), 404
        
        votacion = VOTACIONES[votacion_id]
        
        # Calcular total de votos
        total_votos = sum(votacion["votos"].values())
        
        # Preparar resultados por opción
        resultados_opciones = []
        for opcion in votacion["opciones"]:
            votos = votacion["votos"].get(opcion["id"], 0)
            resultados_opciones.append({
                "opcion_id": opcion["id"],
                "texto": opcion["texto"],
                "votos": votos
            })
        
        resultado = {
            "votacion": {
                "id": votacion["id"],
                "pregunta": votacion["pregunta"]
            },
            "total_votos": total_votos,
            "opciones": resultados_opciones
        }
        
        return jsonify(resultado), 200
        
    except Exception as e:
        print(f"Error en resultados: {e}")
        return jsonify({"error": "Error interno del servidor"}), 500

if __name__ == '__main__':
    print("Iniciando servidor en http://127.0.0.1:8001")
    print("Para Android Emulator, usar: http://10.0.2.2:8001")
    print("Usuarios válidos:")
    for user in VALID_USERS:
        print(f"  - {user}: {VALID_USERS[user]}")
    
    app.run(host='0.0.0.0', port=8001, debug=True)
