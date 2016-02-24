package it.polimi.diceH2020.launcher.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * recycled pojo used to handle ssh connection between the web server and the simulator
 */
public class ScpTo_useless {
//	 	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ScpTo_useless.class.getName());
//	    private String host;
//	    private String user;
//	    private String password;
//	    private String pubKeyFile;
//	    private String setKnownHosts;
//	    
//	    public ScpTo_useless(String host, String user, String password, String pubKeyFile, String setKnownHosts) {
//	        this.host = host;
//	        this.user = user;
//	        this.password = password;
//	        this.pubKeyFile = pubKeyFile;
//	        this.setKnownHosts = setKnownHosts;
//	    }

	    /**
	     * main execution function
	     * coping LFile on local machine in RFile on AMPL server
	     * @param LFile
	     * @param RFile
	     * @throws Exception
	     */
		public void sendfile(String LFile, String RFile) throws Exception {
//	        FileInputStream fis = null;
//	        try {
//	            String lfile = LFile;
//	            String rfile = RFile;
//
//	            Session session = createSession();
//	            session.connect();
//
//	            boolean ptimestamp = true;
//	            // exec 'scp -t rfile' remotely
//	            String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
//	            Channel channel = session.openChannel("exec");
//	            ((ChannelExec) channel).setCommand(command);
//	            // get I/O streams for remote scp
//	            OutputStream out = channel.getOutputStream();
//	            InputStream in = channel.getInputStream();
//	            // connecting channel
//	            channel.connect();
//
//	            if (checkAck(in) != 0) {
//	                System.exit(0);
//	            }
//
//	            File _lfile = new File(lfile);
//
//	            if (ptimestamp) {
//	                command = "T" + (_lfile.lastModified() / 1000) + " 0";
//	                command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
//	                out.write(command.getBytes());
//	                out.flush();
//	                if (checkAck(in) != 0) {
//	                    System.exit(0);
//	                }
//	            }
//	            // send "C0644 filesize filename", where filename should not include
//	            // '/'
//	            long filesize = _lfile.length();
//	            command = "C0644 " + filesize + " ";
//	            if (lfile.lastIndexOf('/') > 0) {
//	                command += lfile.substring(lfile.lastIndexOf('/') + 1);
//	            } else {
//	                command += lfile;
//	            }
//	            command += "\n";
//	            out.write(command.getBytes());
//	            out.flush();
//	            if (checkAck(in) != 0) {
//	                System.exit(0);
//	            }
//	            // send a content of lfile
//	            fis = new FileInputStream(lfile);
//	            byte[] buf = new byte[1024];
//	            while (true) {
//	                int len = fis.read(buf, 0, buf.length);
//	                if (len <= 0)
//	                    break;
//	                out.write(buf, 0, len);
//	            }
//	            fis.close();
//	            fis = null;
//	            buf[0] = 0;
//	            out.write(buf, 0, 1);
//	            out.flush();
//	            if (checkAck(in) != 0) {
//	                System.exit(0);
//	            }
//	            out.close();
//
//	            channel.disconnect();
//	            session.disconnect();
//
//	        } catch (Exception e) {
//	            logger.error("Error while sending a file.", e);
//	            try {
//	                if (fis != null)
//	                    fis.close();
//	            } catch (Exception ee) {
//	            }
//	        }
	    }

	    static int checkAck(InputStream in) throws IOException {
	        int b = in.read();
	        // b may be 0 for success,
	        // 1 for error,
	        // 2 for fatal error,
	        // -1
	        if (b == 0)
	            return b;
	        if (b == -1)
	            return b;

	        if (b == 1 || b == 2) {
	            StringBuffer sb = new StringBuffer();
	            int c;
	            do {
	                c = in.read();
	                sb.append((char) c);
	            } while (c != '\n');
	            if (b == 1) { // error
	                System.out.print(sb.toString());
	            }
	            if (b == 2) { // fatal error
	                System.out.print(sb.toString());
	            }
	        }
	        return b;
	    }

	    public void localSendfile(String LFile, String RFile) throws Exception {
	        if (!new File(LFile).exists())
	            throw new FileNotFoundException("File " + LFile + " not found!");

	    //    ExecSSH_useless ex = new ExecSSH_useless(RFile, RFile, RFile, pubKeyFile,setKnownHosts);

	        if (new File(RFile).exists() && new File(RFile).isDirectory() && !RFile.endsWith(File.separator))
	            RFile = RFile + File.separator;

//	        String command = String.format("cp %s %s", LFile, RFile);
	  //      ex.localExec(command);
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
