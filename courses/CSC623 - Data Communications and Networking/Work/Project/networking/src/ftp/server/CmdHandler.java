/**
 * Description: This class is used to handle client's request
 * like, user, pass, pwd, list ...
 * @author: Jing Zhao
 **/
package ftp.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

import ftp.config.Settings;
import ftp.file.FtpFile;
import ftp.file.LocalDirectory;
import ftp.io.CmdSocketWriter;
import ftp.io.DataSocketObjectOutputStream;

public class CmdHandler implements Settings {

	private CmdSocketWriter out;
	private String userName;
	private String passwd;
	private String curDir = DEFAULT_DIR;
	private InetAddress client;

	/**
	 * constructor to handle the socket out and client address
	 * @param out
	 * @param client
	 */
	public CmdHandler(CmdSocketWriter out, InetAddress client) {
		this.out = out;
		this.client = client;
	}

	/**
	 * function to handle incorrect user
	 * @param arg
	 */
	public void user(String arg) {
		userName = parseArg(arg);

		if (userName == null) {
			userName = "anonymous";
		}
		out.write("331 password required for user " + userName);
	}

	/**
	 * authenticates the client passed user name and password
	 * @param arg
	 */
	public void pass(String arg) {
		passwd = parseArg(arg);
		if (passwd == null) {
			passwd = "anonymous";
		}
		out.write(authen());
	}

	/**
	 * gets the current working directory
	 * @param arg
	 */
	public void pwd(String arg) {
		out.write("257 current path is " + curDir);
	}

	/**
	 * changes the current working directory
	 * @param arg
	 */
	public void cwd(String arg) {
		String path = parseArg(arg);
		if (path != null) {
			File dir = new File(path);
			if (dir.exists() && dir.isDirectory()) {
				curDir = LocalDirectory.unixFormat(path);
				out.write("250 current directory is changed to " + curDir);
			} else {
				out.write("550 request directory does not exist");
			}
		} else {
			out.write("501 syntex erro");
		}

	}

	/**
	 * lists all the sub directories and files under the current working
	 * directory
	 * @param arg
	 */
	public void list(String arg) {
		try {
			Socket dataSocket = new Socket(client, DATASOCKET_PORT);
			DataSocketObjectOutputStream dataOut = new DataSocketObjectOutputStream(
					new ObjectOutputStream(dataSocket.getOutputStream()));
			File pwd = new File(curDir);
			File[] files = pwd.listFiles();
			for (File file : files) {
				FtpFile ftpFile = new FtpFile();
				ftpFile.setFileName(file.getName());
				ftpFile.setFileSize(file.length());
				ftpFile.setLastModify(file.lastModified());	
				ftpFile.setIsDirectory(file.isDirectory());
				try {
					dataOut.writeObject(ftpFile);

				} catch (IOException e) {
					out.write("425 Can't open data connection.");
					return;
				}

			}
			dataOut.flush();
			dataOut.close();
			dataSocket.close();
			out.write("250 Requested file action okay, completed.");
		} catch (IOException e) {
			out.write("511 failed to connect to " + client.getHostName() + ":"
					+ DATASOCKET_PORT);
		}
	}

	/**
	 * copy file on server to client
	 * @param arg
	 */
	public void retr(String arg) {
		String retrFile = parseArg(arg);
		Socket dataSocket;
		try {
			dataSocket = new Socket(client, DATASOCKET_PORT);
			File file = new File(curDir + retrFile);
			DataSender sender = new DataSender(file, dataSocket, out);
			sender.start();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		
		}			
	}

	/**
	 * store files passed by client into server's file system
	 */
	public void stor(String arg) {
		String storFile = parseArg(arg);
		Socket dataSocket;
		try {
			dataSocket = new Socket(client, DATASOCKET_PORT);
			File file = new File(curDir + storFile);
			DataAccepter sender = new DataAccepter(file, dataSocket, out);
			sender.start();
						
		} catch (IOException e) {
			e.printStackTrace();
		
		}			
	}
	
	/**
	 * store files passed by client into server's file system
	 */
	public void delete(String arg) {
		String deleteFile = parseArg(arg);
		Socket dataSocket;
		try {
			dataSocket = new Socket(client, DATASOCKET_PORT);
			File file = new File(curDir + deleteFile);
						
		} catch (IOException e) {
			e.printStackTrace();
		
		}			
	}
	
	/**
	 * authenticate returns 530 if login user or password is incorrect 230 if
	 * user logged in 550 if authentication file cannot be opened or read
	 */
	private String authen() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					ACCOUNT_IN));
			String line = null;
			while ((line = reader.readLine()) != null) {
				Scanner scan = new Scanner(line);
				scan.useDelimiter(":");
				String user = scan.next();
				String pass = scan.next();
				if (user.equals(userName)) {
					if (pass.equals(passwd)) {
						return "230 user logged on";
					} else {
						return "530 username or password is incorrect";
					}
				}
			}
			return "530 username or password is incorrect";
		} catch (FileNotFoundException e) {
			return "550 server system error, cannot authenticate user";
		} catch (IOException e) {
			return "550 server system error, cannot authenticate user";
		}

	}

	/**
	 * parses the client request and gets the parameter which follows the
	 * command and divided by space, for instance, "User admin" returns the
	 * parameter of the command
	 * @param arg
	 * @return
	 */
	private String parseArg(String arg) {
		int index = arg.indexOf(' ');
		if (index == -1) {
			return null;
		} else {
			return arg.substring(index + 1);
		}
	}

}
