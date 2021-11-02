package server;

import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;
import java.lang.*;
import java.util.concurrent.atomic.AtomicInteger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import buffers.RequestProtos.Request;
import buffers.RequestProtos.Logs;
import buffers.RequestProtos.Message;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

class SockBaseServer {

    public static AtomicInteger counter = new AtomicInteger(0);
    static String logFilename = "logs.txt";
    static String leaderBoard = "leaderboard.txt";

    ServerSocket serv = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
    Game game;


    public SockBaseServer(Socket sock, Game game){
        this.clientSocket = sock;
        this.game = game;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }
    }

    // Generic greeting with name
    public static String returnGreeting(String name) {

	String greeting = "Hello " + name + " and welcome. \nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit";
	return greeting;

    }

    // Generic quit message
    public static String clientQuit() {

	String quit = "--== Thank you for playing! ==-- \nExiting game...";
	return quit;

    }

    // Multi-threaded server that allows multiple connections at once
    public void start() throws IOException {

    String name = "";
	boolean gameStarted = false;
	boolean quit = false;
	Request op = null;
	Response response = null;
	String result = null;
	int taskCounter = 0;
	int correct = 0;
	String answer = "";
	Response.Builder res = Response.newBuilder().setResponseType(Response.ResponseType.LEADER);

        System.out.println("Ready...");

        try {

		    while(!quit) {
	
		    	// Reads request from client
				op = Request.parseDelimitedFrom(in);
		
				// Reads operation type and responds accordingly
				switch(op.getOperationType()) {
		
					case NAME:
						name = op.getName();
						writeToLog(name, Message.CONNECT);
						response = Response.newBuilder()
		                .setResponseType(Response.ResponseType.GREETING)
		                .setGreeting(returnGreeting(name))
		                .build();
						break;
					case LEADER:
						Path filepath = Paths.get(leaderBoard);
						String content = Files.readString(filepath);
						response = Response.newBuilder()
				                .setResponseType(Response.ResponseType.LEADER)
				                .setGreeting(content)
				                .setTask(returnGreeting(name))
				                .build();
						break;
					case NEW:
						game.newGame();
						gameStarted = true;
						taskCounter = 0;
						taskCounter++;
						correct = 1;
						break;
					case ANSWER:
						if(op.getAnswer().equals(answer)) {
							
							correct = 1;
							
							if(taskCounter == 3) {
								
								response = Response.newBuilder()
								.setResponseType(Response.ResponseType.WON)
								.setImage(replace(game.getIdxMax() / 2))
								.setTask("Yay! You won!")
								.setGreeting(returnGreeting(name))
								.build();
								
								res = writeToLeaderboard(name, res);
								
								game.setWon();
								gameStarted = false;
								
							}else {
								
								break;
								
							}
							
						}else {
							
							taskCounter--;
							correct = 0;
							break;
							
						}
						break;
					case QUIT:
						response = Response.newBuilder()
		                .setResponseType(Response.ResponseType.BYE)
						.setGreeting(clientQuit())
		                .build();
						quit = true;
						break;
					default:
						break;
		
				}
				
				// If statements to handle wrong answers given by the client
				if(taskCounter != 3) {
				
					if(gameStarted && correct == 1) {
						
						if(taskCounter == 1) {
							
							response = Response.newBuilder()
					        .setResponseType(Response.ResponseType.TASK)
					        .setImage(game.getImage())
					        .setGreeting("--== Starting a new game! ==--")
					        .setTask("Type Hello")
					        .build();
							answer = "Hello";
							taskCounter++;
							
						}else {
							
							response = Response.newBuilder()
							.setResponseType(Response.ResponseType.TASK)
							.setImage(replace(game.getIdxMax() / 2))
							.setTask("Correct! Type World")
							.build();
							answer = "World";
							taskCounter++;
							
						}
				
					}
					
					if(gameStarted && correct == 0) {
						
						if(taskCounter == 1) {
							
							response = Response.newBuilder()
							.setResponseType(Response.ResponseType.TASK)
							.setImage(game.getImage())
							.setGreeting("Your answer was incorrect, please try again.")
							.setTask("Type Hello")
							.build();
							answer = "Hello";
							taskCounter++;
							
						}else {
							
							response = Response.newBuilder()
							.setResponseType(Response.ResponseType.TASK)
							.setImage(game.getImage())
							.setGreeting("Your answer was incorrect, please try again.")
							.setTask("Type World")
							.build();
							answer = "World";
							taskCounter++;
							
						}
						
					}
				
				}
		
				response.writeDelimitedTo(out);
	
		    }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (out != null)  out.close();
            if (in != null)   in.close();
            if (clientSocket != null) clientSocket.close();
        }
    }

    /**
     * Replaces num characters in the image. I used it to turn more than one x when the task is fulfilled
     * @param num -- number of x to be turned
     * @return String of the new hidden image
     */
    public String replace(int num){
        for (int i = 0; i < num; i++){
            if (game.getIdx()< game.getIdxMax())
                game.replaceOneCharacter();
        }
        return game.getImage();
    }


    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect) 
     * @return String of the new hidden image
     */
    public static void writeToLog(String name, Message message){
        try {
        	
            // read old log file 
            Logs.Builder logs = readFile(logFilename);

            // get current time and data
            Date date = java.util.Calendar.getInstance().getTime();

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date.toString() + ": " +  name + " - " + message);
            

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // This is only to show how you can iterate through a Logs object which is a protobuf object
            // which has a repeated field "log"

            for (String log: logsObj.getLogList()){

                System.out.println(log);
            }

            // write to log file
            logsObj.writeTo(output);
        }catch(Exception e){
            System.out.println("Issue while trying to save");
        }
    }
    
    // Writes to the leaderboard when client wins a game
    public static Response.Builder writeToLeaderboard(String name, Response.Builder res) {
    	
    	boolean check = false;
    	Entry leader = null;
    	
    	try {
    		
    		// read old log file 
            Logs.Builder logs = readFile(leaderBoard);
    	
    	Response response3 = res.build();

        for (Entry lead: response3.getLeaderList()){
        	
            if(lead.getName().equals(name)) {
            	
            	int wins = lead.getWins() + 1;
            	
            	leader = Entry.newBuilder()
                        .setName(name)
                        .setWins(wins)
                        .build();
            	
            	// we are writing a new log entry to our log
                // add a new log entry to the log list of the Protobuf object
                logs.addLog(name + ": " +  wins);
            	
            	check = true;
            	
            }
            
        }
        
        if(!check) {
        	
        	leader = Entry.newBuilder()
                    .setName(name)
                    .setWins(1)
                    .build();
        	
        	// we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(name + ": " +  leader.getWins());
        	
        }
        
        // open log file
        FileOutputStream output = new FileOutputStream(leaderBoard);
        Logs logsObj = logs.build();
        
        // write to log file
        logsObj.writeTo(output);
    		
    	}catch(Exception e) {
    		
    		System.out.println("Issue while trying to save to leaderboard.");
    		
    	}
    	
    	return res;
    	
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public static Logs.Builder readFile(String file) throws Exception{
    	
        Logs.Builder logs = Logs.newBuilder();

        try {
        	
            // just read the file and put what is in it into the logs/leader object
        	if(file.equals(logFilename)) {
        		
        		return logs.mergeFrom(new FileInputStream(logFilename));
        		
        	}
        	
        	if(file.equals(leaderBoard)) {
        		
        		return logs.mergeFrom(new FileInputStream(leaderBoard));
        		
        	}
            
        } catch (FileNotFoundException e) {
        	
            System.out.println(file + ": File not found.  Creating a new file.");
            return logs;
            
        }
        
        return logs;
        
    }

    public static void main (String args[]) throws Exception {

        Game game = new Game();

        if (args.length != 2) {

            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);

        }

        int port = 9099; // default port
        int sleepDelay = 10000; // default delay
        ServerSocket serv = null;

        try {

            port = Integer.parseInt(args[0]);
            sleepDelay = Integer.parseInt(args[1]);

        } catch (NumberFormatException nfe) {

            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);

        }

        try {

	    System.out.println("Activation protocol detected. Server-start imminent...");
            serv = new ServerSocket(port);
	    serv.setReuseAddress(true);

	    while(true) {

	    Socket clientSocket = serv.accept();
		System.out.println("A client has joined the game.");

		SockBaseServer server = new SockBaseServer(clientSocket, game);

		new Thread() {

			public void run() {

				try {

					server.start();
					System.out.println("A client has left the game");
					clientSocket.close();

				} catch(Exception e) {

					e.printStackTrace();

				}

			}

		}.start();

	    }

        } catch(Exception e) {

            e.printStackTrace();
            System.exit(2);

        }

       	finally {

		if(serv != null) {

			try {

				serv.close();

			} catch(IOException e) {

				e.printStackTrace();

			}

		}

	}

    }
}

