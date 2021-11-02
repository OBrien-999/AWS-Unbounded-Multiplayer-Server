package client;

import java.net.*;
import java.io.*;

import org.json.*;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

import java.util.*;
import java.util.stream.Collectors;

class SockBaseClient {

    public static void main (String args[]) throws Exception {
    	
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int i1=0, i2=0;
        int port = 9099; // default port

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Ask user for username
        System.out.println("Please provide your name for the server. ( ͡❛ ͜ʖ ͡❛)");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend = stdin.readLine();

        // Build the first request object just including the name
        Request op = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();
        Response response;

        try {

        // connect to the server
        serverSock = new Socket(host, port);

        // write to the server
        out = serverSock.getOutputStream();
	    op.writeDelimitedTo(out);

	    boolean quit = false;
	    boolean gameStarted = false;
	    Request req = null;

	    while(!quit) {

		    // Response from the server
		    in = serverSock.getInputStream();
		    response = Response.parseDelimitedFrom(in);

		    // print the server response. 
		    
		    if(response.getResponseType() == Response.ResponseType.WON) {
		    	
		    	System.out.println(response.getTask());
		    	System.out.println("Here is the full image: " + response.getImage());
		    	
		    }
		    
		    
		    // Print greeting response
		    System.out.println();
            System.out.println(response.getGreeting());

            // Checks response type and handles accordingly
		    switch(response.getResponseType()) {

			    case GREETING:
					strToSend = stdin.readLine();
			    	break;
				case LEADER:
					System.out.println();
					System.out.println(response.getTask());
					strToSend = stdin.readLine();
					break;
				case TASK:
					System.out.println();
					System.out.println("Image: \n" + response.getImage());
					System.out.println("Task: \n" + response.getTask());
					strToSend = stdin.readLine();
					op = Request.newBuilder()
		        	.setOperationType(Request.OperationType.ANSWER)
		       		.setAnswer(strToSend)
		       		.build();
					break;
				case WON:
					strToSend = stdin.readLine();
					break;
				case ERROR:
					break;
				case BYE:
					quit = true;
					break;
				default:
					System.out.println("Well this wasn't supposed to happen...");
					quit = true;
					break;

		    }
		    
		    // If the response type requires the user input for menu board

		    if(response.getResponseType() == Response.ResponseType.GREETING || response.getResponseType() == Response.ResponseType.LEADER || response.getResponseType() == Response.ResponseType.WON) {

		    	int ans = Integer.parseInt(strToSend);

			    switch(ans) {

			    case 1:
					op = Request.newBuilder()
        			.setOperationType(Request.OperationType.LEADER)
       				.setName(strToSend).build();
					break;
				case 2:
					op = Request.newBuilder()
        			.setOperationType(Request.OperationType.NEW)
       				.build();
					gameStarted = true;
					break;
				case 3: 
					op = Request.newBuilder()
        			.setOperationType(Request.OperationType.QUIT)
       				.build();
					break;
				default:
					System.out.println("Please enter an integer (1 - 3).");
					break;

			    }

		    }

		    // Write request to server
		    out = serverSock.getOutputStream();
		    op.writeDelimitedTo(out);

	    }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null)   in.close();
            if (out != null)  out.close();
            if (serverSock != null) serverSock.close();
        }
    }
}


