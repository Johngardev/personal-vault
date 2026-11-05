# Personal Vault

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=java" alt="Java 17+" />
  <img src="https://img.shields.io/badge/Quarkus-3.34.3-4695EB?style=for-the-badge&logo=quarkus" alt="Quarkus 3.34.3" />
  <img src="https://img.shields.io/badge/Status-MVP-28A745?style=for-the-badge" alt="MVP" />
  <img src="https://img.shields.io/badge/License-Repository%20Default-6C757D?style=for-the-badge" alt="License" />
</p>

Data Personal Vault MVP is a Java-based personal identity and data protection project designed to help users securely manage their digital identity, professional profile information, and sensitive personal data. The project combines self-sovereign identity principles, cryptographic protection, and practical data management workflows in a single application.

## Overview

Personal Vault is an MVP focused on creating a secure local digital vault for personal and professional information. It is designed around the idea that users should maintain exclusive control over their identity data, credentials, and personal records without relying on centralized platforms for trust or storage.

The system supports key concepts such as decentralized identifiers (DIDs), local encryption, secure profile synchronization, and audit-oriented access tracking. The current implementation provides a practical foundation for future expansion into cross-platform and mobile experiences.

## Key capabilities

- Decentralized identity model using DID generation based on the `did:key` approach.
- Strong local encryption with AES-256-GCM and PBKDF2-based key derivation.
- Seed recovery flow based on BIP-39 mnemonic phrases.
- Profile aggregation from external sources such as GitHub and LinkedIn.
- Digital signing support for proving authorship and integrity.
- Audit trail for recent successful access events.
- Local vault persistence with secure storage patterns.

## Architecture

The application is organized into logical facets that separate responsibilities and allow the system to evolve without coupling critical concerns together.

### Core
- DID creation and management
- public key handling
- mnemonic recovery support

### Professional
- GitHub profile synchronization
- LinkedIn profile data ingestion
- professional identity representation

### Security
- encryption metadata
- hashing and key derivation details
- verification and validation logic

## Technology stack

| Layer | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Quarkus |
| CLI support | Picocli |
| Serialization | Jackson |
| Encryption | AES-256-GCM, PBKDF2 |
| Recovery model | BIP-39 mnemonic phrases |
| Testing | JUnit |

## Project structure

```text
personal-vault/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── DOCUMENTATION.md
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── identity.vaul
├── identidad_soberana.json
└── .gitignore
```

## Security model

The system focuses on protecting personal information through layered security principles:

- local-first storage model
- encrypted vault payloads
- derived encryption keys from user-controlled secrets
- deterministic identity generation
- verification of signed data and access history

This MVP is designed to demonstrate a strong privacy-oriented architecture and serves as a foundation for more advanced personal identity workflows.

## Getting started

### Prerequisites

- Java 17 or higher
- Maven or the included Maven wrapper
- Git

### Run in development mode

```bash
./mvnw quarkus:dev
```

### Build the application

```bash
./mvnw package
```

### Run the packaged application

```bash
java -jar target/personal-vault-1.0.0-SNAPSHOT-runner.jar --view
```

## Useful commands

```bash
./mvnw test
./mvnw package -DskipTests
./mvnw clean package
```

## Roadmap

The current implementation is positioned as an MVP and is intended to evolve toward broader capabilities, including:

- mobile-first deployment through Kotlin Multiplatform
- more advanced biometric and secure hardware integration
- richer profile synchronization and validation flows
- stronger export, backup, and recovery experiences

## Project status

This repository is a working MVP focused on the core ideas of personal digital identity, secure storage, and controlled access to personal information. It is intended as a functional foundation for subsequent iterations and a research-oriented prototype.

## Author

John Garcia

## License

Please review the repository for the specific licensing terms applied to this project.
