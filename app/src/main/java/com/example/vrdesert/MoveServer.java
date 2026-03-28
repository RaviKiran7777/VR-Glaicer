package com.example.vrdesert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MoveServer extends Thread {
    private ServerSocket serverSocket;
    private boolean running = true;
    private final VRRenderer vrRenderer;

    public MoveServer(VRRenderer vrRenderer) {
        this.vrRenderer = vrRenderer;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(8080);
            while (running) {
                Socket client = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String requestLine = in.readLine();
                
                if (requestLine != null) {
                    OutputStream out = client.getOutputStream();
                    
                    if (requestLine.contains("GET /control ") || requestLine.contains("GET /control?")) {
                        String html = "<html><head>" +
                            "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no, maximum-scale=1.0'>" +
                            "<style>" +
                            "body { background: #112; color: #fff; font-family: sans-serif; margin: 0; padding: 20px; text-align: center; overscroll-behavior: none; }" +
                            ".card { background: #223; padding: 20px; border-radius: 10px; margin-bottom: 20px; box-shadow: 0 4px 8px rgba(0,0,0,0.5); }" +
                            "h1 { color: #00f0ff; font-size: 28px; }" +
                            "#fact-box { color: #55ff55; font-size: 1.2em; font-weight: bold; min-height: 50px; text-align: left; }" +
                            "#inv-box { text-align: left; }" +
                            "#walk-btn { background: #00f0ff; color: #000; padding: 20px; font-size: 24px; font-weight: bold; border-radius: 10px; user-select: none; width: 100%; box-sizing: border-box; touch-action: none;}" +
                            "#walk-btn:active { background: #0088aa; }" +
                            "</style></head><body>" +
                            "<h1>Glacier Dashboard</h1>" +
                            "<div class='card'>" +
                            "  <h2 style='text-align:left; color:#aaa; font-size:16px;'>Recently Discovered Fact:</h2>" +
                            "  <div id='fact-box'>Waiting for observer...</div>" +
                            "</div>" +
                            "<div class='card'>" +
                            "  <h2 style='text-align:left; color:#aaa; font-size:16px;'>Data Log:</h2>" +
                            "  <div id='inv-box'>No Data</div>" +
                            "</div>" +
                            "<div id='walk-btn'>WALK FORWARD</div>" +
                            "<script>" +
                            "document.getElementById('walk-btn').addEventListener('touchstart', (e) => { e.preventDefault(); fetch('/move/click'); }, {passive: false});" +
                            "document.getElementById('walk-btn').addEventListener('mousedown', (e) => { e.preventDefault(); fetch('/move/click'); });" +
                            "setInterval(() => {" +
                            "  fetch('/status').then(r => r.json()).then(data => {" +
                            "    if(data.fact) document.getElementById('fact-box').innerText = data.fact;" +
                            "    if(data.inventory) document.getElementById('inv-box').innerHTML = data.inventory;" +
                            "  }).catch(e => console.log(e));" +
                            "}, 1000);" +
                            "</script></body></html>";

                        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: " + html.length() + "\r\nConnection: close\r\n\r\n" + html;
                        out.write(response.getBytes());
                    }
                    else if (requestLine.contains("GET /move/click")) {
                        if (vrRenderer != null) {
                            vrRenderer.moveForward();
                        }
                        
                        String response = "HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
                        out.write(response.getBytes());
                    }
                    else if (requestLine.contains("GET /status")) {
                        String inv = vrRenderer != null && vrRenderer.getInventoryString() != null ? vrRenderer.getInventoryString() : "No Data";
                        String fact = vrRenderer != null && vrRenderer.getDisplayFact() != null ? vrRenderer.getDisplayFact() : "";
                        
                        inv = inv.replace("\n", "<br>").replace("\"", "\\\"");
                        fact = fact.replace("\n", " ").replace("\"", "\\\"");

                        String json = "{\"inventory\": \"" + inv + "\", \"fact\": \"" + fact + "\"}";
                        String response = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + json.length() + "\r\nConnection: close\r\n\r\n" + json;
                        out.write(response.getBytes());
                    }
                    else {
                        String response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
                        out.write(response.getBytes());
                    }
                    
                    out.flush();
                }
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {}
    }
}
