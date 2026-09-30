import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final ConcurrentHashMap<String,String> store = new ConcurrentHashMap<>();
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        int port = 6379;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);  // Set BEFORE accepting
            System.out.println("Server listening on port " + port);
//            clientSocket = serverSocket.accept();
//
//            InputStream inputStream = clientSocket.getInputStream();
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                clientSocket.getOutputStream().write("+PONG\r\n".getBytes());
//            }
            while (true){
                Socket client  = serverSocket.accept();
                new Thread(()-> handleClient(client)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing server: " + e.getMessage());
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (clientSocket;
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            String line;
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("*")) continue;

                int count = Integer.parseInt(line.substring(1));
                List<String> parts = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    in.readLine();              // skip the "$4" length line
                    parts.add(in.readLine());   // the actual value
                }

                String command = parts.get(0).toUpperCase();
                if (command.equals("PING")) {
                    outputStream.write("+PONG\r\n".getBytes());
                } else if (command.equals("ECHO")) {
                    String arg = parts.get(1);
                    outputStream.write(("$" + arg.length() + "\r\n" + arg + "\r\n").getBytes());
                } else if(command.equals("SET")){
                    store.put(parts.get(1), parts.get(2));
                    outputStream.write("+OK\r\n".getBytes());
                } else if (command.equals("GET")){
                    String value = store.get(parts.get(1));
                    if(value==null){
                        outputStream.write("$-1\r\n".getBytes());
                    }
                    else{
                        outputStream.write(("$"+value.length()+"\r\n"+ value +"\r\n").getBytes());
                    }
                }

                outputStream.flush();
            }

            System.out.println("Client disconnected");
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}