# GestionSeries

Application de bureau Java (Swing) pour **renommer automatiquement des fichiers d'épisodes de séries TV** à partir des informations de [TheTVDB](https://thetvdb.com).

À partir de noms de fichiers comme `show.s02e05.720p.mkv`, GestionSeries retrouve le titre de chaque épisode (en français quand il existe) et renomme les fichiers sous une forme homogène :

```
Nom de la série - S02E05 - Titre de l'épisode.mkv
```

![Fenêtre principale de GestionSeries](docs/screenshot.png)

## Fonctionnalités

- **Recherche de la série sur TheTVDB** (API v4) : liste des résultats avec date de première diffusion, réseau, statut et résumé, pour distinguer les séries homonymes.
- **Titres d'épisodes en français**, avec repli sur la langue d'origine quand la traduction n'existe pas.
- **Lecture des numéros de saison et d'épisode dans les noms de fichiers**, par position : les caractères sélectionnés sont colorés dans le tableau pour vérifier le réglage d'un coup d'œil.
- **Numéro de saison** lu dans le nom du fichier, dans le nom du dossier, ou saisi manuellement.
- **Parcours récursif des sous-dossiers** (par exemple un dossier par saison).
- **Décalage de numérotation** (offset) et **longueur du numéro d'épisode** paramétrables.
- **Contrôle avant renommage** : sélection des fichiers à traiter, fichiers en erreur signalés avec leur cause, aperçu du nouveau nom.
- Interface en **thème sombre** ([FlatLaf](https://www.formdev.com/flatlaf/)).

## Prérequis

- **Java 21** ou plus récent (JDK pour compiler).
- Une **clé API TheTVDB v4**, à obtenir gratuitement depuis un compte sur [thetvdb.com](https://thetvdb.com/dashboard/account/apikey).

Les bibliothèques nécessaires sont fournies dans le dossier `lib/`.

## Configuration

La clé API n'est pas stockée dans le code. Copiez le modèle de configuration :

```
gestionseries.example.properties  →  gestionseries.properties
```

puis renseignez votre clé :

```properties
# Clé API TVDB v4
tvdb.apikey=votre-cle-api

# PIN d'abonné : uniquement pour une clé "user-supported", laisser vide sinon
tvdb.pin=
```

Le fichier `gestionseries.properties` est exclu de Git : **ne versionnez jamais votre clé**.

Il est recherché, dans l'ordre :

1. au chemin indiqué par l'option `-Dgestionseries.config=...` ;
2. dans le dossier courant (la racine du projet) ;
3. dans votre dossier personnel.

## Lancement

### Avec VS Code

Le projet s'ouvre directement dans VS Code avec l'extension [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack). Dans la vue **Exécuter et déboguer** (`Ctrl+Maj+D`), deux configurations sont disponibles :

- **GestionSeries** : lance l'application ;
- **Test API TVDB** : vérifie la connexion à TheTVDB sans passer par l'interface.

### En ligne de commande (Windows, PowerShell)

Depuis la racine du projet :

```powershell
javac -encoding UTF-8 -d bin -cp "lib/*" (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "bin;lib/*" ui.Application
```

Le projet peut aussi être importé dans Eclipse (fichiers `.project` et `.classpath` fournis).

## Utilisation

La fenêtre principale suit les étapes du traitement :

1. **Fichiers** : cliquez sur *Scanner Dossier* et choisissez le dossier contenant les épisodes. Cochez *Inclure les sous-dossiers* pour parcourir aussi les sous-dossiers.
2. **Série** : saisissez le nom de la série, lancez la recherche (bouton ou touche `Entrée`), puis choisissez la bonne série dans la liste (bouton *Sélectionner* ou double-clic).
3. **Numérotation** : indiquez où lire les numéros de saison et d'épisode dans les noms. Les sélections sont colorées dans la colonne *Nom fichier original* (vert pour la saison, bleu pour l'épisode, rouge en cas de chevauchement). Les positions se comptent à partir de 0.
4. **Actions** :
   - *Traitement des données* : lit les numéros et récupère le titre de chaque épisode ;
   - *Déterminer nom* : calcule le nouveau nom de chaque fichier ;
   - *Renommer* : renomme les fichiers sur le disque.

Seuls les fichiers cochés dans la colonne *Sel.* sont traités. Un fichier dont les numéros ne peuvent pas être lus passe au statut *Erreur* : survolez la ligne pour en voir la raison.

> ⚠️ Le renommage modifie directement les fichiers : vérifiez la colonne *Nom fichier traité* avant de cliquer sur *Renommer*.

## Structure du projet

```
src/
├── api/    Client TheTVDB v4 : connexion, recherche, épisodes, configuration, erreurs
├── data/   Modèle : fichiers à traiter, paramètres de lecture des numéros
└── ui/     Interface Swing : fenêtre principale, tableau, recherche de série, thème
lib/        Bibliothèques (Apache HttpClient, json-simple, FlatLaf)
```

## Bibliothèques utilisées

| Bibliothèque | Version | Usage | Licence |
|---|---|---|---|
| [Apache HttpClient](https://hc.apache.org/httpcomponents-client-4.5.x/) (avec HttpCore et Commons Logging) | 4.5.3 | appels HTTP à l'API TheTVDB | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| [json-simple](https://github.com/fangyidong/json-simple) | 1.1.1 | lecture des réponses JSON | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| [FlatLaf](https://www.formdev.com/flatlaf/) | 3.5.4 | thème de l'interface | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |

Ces bibliothèques, fournies dans `lib/`, restent soumises à leur propre licence.

## Auteurs

- [NBSamael](https://github.com/NBSamael)
- Francis Bellanger
- Elise

## Données

Les informations sur les séries et les épisodes sont fournies par [TheTVDB](https://thetvdb.com). Leur utilisation est soumise aux [conditions de l'API TheTVDB](https://thetvdb.com/api-information).

## Licence

GestionSeries est distribué sous licence [PolyForm Noncommercial 1.0.0](LICENSE.md).

Vous pouvez utiliser, modifier et redistribuer le logiciel pour tout **usage non commercial** : usage personnel, associatif, éducatif ou de recherche. **Toute utilisation commerciale est interdite** sans l'accord des auteurs.

Le texte de la licence (en anglais) fait foi ; ce résumé n'a qu'une valeur indicative.
