package ca.discotek.proxy.cert.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

import ca.discotek.proxy.cert.CertUtil;
import sun.security.x509.AlgorithmId;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

@SuppressWarnings("restriction")
public class CreateCertificateAuthorityUtil {
    
    static final File TMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));
    static final File DEFAULT_CA_KEY_STORE_FILE = new File(TMP_DIRECTORY, "cakeystore.ks");
    static final File DEFAULT_CA_CERT_FILE = new File(TMP_DIRECTORY, "cacert.der");
    
    static final String CA_KEY_STORE_FILE_PROPERTY_NAME = "ca-keystore-file";
    static final String CA_CERT_FILE_PROPERTY_NAME = "ca-cert-file";
    
    public static final String CA_KEYSTORE_PASSWORD = "password";
    public static final String CA_KEY_ALIAS = "ca_alias";
    
    public static final String X500_CN_COMMON_NAME_VALUE = "Fake Certificate Authority Inc.";
    public static final String X500_OU_ORGANIZATIONAL_UNIT_VALUE = "Fake Certificate Authority OrgUnit";
    public static final String X500_O_ORGANIZATIONAL_VALUE = "Fake Certificate Authority Org";
    public static final String X500_L_LOCALITY_VALUE = "Fake Certificate Authority City";
    public static final String X500_ST_STATE_PROVINCE_NAME_VALUE = "Fake Certificate Authority State";
    public static final String X500_C_COUNTRY_NAME_VALUE = "Fake Certificate Authority Country";
    
    public static final String CA_KEYSTORE_FILE;
    public static final String CA_CERT_FILE;

    public static final X500Name CA_X500_NAME;

    static {
        String value = System.getProperty(CA_KEY_STORE_FILE_PROPERTY_NAME);
        CA_KEYSTORE_FILE = value == null ? DEFAULT_CA_KEY_STORE_FILE.getAbsolutePath() : value;
        value = System.getProperty(CA_CERT_FILE_PROPERTY_NAME);
        CA_CERT_FILE = value == null ? DEFAULT_CA_CERT_FILE.getAbsolutePath() : value;
        try { CA_X500_NAME = CertUtil.createName(X500_CN_COMMON_NAME_VALUE, X500_OU_ORGANIZATIONAL_UNIT_VALUE, X500_O_ORGANIZATIONAL_VALUE, X500_L_LOCALITY_VALUE, X500_ST_STATE_PROVINCE_NAME_VALUE, X500_C_COUNTRY_NAME_VALUE); } 
        catch (IOException e) {
            throw new Error("Couldn't create Certificate Authority X500 Name", e);
        }
        System.out.println("CA Keystore file: " + CA_KEYSTORE_FILE + ", and CA Cert file: " + CA_CERT_FILE);
        System.out.println("CA X500 Name: " + CA_X500_NAME);
    }

	public static void main(String[] args) throws Exception {
		createAndExportSelfSignedCertificateTo(CertUtil.KEY_PAIR_GENERATOR.generateKeyPair(), CA_KEYSTORE_FILE, CA_CERT_FILE);
	}
	
    public static X509Certificate createAndExportSelfSignedCertificateTo(KeyPair caKeyPair, String keyStoreLocation, String derFileLocation) throws Exception {
        X509Certificate caCert = CertUtil.createCertificate(CA_X500_NAME, CA_X500_NAME, caKeyPair.getPublic(), caKeyPair.getPrivate(), true);
    	KeyStore ks = KeyStore.getInstance("JKS");
    	ks.load(null, CA_KEYSTORE_PASSWORD.toCharArray());
    	ks.setKeyEntry(CA_KEY_ALIAS, caKeyPair.getPrivate(), CA_KEYSTORE_PASSWORD.toCharArray(), new X509Certificate[]{caCert});
    	
        try (PrintStream ps = new PrintStream(derFileLocation); 
             FileOutputStream fos = new FileOutputStream(keyStoreLocation);) {
            CertUtil.printCertificateTo(caCert, ps);
            ks.store(fos, CA_KEYSTORE_PASSWORD.toCharArray());
        }
		
		return caCert;
    }
}
