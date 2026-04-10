# Personal Vault: Bóveda de Identidad Soberana

**Personal Vault** es una herramienta de línea de comandos (CLI) de alto rendimiento desarrollada con **Quarkus** y **Java 17**. Permite a los usuarios gestionar su identidad digital de forma descentralizada, segura y totalmente privada.

A diferencia de las plataformas tradicionales, esta bóveda utiliza criptografía de grado militar para asegurar que tú seas el único dueño de tus datos profesionales y personales.

---

## Características Principales

* **Identidad Soberana (DID):** Generación de identificadores únicos basados en el estándar `did:key` con llaves Ed25519.
* **Cifrado Bancario:** Almacenamiento local protegido con **AES-256-GCM** y derivación de llaves mediante **PBKDF2**.
* **Resiliencia Total (BIP-39):** Sistema de recuperación basado en frases semilla de **12 palabras mnemónicas**.
* **Conectividad de Perfil:**
    * **GitHub:** Sincronización automática de bio y repositorios vía API REST.
    * **LinkedIn:** Ingesta manual de extractos profesionales.
* **Sello Digital:** Capacidad de firmar mensajes para demostrar autoría e integridad de los datos.

---

## Instalación y Uso

### Requisitos
* **Java 17** o superior (recomendado GraalVM o OpenJDK).

### Ejecución rápida (Beta)
Si ya tienes el archivo `.jar` empaquetado, simplemente ejecuta:
```powershell
java -jar target/personal-vault-1.0.0-SNAPSHOT-runner.jar --view
```

### Iniciar en modo desarrollo (Live Coding)
./mvnw quarkus:dev

### Generar el binario portable (Uber-JAR)
./mvnw package "-Dquarkus.package.type=uber-jar" -DskipTests

---

## Ciclo de Vida de la Información
1. **Entrada:** Se reciben datos de fuentes externas (GitHub/LinkedIn).
2. **Normalización:** Los datos se estructuran en facetas JSON.
3. **Cifrado:** Se aplica AES-256-GCM usando una llave derivada de tu contraseña.
4. **Firma:** Se genera un sello Ed25519 que garantiza que los datos no han sido alterados.
5. **Persistencia:** Se guarda el archivo `.vault` en el disco local.

---

## Estructura de Facetas
La identidad se organiza en módulos independientes:
* **Core:** DID, Claves Públicas y Mnemónico.
* **Professional:** Repositorios, Bio de GitHub y resúmenes de LinkedIn.
* **Security:** Metadatos de cifrado y algoritmos utilizados.