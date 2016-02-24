package it.polimi.diceH2020.launcher.service;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import it.polimi.diceH2020.launcher.utility.SshConnector_useles;

/**
 * semi-recycled pojo used to handle ssh connection between the web server and the simulator
 */
public class GreatSPN {
	
   private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GreatSPN.class.getName());

   public GreatSPN(String host, String user, String pk, String setKnownHosts, String psw) {
		this.host = host;
		this.user = user;
		this.pk = pk;
		this.psw = psw;
		this.setKnownHosts = setKnownHosts;
		GreatSPN.execute = new SshConnector_useles(host,user,psw,pk,setKnownHosts);
	}
	private String host;
	private String user;
	private String pk;
	private String setKnownHosts;
	private String psw;
	
	
	//ch14r4.dei.polimi.it
	//static SshConnector_useles execute=new SshConnector_useles("greatspn.dei.polimi.it","user","b9c744be"); <--- with PSW
	static SshConnector_useles execute;
	//String fileCommonPartString = "SWN_ProfileR";//TOBEINJECTED!
	
	//private static Logger logger = Logger.getLogger(GreatSPN.class.getName());
	//@Value("${dir.value}")
	private String strinvalue;// ="/usr/local/GreatSPN/scripts/swn_sym_sim /home/user/Desktop/mapreduce -a 10 -c 6";

	public String getStrinvalue() {
		return strinvalue;
	}

	public void setStrinvalue(String strinvalue) {
		this.strinvalue = strinvalue;
	}
	
	public SshConnector_useles getConnection(){
		return execute;
	}
	
	//necessita del complete path. Es.     directory+net+".net", /home/user/Desktop/"+ name+".net"
	public void sendFile(String localPath, String remotePath) throws Exception{
		execute.sendFile(localPath, remotePath);
		//logger.info("file "+ localPath +" has been sended and created in "+ remotePath );
	    //logger.info("file"+ localPath +" has been sended and created in "+ remotePath );
	}
	
	/**
	 * This method builds and executes the simulator's command used to launch one simulation (with the inserted values)
	 * 
	 * Command structure:
	 * remoteSimulatorScript remotePathToSimulationsFolder -m minBatch -M maxBatch -a accuracy -c confidenceInterval
	 * 
	 * @param simulationType
	 * @param remoteFilesPathWithoutExtension remote simulations' folder where files have been sended
	 * @param accuracy
	 * @param localSolPath local WS simulations' folder
	 * @param batchString  min batch, max batch or empty 
	 * @return
	 * @throws Exception
	 */
	public double runSimulation( String simulationType, String remoteFilesPathWithoutExtension, int accuracy, String localSolPath, String batchString) throws Exception{
		String str = null;
		double x = 0;
		
		if(!simulationType.equals("sym")){
			this.setStrinvalue("/home/user/Desktop/newSimulator/WNSIM "+remoteFilesPathWithoutExtension+" "+batchString+" -a "+accuracy+" -c 99");
		}else{
			this.setStrinvalue("/usr/local/GreatSPN/scripts/swn_"+simulationType+"_sim "+remoteFilesPathWithoutExtension+" "+batchString+" -a "+accuracy+" -c 6");
		}
		logger.info(this.getStrinvalue());
		execute.exec(strinvalue);
		File file=new File(localSolPath);
		if(!file.exists()){	
			file.createNewFile();
		}
		execute.receiveFile(localSolPath, remoteFilesPathWithoutExtension+".sta" );
	    str = FileUtils.readFileToString(file);	
	    String s="Thru_end = ";
	    int inizio = str.indexOf(s);
	    int fine = str.indexOf('\n',inizio);
	    x = Double.parseDouble(str.substring(inizio+s.length(),fine));
	    logger.info("ThroughputEND: "+x);
	    return x;
	}
	
	/**
	 * This method builds and executes the simulator's command used to launch one simulation (with the inserted values)
	 * 
	 * Command structure:
	 * remoteSimulatorScript remotePathToSimulationsFolder -m minBatch -M maxBatch -a accuracy -c confidenceInterval
	 * 
	 * @param simulationType
	 * @param remoteFilesPathWithoutExtension remote simulator current simulations' folder
	 * @param accuracy 
	 * @param localSolPath local WS simulations' folder
	 * @param batchString min batch, max batch or empty 
	 * @return
	 * @throws Exception
	 */
	public double[] runSimulation2( String simulationType, String remoteFilesPathWithoutExtension, int accuracy, String localSolPath, String batchString) throws Exception{
		String str = null;
		double x = 0;
		double x1 = 0;
		double x2 = 0;
		
		if(!simulationType.equals("sym")){
			this.setStrinvalue("/home/user/Desktop/newSimulator/WNSIM "+remoteFilesPathWithoutExtension+" "+batchString+" -a "+accuracy+" -c 99");
		}else{
			this.setStrinvalue("/usr/local/GreatSPN/scripts/swn_"+simulationType+"_sim "+remoteFilesPathWithoutExtension+" "+batchString+" -a "+accuracy+" -c 6");
		}
		
		logger.info(this.getStrinvalue());
		
		execute.exec(strinvalue);
		
		File file=new File(localSolPath);
		if(!file.exists()){	
			file.createNewFile();
		}

		execute.receiveFile(localSolPath, remoteFilesPathWithoutExtension+".sta" );
	    str = FileUtils.readFileToString(file);	
	    String s="Thru_end = ";
	    String thr_class1 = "Thru_join2Ca = ";
	    String thr_class2 = "Thru_join2Cb = ";  
	    
	    int inizio = str.indexOf(s);
	    int fine = str.indexOf('\n',inizio);
	    x = Double.parseDouble(str.substring(inizio+s.length(),fine));
	    
	    
	    inizio = str.indexOf(thr_class1);
	    fine = str.indexOf('\n',inizio);
	    x1 = Double.parseDouble(str.substring(inizio+thr_class1.length(),fine));
	    
	    inizio = str.indexOf(thr_class2);
	    fine = str.indexOf('\n',inizio);
	    x2 = Double.parseDouble(str.substring(inizio+thr_class2.length(),fine));
	    
	    logger.info("ThroughputEND: "+x+", Throughput1: "+x1+", Throughput2: "+x2);
	    
	    double[] throughputs = {x,x1,x2};	  
	    return throughputs;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPk() {
		return pk;
	}

	public void setPk(String pk) {
		this.pk = pk;
	}

	public String getSetKnownHosts() {
		return setKnownHosts;
	}

	public void setSetKnownHosts(String setKnownHosts) {
		this.setKnownHosts = setKnownHosts;
	}

	public String getPsw() {
		return psw;
	}

	public void setPsw(String psw) {
		this.psw = psw;
	}
	
	
	
}
