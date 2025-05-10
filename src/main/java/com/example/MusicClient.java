package com.example;

import javax.sound.sampled.*;
import java.net.*;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.util.Arrays;
import java.io.*;

public class MusicClient {
    private static final String SERVER_IP = "xxx.xxx.xxx.xxx"; // Your server IP
    private static final int SERVER_PORT = 5000;
    private static final int CLIENT_PORT = 5001; // Client's receiving port
    private static final String OUTPUT_FILE = "received_audio.wav";
    private static final int BUFFER_SIZE = 1040;
     private static int totalBytesWritten = 0;

    public static void main(String[] args) {
        DatagramSocket socket = null;
        SourceDataLine speakers = null;
        FileOutputStream fileOutput = null;
        
        try {
            Security.addProvider(new BouncyCastleProvider());
            
            // Create socket with specific port for client
            socket = new DatagramSocket(CLIENT_PORT);
            System.out.println(" Client started on port " + CLIENT_PORT);

            // Register with server
            registerWithServer(socket);

            // Setup audio file output
            fileOutput = new FileOutputStream(OUTPUT_FILE);
            writeWavHeader(fileOutput, 0); // Write initial empty header

            // Setup audio playback
            AudioFormat format = new AudioFormat(48000, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(format);
            speakers.start();

            System.out.println("Waiting for audio stream...");

            byte[] buffer = new byte[BUFFER_SIZE];
            int totalBytesWritten = 0;

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                byte[] encryptedData = Arrays.copyOf(packet.getData(), packet.getLength());
                byte[] audioData = AESUtils.decrypt(encryptedData);
                
                if (audioData != null && audioData.length > 0) {
                    // Save to file
                    fileOutput.write(audioData);
                    totalBytesWritten += audioData.length;
                    
                    // Play audio
                    speakers.write(audioData, 0, audioData.length);
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Connection timed out. Finalizing audio file...");
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // Update WAV header with actual size
                if (fileOutput != null) {
                    updateWavHeader(fileOutput, totalBytesWritten);
                    fileOutput.close();
                }
                if (speakers != null) {
                    speakers.drain();
                    speakers.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Audio saved to: " + new File(OUTPUT_FILE).getAbsolutePath());
        }
    }

    private static void registerWithServer(DatagramSocket socket) throws IOException {
        String registerMsg = "REGISTER:" + CLIENT_PORT;
        byte[] sendData = registerMsg.getBytes();
        
        DatagramPacket sendPacket = new DatagramPacket(
            sendData, 
            sendData.length, 
            InetAddress.getByName(SERVER_IP), 
            SERVER_PORT
        );
        socket.send(sendPacket);
        System.out.println("Registration request sent to server");

        // Wait for acknowledgement
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        
        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        if ("REGISTERED".equals(response)) {
            System.out.println("Successfully registered with server");
        } else {
            throw new IOException("Registration failed: " + response);
        }
    }

    // WAV file handling methods (same as server)
    private static void writeWavHeader(FileOutputStream fileOutput, int dataLength) throws IOException {
        AudioFormat format = new AudioFormat(48000, 16, 2, true, false);
        int byteRate = (int) (format.getSampleRate() * format.getFrameSize());
        
        byte[] header = new byte[44];
        // RIFF header
        System.arraycopy("RIFF".getBytes(), 0, header, 0, 4);
        // File size - 8
        writeInt(header, 4, 36 + dataLength);
        // WAVE
        System.arraycopy("WAVE".getBytes(), 0, header, 8, 4);
        // fmt chunk
        System.arraycopy("fmt ".getBytes(), 0, header, 12, 4);
        // Subchunk size
        writeInt(header, 16, 16);
        // Audio format (1 = PCM)
        writeShort(header, 20, (short)1);
        // Channels
        writeShort(header, 22, (short)format.getChannels());
        // Sample rate
        writeInt(header, 24, (int)format.getSampleRate());
        // Byte rate
        writeInt(header, 28, byteRate);
        // Block align
        writeShort(header, 32, (short)format.getFrameSize());
        // Bits per sample
        writeShort(header, 34, (short)16);
        // Data chunk header
        System.arraycopy("data".getBytes(), 0, header, 36, 4);
        // Data size
        writeInt(header, 40, dataLength);
        
        fileOutput.write(header);
    }

    private static void updateWavHeader(FileOutputStream fileOutput, int dataLength) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(OUTPUT_FILE, "rw");
        // Update file size
        raf.seek(4);
        raf.write(writeInt(new byte[4], 0, 36 + dataLength));
        // Update data size
        raf.seek(40);
        raf.write(writeInt(new byte[4], 0, dataLength));
        raf.close();
    }

    private static byte[] writeInt(byte[] array, int offset, int value) {
        array[offset] = (byte)(value & 0xff);
        array[offset+1] = (byte)((value >> 8) & 0xff);
        array[offset+2] = (byte)((value >> 16) & 0xff);
        array[offset+3] = (byte)((value >> 24) & 0xff);
        return array;
    }

    private static byte[] writeShort(byte[] array, int offset, short value) {
        array[offset] = (byte)(value & 0xff);
        array[offset+1] = (byte)((value >> 8) & 0xff);
        return array;
    }
}