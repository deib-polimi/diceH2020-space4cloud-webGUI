package it.polimi.diceH2020.launcher.utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * recycled pojo used to handle ssh connection between the web server and the simulator
 */
public class ExecSSH_useless {


	    // main execution function
	    // returns in List<Strings> all answers of the server
	    public List<String> mainExec(String command) throws Exception {
	        List<String> res = new ArrayList<String>();

//	        Session session = createSession();
	        
	        // creating connection
//	        session.connect();
	        

	        // creating channel in execution mod
//	        Channel channel = session.openChannel("exec");
	        // sending command which runs bash-script in Configuration.RUN_WORKING_DIRECTORY directory
//	        ((ChannelExec) channel).setCommand(command);
	        // taking input stream
//	        channel.setInputStream(null);
//	        ((ChannelExec) channel).setErrStream(System.err);
//	        InputStream in = channel.getInputStream();
	        // connecting channel
//	        channel.connect();
//	        // read buffer
//	        byte[] tmp = new byte[1024];
//
//	        // reading channel while server responses smth or until it does not
//	        // close connection
//	        while (true) {
//	            while (in.available() > 0) {
//	                int i = in.read(tmp, 0, 1024);
//	                if (i < 0)
//	                    break;
//	                res.add(new String(tmp, 0, i));
//	            }
//	            if (channel.isClosed()) {
//	                res.add("exit-status: " + channel.getExitStatus());
//	                break;
//	            }
//	            try {
//	                Thread.sleep(1000);
//	            } catch (Exception ee) {
//	            }
//	        }
//	        // closing connection
//	        channel.disconnect();
//	        session.disconnect();

	        return res;
	    }

	    public List<String> localExec(String command) throws Exception {
	        List<String> res = new ArrayList<String>();
	        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
	        pb.redirectErrorStream(true);

	        Process p = pb.start();
	        BufferedReader stream = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String line = stream.readLine();
	        while (line != null) {
	            res.add(line);
	            line = stream.readLine();
	        }
	        stream.close();

	        return res;
	    }
	    
//	    private Session createSession() throws Exception{
//	        JSch jsch = new JSch();
//	        jsch.addIdentity(pubKeyFile, password);
//			jsch.setKnownHosts(setKnownHosts);
//			
//			Session session = jsch.getSession(user, host, 22);
//	        
//			//Jsch 0.1.53 supports ecdsa-sha2-nistp256 key but default configuration look for RSA key
//			HostKeyRepository hkr = jsch.getHostKeyRepository();
//		    for(HostKey hk : hkr.getHostKey()){ 
//		        if(hk.getHost().contains(host)){ //So the variable host inserted by the user must be contained in setKnownHosts
//		            String type = hk.getType();
//		            session.setConfig("server_host_key",type); //set the real key type instead of using the default one
//		        }
//		    }
//	       return session;
//	    }
}
