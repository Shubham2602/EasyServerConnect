package Connection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SSH {
	
	static String host = "<server host name>";
	static String username = "user_adm_id";
	static String password = "user_password";
	
	Session session = null;
	static ChannelShell channel = null;
	JSch jsch = new JSch();
	static ByteArrayOutputStream output = null;
	PipedInputStream input = null;
	PipedOutputStream reply = null;

	public boolean loginToPuttyHost(String host,String username,String password) {
		try {
			if(session==null) {
				session=jsch.getSession(username,host,22);
				session.setConfig("StrictHostKeyConfig", "no");
				session.setPassword(password);
				
				session.connect();
				
				channel = (ChannelShell) session.openChannel("shell");
				reply = new PipedOutputStream();
				input = new PipedInputStream(reply);
				output = new ByteArrayOutputStream();
				channel.setInputStream(input,true);
				channel.setOutputStream(output,true);
				channel.connect();
				System.out.println("Connected to Putty");
			}
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean transferFileServer(String host, String username,String password,String localFilePath,
			String remoteFilePath) {
		ChannelSftp sftpChannel = null;
		Session session = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(username,host,22);
			session.setConfig("StrictHostKeyConfig","no");
			session.setPassword(password);
			session.connect();
			
			sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			System.out.println("transferred File to Server");
			if(!remoteFilePath.equalsIgnoreCase("")) {
				sftpChannel.cd(remoteFilePath);
			}
			File f = new File(localFilePath);
			sftpChannel.put(new FileInputStream(f),f.getName());
			
			sftpChannel.exit();
			sftpChannel.disconnect();
			
			return true;
		}catch(Exception e) {
			e.printStackTrace();
			sftpChannel.exit();
			sftpChannel.disconnect();
			session.disconnect();
			session = null;
			return false;
		}
	}
	
	public void executeCommand(String command,int pollingTime, String... till) {
		try {
			writeCommand(reply,command);
			getPrompt(channel,output,pollingTime,pollingTime*10,till);
			System.out.println(command + " command executed");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String executeCommandWithOutput(String command,int pollingTime, String... till) 
	{
		try {
			writeCommand(reply,command);
			return getPromptOutput(channel,output, pollingTime, pollingTime*10, till);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public void executeviCommand(String command,int pollingTime, String... till) {
		try {
			writeviCommand(reply,command);
			getPrompt(channel,output,pollingTime, pollingTime*10, till);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void getPrompt(ChannelShell channel,ByteArrayOutputStream output, int pollingTime, int maximumPollingTime,
			String... tillLetter )throws UnsupportedEncodingException, InterruptedException 
	{
		if(tillLetter.length==0) {
			tillLetter = new String[1];
			tillLetter[0] = ">";
		}
		int totalPollingTime = 0;
		
		while(!channel.isClosed()) {
			String response = new String(output.toByteArray(), "UTF-8");
			System.out.println(response);
			for(String tl : tillLetter) {
				String[] str = response.split("\r\n");
				if(str[str.length-1].trim().contains(tl)) {
					output.reset();
					return;
				}
			}
			Thread.sleep(1000*pollingTime);
			totalPollingTime = totalPollingTime + 1000*pollingTime;
			if(totalPollingTime > maximumPollingTime *1000) {
				break;
			}
		}
	}
	
	public String getPromptOutput(ChannelShell channel,ByteArrayOutputStream output, int pollingTime,
			int maximumPollingTime, String... tillLetter)throws UnsupportedEncodingException,InterruptedException{
		String finalResponse = "";
		if(tillLetter.length == 0) {
			tillLetter = new String[1];
			tillLetter[0] = ">";
		}
		
		int totalPollingTime = 0;
		while(!channel.isClosed()) {
			String response = new String(output.toByteArray(),"UTF-8");
			System.out.println(response);
			finalResponse = finalResponse + response;
			for(String tl : tillLetter) {
				String[] str = response.split("\r\n");
				if(str[str.length-1].trim().contains(tl)) {
					output.reset();
					return finalResponse;
				}
			}
			Thread.sleep(1000*pollingTime);
			totalPollingTime = totalPollingTime + 1000*pollingTime;
			if(totalPollingTime > maximumPollingTime * 1000) {
				break;
			}
			
		}
		return finalResponse;
	}
	
	protected void writeCommand(PipedOutputStream reply, String command) throws IOException {
		reply.write(command.getBytes());
		reply.write("\n".getBytes());
	}
	
	protected void writeviCommand(PipedOutputStream reply, String command) throws IOException {
		if(command.equalsIgnoreCase("ESC")) {
			byte[] esc = {(byte)0x1b};
			reply.write(esc);
		}
		else {
			reply.write(command.getBytes());
		}
	}
	
	public void close() {
		try {
			channel.disconnect();
			System.out.println("Channel is closed " + channel.isClosed());
			if(!channel.isClosed()) {
				System.out.println("Channel exit status is "+ channel.getExitStatus());
			}
			channel = null;
			session.disconnect();
			session = null;
			System.out.println("Disconnected channel and session");
			Thread.sleep(3000);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void downloadfromServer()
	{
		String pbcommand = "<unix command>";
		String scpcommand = "<scp command>";
		loginToPuttyHost(host, username, password);
		executeCommand(pbcommand, 5, ":"); 
		executeCommand(password, 3, ">");
		executeCommand(scpcommand, 90, ":");
		executeCommand(password, 20, ">");
		close();
	}
	
	public void uploadToServer() {
		String pbcommand = "<unix command>";
		String scpcommand = "<scp command>";
		loginToPuttyHost(host, username, password);
		executeCommand(pbcommand, 5, ":"); 
		executeCommand(password, 3, ">");
		executeCommand(scpcommand, 90, ":");
		executeCommand(password, 20, ">");
		close();
	}
	
	public static void main(String args[]) {
		SSH ssh = new SSH();
		ssh.uploadToServer(); // uploading to Server
		ssh.downloadfromServer(); //downloading to Server
	}
}
