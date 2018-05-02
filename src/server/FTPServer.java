
package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import utils.BinaryTransfer;


public class FTPServer {

	public static void main(String[] args) throws Exception  {
		
		System.out.println("Server up and running");

		ServerSocket serverConnection = new ServerSocket(21000);

		// starts an FTP connection, supports multiple users
		// when a user is logged in another FTPConnection is created
		new FTPConnection(serverConnection);
	}
	
}


class FTPConnection extends Thread {

	// which  user created this FTPConnection
	private String username = null;

	// directory of where all the user directories are
	private String startDirectory = "D:\\code\\Java\\FTPService\\bin\\server";

	// the directory where the user currently is, user can only see in his directory
	private String currentDirectory = "\\";

	private ServerSocket serverConnection;

	public FTPConnection(ServerSocket socket) {
		serverConnection = socket;
		start();
	}

	@Override
	public void run()   {
		
		Socket connection = null;
		BufferedReader inputFromClient = null;
		DataOutputStream outputToClient = null;
		String response = null;
		String input = null;
		OutputStream outputStreamToClient = null;
		
		try {
			connection = serverConnection.accept();

			/* create a new FTPConnection on the same server and wait
			 * for a user to log in */
			new FTPConnection(serverConnection);

			inputFromClient = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
			outputStreamToClient = connection.getOutputStream();
			outputToClient = new DataOutputStream( outputStreamToClient );

			// while the user is not yet authenticated
			while (true) {

				input = inputFromClient.readLine();

				if(input.startsWith("HELLO")) {

					// if no username is given close connection
					if (input.length() < 7) {
						outputToClient.writeBytes("ERROR 007\n");
						connection.close();
						return;
					}


					username = input.substring(6);
					startDirectory += currentDirectory + username;

					// check if user is a valid one
					File tmp = new File(startDirectory);
					assert(tmp != null);

					if (tmp.exists()) {
						response = "HELLO OK " + username + "\n";
						outputToClient.writeBytes(response);
						System.out.println("User " + username + " has connected");
						break; // user is authenticated
					}

					// unknown user
					else {
						response = "ERROR 100\n";
						outputToClient.writeBytes(response);
						connection.close();
						return;
					}
				}
				else if (input.equals("BYE"))  {
					response = "BYE OK\n";
					outputToClient.writeBytes(response);
					connection.close();
					return;
				}
				else {
					outputToClient.writeBytes("ERROR 007\n"); // unknown command, unknown error
					connection.close();
					return;
				}
			}
		}
		catch (IOException e) {
			System.err.println("Problem in creating ftp connection: " + e.getMessage());
			return; // close thread
		}
		catch (NullPointerException e) {
			System.err.println("A user logged off");
			return; // close thread
		}


		/*********************************************************/
		/* user is authenticated and can now run now any command */
		try {
			while(true) {

				// close the connection if the user is idle for 5 minutes
				connection.setSoTimeout(5 * 1000 * 60);
				input = inputFromClient.readLine();

				// no input
				if (input == null)
					continue;

				// client denotes start of disconnection procedure
				if (input.equals("BYE"))  {
					response = "BYE OK\n";
					outputToClient.writeBytes(response);
					connection.close();
					System.out.println("User " + username + " has disconnected");
					break;
				}

				// client requests a file list of the current directory
				else if (input.equals("LIST")) {
					StringBuffer buffer = new StringBuffer("LIST OK ");
					File current = new File(startDirectory + currentDirectory);

					String[] fileList;
					try {
						fileList = current.list();
						assert fileList != null: "File must exist, user is authetincated";
					}
					catch (SecurityException e) {
						outputToClient.writeBytes("ERROR 110\n");
						continue;
					}

					// no errors, return the list of all files
					for (int i = 0; i < fileList.length; i++)
						buffer.append(fileList[i] + "\\n");

					outputToClient.writeBytes(buffer.toString() + "\n");
				}

				// client requests to transfer a file
				else if (input.startsWith("GET")) {
					input = input.substring(4);

					String filePath = startDirectory + currentDirectory + input;
					File file = new File(filePath);

					if (!file.exists())
						outputToClient.writeBytes("ERROR 200\n");
					else if (!file.canRead())
						outputToClient.writeBytes("ERROR 201\n");
					else {
						outputToClient.writeBytes("GET OK \n");
						
						BinaryTransfer.toStream(outputStreamToClient, file);
					}
				}

				// client requests a deletion for a file
				else if (input.startsWith("DELETE")) {
					input = input.substring(7);

					String filePath = startDirectory + currentDirectory + input;
					File file = new File(filePath);

					try {
						if (file.delete()) // file successfully deleted
							outputToClient.writeBytes("DELETE OK " + input + "\n");
						else
							throw new SecurityException("cannot delete file");
					}
					catch (SecurityException e) {
						outputToClient.writeBytes("ERROR 202\n");
					}
				}

				// client requests to create a new directory
				else if (input.startsWith("MKDIR")) {
					input = input.substring(6);

					String directoryPath = startDirectory + currentDirectory + input;
					File directory = new File(directoryPath);

					try {
						if (directory.mkdir()) // directory successfully created
							outputToClient.writeBytes("MKDIR OK " + input + "\n");
						else
							throw new SecurityException("cannot delete file");
					}
					catch (SecurityException e) {
						outputToClient.writeBytes("ERROR 203\n");
					}
				}

				// client requests to get the present working directory
				else if (input.equals("PWD")) {
					// creates a better format for printing, removes .. and .
					File tmp = new File(startDirectory + currentDirectory);
					outputToClient.writeBytes(tmp.getCanonicalPath() + "\n");
				}

				// client requests to change directory
				else if (input.startsWith("CHDIR")) {
					String dirName = input.substring(6);

					File file = new File(startDirectory + currentDirectory + dirName);

					try {
						// security check, user may use ".." directory to see other users files
						// the canonical file must have length greater or equal than the startDirectory
						if (file.getCanonicalFile().toString().length() < startDirectory.length()) {
							outputToClient.writeBytes("ERROR 007\n");
						}
						else if (file.isDirectory() && file.exists()) {
							currentDirectory += dirName + "/";
							outputToClient.writeBytes("CHDIR OK " + dirName + "\n");
						}
						else
							throw new SecurityException("file access problems");
					}
					catch (SecurityException e) {
						outputToClient.writeBytes("ERROR 204\n");
					}
				}

				// no correct request/command
				else {
					outputToClient.writeBytes("ERROR 007\n");
				}
			}
		}
		catch (SocketTimeoutException e) {
			System.err.println("A user was idle for 5 minutes, closed connection");
			try {
				connection.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

}
