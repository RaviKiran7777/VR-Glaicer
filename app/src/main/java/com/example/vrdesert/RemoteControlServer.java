package com.example.vrdesert;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A lightweight HTTP server running on the VR phone.
 * Allows remote control of the VR experience from any browser on the same Wi-Fi.
 */
public class RemoteControlServer extends Thread {

    private static final String TAG = "RemoteServer";
    private final int port;
    private final MainActivity activity;
    private boolean running = true;

    public RemoteControlServer(int port, MainActivity activity) {
        this.port = port;
        this.activity = activity;
    }

    public void stopServer() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Log.d(TAG, "Server started on port " + port);
            while (running) {
                try (Socket client = serverSocket.accept()) {
                    handleClient(client);
                } catch (Exception e) {
                    if (running) Log.e(TAG, "Error accepting client", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Server could not start", e);
        }
    }

    private void handleClient(Socket client) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(client.getOutputStream());

        String line = in.readLine();
        if (line == null) return;

        String[] parts = line.split(" ");
        if (parts.length < 2) return;

        String path = parts[1];
        Log.d(TAG, "Request: " + path);

        // Routing
        if (path.equals("/") || path.equals("/index.html")) {
            sendResponse(out, "text/html", getDashboardHtml());
        } else if (path.startsWith("/api/")) {
            handleApiCall(path.substring(5));
            sendResponse(out, "application/json", "{\"status\":\"ok\"}");
        } else {
            sendResponse(out, "text/plain", "Not Found", 404);
        }
        out.flush();
    }

    private void handleApiCall(String command) {
        new Handler(Looper.getMainLooper()).post(() -> {
            switch (command) {
                case "move":
                    activity.onRemoteMove();
                    break;
                case "reset":
                    activity.onRemoteReset();
                    break;
                case "climate":
                    activity.onRemoteClimateToggle();
                    break;
                case "sensitivity":
                    activity.onRemoteSensitivityCycle();
                    break;
                case "sound":
                    activity.onRemoteSoundToggle();
                    break;
                case "reload":
                    activity.recreate();
                    break;
            }
        });
    }

    private void sendResponse(PrintWriter out, String contentType, String content) {
        sendResponse(out, contentType, content, 200);
    }

    private void sendResponse(PrintWriter out, String contentType, String content, int code) {
        out.println("HTTP/1.1 " + code + (code == 200 ? " OK" : " Not Found"));
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + content.getBytes().length);
        out.println("Connection: close");
        out.println();
        out.println(content);
    }

    private String getDashboardHtml() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    <title>VR Glacier Remote</title>\n" +
                "    <style>\n" +
                "        body { font-family: 'Segoe UI', sans-serif; background: #0a0e14; color: #e0e6ed; text-align: center; margin: 0; padding: 20px; }\n" +
                "        .container { max-width: 500px; margin: auto; }\n" +
                "        h1 { font-weight: 300; letter-spacing: 2px; color: #4fc3f7; margin-bottom: 30px; }\n" +
                "        .btn { \n" +
                "            display: block; width: 100%; padding: 18px; margin: 15px 0; \n" +
                "            background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); \n" +
                "            border-radius: 12px; color: white; font-size: 16px; cursor: pointer; \n" +
                "            transition: all 0.2s ease; backdrop-filter: blur(10px);\n" +
                "        }\n" +
                "        .btn:active { transform: scale(0.98); background: rgba(79, 195, 247, 0.2); }\n" +
                "        .btn-main { background: #4fc3f7; color: #0a0e14; font-weight: bold; border: none; }\n" +
                "        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }\n" +
                "        .footer { margin-top: 40px; font-size: 12px; opacity: 0.4; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>GLACIER VR</h1>\n" +
                "        <button class=\"btn btn-main\" onclick=\"call('move')\">MOVE FORWARD</button>\n" +
                "        <div class=\"grid\">\n" +
                "            <button class=\"btn\" onclick=\"call('reset')\">RESET VIEW</button>\n" +
                "            <button class=\"btn\" onclick=\"call('climate')\">TOGGLE ERA</button>\n" +
                "            <button class=\"btn\" onclick=\"call('sensitivity')\">SENSITIVITY</button>\n" +
                "            <button class=\"btn\" onclick=\"call('sound')\">SOUND ON/OFF</button>\n" +
                "        </div>\n" +
                "        <button class=\"btn\" style=\"opacity:0.6\" onclick=\"call('reload')\">RELOAD SCENE</button>\n" +
                "        <div class=\"footer\">VR Remote Control v2.0</div>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        function call(cmd) { fetch('/api/' + cmd); }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
