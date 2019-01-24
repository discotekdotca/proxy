package ca.discotek.proxy.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

import ca.discotek.proxy.vo.RequestResponse;

public class DataManager {
	
//	static final File TMP_DIR = new File(System.getProperty("java.io.tmp"));
    
    public static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    public static final File DEFAULT_TMP_DIR = new File(TMP_DIR, "proxy-data");

    public static final String DATA_DIRECTORY_PROPERTY_NAME = "proxy-data-dir";
    public static final File DATA_DIRECTORY;
    
	static {
	    String value = System.getProperty(DATA_DIRECTORY_PROPERTY_NAME);
	    DATA_DIRECTORY = value == null ? DEFAULT_TMP_DIR : new File(value);
		DATA_DIRECTORY.mkdirs();
		reset();
	}
	
	static AtomicInteger idGenerator = new AtomicInteger();
	
	public static DataManager INSTANCE = new DataManager();

	private DataManager() {}

	static void reset() {
		File files[] = DATA_DIRECTORY.listFiles();
		for (int i=0; i<files.length; i++)
			files[i].delete();
	}

	public void add(RequestResponse rr) throws IOException {
		int id = idGenerator.getAndIncrement();
		File file = new File(DATA_DIRECTORY, "RequestResponse-" + id + ".txt");
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(rr);
		oos.close();
		fos.close();
	}

	public RequestResponse[] getRequestResponses() throws IOException, ClassNotFoundException {
	    File files[] = DATA_DIRECTORY.listFiles();
	    RequestResponse rrs[] = new RequestResponse[files.length];
	    FileInputStream fis;
	    ObjectInputStream ois;
	    for (int i=0; i<files.length; i++) {
	        fis = new FileInputStream(files[i]);
	        ois = new ObjectInputStream(fis);
	        rrs[i] = (RequestResponse) ois.readObject();
	    }
	    
	    return rrs;
	}
}
