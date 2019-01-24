package ca.discotek.proxy.cert.utils;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class CertUtil {

    public static final long ONE_YEAR = 1000 * 60 * 60 * 24 * 365;
    
    public static final String SHA1WITHRSA = "SHA1withRSA";
    
    public static final String X500_CN_COMMON_NAME = "CN";
    public static final String X500_OU_ORGANIZATIONAL_UNIT = "OU";
    public static final String X500_O_ORGANIZATIONAL = "O";
    public static final String X500_L_LOCALITY = "L";
    public static final String X500_ST_STATE_PROVINCE_NAME = "ST";
    public static final String X500_C_COUNTRY_NAME = "C";
    
    public static final String X500_HOST_OU_ORGANIZATIONAL_UNIT_VALUE = "Fake Host OrgUnit";
    public static final String X500_HOST_O_ORGANIZATIONAL_VALUE = "Fake Host Org";
    public static final String X500_HOST_L_LOCALITY_VALUE = "Fake Host City";
    public static final String X500_HOST_ST_STATE_PROVINCE_NAME_VALUE = "Fake Host State";
    public static final String X500_HOST_C_COUNTRY_NAME_VALUE = "Fake Host Country";
    
    public static final KeyPairGenerator KEY_PAIR_GENERATOR;
	
	
	static Map<String, SSLSocketFactory> factoryMap = new HashMap<>();
	
	static final String PASSWORD = "password";
	static final String HOST_ALIAS = "host_alias";
	
    static {
        try { KEY_PAIR_GENERATOR = KeyPairGenerator.getInstance("RSA"); } 
        catch (Exception e) { throw new Error("Could not create keystore.", e); }
        
    }
	
    @SuppressWarnings("restriction")
    public static X500Name createName(String commonName, String organizationUnit, String organization, String locality, String provinceState, String country) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append(X500_CN_COMMON_NAME + "=" + commonName);
        buffer.append(", ");
        buffer.append(X500_OU_ORGANIZATIONAL_UNIT + "=" + organizationUnit);
        buffer.append(", ");
        buffer.append(X500_O_ORGANIZATIONAL + "=" + organization);
        buffer.append(", ");
        buffer.append(X500_L_LOCALITY + "=" + locality);
        buffer.append(", ");
        buffer.append(X500_ST_STATE_PROVINCE_NAME + "=" + provinceState);
        buffer.append(", ");
        buffer.append(X500_C_COUNTRY_NAME + "=" + country);
        
        return new X500Name(buffer.toString());
    }
	
	@SuppressWarnings("restriction")
    public static SSLSocketFactory getTunnelSSLSocketFactory(String hostname) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, Exception {

		SSLSocketFactory factory = factoryMap.get(hostname);
		if (factory != null)
			return factory;
		
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

	    	KeyStore ks = generateHostKeystore(hostname);
	    	
			kmf.init(ks, PASSWORD.toCharArray());
			java.security.SecureRandom random = new java.security.SecureRandom();
			ctx.init(kmf.getKeyManagers(), null, random);

			SSLSocketFactory tunnelSSLFactory = ctx.getSocketFactory();
			
			factoryMap.put(hostname, tunnelSSLFactory);
			
			return tunnelSSLFactory;

        } 
		catch (Exception e) {
            throw new RuntimeException(e);
        }
	}

    @SuppressWarnings("restriction")
   public synchronized static KeyStore generateHostKeystore(String host) throws Exception {
        try (FileInputStream fis = new FileInputStream(CreateCertificateAuthorityUtil.CA_KEYSTORE_FILE); ) {
            KeyStore caKeyStore = KeyStore.getInstance("JKS");
            caKeyStore.load(fis, CreateCertificateAuthorityUtil.CA_KEYSTORE_PASSWORD.toCharArray());
            
            PrivateKey caPrivateKey = (PrivateKey) caKeyStore.getKey(CreateCertificateAuthorityUtil.CA_KEY_ALIAS, CreateCertificateAuthorityUtil.CA_KEYSTORE_PASSWORD.toCharArray());

            KeyPair hostKeyPair = KEY_PAIR_GENERATOR.generateKeyPair();
            X500Name hostName = createName(host, X500_HOST_OU_ORGANIZATIONAL_UNIT_VALUE, X500_HOST_O_ORGANIZATIONAL_VALUE, X500_HOST_L_LOCALITY_VALUE, X500_HOST_ST_STATE_PROVINCE_NAME_VALUE, X500_HOST_C_COUNTRY_NAME_VALUE);

            java.security.cert.Certificate cert = createCertificate(hostName, CreateCertificateAuthorityUtil.CA_X500_NAME, hostKeyPair.getPublic(), caPrivateKey, false);
            PrivateKey privateKey = hostKeyPair.getPrivate();
            
            KeyStore hostKeyStore = KeyStore.getInstance("JKS");
            hostKeyStore.load(null, PASSWORD.toCharArray());
            hostKeyStore.setKeyEntry(HOST_ALIAS, privateKey, PASSWORD.toCharArray(), new java.security.cert.Certificate[] { cert });
            
            return hostKeyStore;
        }
    }

    @SuppressWarnings("restriction")
    public static X509Certificate createCertificate(X500Name name, X500Name issuerName, PublicKey publicKey, PrivateKey signerPrivateKey, boolean isCertificateAuthority) throws Exception {
        X509CertInfo certInfo = new X509CertInfo();
        certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, new SecureRandom())));
        certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));

        // Validity
        Date validFrom = new Date();
        Date validTo = new Date(validFrom.getTime() + ONE_YEAR);
        certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(validFrom, validTo));

        boolean justName = isJavaAtLeast(1.8);
        
        if (justName) {
            certInfo.set(X509CertInfo.SUBJECT, name);
            certInfo.set(X509CertInfo.ISSUER, issuerName);
        } else {
            certInfo.set(X509CertInfo.SUBJECT, new CertificateSubjectName(name));
            certInfo.set(X509CertInfo.ISSUER, new CertificateIssuerName(issuerName));
        }
        
        if (isCertificateAuthority) {
            CertificateExtensions ext = new CertificateExtensions();
            ext.set(BasicConstraintsExtension.NAME, new BasicConstraintsExtension(Boolean.TRUE, Boolean.TRUE, 0));
            certInfo.set(X509CertInfo.EXTENSIONS, ext);
        }

        // Key and algorithm
        certInfo.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
        AlgorithmId algorithm = new AlgorithmId(AlgorithmId.sha1WithRSAEncryption_oid);
        certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algorithm));

        // Create a new certificate and sign it
        X509CertImpl cert = new X509CertImpl(certInfo);
        cert.sign(signerPrivateKey, SHA1WITHRSA);

        return cert;
    }
	
    public static void printCertificate(X509Certificate certificate) throws Exception {
        printCertificateTo(certificate, System.out);
    }
    
    @SuppressWarnings("restriction")
    public static void printCertificateTo(X509Certificate certificate, PrintStream ps) throws Exception {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        
        ps.println(sun.security.provider.X509Factory.BEGIN_CERT);
        encoder.encodeBuffer(certificate.getEncoded(), ps);
        ps.println(sun.security.provider.X509Factory.END_CERT);
    }
	
    
    public static final Pattern JAVA_VERSION = Pattern.compile("([0-9]*.[0-9]*)(.*)?");

    /**
     * Checks whether the current JAva runtime has a version equal or higher then the given one. As Java version are
     * not double (because they can use more digits such as 1.8.0), this method extracts the two first digits and
     * transforms it as a double.
     * @param version the version
     * @return {@literal true} if the current Java runtime is at least the specified one,
     * {@literal false} if not or if the current version cannot be retrieve or is the retrieved version cannot be
     * parsed as a double.
     */
    public static boolean isJavaAtLeast(double version) {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) {
            return false;
        }

        // if the retrieved version is one three digits, remove the last one.
        Matcher matcher = JAVA_VERSION.matcher(javaVersion);
        if (matcher.matches()) {
            javaVersion = matcher.group(1);
        }

        try {
            double v = Double.parseDouble(javaVersion);
            return v >= version;
        } catch (NumberFormatException e) { //NOSONAR
            return false;
        }
    }
}
