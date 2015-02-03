/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.archrival.sparkybluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * 
 * @author User
 */
public class TestClient implements Runnable {
	
	Socket requestSocket;

	OutputStream out;
	InputStream in;

	String server_ip = "192.168.0.100";
	int server_socket = 3333;

	String message;
	
	private BluetoothSetupActivity activity;
	
	/**
	 * 
	 * @param activity
	 */
	public TestClient(BluetoothSetupActivity activity){
		this.activity = activity;
	}

	/**
	 * 
	 */
	@Override
	public void run() {
		try {

			System.out.println("Starting connection...");
			requestSocket = new Socket(server_ip, server_socket);
			System.out.println("Connected to localhost port: " + server_socket);

			in = requestSocket.getInputStream();
			out = requestSocket.getOutputStream();

			Scanner scanner = new Scanner(in);

			message = "";

			while (!message.equals("bye")) {
				try {
					if (scanner.hasNextLine()) {
						System.out.println("Getting message...");
						message = scanner.nextLine();
						interpret(message);
						System.out.println("I got a message: " + message);
					}
				} catch (Exception e) {
					e.printStackTrace();
					message = "failed";
				}
				if (message != null) {
					System.out.println("server>" + message);
				}
			}
		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			// 4: Closing connection
			try {
				in.close();
				out.close();
				requestSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			} catch (NullPointerException e) {
				// meh
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param file
	 */
	void sendMessage(String message) {
		if (out != null) {
			try {
				message += "\n";
				System.out.println("client -> " + message);
				out.write(message.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Connection does not exist...");
		}
	}

	/**
	 * 
	 * @param message
	 */
	public void interpret(String message){
		message = message.trim();
		// this will eventually have to be in an if to test commands versus device commands
		activity.performCommand(message);
	}
}
