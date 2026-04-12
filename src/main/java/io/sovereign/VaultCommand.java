package io.sovereign;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.Base64;
import java.util.Map;

@TopCommand
@Command(name = "vault", mixinStandardHelpOptions = true, description = "Motor de Bóveda Personal")
public class VaultCommand implements Runnable {

  @Inject
  @RestClient
  GitHubClient gitHubClient;

  @CommandLine.Option(names = {"-p", "--password"}, description = "Contraseña para la bóveda", interactive = true)
  char[] password;

  @CommandLine.Option(names = {"--import-github"}, description = "GitHub user to import")
  String githubUser;

  @CommandLine.Option(names = {"--import-linkedin"}, description = "Texto del extracto o perfil de LinkedIn")
  String linkedinText;

  @CommandLine.Option(names = {"--view"}, description = "Visualizar el Tablero de Identidad")
  boolean viewMode;

  @CommandLine.Option(names = {"--export-claim"}, description = "Exporta una prueba firma de tu identidad")
  boolean exportClaim;

  @CommandLine.Option(names = {"--verify-claim"}, description = "Verifica la autenticidad de una credencial externa")
  java.io.File claimFile;

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

        System.out.println("[NEW] Generando Identidad Soberana...");

        // GENERAR SEMILLA DE RECUPERACIÓN
        String mnemonic = security.generateMnemonic();

        System.out.println("\n" + "!".repeat(60));
        System.out.println("IMPORTANTE: GUARDA ESTAS 12 PALABRAS EN UN LUGAR SEGURO.");
        System.out.println("Si pierdes tu contraseña, esta es la ÚNICA forma de recuperar tu ID.");
        System.out.println("\n>> " + mnemonic + " <<");
        System.out.println("!".repeat(60) + "\n");

        // Guardamos el mnemonic dentro del VaultData (Cifrado!)
        VaultData data = new VaultData();
        data.attributes.put("recovery_phrase", mnemonic);

        // Generar llaves Ed25519 (Asimétricas)
        java.security.KeyPair identityPair = security.generateIdentityKeyPair();

        data.ownerName = "Fundador Soberano";

        // Codificar las llaves en Base64 para el JSON
        data.encodedPublicKey = java.util.Base64.getEncoder().encodeToString(identityPair.getPublic().getEncoded());
        data.encodedPrivateKey = java.util.Base64.getEncoder().encodeToString(identityPair.getPrivate().getEncoded());

        // Generar un DID preliminar basado en la llave pública
        data.did = "did:key:z6M" + data.encodedPublicKey.substring(0, 15);

        // Proceso de cifrado simétrico (Caja fuerte)
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

        // --- NUEVA LÓGICA DE IMPORTACIÓN ---
        if (githubUser != null && !githubUser.isEmpty()) {
          System.out.println("[FETCH] Consultando datos de GitHub para: " + githubUser + "...");
          try {
            // Llamada a la API externa
            Map<String, Object> githubData = gitHubClient.getUserData(githubUser);

            // Mapeamos los datos al perfil profesional de la bóveda
            restored.professionalProfile.put("github_user", githubData.get("login"));
            restored.professionalProfile.put("bio", githubData.get("bio"));
            restored.professionalProfile.put("public_repos", githubData.get("public_repos"));
            restored.professionalProfile.put("location", githubData.get("location"));
            restored.professionalProfile.put("last_sync", java.time.LocalDateTime.now().toString());

            System.out.println("[UPDATED] Datos de GitHub integrados localmente.");

            // Volvemos a guardar la bóveda actualizada (importante!)
            byte[] salt = security.extractSalt(encryptedFile); // Necesitamos un método para extraer el salt viejo
            var secretKey = security.deriveKeyFromPassword(password, salt);
            byte[] updatedEncrypted = security.encrypt(objectMapper.writeValueAsBytes(restored), secretKey, salt);

            java.nio.file.Files.write(vaultPath, updatedEncrypted);
            System.out.println("[FILE] Bóveda sincronizada y cerrada.");

          } catch (Exception e) {
            System.err.println("[ERROR] No se pudieron obtener datos de GitHub: " + e.getMessage());
          }
        }

        if (linkedinText != null && !linkedinText.isEmpty()) {
          System.out.println("[IMPORT] Procesando perfil profesional de LinkedIn...");

          // Normalizamos el dato
          restored.professionalProfile.put("linkedin_summary", linkedinText);
          restored.professionalProfile.put("professional_status", "Verificado por Usuario");

          // Volvemos a sellar la bóveda
          byte[] salt = security.extractSalt(encryptedFile);
          var secretKey = security.deriveKeyFromPassword(password, salt);
          byte[] updatedEncrypted = security.encrypt(objectMapper.writeValueAsBytes(restored), secretKey, salt);

          java.nio.file.Files.write(vaultPath, updatedEncrypted);
          System.out.println("[SUCCESS] LinkedIn integrado en tu identidad soberana.");
        }

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

