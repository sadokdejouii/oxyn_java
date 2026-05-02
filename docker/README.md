# Stack Mercure pour OXYN JavaFX

Bus temps réel **Mercure** (SSE) utilisé par le client JavaFX pour :
- discussion temps réel (chat encadrant ↔ client) ;
- notifications messages non lus ;
- présence en ligne / hors ligne ;
- indicateur « en train d'écrire… » ;
- mises à jour Planning sans rafraîchissement de page.

> ℹ️ MySQL **n'est pas** dans cette stack. La base `oxyn` continue de tourner
> sur le poste hôte (port `3306`) — aucun changement côté DB.

## Prérequis

- Docker Desktop (Windows/Mac) ou Docker Engine (Linux).

## Variables d'environnement

| Variable | Rôle | Défaut |
|---|---|---|
| `MERCURE_URL` | URL interne d'appel `POST /.well-known/mercure` (publish) | `http://localhost:3000/.well-known/mercure` |
| `MERCURE_PUBLIC_URL` | URL exposée aux clients SSE (subscribe) | `http://localhost:3000/.well-known/mercure` |
| `MERCURE_JWT_SECRET` | Secret HMAC partagé publishers + subscribers | `oxyn-dev-secret-change-me-please-use-32+chars` |

Le client JavaFX lit ces variables avec la priorité : **env > `realtime.properties` > défauts internes**.

## Démarrer / arrêter

```powershell
# Démarrer (depuis la racine du projet)
docker compose -f docker/docker-compose.mercure.yml up -d

# Logs en direct
docker compose -f docker/docker-compose.mercure.yml logs -f mercure

# Healthcheck rapide
curl http://localhost:3000/healthz

# Arrêter
docker compose -f docker/docker-compose.mercure.yml down
```

## Repli (fallback) côté JavaFX

Si Mercure est indisponible :
- l'app **ne plante jamais** ;
- elle bascule en **polling 5 s** automatique pour discussion / notifications / planning ;
- un **bandeau « Mode dégradé »** discret apparaît dans le header Planning ;
- un log console explicite `[Realtime] Mercure indisponible, fallback polling activé` est tracé.

## Sécurité

- Le port `3000` est volontairement mappé sur `127.0.0.1` (loopback) :
  Mercure n'est pas exposé sur le réseau local.
- En production : utilisez un secret HMAC ≥ 32 caractères vraiment aléatoire,
  changez `MERCURE_EXTRA_DIRECTIVES` pour retirer `anonymous` si nécessaire,
  passez Mercure derrière un TLS (Caddy le fait nativement avec un domaine).
