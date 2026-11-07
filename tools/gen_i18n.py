#!/usr/bin/env python3
"""Builds the translated home pages (docs/de/, docs/es/, ...) from docs/index.html.

Every page is the full main page in another language, linked to the others with hreflang,
so each one can rank in its own language. Run after changing docs/index.html:
    python3 tools/gen_i18n.py
"""
import json
import re
from pathlib import Path
from datetime import date

TODAY = date.today().isoformat()  # lastmod: the day the pages were last generated

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
SITE = "https://memehammad.github.io/clash-timers/"
LANGS = ["de", "es", "fr", "pt"]
NAMES = {"en": "English", "de": "Deutsch", "es": "Español", "fr": "Français", "pt": "Português"}
HTML_LANG = {"de": "de", "es": "es", "fr": "fr", "pt": "pt-BR"}

# English snippet -> translations (de, es, fr, pt). Applied longest first, everywhere in the page
# (text, attributes, JSON-LD and the demo scripts).
T = {
    "Clash of Clans Widget for Android – Clash Timers": (
        "Clash of Clans Widget für Android – Clash Timers", "Widget de Clash of Clans para Android – Clash Timers",
        "Widget Clash of Clans pour Android – Clash Timers", "Widget de Clash of Clans para Android – Clash Timers"),
    "Clash Timers is a free Clash of Clans widget for Android: free builders, upgrade timers, lab research, pets and heroes on your home screen, with notifications. No login, no ads.": (
        "Clash Timers ist ein kostenloses Clash of Clans Widget für Android: freie Bauarbeiter, Upgrade-Timer, Forschung, Begleiter und Helden auf dem Startbildschirm, mit Benachrichtigungen. Ohne Login, ohne Werbung.",
        "Clash Timers es un widget gratuito de Clash of Clans para Android: constructores libres, temporizadores de mejoras, investigación, animales y héroes en tu pantalla de inicio, con notificaciones. Sin registro ni anuncios.",
        "Clash Timers est un widget Clash of Clans gratuit pour Android : ouvriers libres, minuteurs d'amélioration, recherches, familiers et héros sur ton écran d'accueil, avec notifications. Sans compte, sans pub.",
        "Clash Timers é um widget gratuito de Clash of Clans para Android: construtores livres, timers de melhorias, pesquisas, animais e heróis na tela inicial, com notificações. Sem login, sem anúncios."),
    "Free builders, upgrade timers, research and heroes for Clash of Clans on your Android home screen. Free and open source.": (
        "Freie Bauarbeiter, Upgrade-Timer, Forschung und Helden von Clash of Clans auf deinem Android-Startbildschirm. Kostenlos und Open Source.",
        "Constructores libres, temporizadores de mejoras, investigación y héroes de Clash of Clans en tu pantalla de inicio de Android. Gratis y de código abierto.",
        "Ouvriers libres, minuteurs d'amélioration, recherches et héros de Clash of Clans sur ton écran d'accueil Android. Gratuit et open source.",
        "Construtores livres, timers de melhorias, pesquisas e heróis do Clash of Clans na tela inicial do Android. Grátis e de código aberto."),
    "The Clash of Clans widget for Android": (
        "Das Clash of Clans Widget für Android", "El widget de Clash of Clans para Android",
        "Le widget Clash of Clans pour Android", "O widget de Clash of Clans para Android"),
    "See your free builders, upgrade timers, research and heroes on your home screen, and get a ping the moment a builder is free.": (
        "Sieh freie Bauarbeiter, Upgrade-Timer, Forschung und Helden auf dem Startbildschirm, und bekomm sofort Bescheid, wenn ein Bauarbeiter frei ist.",
        "Mira tus constructores libres, temporizadores, investigaciones y héroes en la pantalla de inicio, y recibe un aviso en cuanto un constructor quede libre.",
        "Vois tes ouvriers libres, minuteurs, recherches et héros sur ton écran d'accueil, et sois prévenu dès qu'un ouvrier est libre.",
        "Veja seus construtores livres, timers, pesquisas e heróis na tela inicial, e receba um aviso assim que um construtor ficar livre."),
    "Download the app": ("App herunterladen", "Descargar la app", "Télécharger l'appli", "Baixar o app"),
    "Get it with Obtainium": ("Mit Obtainium holen", "Obtener con Obtainium", "Obtenir avec Obtainium", "Obter com Obtainium"),
    'aria-label="Page colours"': ('aria-label="Seitenfarben"', 'aria-label="Colores de la página"', 'aria-label="Couleurs de la page"', 'aria-label="Cores da página"'),
    ">Colours<": (">Farben<", ">Colores<", ">Couleurs<", ">Cores<"),
    'aria-label="Teal"': ('aria-label="Petrol"', 'aria-label="Turquesa"', 'aria-label="Bleu canard"', 'aria-label="Azul-petróleo"'),
    'aria-label="Rose"': ('aria-label="Rosé"', 'aria-label="Rosa"', 'aria-label="Rose"', 'aria-label="Rosa"'),
    "Try the widget": ("Probier das Widget aus", "Prueba el widget", "Essaie le widget", "Experimente o widget"),
    "Use a Builder Potion": ("Bauarbeitertrank nutzen", "Usar Pócima del constructor", "Utiliser une Potion de l'ouvrier", "Usar Poção do Construtor"),
    "Start an upgrade": ("Upgrade starten", "Empezar una mejora", "Lancer une amélioration", "Iniciar uma melhoria"),
    "A live preview with demo timers. Try the buttons.": (
        "Eine Live-Vorschau mit Demo-Timern. Probier die Knöpfe aus.", "Una vista previa en vivo con temporizadores de prueba. Prueba los botones.",
        "Un aperçu en direct avec des minuteurs de démo. Essaie les boutons.", "Uma prévia ao vivo com timers de demonstração. Experimente os botões."),
    "Never leave a builder idle again": (
        "Nie wieder untätige Bauarbeiter", "No vuelvas a dejar un constructor sin trabajo", "Ne laisse plus jamais un ouvrier inactif", "Nunca mais deixe um construtor parado"),
    "Free, open source and private. No login, no ads. It reads the village export the game already gives you.": (
        "Kostenlos, Open Source und privat. Ohne Login, ohne Werbung. Es liest den Dorf-Export, den das Spiel dir sowieso gibt.",
        "Gratis, de código abierto y privado. Sin registro ni anuncios. Lee la exportación de aldea que el juego ya te da.",
        "Gratuit, open source et privé. Sans compte, sans pub. Il lit l'export de village que le jeu te donne déjà.",
        "Grátis, de código aberto e privado. Sem login, sem anúncios. Ele lê a exportação da vila que o jogo já oferece."),
    "Free builders, live": ("Freie Bauarbeiter, live", "Constructores libres, en vivo", "Ouvriers libres, en direct", "Construtores livres, ao vivo"),
    "How many builders are idle, or a countdown to the next one.": (
        "Wie viele Bauarbeiter frei sind, oder ein Countdown bis zum nächsten.", "Cuántos constructores están libres o una cuenta atrás hasta el siguiente.",
        "Combien d'ouvriers sont libres, ou un compte à rebours jusqu'au prochain.", "Quantos construtores estão livres ou uma contagem até o próximo."),
    "Every upgrade timer": ("Jeder Upgrade-Timer", "Todos los temporizadores", "Tous les minuteurs", "Todos os timers"),
    "Buildings, heroes, Gear Ups, Crafting Station, lab, pets, Builder Base, Apprentice and Clock Tower.": (
        "Gebäude, Helden, Weiterentwicklungen, Werkstatt, Labor, Begleiter, Bauarbeiterbasis, Lehrling und Uhrenturm.",
        "Edificios, héroes, perfeccionamientos, taller, laboratorio, animales, base del constructor, aprendiz y Torre del reloj.",
        "Bâtiments, héros, équipements, atelier, laboratoire, familiers, base des ouvriers, apprenti et tour de l'horloge.",
        "Construções, heróis, aprimoramentos, oficina, laboratório, animais, base do construtor, aprendiz e Torre de Relógios."),
    "A ping when it’s done": ("Bescheid, sobald es fertig ist", "Un aviso al terminar", "Une alerte quand c'est fini", "Um aviso quando terminar"),
    "Notifications the moment something finishes, and a nudge if a builder sits idle.": (
        "Benachrichtigungen, sobald etwas fertig ist, und eine Erinnerung, wenn ein Bauarbeiter untätig ist.",
        "Notificaciones en cuanto algo termina y un recordatorio si un constructor está sin trabajo.",
        "Des notifications dès qu'une chose se termine, et un rappel si un ouvrier reste inactif.",
        "Notificações assim que algo termina e um lembrete se um construtor ficar parado."),
    "Heroes ready to fight": ("Helden bereit zum Kampf", "Héroes listos para luchar", "Héros prêts au combat", "Heróis prontos para lutar"),
    "Which heroes can attack, and when an upgrading one is back.": (
        "Welche Helden angreifen können und wann ein verbesserter zurück ist.", "Qué héroes pueden atacar y cuándo vuelve uno que se está mejorando.",
        "Quels héros peuvent attaquer, et quand un héros en amélioration revient.", "Quais heróis podem atacar e quando um em melhoria volta."),
    "Plan your potions": ("Plane deine Tränke", "Planifica tus pócimas", "Planifie tes potions", "Planeje suas poções"),
    "See what a Builder Potion, Research Potion or Builder Bite would finish before you use it.": (
        "Sieh vorher, was ein Bauarbeitertrank, Forschungstrank oder Builder Bite fertig machen würde.",
        "Mira qué terminaría una Pócima del constructor, una Pócima de investigación o un Builder Bite antes de usarlos.",
        "Vois ce qu'une Potion de l'ouvrier, une Potion de recherche ou un Builder Bite terminerait avant de l'utiliser.",
        "Veja o que uma Poção do Construtor, Poção de pesquisa ou Builder Bite terminaria antes de usar."),
    "Private by design": ("Privat von Grund auf", "Privado por diseño", "Privé par conception", "Privado desde o início"),
    "No account and no server. Your village data stays on your phone.": (
        "Kein Konto und kein Server. Deine Dorfdaten bleiben auf deinem Handy.", "Sin cuenta y sin servidor. Los datos de tu aldea se quedan en tu móvil.",
        "Pas de compte ni de serveur. Les données de ton village restent sur ton téléphone.", "Sem conta e sem servidor. Os dados da sua vila ficam no seu celular."),
    "A widget for every spot": ("Ein Widget für jeden Platz", "Un widget para cada rincón", "Un widget pour chaque place", "Um widget para cada espaço"),
    "From a 1×1 builder badge to a full list. It follows your wallpaper colours, in light and dark mode.": (
        "Vom 1×1-Bauarbeiter-Abzeichen bis zur ganzen Liste. Es passt sich deinen Hintergrundfarben an, hell und dunkel.",
        "Desde una insignia de 1×1 hasta una lista completa. Sigue los colores de tu fondo, en modo claro y oscuro.",
        "D'un badge 1×1 à une liste complète. Il suit les couleurs de ton fond d'écran, en mode clair et sombre.",
        "De um selo 1×1 a uma lista completa. Segue as cores do seu papel de parede, no modo claro e escuro."),
    "Live widget previews": ("Live-Widget-Vorschauen", "Vistas previas en vivo", "Aperçus en direct", "Prévias ao vivo"),
    "Clash of Clans widget, 4×2: free builders, lab, pets and heroes": (
        "Clash of Clans Widget, 4×2: freie Bauarbeiter, Labor, Begleiter und Helden", "Widget de Clash of Clans, 4×2: constructores libres, laboratorio, animales y héroes",
        "Widget Clash of Clans, 4×2 : ouvriers libres, labo, familiers et héros", "Widget de Clash of Clans, 4×2: construtores livres, laboratório, animais e heróis"),
    "Clash of Clans builder widget, 4×1": ("Clash of Clans Bauarbeiter-Widget, 4×1", "Widget de constructores de Clash of Clans, 4×1",
                                          "Widget des ouvriers Clash of Clans, 4×1", "Widget de construtores do Clash of Clans, 4×1"),
    "Free builder badge, 1×1": ("Abzeichen für freie Bauarbeiter, 1×1", "Insignia de constructores libres, 1×1", "Badge des ouvriers libres, 1×1", "Selo de construtores livres, 1×1"),
    "Heroes 2/3 ready · AQ back in 1d 9h": ("Helden 2/3 bereit · BK zurück in 1T 9h", "Héroes 2/3 listos · RA vuelve en 1d 9h",
                                            "Héros 2/3 prêts · RA revient dans 1j 9h", "Heróis 2/3 prontos · RA volta em 1d 9h"),
    "<span>Lab</span>": ("<span>Labor</span>", "<span>Lab.</span>", "<span>Labo</span>", "<span>Lab.</span>"),
    "<span>Star Lab</span>": ("<span>Sternlabor</span>", "<span>Lab. astral</span>", "<span>Labo astral</span>", "<span>Lab. Estelar</span>"),
    "<span>Pets</span>": ("<span>Begleiter</span>", "<span>Animales</span>", "<span>Familiers</span>", "<span>Animais</span>"),
    "<span>BB</span>": ("<span>BB</span>", "<span>BC</span>", "<span>BO</span>", "<span>BC</span>"),
    "<b>Idle</b>": ("<b>Frei</b>", "<b>Libre</b>", "<b>Libre</b>", "<b>Livre</b>"),
    "Set up in a minute": ("In einer Minute eingerichtet", "Listo en un minuto", "Prêt en une minute", "Pronto em um minuto"),
    "Android 12 or newer.": ("Android 12 oder neuer.", "Android 12 o posterior.", "Android 12 ou plus récent.", "Android 12 ou mais recente."),
    "Install Clash Timers": ("Clash Timers installieren", "Instala Clash Timers", "Installe Clash Timers", "Instale o Clash Timers"),
    ">Download the APK<": (">Lade die APK herunter<", ">Descarga el APK<", ">Télécharge l'APK<", ">Baixe o APK<"),
    ", or with Obtainium to get updates automatically.": (", oder hol sie mit Obtainium für automatische Updates.", ", o con Obtainium para recibir actualizaciones automáticas.",
                                                          ", ou avec Obtainium pour les mises à jour automatiques.", ", ou com o Obtainium para receber atualizações automáticas."),
    "Copy your village": ("Kopiere dein Dorf", "Copia tu aldea", "Copie ton village", "Copie sua vila"),
    "In Clash of Clans: Settings, More Settings, Data Export, Copy.": (
        "In Clash of Clans: Einstellungen, Mehr Einst., Datenexport, Kopieren.", "En Clash of Clans: Ajustes, Más ajustes, Exportar datos, Copiar.",
        "Dans Clash of Clans : Paramètres, Paramètres supplémentaires, Export des données, Copier.", "No Clash of Clans: Configurações, Mais configurações, Exportar dados, Copiar."),
    "Open the app": ("Öffne die App", "Abre la app", "Ouvre l'appli", "Abra o app"),
    "The export is picked up on its own.": ("Der Export wird automatisch übernommen.", "La exportación se recoge sola.", "L'export est récupéré tout seul.", "A exportação é captada sozinha."),
    "Add the widget": ("Füge das Widget hinzu", "Añade el widget", "Ajoute le widget", "Adicione o widget"),
    "Long-press your home screen, open Widgets and pick Clash Timers.": (
        "Halte den Startbildschirm gedrückt, öffne Widgets und wähle Clash Timers.", "Mantén pulsada la pantalla de inicio, abre Widgets y elige Clash Timers.",
        "Appuie longuement sur l'écran d'accueil, ouvre Widgets et choisis Clash Timers.", "Toque e segure a tela inicial, abra Widgets e escolha Clash Timers."),
    ">Questions<": (">Fragen<", ">Preguntas<", ">Questions<", ">Perguntas<"),
    "Is there a Clash of Clans widget for Android?": ("Gibt es ein Clash of Clans Widget für Android?", "¿Hay un widget de Clash of Clans para Android?",
                                                      "Existe-t-il un widget Clash of Clans pour Android ?", "Existe um widget de Clash of Clans para Android?"),
    "Clash of Clans has no widget of its own. Clash Timers adds one: free builders, upgrade timers, research and heroes on your home screen.": (
        "Clash of Clans hat kein eigenes Widget. Clash Timers bringt eins: freie Bauarbeiter, Upgrade-Timer, Forschung und Helden auf dem Startbildschirm.",
        "Clash of Clans no tiene widget propio. Clash Timers añade uno: constructores libres, temporizadores, investigación y héroes en tu pantalla de inicio.",
        "Clash of Clans n'a pas de widget. Clash Timers en ajoute un : ouvriers libres, minuteurs, recherches et héros sur ton écran d'accueil.",
        "O Clash of Clans não tem widget próprio. O Clash Timers adiciona um: construtores livres, timers, pesquisas e heróis na tela inicial."),
    "Do I need to log in?": ("Muss ich mich anmelden?", "¿Tengo que iniciar sesión?", "Dois-je me connecter ?", "Preciso fazer login?"),
    "No. It uses the game’s own Data Export. No account or Supercell ID is needed.": (
        "Nein. Es nutzt den Datenexport des Spiels. Kein Konto und keine Supercell-ID nötig.", "No. Usa la exportación de datos del propio juego. No necesitas cuenta ni Supercell ID.",
        "Non. Il utilise l'export de données du jeu. Ni compte ni Supercell ID nécessaires.", "Não. Ele usa a exportação de dados do próprio jogo. Não precisa de conta nem Supercell ID."),
    "No. It uses the game’s own": ("Nein. Es nutzt den", "No. Usa la", "Non. Il utilise l'", "Não. Ele usa a"),
    ">Data Export<": (">Datenexport des Spiels<", ">exportación de datos del juego<", ">export de données du jeu<", ">exportação de dados do jogo<"),
    ". No account or Supercell ID is needed.": (". Kein Konto und keine Supercell-ID nötig.", ". No necesitas cuenta ni Supercell ID.",
                                               ". Ni compte ni Supercell ID nécessaires.", ". Não precisa de conta nem Supercell ID."),
    "Can I get banned?": ("Kann ich gebannt werden?", "¿Me pueden banear?", "Puis-je être banni ?", "Posso ser banido?"),
    "Clash Timers never touches the game. It only reads the export the game gives you, and nothing is sent anywhere.": (
        "Clash Timers greift nie ins Spiel ein. Es liest nur den Export, den dir das Spiel gibt, und sendet nichts irgendwohin.",
        "Clash Timers nunca toca el juego. Solo lee la exportación que te da el juego y no envía nada a ninguna parte.",
        "Clash Timers ne touche jamais au jeu. Il lit seulement l'export que le jeu te donne, et rien n'est envoyé nulle part.",
        "O Clash Timers nunca mexe no jogo. Ele só lê a exportação que o jogo oferece, e nada é enviado para lugar nenhum."),
    "Does it update by itself?": ("Aktualisiert es sich von selbst?", "¿Se actualiza solo?", "Se met-il à jour tout seul ?", "Ele se atualiza sozinho?"),
    "Timers count down live. After starting new upgrades, copy a fresh export, or turn on auto-import with Shizuku and it happens on its own.": (
        "Die Timer laufen live. Nach neuen Upgrades kopierst du einen frischen Export, oder du schaltest den Auto-Import mit Shizuku ein und es passiert von selbst.",
        "Los temporizadores corren en vivo. Tras empezar mejoras, copia una exportación nueva o activa la importación automática con Shizuku y se hace sola.",
        "Les minuteurs défilent en direct. Après de nouvelles améliorations, copie un nouvel export, ou active l'import automatique avec Shizuku et tout se fait seul.",
        "Os timers correm ao vivo. Depois de iniciar melhorias, copie uma nova exportação ou ative a importação automática com o Shizuku e tudo acontece sozinho."),
    "Clash of Clans notifications not working?": (
        "Clash of Clans Benachrichtigungen kommen nicht?", "¿No te llegan las notificaciones de Clash of Clans?",
        "Les notifications de Clash of Clans ne marchent pas ?", "Notificações do Clash of Clans não funcionam?"),
    "The game’s alerts come from Supercell’s servers and are easily blocked by battery saving or notification settings. Clash Timers schedules its alerts on your phone instead, so they arrive on time.": (
        "Die Benachrichtigungen des Spiels kommen von Supercells Servern und werden leicht durch Energiesparen oder Benachrichtigungseinstellungen blockiert. Clash Timers plant seine Benachrichtigungen direkt auf deinem Handy, deshalb kommen sie pünktlich.",
        "Los avisos del juego vienen de los servidores de Supercell y el ahorro de batería o los ajustes de notificaciones los bloquean fácilmente. Clash Timers programa sus avisos en tu móvil, así que llegan a tiempo.",
        "Les alertes du jeu viennent des serveurs de Supercell et sont facilement bloquées par l'économie d'énergie ou les réglages de notifications. Clash Timers programme ses alertes sur ton téléphone, elles arrivent donc à l'heure.",
        "Os avisos do jogo vêm dos servidores da Supercell e são facilmente bloqueados pela economia de bateria ou pelas configurações de notificação. O Clash Timers agenda os avisos no seu celular, então eles chegam na hora."),
    ">How to fix the game’s notifications<": (">So reparierst du die Benachrichtigungen des Spiels (EN)<", ">Cómo arreglar las notificaciones del juego (EN)<",
                                              ">Comment réparer les notifications du jeu (EN)<", ">Como consertar as notificações do jogo (EN)<"),
    "Is there an iPhone version?": ("Gibt es eine iPhone-Version?", "¿Hay versión para iPhone?", "Existe-t-il une version iPhone ?", "Existe versão para iPhone?"),
    "No, Clash Timers is for Android 12 and newer.": ("Nein, Clash Timers ist für Android 12 und neuer.", "No, Clash Timers es para Android 12 y posteriores.",
                                                      "Non, Clash Timers est pour Android 12 et plus récent.", "Não, o Clash Timers é para Android 12 ou mais recente."),
    "Clash Timers is free and open source under the MIT license.": (
        "Clash Timers ist kostenlos und Open Source unter der MIT-Lizenz.", "Clash Timers es gratis y de código abierto con licencia MIT.",
        "Clash Timers est gratuit et open source sous licence MIT.", "O Clash Timers é grátis e de código aberto sob a licença MIT."),
    "Source code on GitHub": ("Quellcode auf GitHub", "Código fuente en GitHub", "Code source sur GitHub", "Código-fonte no GitHub"),
    ">Privacy policy<": (">Datenschutz<", ">Privacidad<", ">Confidentialité<", ">Privacidade<"),
    ">Guides<": (">Anleitungen (EN)<", ">Guías (EN)<", ">Guides (EN)<", ">Guias (EN)<"),
    "This material is unofficial and is not endorsed by Supercell. For more information see Supercell’s": (
        "Dieses Material ist inoffiziell und wird nicht von Supercell unterstützt. Weitere Informationen in Supercells",
        "Este material no es oficial y no está respaldado por Supercell. Para más información, consulta la",
        "Ce contenu n'est pas officiel et n'est pas approuvé par Supercell. Pour plus d'informations, voir la",
        "Este material não é oficial e não é endossado pela Supercell. Para mais informações, veja a"),
    ">Fan Content Policy<": (">Fan-Content-Richtlinie<", ">Política de contenido de fans de Supercell<", ">politique de contenu des fans de Supercell<", ">Política de Conteúdo de Fãs da Supercell<"),
    "Your download has started": ("Dein Download hat begonnen", "Tu descarga ha empezado", "Ton téléchargement a commencé", "Seu download começou"),
    "Installing takes about a minute.": ("Die Installation dauert etwa eine Minute.", "La instalación tarda alrededor de un minuto.",
                                         "L'installation prend environ une minute.", "A instalação leva cerca de um minuto."),
    "Open the file": ("Öffne die Datei", "Abre el archivo", "Ouvre le fichier", "Abra o arquivo"),
    "Tap the download notification, or find ClashTimers.apk in Files &gt; Downloads.": (
        "Tippe auf die Download-Benachrichtigung oder öffne ClashTimers.apk unter Dateien &gt; Downloads.",
        "Toca la notificación de descarga o busca ClashTimers.apk en Archivos &gt; Descargas.",
        "Touche la notification de téléchargement, ou trouve ClashTimers.apk dans Fichiers &gt; Téléchargements.",
        "Toque na notificação do download ou encontre ClashTimers.apk em Arquivos &gt; Downloads."),
    "Allow the install": ("Installation erlauben", "Permite la instalación", "Autorise l'installation", "Permita a instalação"),
    "If Android asks, tap Settings, turn on \"Allow from this source\", then go back.": (
        "Wenn Android fragt, tippe auf Einstellungen, schalte „Dieser Quelle vertrauen“ ein und geh zurück.",
        "Si Android lo pide, toca Ajustes, activa \"Permitir de esta fuente\" y vuelve.",
        "Si Android le demande, touche Paramètres, active « Autoriser cette source », puis reviens.",
        "Se o Android pedir, toque em Configurações, ative \"Permitir desta fonte\" e volte."),
    "Tap Install": ("Tippe auf Installieren", "Toca Instalar", "Touche Installer", "Toque em Instalar"),
    "If Play Protect offers to scan the app, go ahead, it's open source. If it warns about an unknown app, tap More details &gt; Install anyway.": (
        "Wenn Play Protect die App prüfen möchte, nur zu, sie ist Open Source. Warnt es vor einer unbekannten App, tippe auf Weitere Details &gt; Trotzdem installieren.",
        "Si Play Protect ofrece analizar la app, adelante, es de código abierto. Si avisa de una app desconocida, toca Más detalles &gt; Instalar de todas formas.",
        "Si Play Protect propose d'analyser l'appli, vas-y, elle est open source. S'il signale une appli inconnue, touche Plus de détails &gt; Installer quand même.",
        "Se o Play Protect oferecer verificar o app, pode deixar, ele é de código aberto. Se avisar sobre um app desconhecido, toque em Mais detalhes &gt; Instalar mesmo assim."),
    "Copy your export": ("Kopiere deinen Export", "Copia tu exportación", "Copie ton export", "Copie sua exportação"),
    "In Clash: Settings &gt; More Settings &gt; Data Export &gt; Copy. Then open Clash Timers.": (
        "In Clash: Einstellungen &gt; Mehr Einst. &gt; Datenexport &gt; Kopieren. Dann Clash Timers öffnen.",
        "En Clash: Ajustes &gt; Más ajustes &gt; Exportar datos &gt; Copiar. Después abre Clash Timers.",
        "Dans Clash : Paramètres &gt; Paramètres supplémentaires &gt; Export des données &gt; Copier. Puis ouvre Clash Timers.",
        "No Clash: Configurações &gt; Mais configurações &gt; Exportar dados &gt; Copiar. Depois abra o Clash Timers."),
    "Want automatic updates? Install with": ("Automatische Updates? Installiere mit", "¿Quieres actualizaciones automáticas? Instala con",
                                             "Tu veux des mises à jour automatiques ? Installe avec", "Quer atualizações automáticas? Instale com"),
    "instead. Didn't start?": ("statt dessen. Nicht gestartet?", "en su lugar. ¿No empezó?", "à la place. Ça n'a pas démarré ?", "em vez disso. Não começou?"),
    "Download again": ("Nochmal herunterladen", "Descargar de nuevo", "Télécharger à nouveau", "Baixar de novo"),
    "Got it": ("Verstanden", "Entendido", "Compris", "Entendi"),
    # Demo and gallery scripts.
    "'All builders busy'": ("'Alle Bauarbeiter beschäftigt'", "'Todos los constructores ocupados'", "'Tous les ouvriers occupés'", "'Todos os construtores ocupados'"),
    ">All builders busy<": (">Alle Bauarbeiter beschäftigt<", ">Todos los constructores ocupados<", ">Tous les ouvriers occupés<", ">Todos os construtores ocupados<"),
    "'1 builder free'": ("'1 Bauarbeiter frei'", "'1 constructor libre'", "'1 ouvrier libre'", "'1 construtor livre'"),
    ">1 builder free<": (">1 Bauarbeiter frei<", ">1 constructor libre<", ">1 ouvrier libre<", ">1 construtor livre<"),
    "' builders free'": ("' Bauarbeiter frei'", "' constructores libres'", "' ouvriers libres'", "' construtores livres'"),
    "'Next free in 0:'": ("'Nächster frei in 0:'", "'Próximo libre en 0:'", "'Prochain libre dans 0:'", "'Próximo livre em 0:'"),
    "'Next free in '": ("'Nächster frei in '", "'Próximo libre en '", "'Prochain libre dans '", "'Próximo livre em '"),
    ">Next free in 0:20<": (">Nächster frei in 0:20<", ">Próximo libre en 0:20<", ">Prochain libre dans 0:20<", ">Próximo livre em 0:20<"),
    "'Next done in '": ("'Nächstes fertig in '", "'Lo próximo termina en '", "'Prochaine fin dans '", "'Próximo termina em '"),
    "' \\u00b7 potion '": ("' \\u00b7 Trank '", "' \\u00b7 pócima '", "' \\u00b7 potion '", "' \\u00b7 poção '"),
    "'Time to build something'": ("'Zeit, etwas zu bauen'", "'Hora de construir algo'", "'Il est temps de construire'", "'Hora de construir algo'"),
    ">Free builder<": (">Freier Bauarbeiter<", ">Constructor libre<", ">Ouvrier libre<", ">Construtor livre<"),
    ">Done<": (">Fertig<", ">Listo<", ">Terminé<", ">Pronto<"),
    "'Level '": ("'Level '", "'Nivel '", "'Niveau '", "'Nível '"),
    "\">Level ' + (b.lvl + 1) + ' reached<": ("\">Level ' + (b.lvl + 1) + ' erreicht<", "\">Nivel ' + (b.lvl + 1) + ' alcanzado<",
                                             "\">Niveau ' + (b.lvl + 1) + ' atteint<", "\">Nível ' + (b.lvl + 1) + ' alcançado<"),
    "' \\u00b7 boosted'": ("' \\u00b7 beschleunigt'", "' \\u00b7 acelerado'", "' \\u00b7 accéléré'", "' \\u00b7 acelerado'"),
    "' of 6 working'": ("' von 6 arbeiten'", "' de 6 trabajando'", "' sur 6 au travail'", "' de 6 trabalhando'"),
    ">5 of 6 working<": (">5 von 6 arbeiten<", ">5 de 6 trabajando<", ">5 sur 6 au travail<", ">5 de 6 trabalhando<"),
}

