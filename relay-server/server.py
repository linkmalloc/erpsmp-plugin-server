import os
import json
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# Track connected SSE clients
clients = set()
clients_lock = threading.Lock()

# Load ADMIN_TOKEN from secrets
admin_token = "ErpAdmin$2026!Secure"
try:
    secret_path = os.path.join(os.path.dirname(__file__), "../.secret/tokens.properties")
    if os.path.exists(secret_path):
        with open(secret_path, "r", encoding="utf-8") as f:
            for line in f:
                if line.startswith("ADMIN_TOKEN="):
                    admin_token = line.split("=", 1)[1].strip()
                    break
except Exception as e:
    print(f"Error loading admin token: {e}")

def broadcast(data):
    payload = f"data: {json.dumps(data)}\n\n"
    with clients_lock:
        to_remove = []
        for client in clients:
            try:
                client.wfile.write(payload.encode('utf-8'))
                client.wfile.flush()
            except Exception:
                to_remove.append(client)
        for client in to_remove:
            if client in clients:
                clients.remove(client)
    if to_remove:
        broadcast_spectator_count()

def broadcast_spectator_count():
    with clients_lock:
        count = 64 + len(clients)
    broadcast({"type": "spectators", "count": count})

class SSERelayHandler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()

    def do_GET(self):
        if self.path == '/events':
            self.send_response(200)
            self.send_header('Content-Type', 'text/event-stream')
            self.send_header('Cache-Control', 'no-cache')
            self.send_header('Connection', 'keep-alive')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()

            with clients_lock:
                clients.add(self)
            
            # Broadcast the new count
            broadcast_spectator_count()

            # Keep the connection alive
            try:
                while True:
                    # Send a heartbeat/ping every 20 seconds to keep connection alive
                    time.sleep(20)
                    try:
                        self.wfile.write(b": ping\n\n")
                        self.wfile.flush()
                    except Exception:
                        break
            finally:
                with clients_lock:
                    if self in clients:
                        clients.remove(self)
                broadcast_spectator_count()
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if self.path == '/api/publish':
            auth_header = self.headers.get('Authorization', '')
            expected_auth = f"Bearer {admin_token}"
            if auth_header != expected_auth:
                self.send_response(401)
                self.send_header('Access-Control-Allow-Origin', '*')
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({"error": "Unauthorized"}).encode('utf-8'))
                return

            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length)
            try:
                data = json.loads(body.decode('utf-8'))
            except Exception:
                self.send_response(400)
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                return

            cls = data.get('cls')
            icon = data.get('icon')
            text = data.get('text')

            if not cls or not icon or not text:
                self.send_response(400)
                self.send_header('Access-Control-Allow-Origin', '*')
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({"error": "Missing fields"}).encode('utf-8'))
                return

            # Broadcast to all SSE clients
            broadcast({"type": "event", "cls": cls, "icon": icon, "text": text})

            self.send_response(200)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"status": "ok"}).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

    # Disable default log writing to stdout/stderr to keep terminal clean
    def log_message(self, format, *args):
        pass

def run():
    port = int(os.environ.get("PORT", 8080))
    server = ThreadingHTTPServer(('0.0.0.0', port), SSERelayHandler)
    print(f"Relay server listening on port {port} (using SSE)...")
    server.serve_forever()

if __name__ == '__main__':
    run()
