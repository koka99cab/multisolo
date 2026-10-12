# 0002 — Connected Solitude et relais indépendant

- Statut : adopté pour l'implémentation v0.1 en développement.
- Date : 2026-10-12.

La v0.1 est limitée à seed commune 0, spawn vanilla, identité réseau et messages
persistants géolocalisés. L'origine passe à la révision 2 ; aucune ancienne
sauvegarde n'est convertie. Une nouvelle sauvegarde marquée est créée avec les
API vanilla, sans réécrire leur calcul de spawn.

`core` conserve les règles Java. `network` ajoute contrat, identité Ed25519 et
transport HTTP sans Minecraft. `relay` fournit le service autonome et un stockage
borné sur disque. `fabric` inclut `core`, `network` et quatre modules Fabric API
dans un seul JAR joueur. Gson est déjà fourni par Minecraft.

Le menu utilise les événements Fabric Screen API. Un mixin limité aux setters
de difficulté Minecraft est nécessaire : aucun événement Fabric n'offre de veto
pour les appels avec `ignoreLock=true` ou le déverrouillage. Il s'applique
uniquement aux serveurs d'expériences marquées, jamais aux mondes ordinaires.

L'identité cryptographique prouve une clé, sans revendiquer une authentification
Microsoft. Le consentement est local, persistant et désactivé par défaut. Les
requêtes sortent sur un worker et l'expérience fonctionne hors ligne. Le relais
est configurable, HTTPS hors localhost, et aucun hébergement n'est acheté ou
déployé lors de cette étape.

Objectif de maintenance : toutes les versions Java depuis 26.1. L'implémentation
actuelle conserve une dépendance exacte à la dernière snapshot ; les échanges
restent séparés par version. La v0.1 ne promet pas des cartes équivalentes entre
versions ni la compatibilité avec des worldgen mods arbitraires.
