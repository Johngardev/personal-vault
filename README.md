# Personal Vault: Bóveda de Identidad Soberana (v1.5)

**Personal Vault** s una infraestructura de identidad descentralizada (SSI) de alto rendimiento. Desarrollada originalmente como un motor CLI con **Quarkus**, está evolucionando hacia un SDK multiplataforma para el empoderamiento de la identidad digital.
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

## Estructura de Facetas
La identidad se organiza en módulos independientes:
* **Core:** DID, Claves Públicas y Mnemónico.
* **Professional:** Repositorios, Bio de GitHub y resúmenes de LinkedIn.
* **Security:** Metadatos de cifrado y algoritmos utilizados.

---

## Innovaciones de Seguridad (Fase 6)

* **Identidad Soberana (DID):** Generación de identificadores únicos `did:key` con curvas Ed25519 (EdDSA).
* **Cifrado Blindado (Peppered KDF):** Implementación de un **Secret Pepper** estático que protege la bóveda contra ataques de fuerza bruta, incluso si el archivo `.vault` es comprometido.
* **Oráculo de Verificación:** Motor integrado para validar Credenciales Verificables (VC) externas mediante firmas criptográficas de no-repudio.
* **Registro de Auditoría (Anti-Intrusión):** Historial inmutable de los últimos 5 accesos exitosos almacenado dentro del payload cifrado.
* **Resiliencia BIP-39:** Recuperación mediante frases mnemónicas de 12 palabras.

---

## Roadmap hacia la Movilidad (Kotlin Multiplatform)

Estamos transicionando el núcleo hacia una arquitectura de **SDK Compartido**:
1.  **Core Shared:** Lógica criptográfica y de negocio en Kotlin puro.
2.  **Native UI:** Interfaces reactivas en Jetpack Compose (Android) y SwiftUI (iOS).
3.  **Hardware Binding:** Integración con Secure Enclave y Android Keystore.

---

## Ciclo de Vida del Dato
1. **Ingesta:** Captura de datos desde adaptadores (GitHub/LinkedIn).
2. **Blindaje:** Derivación de llave mediante `PBKDF2 + Salt + Pepper`.
3. **Sellado:** Cifrado autenticado con **AES-256-GCM**.
4. **Auditoría:** Estampado de tiempo del acceso actual.
5. **Persistencia:** Almacenamiento atómico en `identity.vault`.

---