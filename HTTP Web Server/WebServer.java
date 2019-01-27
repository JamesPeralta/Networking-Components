

/**
 * WebServer Class
 * 
 */

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;


public class WebServer extends Thread {

	//Properties
	private ServerSocket serverSocket;
	private volatile boolean shutdown = false;
	private ExecutorService pool;

    /**
     * Default constructor to initialize the web server
     * 
     * @param port 	The server port at which the web server listens > 1024
     * 
     */
	public WebServer(int port) {
		//Open server socket connection
		//create Thread Pool   
		try{
			serverSocket = new ServerSocket(port); 
			pool = Executors.newFixedThreadPool(8);
		}catch(IOException e){
			System.out.println("ServerSocket connection Fail/nError: "+e);	
		}
	}
    /**
     * The main loop of the web server
     *   Opens a server socket at the specified server port
	 *   Remains in listening mode until shutdown signal
	 * 
     */
	public void run() {
			
		try{
			while(!shutdown){
				Socket socket = serverSocket.accept(); 
				pool.execute(new WebThreads(socket)); 
				}

		}catch(Exception e){
			pool.shutdown();
		}
	}
    /**
     * Signals the server to shutdown.
	 *
     */
	public void shutdown() {
		
		try {
			pool.shutdown(); // Cancel currently executing tasks
			// Wait a while for tasks to respond to being cancelled
			if (!pool.awaitTermination(60, TimeUnit.SECONDS))
				
				pool.shutdownNow();
		  
		} catch (InterruptedException ie) {
		  // Cancel if current thread also interrupted
		  pool.shutdownNow();
		}

		//shutdown the ServerSocket 
		try{
			serverSocket.close();
		}catch(Exception e){
		}		
	}

	
	/**
	 * A simple driver.
	 */
	public static void main(String[] args) {
		int serverPort = 2225;

		// parse command line args
		if (args.length == 1) {
			serverPort = Integer.parseInt(args[0]);
		}
		
		if (args.length >= 2) {
			System.out.println("wrong number of arguments");
			System.out.println("usage: WebServer <port>");
			System.exit(0);
		}
		
		System.out.println("starting the server on port " + serverPort);
		
		WebServer server = new WebServer(serverPort);
		
		server.start();
		System.out.println("server started. Type \"quit\" to stop");
		System.out.println(".....................................");

		Scanner keyboard = new Scanner(System.in);
		while ( !keyboard.next().equals("quit") );
		
		System.out.println();
		System.out.println("shutting down the server...");
		server.shutdown();
		System.out.println("server stopped");
	}
	
}




