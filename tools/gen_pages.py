#!/usr/bin/env python3
"""Builds the small content pages of the website (guides, German page) from one template.

Run from the repo root: python3 tools/gen_pages.py
"""
import json
from pathlib import Path

SITE = "https://memehammad.github.io/clash-timers/"
APK = "https://github.com/memehammad/clash-timers/releases/latest/download/ClashTimers.apk"
OBTAINIUM = "https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/memehammad/clash-timers"
DOCS = Path(__file__).resolve().parent.parent / "docs"

CSS = """
  :root { --surface:#F5FAFD; --surface-high:#E4E9EC; --on-surface:#171C1F; --on-surface-variant:#40484C;
          --primary:#0C6780; --on-primary:#FFFFFF; --primary-container:#BAEAFF; --on-primary-container:#004D62; --outline-variant:#C0C8CC; }
  @media (prefers-color-scheme: dark) {
    :root { --surface:#0F1417; --surface-high:#252B2D; --on-surface:#DEE3E6; --on-surface-variant:#C0C8CC;
            --primary:#89D0ED; --on-primary:#003545; --primary-container:#004D62; --on-primary-container:#BAEAFF; --outline-variant:#40484C; }
  }
  * { box-sizing: border-box; }
  body { margin:0; background:var(--surface); color:var(--on-surface); font:17px/1.65 "Roboto Flex", system-ui, sans-serif; }
  main { max-width: 760px; margin: 0 auto; padding: 32px 16px 64px; }
  a { color: var(--primary); }
  nav.crumbs { font-size: 15px; margin-bottom: 20px; }
  nav.crumbs a { text-decoration: none; font-weight: 600; }
  h1 { font-size: clamp(32px, 7vw, 48px); line-height:1.1; font-variation-settings:"wdth" 110; font-weight:750; margin:0 0 12px; }
  .lead { font-size: 19px; color: var(--on-surface-variant); margin: 0 0 24px; }
  h2 { font-size: 24px; line-height: 1.25; margin: 36px 0 10px; }
  ol.steps { list-style: none; counter-reset: s; padding: 0; display: grid; gap: 10px; }
  ol.steps li { counter-increment: s; background: var(--surface-high); border-radius: 24px; padding: 16px 18px 16px 64px; position: relative; }
  ol.steps li::before { content: counter(s); position: absolute; left: 16px; top: 14px; width: 34px; height: 34px; border-radius: 50%;
    display: grid; place-items: center; background: var(--primary-container); color: var(--on-primary-container); font-weight: 700; }
  .card { background: var(--primary-container); color: var(--on-primary-container); border-radius: 28px; padding: 22px 24px; margin: 32px 0; }
  .card h2 { margin-top: 0; }
  .btn { display:inline-flex; align-items:center; min-height:52px; padding:0 26px; border-radius:999px; font-weight:650; text-decoration:none;
    background: var(--primary); color: var(--on-primary); margin: 6px 8px 0 0; }
  .btn.tonal { background: var(--surface); color: var(--primary); }
  img.shot { width: 100%; max-width: 360px; height: auto; display: block; margin: 16px 0; }
  code { background: var(--surface-high); padding: 1px 6px; border-radius: 6px; }
  footer { margin-top: 48px; padding-top: 20px; border-top: 1px solid var(--outline-variant); color: var(--on-surface-variant); font-size: 15px; }
"""


