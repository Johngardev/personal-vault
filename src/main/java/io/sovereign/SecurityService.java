package io.sovereign;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Collectors;

public class SecurityService {

  private static final int ITERATIONS = 600_000;
  private static final int KEY_LENGTH = 256;
  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String SECRET_PEPPER = "S0v3r31gn_P3pp3r_2026!#";

  /**
   * Deriva una clave AES de 256 bits a partir de una contraseña y un salt.
   */
  public SecretKeySpec deriveKeyFromPassword(char[] password, byte[] salt) throws Exception {
    // Combinamos el password con el pepper
    char[] pepperedPassword = (new String(password) + SECRET_PEPPER).toCharArray();

    PBEKeySpec spec = new PBEKeySpec(pepperedPassword, salt, 65536, 256);
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
  }

  /*
   * Genera un Salt aleatorio de 16 bytes (128 bits).
   * */
  public byte[] generateSalt() {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return salt;
  }

  public byte[] encrypt(byte[] data, SecretKeySpec key, byte[] salt) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);

    GCMParameterSpec spec = new GCMParameterSpec(128, iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, spec);

    byte[] cipherText = cipher.doFinal(data);

    return ByteBuffer.allocate(salt.length + iv.length + cipherText.length)
            .put(salt)
            .put(iv)
            .put(cipherText)
            .array();
  }

  public byte[] decrypt(byte[] fullPackage, char[] password) throws Exception {
    ByteBuffer bb = ByteBuffer.wrap(fullPackage);

    // 1. Extraer el Salt
    byte[] salt = new byte[16];
    bb.get(salt);

    // 2. Re-derivar la llave usando la password del usuario y el salt extraído
    SecretKeySpec key = deriveKeyFromPassword(password, salt);

    // 3. Extraer el IV
    byte[] iv = new byte[12];
    bb.get(iv);

    // 4. El resto es el contenido cifrado
    byte[] cipherText = new byte[bb.remaining()];
    bb.get(cipherText);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec spec = new GCMParameterSpec(128, iv);
    cipher.init(Cipher.DECRYPT_MODE, key, spec);

    return cipher.doFinal(cipherText);
  }

  /**
   * Genera un par de llaves de identidad asimétrica.
   * Estas llaves permiten al usuario firmar documentos y autenticarse.
   */
  public KeyPair generateIdentityKeyPair() throws Exception {
    // Ed25519 es parte de la Java Standard Edition desde Java 15+
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
    return kpg.generateKeyPair();
  }

  /**
   * Firma un mensaje usando la llave privada Ed25519.
   */
  public String signMessage(String message, String encodedPrivateKey) throws Exception {
    byte[] privateKeyBytes = Base64.getDecoder().decode(encodedPrivateKey);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
    KeyFactory kf = KeyFactory.getInstance("Ed25519");

    Signature sig = Signature.getInstance("Ed25519");
    sig.initSign(kf.generatePrivate(spec));
    sig.update(message.getBytes());

    return Base64.getEncoder().encodeToString(sig.sign());
  }

  /**
   * Verifica si una firma es válida para un mensaje y una llave pública dada.
   */
  public boolean verifySignature(String message, String signature, String encodedPublicKey) throws Exception {
    byte[] publicKeyBytes = Base64.getDecoder().decode(encodedPublicKey);
    X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
    KeyFactory kf = KeyFactory.getInstance("Ed25519");

    Signature sig = Signature.getInstance("Ed25519");
    sig.initVerify(kf.generatePublic(spec));
    sig.update(message.getBytes());

    byte[] sigBytes = Base64.getDecoder().decode(signature);
    return sig.verify(sigBytes);
  }

  public byte[] extractSalt(byte[] encryptedPackage) {
    byte[] salt = new byte[16];
    System.arraycopy(encryptedPackage, 0, salt, 0, 16);
    return salt;
  }

  public String generateMnemonic() {
    // Lista simplificada de 32 palabras para la prueba (BIP-39 real tiene 2048)
    // En producción usaríamos la lista completa, pero para la Beta esto es perfecto.
    String[] wordList = {
            "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
            "absurd", "abuse", "access", "accident", "account", "accuse", "achieve", "acid",
            "acoustic", "acquire", "across", "act", "action", "actor", "actress", "actual",
            "adapt", "add", "addict", "address", "adjust", "admit", "adult", "advance"
    };

    SecureRandom random = new SecureRandom();
    return random.ints(12, 0, wordList.length)
            .mapToObj(i -> wordList[i])
            .collect(Collectors.joining(" "));
  }

  /*
  * Firma un mensaje usando la llave privada Ed25519 del usuario
  * */
  public String sign (String message, java.security.PrivateKey privateKey) {
    try {
      Signature s = Signature.getInstance("Ed25519");
      s.initSign(privateKey);
      s.update(message.getBytes(StandardCharsets.UTF_8));
      byte[] signature = s.sign();
      return Base64.getEncoder().encodeToString(signature);
    } catch (Exception e) {
      throw new RuntimeException("Error al generar sello digital", e);
    }
  }

  /**
   * Verifica si una firma es válida para un mensaje y una llave pública dados.
   */
  public boolean verify(String message, String signatureBase64, java.security.PublicKey publicKey) {
    try {
      Signature s = Signature.getInstance("Ed25519");
      s.initVerify(publicKey);
      s.update(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      byte[] signature = Base64.getDecoder().decode(signatureBase64);
      return s.verify(signature);
    } catch (Exception e) {
      // Si hay un error en el formato de la firma, asumimos que no es válida
      return false;
    }
  }

  public java.security.PrivateKey decodePrivateKey(String base64Key) throws Exception {
    byte[] keyBytes = java.util.Base64.getDecoder().decode(base64Key);
    return java.security.KeyFactory.getInstance("Ed25519")
            .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyBytes));
  }

  public java.security.PublicKey decodePublicKey(String base64Key) throws Exception {
    byte[] keyBytes = java.util.Base64.getDecoder().decode(base64Key);
    return java.security.KeyFactory.getInstance("Ed25519")
            .generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));
  }
}
