# Contribuer à Multisolo

Le projet démarre. Le premier objectif est une expérience vanilla cohérente,
avec une installation simple et des sauvegardes protégées.

1. Lire le [concept](docs/DESIGN.md) et l'[architecture](docs/ARCHITECTURE.md).
2. Prendre une tâche limitée dans la [roadmap](docs/ROADMAP.md).
3. Créer une branche `codex/<sujet>` pour les agents, ou `feature/<sujet>`.
4. Garder les règles métier dans `core` et les appels Minecraft dans `fabric`.
5. Lancer les [vérifications](docs/TESTING.md), puis décrire le résultat dans la PR.

Une PR doit expliquer le problème, le comportement obtenu, les essais réalisés
et les limites restantes. Une modification de seed, difficulté, protocole,
permissions ou format de sauvegarde nécessite une justification et des tests
des invariants concernés. Les changements d'architecture vont dans `docs/decisions/`.

Ne pas ajouter de nouveau mod obligatoire pour les joueurs. Le menu Multisolo
doit rester accessible sans Mod Menu. Traductions françaises et anglaises seront
ajoutées avec le premier écran, sans textes dispersés dans le code.

Le code original est sous [MIT](LICENSE). Conserver les licences et notices
des composants tiers. Ne jamais inclure de sauvegarde personnelle, jeton ou
contenu du jeu dans le dépôt.
