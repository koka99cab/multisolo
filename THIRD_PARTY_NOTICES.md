# Composants tiers

Le code original de Multisolo est sous licence MIT. Aucune source de Simple
Voice Chat, Sodium ou Lithium n'a été copiée.

Les scripts `gradlew`, `gradlew.bat` et le JAR du Gradle Wrapper proviennent du
[template officiel Fabric, branche 26.1](https://github.com/FabricMC/fabric-example-mod/tree/26.1).
Le Wrapper est un composant Gradle sous [Apache-2.0](licenses/Apache-2.0.txt),
copyright des auteurs Gradle. Il sert uniquement à la construction du projet
et n'est pas intégré au mod distribué.

Fabric Loader, Minecraft et leurs bibliothèques sont résolus par Loom ; ils ne
sont pas redistribués dans le JAR Multisolo. Gson et SLF4J sont fournis par le
runtime du jeu. JUnit est une dépendance de test uniquement.

Le JAR joueur embarque quatre modules Fabric API : `fabric-api-base`,
`fabric-lifecycle-events-v1`, `fabric-screen-api-v1` et `fabric-resource-loader-v1`, sous Apache-2.0,
copyright FabricMC. Chaque JAR imbriqué conserve son manifeste et sa licence.
Ils ne demandent aucune installation séparée au joueur.

Le relais autonome distribue Gson 2.13.2, sous Apache-2.0, copyright Google.
Ses distributions incluent la licence Apache-2.0 et cette notice. Les autres
modules Fabric utilisés par les tests client ne sont pas distribués dans le mod.