# Demo building names, in the game's own translations.
DEMO_NAMES = ["Archer Tower", "Inferno Tower", "Barbarian King", "Wizard Tower", "Elixir Storage", "Archer Queen", "Cannon", "X-Bow", "Grand Warden"]


def game_names(lang):
    data = json.load(open(ROOT / "app/src/main/assets/coc_data.json"))
    names = json.load(open(ROOT / "app/src/main/assets/coc_names.json")).get(lang, {})
    by_en = {v["n"]: names.get(k) for k, v in data.items() if names.get(k)}
    return {n: by_en.get(n, n) for n in DEMO_NAMES}


def alternates():
    out = [f'<link rel="alternate" hreflang="en" href="{SITE}">']
    out += [f'<link rel="alternate" hreflang="{l}" href="{SITE}{l}/">' for l in LANGS]
    out.append(f'<link rel="alternate" hreflang="x-default" href="{SITE}">')
    return "\n".join(out)


def switcher(current):
    links = []
    for code in ["en"] + LANGS:
        href = SITE if code == "en" else f"{SITE}{code}/"
        if code == current:
            links.append(f"<b>{NAMES[code]}</b>")
        else:
            links.append(f'<a href="{href}" hreflang="{code}" lang="{code}">{NAMES[code]}</a>')
    return '<p class="langs">' + " · ".join(links) + "</p>"


