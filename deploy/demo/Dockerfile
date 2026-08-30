# =============================================================================
# CrossDesk public demo — the sandbox and the desk in ONE container.
# =============================================================================
# WHY THIS EXISTS. The hosted desk points at a Canton participant that went
# offline when HackCanton Season 2 closed, so every ledger-backed call returns
# 503 and a visitor sees a broken product. Waiting for a participant to fix that
# is the wrong trade: acquiring one is the project's standing blocker and has
# been for weeks, while `.env.example` profile A has said the whole desk runs on
# a local sandbox with NO node operator and NO secrets the entire time.
#
# So this image carries its own ledger. Nothing to onboard, nothing to expire —
# the JWT that broke the hosted desk does not exist in this configuration.
#
# WHAT A VISITOR GETS. A real Canton ledger executing real Daml: sealed auctions,
# a K-of-N committee, in-kind creation and redemption, atomic DvP. Not a mock and
# not a recording. What they do NOT get is a shared world — see the entrypoint.
#
# THE HONEST LIMITS, so nobody has to discover them:
#   * State is in-memory and resets whenever the instance recycles. For a demo
#     that is a feature — every visitor starts from the seeded book.
#   * Two JVMs, so this wants ~4Gi. It is not a cheap container.
#   * Cold start is roughly a minute (sandbox up, DAR uploaded, script run).
#     Run it with --min-instances=1 or the first visitor after an idle period
#     waits, and a demo that makes you wait is a demo nobody finishes.
# =============================================================================

# ---- stage 1: fetch the Daml SDK, which carries the Canton sandbox ----------
FROM eclipse-temurin:17-jdk AS sdk

ARG DAML_VERSION=3.4.11
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates tar gzip \
 && rm -rf /var/lib/apt/lists/*

# The installer drops the SDK under $HOME/.daml/sdk/<version>.
RUN curl -sSL https://get.daml.com | sh -s ${DAML_VERSION}

# ---- stage 2: the runtime --------------------------------------------------
FROM eclipse-temurin:17-jre

ARG DAML_VERSION=3.4.11
ENV DAML_HOME=/opt/daml \
    DAML_SDK=/opt/daml/sdk/3.4.11 \
    LEDGER_PORT=6900 \
    PORT=8080

COPY --from=sdk /root/.daml/sdk/${DAML_VERSION} /opt/daml/sdk/${DAML_VERSION}

WORKDIR /app

# The desk, and the package it runs. Both built by the caller — see README.md.
COPY app.jar        /app/app.jar
COPY crossdesk.dar  /app/crossdesk.dar
COPY bind-all.conf  /app/bind-all.conf
COPY entrypoint.sh  /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh

# Cloud Run injects $PORT and routes to it. The sandbox stays on LEDGER_PORT and
# is never exposed — the only way in is through the desk's own API, which is the
# same boundary the product has everywhere else.
EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
