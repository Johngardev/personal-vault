package io.sovereign;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.Base64;

@TopCommand
@Command(name = "vault", mixinStandardHelpOptions = true, description = "Motor de Bóveda Personal")
public class VaultCommand implements Runnable {

  @CommandLine.Option(names = {"-p", "--password"}, description = "Contraseña para la bóveda", interactive = true)
  char[] password;

  @Override
  public void run() {
    // 1. Captura de contraseña (UX de consola)
    if (this.password == null || this.password.length == 0) {
      System.out.print("Introduzca la clave maestra para su Bóveda: ");
      java.util.Scanner sc = new java.util.Scanner(System.in);
      String input = sc.nextLine();
      this.password = input.toCharArray();
    }

    SecurityService security = new SecurityService();
    var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    java.nio.file.Path vaultPath = java.nio.file.Paths.get("identity.vault");

    try {
      if (!java.nio.file.Files.exists(vaultPath)) {
        // --- MODO CREACIÓN (Nacimiento de la Identidad) ---
        System.out.println("[NEW] No se encontró bóveda. Generando Identidad Soberana...");

        // A. Generar llaves Ed25519 (Asimétricas)
        java.security.KeyPair identityPair = security.generateIdentityKeyPair();

        // B. Preparar el objeto de datos
        VaultData data = new VaultData();
        data.ownerName = "Fundador Soberano";

        // Codificamos las llaves en Base64 para el JSON
        data.encodedPublicKey = java.util.Base64.getEncoder().encodeToString(identityPair.getPublic().getEncoded());
        data.encodedPrivateKey = java.util.Base64.getEncoder().encodeToString(identityPair.getPrivate().getEncoded());

        // Generamos un DID preliminar basado en la llave pública
        data.did = "did:key:z6M" + data.encodedPublicKey.substring(0, 15);

        // C. Proceso de cifrado simétrico (Caja fuerte)
        byte[] salt = security.generateSalt();
        var secretKey = security.deriveKeyFromPassword(password, salt);

        byte[] jsonBytes = objectMapper.writeValueAsBytes(data);
        byte[] encryptedPackage = security.encrypt(jsonBytes, secretKey, salt);

        java.nio.file.Files.write(vaultPath, encryptedPackage);

        System.out.println("[SUCCESS] Bóveda creada con Identidad Ed25519.");
        System.out.println("[ID] Tu DID es: " + data.did);

      } else {
        // --- MODO APERTURA (Recuperación) ---
        System.out.println("[LOAD] Bóveda detectada. Desbloqueando llaves...");

        byte[] encryptedFile = java.nio.file.Files.readAllBytes(vaultPath);
        byte[] decrypted = security.decrypt(encryptedFile, password);
        VaultData restored = objectMapper.readValue(decrypted, VaultData.class);

        System.out.println("[SUCCESS] Bienvenido de nuevo, " + restored.ownerName);
        System.out.println("[DATA] DID: " + restored.did);

        // --- NUEVO: PRUEBA DE FIRMA DIGITAL ---
        System.out.println("\n[SIGN] Probando Sello Digital...");
        String message = "Yo, " + restored.ownerName + ", autorizo el acceso a mis datos.";

        // Generamos la firma con la llave privada que acabamos de descifrar
        String signature = security.signMessage(message, restored.encodedPrivateKey);
        System.out.println("[+] Mensaje: " + message);
        System.out.println("[+] Firma (Base64): " + signature);

        // Verificamos con la pública (lo que haría un tercero)
        boolean isValid = security.verifySignature(message, signature, restored.encodedPublicKey);

        if (isValid) {
          System.out.println("[VERIFIED] ¡Firma auténtica! La identidad es soberana e íntegra.");
        } else {
          System.err.println("[FAILED] La firma no pudo ser verificada.");
        }
      }
    } catch (javax.crypto.AEADBadTagException e) {
      System.err.println("[ERROR] Contraseña incorrecta. El acceso a las llaves de identidad fue denegado.");
    } catch (Exception e) {
      System.err.println("[ERROR] Error en el flujo soberano: " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (password != null) java.util.Arrays.fill(password, ' ');
    }
  }
}