def page(path, lang, title, description, h1, lead, body, crumbs, alternates=None, cta=None):
    url = SITE + path
    alt = ""
    for code, href in (alternates or {}).items():
        alt += f'<link rel="alternate" hreflang="{code}" href="{SITE + href}">\n'
    ld = [
        {"@context": "https://schema.org", "@type": "Article", "headline": h1, "description": description,
         "inLanguage": lang, "url": url, "image": SITE + "img/hero.png",
         "author": {"@type": "Person", "name": "memehammad", "url": "https://github.com/memehammad"},
         "publisher": {"@type": "Organization", "name": "Clash Timers", "logo": {"@type": "ImageObject", "url": SITE + "img/icon.png"}},
         "dateModified": "2026-11-06"},
        {"@context": "https://schema.org", "@type": "BreadcrumbList", "itemListElement": [
            {"@type": "ListItem", "position": i + 1, "name": n, "item": SITE + h} for i, (n, h) in enumerate(crumbs)]},
    ]
    crumb_html = " › ".join(f'<a href="{SITE + h}">{n}</a>' for n, h in crumbs[:-1])
    cta = cta or {
        "title": "Get Clash Timers",
        "text": "Free and open source, for Android 12 and newer. No login, no ads.",
        "download": "Download the app", "obtainium": "Get it with Obtainium",
    }
    html = f"""<!doctype html>
<html lang="{lang}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<meta name="description" content="{description}">
<link rel="canonical" href="{url}">
{alt}<link rel="icon" href="{SITE}img/icon.png">
<meta property="og:type" content="article">
<meta property="og:title" content="{title}">
<meta property="og:description" content="{description}">
<meta property="og:url" content="{url}">
<meta property="og:image" content="{SITE}img/hero.png">
<meta name="twitter:card" content="summary_large_image">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Roboto+Flex:opsz,wdth,wght@8..144,25..151,100..1000&display=swap" rel="stylesheet" media="print" onload="this.media='all'">
<script type="application/ld+json">{json.dumps(ld, ensure_ascii=False)}</script>
<style>{CSS}</style>
</head>
<body>
<main>
  <nav class="crumbs" aria-label="Breadcrumb">{crumb_html}</nav>
  <h1>{h1}</h1>
  <p class="lead">{lead}</p>
{body}
  <div class="card">
    <h2>{cta['title']}</h2>
    <p>{cta['text']}</p>
    <a class="btn" href="{APK}">{cta['download']}</a><a class="btn tonal" href="{OBTAINIUM}">{cta['obtainium']}</a>
  </div>
  <footer>This material is unofficial and is not endorsed by Supercell. For more information see Supercell’s <a href="https://supercell.com/en/fan-content-policy/">Fan Content Policy</a>.</footer>
</main>
</body>
</html>
"""
    out = DOCS / path / "index.html" if path.endswith("/") else DOCS / path
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(html)
    return url


HOME = ("Clash Timers", "")
GUIDES = ("Guides", "guides/")

pages = []

pages.append(page(
    "guides/", "en",
    "Clash of Clans guides for Android – Clash Timers",
    "Short guides for Clash of Clans on Android: the Data Export, builder notifications and home screen widgets.",
    "Guides",
    "Short, practical guides for keeping your Clash of Clans village busy from your Android phone.",
    """  <ol class="steps">
    <li><a href="data-export/"><b>How to use the Clash of Clans Data Export</b></a><br>Where to find it, what's in it and what you can do with it.</li>
    <li><a href="notifications-not-working/"><b>Clash of Clans notifications not working?</b></a><br>Why the game's alerts go missing on Android, and how to fix it.</li>
    <li><a href="builder-notifications/"><b>Get a notification when your builders are free</b></a><br>Alerts for builders, lab, pets and the Goblin Builder on Android.</li>
    <li><a href="../"><b>The Clash of Clans widget for Android</b></a><br>Free builders and timers on your home screen.</li>
  </ol>""",
    [HOME, GUIDES],
))

pages.append(page(
    "guides/data-export/", "en",
    "Clash of Clans Data Export: where to find it and how to use it",
    "Find the Data Export in Clash of Clans settings, copy your village data and turn it into timers, widgets and notifications on Android.",
    "How to use the Clash of Clans Data Export",
    "Clash of Clans can copy your whole village as text. Here's where to find it, what's inside and how to put it to use.",
    """  <h2>Where to find the Data Export</h2>
  <ol class="steps">
    <li>Open Clash of Clans and tap <b>Settings</b> (the gear icon).</li>
    <li>Tap <b>More Settings</b>.</li>
    <li>Scroll to <b>Data Export</b> and tap <b>Copy</b>. Your village is now on the clipboard.</li>
  </ol>
  <h2>What's in the export</h2>
  <p>The export is a block of text (JSON) with your player tag and a snapshot of your village: buildings, traps, heroes, troops, spells and pets with their levels, plus every upgrade that's running right now and how much time it has left. Builder Base, the Builder's Apprentice, Lab Assistant and active boosts are in there too.</p>
  <p>It doesn't include your Supercell ID, password or anything that lets someone log in, so it's safe to paste into tools you trust.</p>
  <h2>What you can do with it</h2>
  <p>Because the export lists every running timer, an app can work out when each builder, the laboratory and the pet house will be free. <a href="../../">Clash Timers</a> does exactly that on Android: paste the export once and you get a home screen widget, a notification when each upgrade finishes, and a planner that shows what a Builder Potion would finish.</p>
  <h2>Keeping it up to date</h2>
  <p>The export is a snapshot. Copy a new one after you start upgrades or use a potion. With the optional Shizuku auto-import, Clash Timers picks up each new export on its own as soon as you tap Copy.</p>""",
    [HOME, GUIDES, ("Data Export", "guides/data-export/")],
))

