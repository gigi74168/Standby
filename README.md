# StandByMe

Une application Android qui transforme ton téléphone posé à l'horizontale en écran de veille,
dans l'esprit du mode StandBy de l'iPhone : grande horloge, lecteur de musique, mode nuit.

> **Version 1.6.1 reconstruite.** Le code source d'origine a été perdu (panne de disque).
> Ce projet a été réécrit proprement à partir de l'APK release 1.6.1 décompilé
> (comportement, textes, couleurs, polices et icône repris à l'identique ;
> noms de classes et organisation des fichiers recréés, car R8 les avait effacés).

## Télécharger

L'APK prêt à installer est dans le dépôt : [`release/StandByMe-1.7.apk`](release/StandByMe-1.7.apk).

Il est signé avec une **clé de test** (pas la clé d'origine, perdue avec le disque) :
il faut désinstaller l'ancienne version avant de l'installer.

## Fonctionnalités

- **Deux pages** qu'on fait défiler horizontalement (ou avec les flèches sur les bords) :
  - **Widgets** : lecteur « En cours de lecture » pour n'importe quelle appli (Spotify, YouTube Music…),
    avec pochette, halo coloré, barre de progression ondulée, volume, choix de la sortie audio.
    Glisser vers le bas pour voir la **file d'attente**.
  - **Horloge** : 4 styles au choix par **appui long** — Numérique, Analogique, Solaire, Onde musicale.
    Une pastille montre le titre en cours quand de la musique joue.
- **Mode nuit** (bouton en bas à droite) : filtre rouge pour ne pas s'éblouir la nuit.
- **Réglages** (engrenage en haut à gauche) :
  - Design Apple (fond flouté animé, grosses commandes arrondies)
  - Intensité des vagues de la barre de progression
  - Halo lumineux : activé, couleur de la pochette, puissance
- Écran toujours allumé, plein écran, couleurs Material You du fond d'écran.

L'accès aux notifications est demandé une fois : il sert uniquement à lire et contrôler
la session multimédia en cours.

## Structure

```
app/src/main/java/com/standbyme/app/
├── MainActivity.kt                  Plein écran + écran allumé
├── data/SettingsRepository.kt       Réglages (SharedPreferences "standbyme_settings")
├── media/
│   ├── MediaSessionRepository.kt    Suivi de la session multimédia + commandes
│   ├── PlaybackUiState.kt           État de lecture
│   └── StandbyNotificationListenerService.kt
└── ui/
    ├── StandbyApp.kt                Écran racine (pages, boutons, mode nuit, permission)
    ├── StandbyViewModel.kt
    ├── clock/                       Cadrans + sélecteur de style
    ├── player/                      Lecteur standard, design Apple, file d'attente
    ├── settings/SettingsPanel.kt
    ├── components/                  Curseur ondulé, égaliseur, boutons
    └── theme/                       Couleurs dynamiques, polices
```

Les réglages gardent **les mêmes clés** qu'en 1.6.1 : si tu installes cette version par-dessus
l'ancienne (même signature), tes réglages sont conservés.

## Compiler

Prérequis : JDK 17+ et le SDK Android (plateforme 35). Crée `local.properties` :

```
sdk.dir=/chemin/vers/Android/sdk
```

Puis :

```bash
./gradlew :app:assembleDebug
```

L'APK se trouve dans `app/build/outputs/apk/debug/`.

### Version release signée

Crée à la racine un fichier `keystore.properties` (**ignoré par git**, ne le commite jamais) :

```
storeFile=/chemin/vers/ma-cle.jks
storePassword=...
keyAlias=...
keyPassword=...
```

```bash
./gradlew :app:assembleRelease
```

Sans la clé d'origine, Android refusera d'installer la nouvelle version par-dessus l'ancienne :
il faudra désinstaller la 1.6.1 d'abord.

## Licences des polices

Anton, Instrument Sans et Nunito sont distribuées sous licence SIL Open Font License 1.1.
