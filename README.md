# MESHCHESS

- A chess game that runs over Bluetooth or WiFi Mesh Network.

- For Android Only.

- I want to use Python's logic for the game logic and engine, because it is more easy to me to understand and implement it and i'll make me use less code and less time to develop it unlike if i was to use Kotlin.

- The app should adapt to the phone's language setting.
- The app should be playable in 4 modes :
- Solo Mode :
    - Play against the AI.
- Online Mode :
    - P2P Multiplayer
- Offline Mode :
    - Hotseat
- Mesh Network Mode

# Features
- Possibilité de jouer avec des IA comme:
- ChatGPT
- Claude
- Gemini
- DeepSeek
- Qwen

**Pour jouer avec une IA, il faut etre connectée à internet et posséder une clé API pour l'IA que vous voulez utiliser.**

- Possibilité d'exporter une partie en .json et de l'importer pour la rejouer ou analyser plus tard.
** Possibilite de meme exporter les parties jouées avec une IA et de les rejouer plus tard avec ce meme IA ou avec une autre IA.**

-Partage de parties avec des amis en QR Code.
**La fonctionnalité est directement dans le mode P2P Multiplayer**

# ONLINE MODE
- Utilisation d'un serveur personnel hebergé chez railway.app pour les communications entre les joueurs et les moves.
**Les joueurs doivent avoir une connexion internet pour jouer en mode Online.**

**Le serveur est dispo dans cette emplacement:[/home/luberisse/Bureau/MeshChess/mesh-server]

**Pour eviter des problemes avec le systeme android il faut demander les autorisations des l'ouverture de l'app:
acces au ```bluetooth``` et ```wifi```.

Je veux la possibiilitee de pouvoir importer les parties en ```JSON``` pour y rejouer.