pages.append(page(
    "guides/builder-notifications/", "en",
    "Clash of Clans builder notifications on Android – know when builders are free",
    "Get a notification the moment a Clash of Clans builder, the laboratory or the Goblin Builder is free, plus a reminder if a builder sits idle.",
    "Get a notification when your Clash of Clans builders are free",
    "Clash Timers sends an alert for every finished upgrade, counts running potions, and can nudge you when a builder sits idle.",
    """  <h2>Set it up</h2>
  <ol class="steps">
    <li>Install <a href="../../">Clash Timers</a> and allow notifications in the quick setup.</li>
    <li>In Clash of Clans, go to Settings, More Settings, Data Export and tap <b>Copy</b>.</li>
    <li>Open Clash Timers. Every running upgrade now has a notification scheduled for when it finishes.</li>
  </ol>
  <h2>What you get notified about</h2>
  <ul>
    <li><b>Builders</b> in the home village and Builder Base, including Gear Ups.</li>
    <li><b>Laboratory</b> and Star Laboratory research, and the <b>Pet House</b>.</li>
    <li><b>Goblin Builder and Goblin Researcher</b>: “Goblin Builder is free” when his job is done.</li>
    <li>An optional <b>idle builder reminder</b> if a builder stays free for 30 minutes, 1 or 2 hours.</li>
  </ul>
  <h2>Boosts are counted</h2>
  <p>Builder and Research Potions, Builder Bites, Study Soup and the Clock Tower speed timers up. Clash Timers includes active boosts in every finish time, so the alert arrives when the upgrade is really done.</p>
  <h2>Also on your watch and At a Glance</h2>
  <p>Notifications show up on a connected watch as usual. Clash Timers can also show free builders in the At a Glance bar above your clock through Smartspacer.</p>""",
    [HOME, GUIDES, ("Builder notifications", "guides/builder-notifications/")],
))

pages.append(page(
    "guides/notifications-not-working/", "en",
    "Clash of Clans notifications not working on Android? How to fix it",
    "Not getting Clash of Clans notifications when builders finish? Check these Android settings, or use reliable local alerts that don't depend on the game.",
    "Clash of Clans notifications not working?",
    "Upgrade done, builder idle, and your phone stayed quiet? You're not alone. Here's why the game's notifications go missing on Android, what to check, and a way to get alerts that always arrive.",
    """  <h2>Why the alerts go missing</h2>
  <p>Clash of Clans sends its notifications from Supercell's servers through Google's push service. Several things on the phone can stop them along the way: notifications turned off for the game, Android putting the app to sleep to save battery, Do Not Disturb, or a data saver that blocks the game in the background. If you play on more than one device, the alert may also land on the other one.</p>
  <h2>Fix it step by step</h2>
  <ol class="steps">
    <li><b>Allow notifications.</b> Long-press the Clash of Clans icon, tap App info, then Notifications, and make sure they're on, including every category below the main switch.</li>
    <li><b>Check the game's own settings.</b> In Clash of Clans, open Settings and make sure notifications are switched on there too.</li>
    <li><b>Stop battery optimisation.</b> In App info, open Battery and choose Unrestricted (or "Don't optimise"). Samsung, Xiaomi, OnePlus and Huawei phones also have their own sleeping-apps lists. Take the game off them.</li>
    <li><b>Allow background data.</b> In App info, open Mobile data and turn on background data and unrestricted data usage.</li>
    <li><b>Check Do Not Disturb and modes.</b> Make sure Clash of Clans isn't silenced by Do Not Disturb, Bedtime, or a focus mode.</li>
    <li><b>Open the game once on this phone.</b> If you also play on a tablet or another phone, the last device you played on is the safest bet for alerts.</li>
    <li><b>Update and restart.</b> Update the game from the Play Store, then restart your phone.</li>
  </ol>
  <h2>Alerts that always arrive</h2>
  <p>Even with everything set up, server push notifications can arrive late or not at all. <a href="../../">Clash Timers</a> works differently: it reads the game's Data Export and schedules the alerts <b>on your phone</b>, with Android's alarm system. No server and no internet are involved, so they arrive on time, even in flight mode.</p>
  <ul>
    <li>A notification for every finished upgrade, research and pet, including the Goblin Builder.</li>
    <li>Active Builder and Research Potions are counted, so alerts move earlier when you boost.</li>
    <li>An optional reminder when a builder sits idle for 30 minutes or more.</li>
    <li>A home screen widget that shows free builders at a glance.</li>
  </ul>
  <p>See <a href="../builder-notifications/">how to set up builder notifications</a> for the details.</p>""",
    [HOME, GUIDES, ("Notifications not working", "guides/notifications-not-working/")],
))

# Sitemap (gen_i18n.py adds the home page in every language).
urls = [f"  <url><loc>{u}</loc><lastmod>2026-11-06</lastmod></url>" for u in pages]
urls.append(f"  <url><loc>{SITE}privacy.html</loc></url>")
(DOCS / "sitemap.xml").write_text(
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">\n'
    + "\n".join(urls) + "\n</urlset>\n")
print("\n".join(pages))