def prepare_base(html):
    """Puts the shared hreflang links and the language switcher into the English page."""
    html = re.sub(r'(<link rel="alternate" hreflang="[^"]+" href="[^"]+">\n)+', "", html)
    html = html.replace('<link rel="canonical" href="https://memehammad.github.io/clash-timers/">',
                        '<link rel="canonical" href="https://memehammad.github.io/clash-timers/">\n' + alternates(), 1)
    html = re.sub(r'<p class="langs">.*?</p>\n?', "", html)
    html = html.replace(" · <a href=\"de/\">Deutsch</a>", "")
    html = html.replace("  </footer>", "    " + switcher("en") + "\n  </footer>", 1)
    if ".langs" not in html:
        html = html.replace("  footer p {", "  .langs a, .langs b { white-space: nowrap; }\n  footer p {", 1)
    return html


def translate(html, lang):
    i = LANGS.index(lang)
    out = html
    for en in sorted(T, key=len, reverse=True):
        if en not in out:
            raise SystemExit(f"missing in index.html: {en[:70]!r}")
        out = out.replace(en, T[en][i])
    for en, loc in game_names(lang).items():
        out = out.replace(f"'{en}'", "'" + loc.replace("'", "\\'") + "'")
    out = out.replace('<html lang="en"', f'<html lang="{HTML_LANG[lang]}"', 1)
    out = out.replace(f'<link rel="canonical" href="{SITE}">', f'<link rel="canonical" href="{SITE}{lang}/">', 1)
    out = out.replace(f'<meta property="og:url" content="{SITE}">', f'<meta property="og:url" content="{SITE}{lang}/">', 1)
    out = out.replace(f'"url": "{SITE}"', f'"url": "{SITE}{lang}/"')
    # Relative links point one folder up.
    out = re.sub(r'(src|href)="(img/|privacy\.html|guides/)', r'\1="../\2', out)
    out = out.replace(switcher("en"), switcher(lang))
    return out


