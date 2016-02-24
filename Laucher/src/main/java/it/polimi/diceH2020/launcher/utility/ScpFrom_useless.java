package it.polimi.diceH2020.launcher.utility;

import java.io.IOException;
import java.io.InputStream;


/**
 * recycled pojo used to handle ssh connection between the web server and the simulator
 */
public class ScpFrom_useless {
//	private static final org.slf4j.Logger logger =  LoggerFactory.getLogger(ScpFrom_useless.class.getName());
//    private String host;
//    private String user;
//    private String password;
//    private String pubKeyFile;
//    private String setKnownHosts;
//
//    public ScpFrom_useless(String host, String user, String password, String pubKeyFile, String setKnownHosts) {
//        this.host = host;
//        this.user = user;
//        this.password = password;
//        this.pubKeyFile = pubKeyFile;
//        this.setKnownHosts = setKnownHosts;
//    }

    // main execution function
    // coping RFile on AMPL server in LFile on local machine
    public void receivefile(String LFile, String RFile) throws Exception {
//        FileOutputStream fos = null;
//        try {
//            String lfile = LFile;
//            String rfile = RFile;
//
//          
//
//            String prefix = null;
//            if (new File(lfile).isDirectory()) {
//                prefix = lfile + File.separator;
//            }
// 
//            Session session = createSession();
//            session.connect();
//            // exec 'scp -f rfile' remotely
//            String command = "scp -f " + rfile;
//            Channel channel = session.openChannel("exec");
//            ((ChannelExec) channel).setCommand(command);
//            // get I/O streams for remote scp
//            OutputStream out = channel.getOutputStream();
//            InputStream in = channel.getInputStream();
//
//            channel.connect();
//
//            byte[] buf = new byte[1024];
//
//            buf[0] = 0;
//            out.write(buf, 0, 1);
//            out.flush();
//            // reading channel
//            while (true) {
//                int c = checkAck(in);
//                if (c != 'C') {
//                    break;
//                }
//                in.read(buf, 0, 5);
//                long filesize = 0L;
//                while (true) {
//                    if (in.read(buf, 0, 1) < 0) {
//                        break;
//                    }
//                    if (buf[0] == ' ')
//                        break;
//                    filesize = filesize * 10L + (long) (buf[0] - '0');
//                }
//                String file = null;
//                for (int i = 0;; i++) {
//                    in.read(buf, i, 1);
//                    if (buf[i] == (byte) 0x0a) {
//                        file = new String(buf, 0, i);
//                        break;
//                    }
//                }
//                buf[0] = 0;
//                out.write(buf, 0, 1);
//                out.flush();
//                fos = new FileOutputStream(prefix == null ? lfile : prefix
//                        + file);
//                int foo;
//                while (true) {
//                    if (buf.length < filesize)
//                        foo = buf.length;
//                    else
//                        foo = (int) filesize;
//                    foo = in.read(buf, 0, foo);
//                    if (foo < 0) {
//                        break;
//                    }
//                    fos.write(buf, 0, foo);
//                    filesize -= foo;
//                    if (filesize == 0L)
//                        break;
//                }
//                fos.close();
//                fos = null;
//
//                if (checkAck(in) != 0) {
//                    System.exit(0);
//                }
//
//                buf[0] = 0;
//                out.write(buf, 0, 1);
//                out.flush();
//            }
//            session.disconnect();
//        } catch (Exception e) {
//            logger.error("Error while getting a file.", e);
//            try {
//                if (fos != null)
//                    fos.close();
//            } catch (Exception ee) {
//            }
//        }
    }

    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
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
/*
    public void localReceivefile(String LFile, String RFile) throws Exception {
        if (!new File(RFile).exists()){
            throw new FileNotFoundException("File " + RFile + " not found!");
        }
        ExecSSH_useless ex = new ExecSSH_useless(RFile, RFile, RFile, pubKeyFile, setKnownHosts);

        if (new File(LFile).exists() && new File(LFile).isDirectory() && !LFile.endsWith(File.separator)){
            LFile = LFile + File.separator;
        }
        String command = String.format("cp %s %s", RFile, LFile);
        ex.localExec(command);
    }
*/    
//    private Session createSession() throws Exception{
//        JSch jsch = new JSch();
//        jsch.addIdentity(pubKeyFile, password);
//		jsch.setKnownHosts(setKnownHosts);
//		
//		Session session = jsch.getSession(user, host, 22);
//        
//		//Jsch 0.1.53 supports ecdsa-sha2-nistp256 key but default configuration look for RSA key
//		HostKeyRepository hkr = jsch.getHostKeyRepository();
//	    for(HostKey hk : hkr.getHostKey()){ 
//	        if(hk.getHost().contains(host)){ //So the variable host inserted by the user must be contained in setKnownHosts
//	            String type = hk.getType();
//	            session.setConfig("server_host_key",type); //set the real key type instead of using the default one
//	        }
//	    }
//       return session;
//    }
}