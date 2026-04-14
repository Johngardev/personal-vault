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

  @CommandLine.Option(names = {"--add-secret"}, arity = "2", description = "Guarda un secreto (Ej: --add-secret 'Bank' '1234')")
  String[] newSecret;

  @CommandLine.Option(names = {"--list-secrets"}, description = "Muestra todos los secretos guardados")
  boolean listSecrets;

  @Override
  public void run() {
    //Captura de contraseña
    if (this.password == null || this.password.length == 0) {
      System.out.println("Introduzca la clave maestra de su Bóveda: ");
      java.util.Scanner sc = new java.util.Scanner(System.in);
      String input = sc.nextLine();
      this.password = input.toCharArray();
    }

    SecurityService security = new SecurityService();
    var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    java.nio.file.Path vaultPath = java.nio.file.Paths.get("identity.vaul");

    try {
      VaultData vault;

      if (!java.nio.file.Files.exists(vaultPath)) {
        // --- MODO CREACION ---
        System.out.println("[NEW] No se encontro bóveda. Iniciando proceso de identidad...");
        vault = new VaultData();
        String mnemonic = security.generateMnemonic();

        System.out.println("\n" + "!".repeat(60));
        System.out.println("IMPORTANTE: GUARDA ESTAS 12 PALABRAS EN UN LUGAR SEGURO.");
        System.out.println("\n>> " + mnemonic + " <<\n");
        System.out.println("!".repeat(60) + "\n");

        vault.attributes.put("recovery_phrase", mnemonic);
        vault.ownerName = "Fundador y Propietario soberano.";

        //Generacion de Criptografia Asimetrica
        java.security.KeyPair pair = security.generateIdentityKeyPair();
        vault.encodedPublicKey = java.util.Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        vault.encodedPrivateKey = java.util.Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        vault.did = "did:key:z6M" + vault.encodedPublicKey.substring(0, 15);

        // Persistencia inicial
        saveVault(vault, security, password, vaultPath, objectMapper);
        System.out.println("[SUCCESS] Bóveda creada. DID: " + vault.did);

      } else {
        // --- MODO APERTURA ---
        byte[] encryptedFile = java.nio.file.Files.readAllBytes(vaultPath);
        byte[] decrypted = security.decrypt(encryptedFile, password);
        vault = objectMapper.readValue(decrypted, VaultData.class);
        System.out.println("[LOAD] Bienvenido, " + vault.ownerName);
      }

      boolean needsSave = false;

      // GitHub
      if (githubUser != null && !githubUser.isEmpty()) {
        System.out.println("[FETCH] Sincronizando GitHub...");
        var githubData = gitHubClient.getUserData(githubUser);
        vault.professionalProfile.put("github_user", githubData.get("login"));
        vault.professionalProfile.put("bio", githubData.get("bio"));
        vault.professionalProfile.put("last_sync", java.time.LocalDateTime.now().toString());
        needsSave = true;
      }

      // LinkedIn
      if (linkedinText != null && !linkedinText.isEmpty()) {
        vault.professionalProfile.put("linkedin_summary", linkedinText);
        System.out.println("[IMPORT] LinkedIn integrado.");
        needsSave = true;
      }

      // Notas Blindadas (Fase 5)
      if (newSecret != null && newSecret.length == 2) {
        vault.secrets.put(newSecret[0], newSecret[1]);
        System.out.println("[SECURE] Secreto '" + newSecret[0] + "' guardado.");
        needsSave = true;
      }

      // Persistencia si hubo cambios
      if (needsSave) {
        saveVault(vault, security, password, vaultPath, objectMapper);
      }

      // --- CONSULTAS Y ACCIONES (No modifican la bóveda) ---

      if (listSecrets) {
        System.out.println("\n--- 🗝️ ZONA BLINDADA ---");
        if (vault.secrets.isEmpty()) System.out.println("Bóveda vacía.");
        else vault.secrets.forEach((k, v) -> System.out.printf("[%s]: %s\n", k, v));
      }

      if (viewMode) {
        renderDashboard(vault); // Podrías mover el dashboard a su propio método
      }

      if (exportClaim) {
        exportVerifiableCredential(vault, security, objectMapper);
      }

      if (claimFile != null) {
        verifyExternalClaim(claimFile, security, objectMapper);
      }
    } catch (javax.crypto.AEADBadTagException e) {
      System.err.println("[ERROR] Contraseña incorrecta. Acceso denegado!");
    } catch (Exception e) {
      System.err.println("[ERROR] Fallo en el sistema: " + e.getMessage() );
    } finally {
      if (password != null) java.util.Arrays.fill(password, ' ');
    }
  }

  //Oráculo
  private void verifyExternalClaim(java.io.File file, SecurityService security, com.fasterxml.jackson.databind.ObjectMapper mapper) {
    System.out.println("[ORACLE] Analizando credencial externa: " + file.getName());
    try {
      var externalClaim = mapper.readTree(file);
      String data = externalClaim.get("data").asText();
      String signature = externalClaim.get("signature").asText();
      String pubK = externalClaim.get("publicKey").asText();

      boolean isValid = security.verify(data, signature, security.decodePublicKey(pubK));

      System.out.println("\n" + "=".repeat(45));
      if (isValid) {
        System.out.println("VERIFICACIÓN EXITOSA | Sello: AUTÉNTICO");
        System.out.println("Contenido: " + data);
      } else {
        System.out.println("ALERTA: CREDENCIAL INVÁLIDA O MANIPULADA");
      }
      System.out.println("=".repeat(45) + "\n");
    } catch (Exception e) {
      System.err.println("[ERROR] Fallo al verificar credencial: " + e.getMessage());
    }
  }

  //Emisor
  private void exportVerifiableCredential(VaultData vault, SecurityService security, com.fasterxml.jackson.databind.ObjectMapper mapper) {
    try {
      System.out.println("[EXPORT] Firmando credencial de identidad...");
      String claim = String.format("IDENTIDAD: %s | GITHUB: %s",
              vault.ownerName, vault.professionalProfile.getOrDefault("github_user", "N/A"));

      String sig = security.sign(claim, security.decodePrivateKey(vault.encodedPrivateKey));

      java.util.Map<String, String> credential = new java.util.HashMap<>();
      credential.put("data", claim);
      credential.put("signature", sig);
      credential.put("publicKey", vault.encodedPublicKey);
      credential.put("did", vault.did);

      String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(credential);
      java.nio.file.Files.writeString(java.nio.file.Path.of("identidad_soberana.json"), json);
      System.out.println("[SUCCESS] Archivo 'identidad_soberana.json' generado.");
    } catch (Exception e) {
      System.err.println("[ERROR] No se pudo exportar: " + e.getMessage());
    }
  }

  // Método saveVault centralizado
  private void saveVault(VaultData data, SecurityService security, char[] password,
                         java.nio.file.Path path, com.fasterxml.jackson.databind.ObjectMapper mapper) throws Exception {
    byte[] salt = security.generateSalt();
    var secretKey = security.deriveKeyFromPassword(password, salt);
    byte[] encrypted = security.encrypt(mapper.writeValueAsBytes(data), secretKey, salt);
    java.nio.file.Files.write(path, encrypted);
  }

  //Tablero
  private void renderDashboard(VaultData vault) {
    System.out.println("\n" + "█".repeat(60));
    System.out.println("  Sovereign Vault Dashboard v1.0");
    System.out.println("█".repeat(60));
    System.out.printf("  HOLDER : %s\n", vault.ownerName.toUpperCase());
    System.out.printf("  DID    : %s\n", vault.did);
    System.out.println("─".repeat(60));

    System.out.println("  [PROFESSIONAL FACETS]");
    vault.professionalProfile.forEach((k, v) -> System.out.printf("  > %-12s: %s\n", k.toUpperCase(), v));

    System.out.println("─".repeat(60));
    System.out.println("  [SECURITY STATUS]");
    System.out.printf("  > Secrets stored: %d\n", vault.secrets.size());
    System.out.println("  > Ed25519 Keys  : ACTIVE & SEALED");
    System.out.println("█".repeat(60) + "\n");
  }
}