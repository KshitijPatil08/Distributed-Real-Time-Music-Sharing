package com.example;

import javax.sound.sampled.*;
import java.net.*;
import java.security.Security;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.util.Arrays;

public class MusicServer {
    private static final int PORT = 5000;
    private static final int BUFFER_SIZE = 1024;
    private static final Set<InetSocketAddress> clients = new HashSet<>();

    public static void main(String[] args) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            // Make socket final for use in lambda
            final DatagramSocket socket = new DatagramSocket(PORT,InetAddress.getByName("0.0.0.0"));
            System.out.println("🎤 Server running on all interfaces:" + PORT);

           
            new Thread(() -> manageClients(socket)).start();

            AudioFormat format = new AudioFormat(48000, 16, 2, true, false);
            TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
            microphone.start();

            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) continue;

                byte[] encrypted = AESUtils.encrypt(Arrays.copyOf(buffer, bytesRead));
                if (encrypted != null) {
                    broadcastToClients(socket, encrypted);
                }
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void broadcastToClients(DatagramSocket socket, byte[] data) {
        synchronized (clients) {
            clients.forEach(client -> {
                try {
                    DatagramPacket packet = new DatagramPacket(
                        data, data.length, client.getAddress(), client.getPort());
                    socket.send(packet);
                    System.out.println("Sent to " + client.getAddress().getHostAddress());
                } catch (Exception e) {
                    System.err.println("Error sending to client: " + client);
                    clients.remove(client);
                }
            });
        }
    }

    private static void manageClients(final DatagramSocket socket) {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        while (true) {
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                
                if (message.startsWith("REGISTER:")) {
                    InetSocketAddress clientAddress = new InetSocketAddress(
                        packet.getAddress(), Integer.parseInt(message.split(":")[1]));
                    
                    synchronized (clients) {
                        clients.add(clientAddress);
                        System.out.println("New client: " + clientAddress);
                        
                        // Send acknowledgement
                        String ack = "REGISTERED";
                        socket.send(new DatagramPacket(
                            ack.getBytes(), ack.length(), 
                            clientAddress.getAddress(), clientAddress.getPort()));
                    }
                }
            } catch (Exception e) {
                System.err.println("Client manager error: " + e.getMessage());
            }
        }
    }
}