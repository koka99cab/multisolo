# Maintenance et bots

L'objectif est une réaction rapide aux mises à jour, à l'image de Simple Voice
Chat. La séparation `core` / `fabric` réduit la surface à adapter. Elle ne
remplace pas les essais du jeu, notamment lorsque les paquets ou le stockage changent.

## Ce qui est automatisé dans le dépôt

- `ci.yml` compile et teste sous Windows et Linux, valide le dépôt et produit un JAR.
- `watch-minecraft.yml` consulte les métadonnées officielles Fabric chaque heure,
  sur la branche par défaut. Un nouveau candidat ouvre une issue de portage,
  dédupliquée par version. GitHub peut retarder les exécutions planifiées.
- `dependabot.yml` propose des mises à jour Gradle et GitHub Actions chaque semaine.

Les workflows s'activent après publication sur GitHub si Actions est autorisé.
La veille nécessite la permission d'écrire des issues. Elle ne modifie pas les
sources, ne lance pas un agent de programmation et ne publie aucune release.
Une issue fermée n'est pas recréée pour la même version.

```sh
python scripts/check_minecraft_updates.py --output build/update-report.json
```

Le canal `preview` de `compatibility.json` suit les snapshots/préreleases de
la série cible 26.4 jusqu'à sa sortie. Le canal `stable` ignore les previews.
La veille signale aussi une nouvelle série stable supérieure à la cible.
Un échec réseau fait échouer la veille ; il ne signifie pas « aucune mise à jour ».

## Contrat du futur bot de portage

À partir d'une issue de portage autorisée :

1. Lire `AGENTS.md`, les notes Mojang/Fabric et le diff de l'API concernée.
2. Créer `codex/port-<version>` ; conserver les branches de versions déjà maintenues.
3. Mettre à jour les versions exactes de `gradle.properties`, y compris la forme
   normalisée `minecraft_dependency_version` reconnue par le Loader, puis l'adaptateur
   `fabric`. Ne pas modifier le protocole ou les règles métier sans nécessité.
4. Compiler, exécuter les tests et contrôler le contenu du JAR unique.
5. Réaliser les essais de `TESTING.md` ou laisser la validation en jeu en attente.
6. Mettre à jour changelog, documentation et état de compatibilité avec les preuves.
7. Préparer une PR expliquant le résultat. Publier seulement une version validée.

Au départ, maintenir la cible actuelle. Pour supporter plusieurs versions,
ajouter une branche de maintenance par ligne réellement incompatible et une
matrice CI. Ne pas promettre toutes les anciennes versions avant leur validation.

## Préparer une release

- Confirmer le numéro du mod, la cible exacte, le protocole et les migrations.
- Refaire le test d'installation avec uniquement Multisolo et Fabric Loader.
- Exécuter les scénarios applicables de `TESTING.md`.
- Renseigner `validated_game_versions` uniquement après ces essais.
- Produire le JAR avec le Wrapper, publier son changelog et son SHA-256.
- Ajouter les secrets Modrinth/CurseForge à GitHub seulement lors de leur mise en place.

La popularité se construira avec une première expérience agréable, une installation
claire et des mises à jour fiables ; le dépôt donne les outils pour y arriver.
