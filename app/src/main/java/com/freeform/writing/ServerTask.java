package com.freeform.writing;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ServerTask implements Runnable{
    private ServerSocket serverSocket;
    private Socket socket;
    private String dir = String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/test"));

    @Override
    public void run() {
        try {
            Log.e("Server","Socket started");
            serverSocket = new ServerSocket(8988);
            socket = serverSocket.accept();
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            DataInputStream dis = new DataInputStream(bis);

            int filesCount = dis.readInt();
            File[] files = new File[filesCount];

            for(int i = 0; i < filesCount; i++)
            {
                long fileLength = dis.readLong();
                String fileName = dis.readUTF();

                files[i] = new File(dir + "/" + fileName);

                FileOutputStream fos = new FileOutputStream(files[i]);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                for(int j = 0; j < fileLength; j++) bos.write(bis.read());

                bos.close();
            }

            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket!=null) {
                try {
                    socket.close();
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}