def main():
    p = DOCS / "index.html"
    base = prepare_base(p.read_text())
    p.write_text(base)
    for lang in LANGS:
        d = DOCS / lang
        d.mkdir(exist_ok=True)
        (d / "index.html").write_text(translate(base, lang))
    # Sitemap: every language version lists all the others.
    alt = "".join(f'<xhtml:link rel="alternate" hreflang="{c}" href="{SITE}{"" if c == "en" else c + "/"}"/>' for c in ["en"] + LANGS)
    alt += f'<xhtml:link rel="alternate" hreflang="x-default" href="{SITE}"/>'
    sm = (DOCS / "sitemap.xml").read_text()
    sm = re.sub(r"  <url><loc>[^<]*/clash-timers/(?:[a-z]{2}/)?</loc>.*?</url>\n", "", sm)
    homes = "".join(f"  <url><loc>{SITE}{'' if c == 'en' else c + '/'}</loc><lastmod>{TODAY}</lastmod>{alt}</url>\n" for c in ["en"] + LANGS)
    sm = sm.replace('xmlns:xhtml="http://www.w3.org/1999/xhtml">\n', 'xmlns:xhtml="http://www.w3.org/1999/xhtml">\n' + homes, 1)
    (DOCS / "sitemap.xml").write_text(sm)
    print("built", ", ".join(LANGS))


if __name__ == "__main__":
    main()
