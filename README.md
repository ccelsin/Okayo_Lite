# Okayo Lite

Okayo Lite est une application web destinée à la gestion simplifiée de la facturation, des achats et du catalogue produits. Elle s'appuie sur un backend Spring Boot sécurisé par JWT et une interface React propulsée par Vite, Tailwind CSS et DaisyUI.

## Architecture en un coup d'œil

| Couche | Technologies principales | Points clés |
| --- | --- | --- |
| Backend | Java 17, Spring Boot 3, Spring Security, Spring Data JPA, jjwt, Springdoc OpenAPI | API REST sécurisée avec JWT, persistance MySQL, génération de PDF, documentation Swagger |
| Frontend | React 18, Vite 5, Tailwind CSS 3, DaisyUI, React Router, React Hook Form, Axios | Tableau de bord, gestion des factures, achats, TVA, authentification et formulaires riches |
| Infrastructure | Docker Compose, MySQL 8 | Orchestration complète pour le développement local |

## Fonctionnalités principales

- Authentification JWT et gestion de profil utilisateur.
- Tableaux de bord pour le suivi de l'activité, des factures et des achats.
- Gestion du catalogue produits et de la TVA.
- Génération et édition de factures
- Documentation d'API accessible via Swagger UI.

## Prérequis

### Option Docker (recommandé)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) ou une installation Docker Engine + Docker Compose.

### Exécution manuelle
- Java 17+
- Maven 3.9+ (ou l'outil wrapper `mvnw` fourni)
- Node.js 20+
- npm 10+
- Une instance MySQL 8 accessible

## Démarrage rapide avec Docker Compose

1. Facultatif : créer un fichier `.env` à la racine pour personnaliser les variables d'environnement (voir section [Configuration](#configuration)).

2. Lancer la stack de développement :

   ```bash

   docker compose -f docker-compose.dev.yml up -d --build


   ```

3. Accéder aux services :
   - Frontend : http://localhost:3000
   - API : http://localhost:8080
   - Documentation Swagger : http://localhost:8080/swagger-ui/index.html

Les volumes `db-data-dev` et `maven-cache` persistent respectivement les données MySQL et le cache Maven.

## Exécution manuelle des services

### Backend Spring Boot

```bash
cd backend
./mvnw spring-boot:run
```

Par défaut, l'API écoute sur http://localhost:8080 et se connecte à une base MySQL `okayo_litee` avec l'utilisateur `root` / `root`. Vous pouvez surcharger ces valeurs via un fichier `.env` placé dans `backend/`.

### Frontend React

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0 --port 3000
```

L'application Vite sera accessible sur http://localhost:3000. Le frontend consomme l'API via la variable `VITE_API_URL` (voir ci-dessous).

## Configuration

### Variables utilisées par Docker Compose

| Variable | Valeur par défaut | Description |
| --- | --- | --- |
| `DB_NAME` | `okayo` | Nom de la base MySQL créée pour le développement |
| `DB_USER` | `okayo` | Utilisateur MySQL non root |
| `DB_PASSWORD` | `okayo_pass` | Mot de passe de l'utilisateur MySQL |
| `DB_ROOT_PASSWORD` | `root_pass` | Mot de passe root MySQL |
| `SPRING_PROFILES_ACTIVE` | `dev` | Profil Spring actif |
| `BACKEND_URL` | `http://backend:8080` | URL utilisée par le frontend dans le conteneur |

Placez ces variables dans un fichier `.env` à la racine si vous souhaitez les modifier sans toucher au fichier Compose.

### Backend (`backend/.env` ou variables système)

| Variable | Description |
| --- | --- |
| `SPRING_DATASOURCE_URL` | Chaîne de connexion JDBC (par défaut `jdbc:mysql://localhost:3306/okayo_litee`) |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Identifiants MySQL |
| `JWT_SECRET_KEY` / `JWT_EXPIRATION_TIME` | Secrets JWT si vous souhaitez remplacer les valeurs par défaut |

### Frontend (`frontend/.env`)

| Variable | Description |
| --- | --- |
| `VITE_API_URL` | URL de base utilisée par Axios (`/api` par défaut, pratique derrière un proxy) |

## Scripts utiles

### Backend
- `./mvnw clean test` : exécuter la suite de tests backend.
- `./mvnw spring-boot:run` : lancer l'API en mode développement.
- `./mvnw package` : générer l'artefact JAR.

### Frontend
- `npm run dev` : démarrer le serveur Vite.
- `npm run build` : produire la version optimisée.
- `npm run preview` : prévisualiser le build.
- `npm run lint` : exécuter ESLint.

## Structure du dépôt

```
.
├── backend/            # Application Spring Boot (API, sécurité, génération PDF)
├── frontend/           # Application React (pages, composants, styles Tailwind)
├── docker-compose.dev.yml
└── README.md
```

## Ressources complémentaires

- La documentation Swagger est servie via Springdoc et accessible sur `/swagger-ui/index.html` lorsque le backend tourne.
- Le projet est configuré pour fonctionner en fuseau horaire Europe/Paris lors de l'exécution dans Docker.
- Un cache Maven partagé et la synchronisation des sources via volumes Docker permettent un rechargement rapide en développement.

Bon développement !