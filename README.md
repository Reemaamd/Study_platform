#  Study Session Planner

<div align="center">

![Angular](https://img.shields.io/badge/Angular-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)

**Application web intelligente permettant aux étudiants de planifier, organiser et suivre leurs sessions d'étude efficacement.**

</div>

---

##  Aperçu

> Une plateforme complète de gestion du temps d'étude, avec tableau de bord analytique, gestion des objectifs hebdomadaires, et détection automatique des conflits horaires.

---

##  Fonctionnalités

###  Authentification & Sécurité
- Inscription et connexion sécurisées
- Authentification par token **JWT**
- Rôles utilisateur : `USER` / `ADMIN`
- Protection des routes côté frontend et backend

###  Gestion des Sessions d'Étude
- Création automatique de sessions  
- Planification par date et créneau horaire
- Détection automatique des **conflits horaires**
- Contraintes intelligentes :
  -  Pas de chevauchement de sessions
  -  Pause obligatoire entre sessions
  -  Limite de durée par session

###  Dashboard & Statistiques
- Temps d'étude quotidien et hebdomadaire
- Taux de complétion des sessions
- Répartition des heures par matière
- Progression vers les objectifs fixés
- Graphiques interactifs en temps réel

###  Matières & Objectifs
- Gestion des matières personnalisées
- Objectifs hebdomadaires par matière
- Suivi de la progression en pourcentage

###  Groupes d'Étude
- Création et gestion de groupes
- Vue admin complète des groupes et membres

###  Administration
- Dashboard admin global
- Gestion des utilisateurs
- Statistiques de la plateforme

---

##  Stack Technique

| Couche | Technologie |
|--------|------------|
| **Frontend** | Angular 17+, TypeScript, HTML5/CSS3 |
| **Backend** | Spring Boot 4, Java 21 |
| **Sécurité** | Spring Security, JWT |
| **Base de données** | MongoDB |
| **Build** | Maven, Angular CLI |
| **Outils** | Git, Postman, VS Code, IntelliJ IDEA |

---

##  Architecture

```
┌─────────────────────────────────┐
│        Angular Frontend         │
│   (SPA — localhost:4200)        │
└────────────────┬────────────────┘
                 │  HTTP / REST API
                 ▼
┌─────────────────────────────────┐
│      Spring Boot Backend        │
│   (REST API — localhost:8080)   │
│                                 │
│  ┌──────────┐  ┌─────────────┐  │
│  │ Security │  │   Services  │  │
│  │  (JWT)   │  │ Controllers │  │
│  └──────────┘  └─────────────┘  │
└────────────────┬────────────────┘
                 │  MongoDB Driver
                 ▼
┌─────────────────────────────────┐
│           MongoDB               │
│   (Collections NoSQL)           │
└─────────────────────────────────┘
```

---

##  Structure du Projet

### Backend — `com.study.study_platform`

```
backend/
├── src/main/java/com/study/study_platform/
│   ├── config/          # Configuration Spring (CORS, beans...)
│   ├── controller/      # REST Controllers
│   ├── dto/             # Data Transfer Objects (requêtes/réponses)
│   ├── exception/       # Gestion centralisée des erreurs
│   ├── mapper/          # Conversion entité ↔ DTO
│   ├── model/           # Documents MongoDB (entités)
│   ├── repository/      # Interfaces MongoDB Repositories
│   ├── scheduler/       # Tâches planifiées (@Scheduled)
│   ├── security/        # JWT, filtres, UserDetailsService
│   ├── service/         # Logique métier
│   ├── StudyPlatformApplication.java
│   └── TestAuthRunner.java
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

### Frontend — Angular

```
frontend/src/
├── app/
│   ├── components/      # Composants réutilisables (bottom-nav...)
│   ├── guards/          # Route guards (auth, role)
│   ├── interceptors/    # Intercepteur HTTP (JWT token)
│   ├── landing/         # Page d'accueil publique
│   ├── pages/           # Pages de l'application
│   ├── services/        # Services HTTP (auth, sessions...)
│   ├── app.routes.ts    # Routing principal
│   └── app.config.ts    # Configuration Angular
├── environments/        # Variables d'environnement
├── index.html
└── styles.css
```

---

##  Installation & Lancement

### Prérequis

- Node.js 18+ & npm
- Java 21
- Maven 3.8+
- MongoDB (Atlas)

### 1. Cloner le projet

```bash
git clone https://github.com/TON_USERNAME/study-session-planner.git
cd study-session-planner
```

### 2. Lancer le Backend

```bash
cd backend
mvn spring-boot:run
# API disponible sur http://localhost:8080
```

### 3. Lancer le Frontend

```bash
cd frontend
npm install
ng serve
# App disponible sur http://localhost:4200
```

### 4. Configuration (`application.properties`)

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/study_platform
spring.application.name=study_platform
spring.mongodb.uri=mongodb+srv://admin:123123123@cluster0.9tdoyrw.mongodb.net/study_platform
app.jwt.secret=MySecretKey12345678912345678912345678
# 7 jours en millisecondes
app.jwt.expiration=604800000
```

---

##  Variables d'Environnement

| Variable | Description | Exemple |
|----------|-------------|---------|
| `spring.data.mongodb.uri` | URI de connexion MongoDB | `mongodb://localhost:27017/db` |
| `jwt.secret` | Clé secrète JWT | `mySecretKey` |
| `jwt.expiration` | Durée du token (ms) | `86400000` (24h) |

---

##  Endpoints API

###  Publics — `/auth/**`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/auth/login` | Connexion |
| `POST` | `/auth/register` | Inscription |

###  Utilisateur — `/users/**` `USER` `ADMIN`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/users/me` | Profil connecté |
| `PUT` | `/users/me` | Modifier le profil |
| `PUT` | `/users/change-password` | Changer le mot de passe |
| `DELETE` | `/users/me` | Supprimer le compte |
| `GET` | `/users/availabilities/**` | Disponibilités `USER` |

###  Sessions — `/study-sessions/**` `USER`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/study-sessions` | Liste des sessions |
| `POST` | `/study-sessions` | Créer une session |
| `PUT` | `/study-sessions/{id}` | Modifier une session |
| `DELETE` | `/study-sessions/{id}` | Supprimer une session |

###  Onboarding — `/onboarding/**` `USER`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/onboarding/**` | Configuration initiale |

###  Statistiques — `/statistics/**`

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| `GET` | `/statistics/dashboard` | `USER` `ADMIN` | Dashboard global |
| `GET` | `/statistics/study-time` | `USER` `ADMIN` | Temps d'étude |
| `GET` | `/statistics/session-progress` | `USER` `ADMIN` | Progression sessions |
| `GET` | `/statistics/daily-hours` | `USER` `ADMIN` | Heures journalières |
| `GET` | `/statistics/subject-stats` | `USER` `ADMIN` | Stats par matière |
| `GET` | `/statistics/admin/**` | `ADMIN` | Stats admin globales |

###  Groupes — `/groups/**` `USER` `ADMIN`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/groups` | Liste des groupes |
| `POST` | `/groups` | Créer un groupe |
| `PUT` | `/groups/{id}` | Modifier un groupe |
| `DELETE` | `/groups/{id}` | Supprimer un groupe |

###  Admin — `/admin/**` `ADMIN`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/admin/users` | Tous les utilisateurs |
| `DELETE` | `/admin/users/{id}` | Supprimer un utilisateur |

---

##  Équipe du Projet

Projet académique réalisé par :

- Amal Nekhdou
- Rania Saadani
- Karima Ait Ahmid

Encadré par : 

Établissement : ENSA Khouribga
Année universitaire : 2025/2026
---

##  Licence

Ce projet est développé dans un cadre académique.
