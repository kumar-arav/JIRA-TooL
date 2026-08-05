class WebSocketClient {
  private ws: WebSocket | null = null;
  private listeners: Set<(data: any) => void> = new Set();
  private reconnectTimer: ReturnType<typeof setInterval> | null = null;
  private connectedEmail: string | null = null;

  connect() {
    let email = "";

    try {
      const saved = localStorage.getItem("fs_auth");
      if (saved) {
        const authData = JSON.parse(saved);
        if (authData.user?.email) {
          email = authData.user.email;
        }
      }
    } catch (e) {
      console.error(e);
    }

    if (
      this.ws &&
      (this.ws.readyState === WebSocket.OPEN ||
        this.ws.readyState === WebSocket.CONNECTING)
    ) {
      if (this.connectedEmail === email) {
        return;
      }
      this.ws.close();
    }

    this.connectedEmail = email;

    const backendUrl =
      https://jira-tool-1.onrender.com;

    const wsBase = backendUrl
      .replace(/^https:/, "wss:")
      .replace(/^http:/, "ws:/");

    const wsUrl = `${wsBase}/api/ws${
      email ? `?email=${encodeURIComponent(email)}` : ""
    }`;

    console.log("Connecting to:", wsUrl);

    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log("WebSocket connected");

      if (this.reconnectTimer) {
        clearInterval(this.reconnectTimer);
        this.reconnectTimer = null;
      }
    };

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        this.listeners.forEach((listener) => listener(data));
      } catch (e) {
        console.error("Failed to parse WebSocket message:", e);
      }
    };

    this.ws.onclose = () => {
      console.log("WebSocket disconnected. Reconnecting...");
      this.scheduleReconnect();
    };

    this.ws.onerror = (err) => {
      console.error("WebSocket error:", err);
      this.ws?.close();
    };
  }

  private scheduleReconnect() {
    if (!this.reconnectTimer) {
      this.reconnectTimer = setInterval(() => {
        this.connect();
      }, 5000);
    }
  }

  subscribe(callback: (data: any) => void) {
    this.listeners.add(callback);

    return () => {
      this.listeners.delete(callback);
    };
  }

  send(data: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data));
    } else {
      console.warn("WebSocket is not connected.", data);
    }
  }
}

export const wsClient = new WebSocketClient();