        if (viewMode) {
          System.out.println("\n" + "=".repeat(60));
          System.out.println("         📊 TABLERO DE IDENTIDAD SOBERANA (BETA v1.0)");
          System.out.println("=".repeat(60));

          // Sección 1: Identidad Core
          System.out.printf("%-20s : %s\n", "PROPIETARIO", restored.ownerName.toUpperCase());
          System.out.printf("%-20s : %s\n", "DID (ID Digital)", restored.did);
          System.out.println("-".repeat(60));

          // Sección 2: Faceta Profesional (GitHub)
          if (restored.professionalProfile != null && !restored.professionalProfile.isEmpty()) {
            System.out.println("  [FACETA PROFESIONAL - GITHUB]");
            System.out.printf("  > Usuario    : %s\n", restored.professionalProfile.getOrDefault("github_user", "N/A"));
            System.out.printf("  > Repos      : %s\n", restored.professionalProfile.getOrDefault("repos_count", "0"));
            System.out.printf("  > Bio        : %s\n", restored.professionalProfile.getOrDefault("bio", "Sin descripción"));
            System.out.printf("  > Sincro     : %s\n", restored.professionalProfile.getOrDefault("last_sync", "N/A"));
          } else {
            System.out.println("  [!] No hay datos profesionales vinculados.");
          }
          System.out.println("-".repeat(60));

          if (restored.professionalProfile.containsKey("linkedin_summary")) {
            System.out.println("  [FACETA PROFESIONAL - LINKEDIN]");
            String summary = (String) restored.professionalProfile.get("linkedin_summary");
            // Cortamos el texto si es muy largo para que no rompa el diseño
            System.out.printf("  > Extracto    : %s...\n", summary.substring(0, Math.min(summary.length(), 50)));
          }

          // Sección 3: Seguridad RAM
          System.out.println("  [ESTADO DE SEGURIDAD]");
          System.out.println("  [✔] Llave Ed25519: ACTIVA Y CARGADA");
          System.out.println("  [✔] Cifrado      : AES-256-GCM (Hardware Accelerated)");

          System.out.println("=".repeat(60));
          System.out.println("      * Los datos mostrados solo existen en memoria RAM *");
          System.out.println("=".repeat(60) + "\n");
        }

        if (exportClaim) {
          System.out.println("[EXPORT] Generando Crendencial Verificable...");

          String claim = String.format("IDENTIDAD: %s | GITHUB: %s",
                  restored.ownerName,
                  restored.professionalProfile.getOrDefault("github_user", "N/A"));

          java.security.PrivateKey privKey = security.decodePrivateKey(restored.encodedPrivateKey);

          String signatureExport = security.sign(claim, privKey);

          java.util.Map<String, String> jsonMap = new java.util.HashMap<>();
          jsonMap.put("data", claim);
          jsonMap.put("signature", signatureExport);
          jsonMap.put("did", restored.did);
          jsonMap.put("publicKey", restored.encodedPublicKey);

          String finalJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);

          java.nio.file.Files.writeString(java.nio.file.Path.of("identidad_soberana.json"), finalJson);
          System.out.println("[SUCCESS] Credencial exportada a 'identidad_soberana.json'. ¡Puedes enviarla para ser verificada!");

        }

        if (claimFile != null) {
          System.out.println("[ORACLE] Analizando credencial externa: " + claimFile.getName());

          try {
            // 1. Leer el JSON externo
            var externalClaim = objectMapper.readTree(claimFile);
            String externalData = externalClaim.get("data").asText();
            String externalSig = externalClaim.get("signature").asText();
            String externalPubK = externalClaim.get("publicKey").asText();

            // 2. Convertir la llave pública de String a objeto PublicKey
            java.security.PublicKey pubKey = security.decodePublicKey(externalPubK);

            // 3. Verificar la autenticidad matemática
            boolean isClaimValid = security.verify(externalData, externalSig, pubKey);

            System.out.println("\n" + "=".repeat(42));
            if (isClaimValid) {
              System.out.println("***VERIFICACIÓN EXITOSA***");
              System.out.println("Sello digital: AUTÉNTICO");
              System.out.println("Los datos no han sido alterados.");
              System.out.println("Contenido: " + externalData);
            } else {
              System.out.println("¡ALERTA!: CREDENCIAL INVÁLIDA");
              System.out.println("-- El sello no coincide o el contenido fue manipulado. --");
            }
            System.out.println("=".repeat(42) + "\n");

          } catch (Exception e) {
            System.err.println("[ERROR] No se pudo procesar la credencial: " + e.getMessage());
          }
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