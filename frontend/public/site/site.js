/* ===========================================================================
   CrossDesk — public site script. No framework, no external dependencies.

   Consumes the public API (docs/PRODUCT-PLAN.md §5):
     GET /api/benchmarks            → [ { id, name, kind, publishTime, timezone, description,
                                          last: { price, asOf, tier, k, n, signers[], ageSeconds },
                                          referencing: [ { id, name } ] } ]
     GET /api/benchmarks/{id}       → one of the above
     GET /api/series/{id}?limit=30  → [ { date, asOf, price, referencePrice?, wrapperFactor?,
                                          tier, k, n, signers[], fixingCid, restated } ]
     GET /api/methodology           → { version, url, signerProtocolVersion }
     GET /api/signer-protocol       → { version, roles: [ { key, title, uniquelyKnows,
                                          conditions: [ { name, passesWhen } ], requiresObservedRange } ] }

   Every value is painted with its tier and age beside it (claims discipline,
   PRODUCT-PLAN §8). "Attested K of N" is only written when K signatures exist.
   If the API is unreachable the page keeps its skeleton and says so.
   =========================================================================== */
(function () {
  'use strict';

  /* ---- static catalogue: what the site says about each product even when the
          API is down. Ids match FixingSchedule (CBTC, cETH, LX1). ------------- */
  var CATALOGUE = {
    CBTC: {
      id: 'CBTC', name: 'CBTC Close', kind: 'wrapped', identifier: 'CDX-CBTC-D',
      publishTime: '16:00', timezone: 'Europe/London', quotedIn: 'USDC',
      description: 'The daily settlement price of cBTC, the wrapped-bitcoin token on Canton. Struck as a benchmark bitcoin print multiplied by a committee-attested par factor — the wrapper’s discount or premium to the asset it wraps — and published with both inputs as separate signed fields.',
      about: [
        'A wrapped asset is not the asset it wraps. cBTC carries custody and redemption risk that no bitcoin benchmark prices, and it settles contracts that need one struck moment. The CBTC Close carries two inputs, not one: the benchmark print (a public bitcoin reference price nobody argues about) and the par factor the committee attested. The struck price is their product, and a pair that does not reconcile cannot exist on the ledger.',
        'The par factor is the only number in the fixing that no external benchmark administrator produces. It is the issuer’s redemption integrity, the lender’s book acceptance and the venue’s observed range, condensed into one signed ratio.'
      ],
      referencing: [{ id: 'LX1', name: 'LX1 NAV' }]
    },
    cETH: {
      id: 'cETH', name: 'cETH Close', kind: 'wrapped', identifier: 'CDX-CETH-D',
      publishTime: '16:00', timezone: 'Europe/London', quotedIn: 'USDC',
      description: 'The daily settlement price of cETH, the wrapped-ether token on Canton. Same construction as the CBTC Close: a benchmark ether print multiplied by a committee-attested par factor, both published as separate signed fields.',
      about: [
        'cETH is a claim on ether held elsewhere. Its benchmark input is a public ether reference price; its judgement input is the par factor the committee attests. Both are signed, both are published, and the struck price is their product.',
        'Where a component fixing is missing, any basket that references it is not published. A gap is published as a gap; a fixing is never estimated to fill one.'
      ],
      referencing: [{ id: 'LX1', name: 'LX1 NAV' }]
    },
    LX1: {
      id: 'LX1', name: 'LX1 NAV', kind: 'fund', identifier: 'CDX-LX1-D',
      publishTime: '16:00', timezone: 'Europe/London', quotedIn: 'USDC',
      description: 'The official net asset value per share of LX1, a demonstration basket fund holding cETH and CBTC. Computed as the sum of units per share multiplied by each component’s current fixing, and published immediately after its components.',
      about: [
        'A basket fixing is Σ (unitsPerShareᵢ × fixingᵢ) over the components. It therefore requires a current fixing for every component; if one is missing, the basket NAV is not published rather than estimated.',
        'Creation and redemption of LX1 shares settle in kind, atomically, at this number. The official NAV is the number contracts settle against; the indicative value derived from live market data is informational and binding on nobody.',
        'LX1 is a demonstration instrument on the hosted sandbox. Its components inside the fund are issued for demonstration and are not the production tokens.'
      ],
      referencing: []
    }
  };
  var ORDER = ['CBTC', 'cETH', 'LX1'];

  /* Tier labels follow the production waterfall (PRODUCT-PLAN §4). */
  var TIER = {
    1: 'attested', 2: 'alternate seats', 3: 'benchmark × factor', 4: 'carried forward', 5: 'missed'
  };

  /* ---- helpers ------------------------------------------------------------ */
  function $(sel, root) { return (root || document).querySelector(sel); }
  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }
  function getJSON(url, ms) {
    var ctrl = typeof AbortController !== 'undefined' ? new AbortController() : null;
    var timer = ctrl ? setTimeout(function () { ctrl.abort(); }, ms || 8000) : null;
    return fetch(url, { headers: { Accept: 'application/json' }, signal: ctrl ? ctrl.signal : undefined })
      .then(function (r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
      })
      .finally(function () { if (timer) clearTimeout(timer); });
  }
  function fmtNum(x, dp) {
    var n = typeof x === 'number' ? x : parseFloat(x);
    if (!isFinite(n)) return '—';
    if (dp == null) dp = Math.abs(n) >= 1000 ? 2 : 4;
    return n.toLocaleString('en-GB', { minimumFractionDigits: dp, maximumFractionDigits: dp });
  }
  function fmtAge(sec) {
    if (sec == null || !isFinite(sec)) return '';
    sec = Math.max(0, Math.round(sec));
    if (sec < 60) return sec + 's ago';
    var m = Math.round(sec / 60);
    if (m < 60) return m + 'm ago';
    var h = Math.floor(m / 60);
    if (h < 48) return h + 'h ' + (m % 60) + 'm ago';
    return Math.floor(h / 24) + 'd ago';
  }
  function fmtAsOf(iso, tz) {
    if (!iso) return '—';
    var d = new Date(iso);
    if (isNaN(d.getTime())) return String(iso);
    try {
      return new Intl.DateTimeFormat('en-GB', {
        timeZone: tz || 'UTC', year: 'numeric', month: 'short', day: '2-digit',
        hour: '2-digit', minute: '2-digit', timeZoneName: 'short'
      }).format(d);
    } catch (e) {
      return d.toISOString().replace('T', ' ').slice(0, 16) + ' UTC';
    }
  }
  function fmtDate(iso) {
    if (!iso) return '—';
    var d = new Date(iso);
    if (isNaN(d.getTime())) return String(iso);
    try {
      return new Intl.DateTimeFormat('en-GB', { year: 'numeric', month: 'short', day: '2-digit', timeZone: 'UTC' }).format(d);
    } catch (e) { return String(iso).slice(0, 10); }
  }
  function ageOf(last) {
    if (!last) return null;
    if (last.ageSeconds != null) return last.ageSeconds;
    if (last.asOf) return (Date.now() - new Date(last.asOf).getTime()) / 1000;
    return null;
  }
  /* "attested K of N" only when K real signatures exist (PRODUCT-PLAN §8). */
  function attestation(last) {
    if (!last) return { text: 'no fixing published', cls: 'pending' };
    var have = Array.isArray(last.signers) ? last.signers.length : 0;
    var k = last.k, n = last.n;
    if (k != null && n != null && have >= k) return { text: 'attested ' + have + ' of ' + n, cls: 'attested' };
    if (n != null) return { text: 'awaiting attestation · ' + have + ' of ' + n, cls: 'pending' };
    return { text: 'attestation unknown', cls: 'pending' };
  }
  function tierLabel(t) {
    if (t == null) return 'tier —';
    return 'tier ' + t + (TIER[t] ? ' · ' + TIER[t] : '');
  }
  /* Gold only for an officially struck, attested value (tier 1 or 2 with K signatures). */
  function isOfficial(last) {
    if (!last || last.price == null) return false;
    var a = attestation(last);
    return a.cls === 'attested' && (last.tier == null || last.tier <= 2);
  }
  function merge(id, api) {
    var base = CATALOGUE[id] || { id: id, name: id, about: [], referencing: [] };
    var out = {};
    for (var k in base) out[k] = base[k];
    if (api) for (var j in api) if (api[j] != null) out[j] = api[j];
    return out;
  }

  /* ---- tiles (home and /benchmarks/) --------------------------------------- */
  function tileMeta(b) {
    var last = b.last;
    var meta = el('div', 'meta');
    var a = attestation(last);
    meta.appendChild(el('span', a.cls, a.text));
    meta.appendChild(el('span', null, tierLabel(last && last.tier)));
    var age = ageOf(last);
    if (age != null) meta.appendChild(el('span', age > 86400 * 2 ? 'stale' : null, fmtAge(age)));
    return meta;
  }
  function renderTile(b, skeleton) {
    var a = el('a', 'tile' + (skeleton ? ' skeleton' : ''));
    a.href = '/benchmarks/' + encodeURIComponent(b.id);
    a.setAttribute('aria-label', b.name + (b.last && b.last.price != null ? ', ' + fmtNum(b.last.price) + ' ' + (b.quotedIn || '') : ''));
    a.appendChild(el('div', 'id', b.identifier || b.id));
    a.appendChild(el('div', 'name', b.name));
    var v = el('div', 'value' + (isOfficial(b.last) ? ' official' : ''));
    if (b.last && b.last.price != null) {
      v.textContent = fmtNum(b.last.price);
      var s = el('small', null, b.quotedIn || '');
      v.appendChild(s);
    } else {
      v.textContent = skeleton ? '00,000.00' : '—';
    }
    a.appendChild(v);
    var asof = el('div', 'meta');
    asof.appendChild(el('span', null, 'as of ' + (b.last && b.last.asOf ? fmtAsOf(b.last.asOf, b.timezone) : '—')));
    a.appendChild(asof);
    a.appendChild(tileMeta(b));
    if (b.description) a.appendChild(el('p', 'desc', b.description));
    return a;
  }
  function renderTiles(container, state) {
    if (!container) return;
    container.innerHTML = '';
    ORDER.forEach(function (id) { container.appendChild(renderTile(merge(id, null), true)); });
    getJSON('/api/benchmarks').then(function (list) {
      var byId = {};
      (list || []).forEach(function (b) { if (b && b.id) byId[b.id] = b; });
      container.innerHTML = '';
      var ids = ORDER.slice();
      Object.keys(byId).forEach(function (id) { if (ids.indexOf(id) < 0) ids.push(id); });
      ids.forEach(function (id) { container.appendChild(renderTile(merge(id, byId[id]), false)); });
      if (state) { state.textContent = 'Values from the hosted sandbox via /api/benchmarks. Refreshed on load.'; state.className = 'api-state'; }
    }).catch(function (err) {
      if (state) {
        state.textContent = 'Live values unavailable (' + (err && err.message ? err.message : 'no response') + '). The sandbox may be cold-starting; reload in a minute.';
        state.className = 'api-state down';
      }
    });
  }

  /* ---- product page (/benchmarks/{id}) ------------------------------------ */
  function idFromLocation() {
    var q = new URLSearchParams(location.search).get('id');
    if (q) return q;
    var m = location.pathname.match(/\/benchmarks\/([^\/]+)\/?$/);
    if (m && m[1] && !/\.html$/.test(m[1])) return decodeURIComponent(m[1]);
    return null;
  }
  function seriesRow(r, quotedIn) {
    var tr = el('tr');
    tr.appendChild(el('td', 'mono', fmtDate(r.date || r.asOf)));
    var a = attestation(r);
    var official = a.cls === 'attested' && (r.tier == null || r.tier <= 2);
    var price = el('td', 'num' + (official ? ' official' : ''), r.price != null ? fmtNum(r.price) : '—');
    tr.appendChild(price);
    tr.appendChild(el('td', 'num', r.referencePrice != null ? fmtNum(r.referencePrice) : '—'));
    tr.appendChild(el('td', 'num', r.wrapperFactor != null ? fmtNum(r.wrapperFactor, 4) : '—'));
    tr.appendChild(el('td', 'mono', tierLabel(r.tier)));
    var kn = el('td', 'mono' + (a.cls === 'attested' ? '' : ' restated'), a.text);
    tr.appendChild(kn);
    tr.appendChild(el('td', 'mono' + (r.restated ? ' restated' : ''), r.restated ? 'restated' : ''));
    return tr;
  }
  function renderSeats(container, roles) {
    if (!container) return;
    container.innerHTML = '';
    roles.forEach(function (r) {
      if (r.key === 'operator') return;
      var card = el('div', 'card');
      card.appendChild(el('h3', null, r.title));
      card.appendChild(el('p', null, r.uniquelyKnows ? 'Uniquely knows: ' + r.uniquelyKnows.replace(/\.?$/, '.') : ''));
      var ul = el('ul');
      ul.style.marginTop = '10px'; ul.style.fontSize = '0.85rem';
      (r.conditions || []).forEach(function (c) {
        var li = el('li');
        li.appendChild(el('code', null, c.name));
        li.appendChild(document.createTextNode(' — ' + c.passesWhen));
        ul.appendChild(li);
      });
      card.appendChild(ul);
      if (r.requiresObservedRange) card.appendChild(el('p', 'src', 'Observed low/high enforced on-ledger.'));
      container.appendChild(card);
    });
  }
  var FALLBACK_ROLES = [
    { key: 'issuer', title: 'Issuer', uniquelyKnows: 'Whether the wrapper can actually be redeemed right now', conditions: [
      { name: 'attestor-quorum', passesWhen: 'At least the issuer’s own threshold of attestors are online and signing' },
      { name: 'reserves-current', passesWhen: 'The most recent proof-of-reserve attestation is less than 24h old' },
      { name: 'reserves-cover-supply', passesWhen: 'Attested reserves are at least the circulating supply of the wrapped token' },
      { name: 'redemption-queue-clear', passesWhen: 'No redemption request is unfilled beyond its stated window' }], requiresObservedRange: false },
    { key: 'lender', title: 'Lender', uniquelyKnows: 'Whether it will actually carry this number on its own book', conditions: [
      { name: 'independent-mark-within-tolerance', passesWhen: 'The proposed mark is within the lender’s declared tolerance of its own valuation' },
      { name: 'liquidations-consistent', passesWhen: 'No liquidation the lender ran in the session cleared materially away from the proposed mark' },
      { name: 'book-acceptance', passesWhen: 'The lender will mark its own collateral at this level for the period the fixing governs' }], requiresObservedRange: false },
    { key: 'venue', title: 'Venue', uniquelyKnows: 'The transaction data — where the asset actually traded', conditions: [
      { name: 'traded-range', passesWhen: 'The proposed mark lies within the high/low the venue’s own book traded in the window' },
      { name: 'spread-within-tolerance', passesWhen: 'Best bid/ask spread at the strike is inside the declared tolerance' },
      { name: 'sufficient-volume', passesWhen: 'Traded volume in the window meets the declared minimum' }], requiresObservedRange: true }
  ];

  function renderBenchmarkPage() {
    var root = $('[data-benchmark-page]');
    if (!root) return;
    var id = idFromLocation();
    var notfound = $('[data-notfound]');
    if (!id || (!CATALOGUE[id] && !/^[A-Za-z0-9_-]{1,24}$/.test(id))) {
      root.hidden = true; if (notfound) notfound.hidden = false; return;
    }
    var b = merge(id, null);
    var setText = function (sel, txt) { var n = $(sel, root); if (n) n.textContent = txt; };
    var paint = function (bm) {
      document.title = bm.name + ' — CrossDesk benchmark';
      setText('[data-id]', bm.identifier || bm.id);
      setText('[data-name]', bm.name);
      setText('[data-description]', bm.description || '');
      setText('[data-publish]', (bm.publishTime || '—') + ' ' + (bm.timezone || ''));
      setText('[data-quoted]', bm.quotedIn || '—');
      var v = $('[data-value]', root);
      if (v) {
        v.className = 'value' + (isOfficial(bm.last) ? ' official' : '');
        v.textContent = bm.last && bm.last.price != null ? fmtNum(bm.last.price) : '—';
        v.appendChild(el('small', null, bm.quotedIn || ''));
      }
      setText('[data-asof]', bm.last && bm.last.asOf ? fmtAsOf(bm.last.asOf, bm.timezone) : '—');
      var a = attestation(bm.last);
      var att = $('[data-attested]', root); if (att) { att.textContent = a.text; att.className = a.cls; }
      setText('[data-tier]', tierLabel(bm.last && bm.last.tier));
      var age = ageOf(bm.last);
      setText('[data-age]', age != null ? fmtAge(age) : '—');
      var signers = $('[data-signers]', root);
      if (signers) signers.textContent = bm.last && bm.last.signers && bm.last.signers.length ? bm.last.signers.join(', ') : '—';
      var about = $('[data-about]', root);
      if (about) { about.innerHTML = ''; (bm.about || []).forEach(function (t) { about.appendChild(el('p', null, t)); }); }
      var ref = $('[data-referencing]', root);
      if (ref) {
        ref.innerHTML = '';
        var list = bm.referencing || [];
        if (!list.length) ref.appendChild(el('p', null, 'No product currently references this fixing.'));
        list.forEach(function (p) {
          var li = el('li'); var link = el('a', null, p.name || p.id); link.href = '/benchmarks/' + encodeURIComponent(p.id); li.appendChild(link); ref.appendChild(li);
        });
      }
      var csv = $('[data-csv]', root); if (csv) csv.href = '/api/series/' + encodeURIComponent(bm.id) + '.csv';
      var json = $('[data-json]', root); if (json) json.href = '/api/benchmarks/' + encodeURIComponent(bm.id);
    };
    paint(b);
    var state = $('[data-api-state]', root);

    getJSON('/api/benchmarks/' + encodeURIComponent(id)).then(function (api) {
      var hv0 = $('.hero-value', root); if (hv0) hv0.classList.remove('skeleton');
      paint(merge(id, api));
      if (state) { state.textContent = 'Live from the hosted sandbox via /api/benchmarks/' + id + '.'; state.className = 'api-state'; }
    }).catch(function (err) {
      var hv = $('.hero-value', root); if (hv) hv.classList.remove('skeleton');
      if (state) { state.textContent = 'Live value unavailable (' + (err && err.message ? err.message : 'no response') + '). Static description shown.'; state.className = 'api-state down'; }
    });

    var tbody = $('[data-series]', root);
    var sstate = $('[data-series-state]', root);
    if (tbody) {
      getJSON('/api/series/' + encodeURIComponent(id) + '?limit=30').then(function (rows) {
        tbody.innerHTML = '';
        if (!rows || !rows.length) {
          var tr = el('tr'); var td = el('td', null, 'No fixing has been published for this identifier.'); td.colSpan = 7; tr.appendChild(td); tbody.appendChild(tr);
        } else rows.slice(0, 30).forEach(function (r) { tbody.appendChild(seriesRow(r, b.quotedIn)); });
        if (sstate) sstate.textContent = 'Newest first. Gold marks a value attested by at least K of N; a restated row supersedes an earlier one.';
      }).catch(function (err) {
        tbody.innerHTML = '';
        var tr = el('tr'); var td = el('td', null, 'Series unavailable (' + (err && err.message ? err.message : 'no response') + ').'); td.colSpan = 7; tr.appendChild(td); tbody.appendChild(tr);
        if (sstate) { sstate.textContent = ''; }
      });
    }

    var seats = $('[data-seats]', root);
    if (seats) {
      renderSeats(seats, FALLBACK_ROLES);
      getJSON('/api/signer-protocol').then(function (p) {
        if (p && p.roles && p.roles.length) renderSeats(seats, p.roles);
        var pv = $('[data-protocol-version]', root); if (pv && p && p.version) pv.textContent = p.version;
      }).catch(function () {});
    }
  }

  /* ---- methodology version --------------------------------------------- */
  function renderMethodologyVersion() {
    var nodes = document.querySelectorAll('[data-methodology-version]');
    var sp = document.querySelectorAll('[data-signer-protocol-version]');
    if (!nodes.length && !sp.length) return;
    getJSON('/api/methodology').then(function (m) {
      if (m && m.version) nodes.forEach(function (n) { n.textContent = m.version; });
      if (m && m.signerProtocolVersion) sp.forEach(function (n) { n.textContent = m.signerProtocolVersion; });
    }).catch(function () {
      getJSON('/api/signer-protocol').then(function (p) {
        if (p && p.version) sp.forEach(function (n) { n.textContent = p.version; });
      }).catch(function () {});
    });
  }

  /* ---- header: current page + mobile toggle ------------------------------ */
  function initNav() {
    var nav = $('.site-nav');
    var toggle = $('.nav-toggle');
    if (nav) {
      var path = location.pathname.replace(/\/index\.html$/, '/').replace(/\.html$/, '');
      nav.querySelectorAll('a[href]').forEach(function (a) {
        var href = a.getAttribute('href').replace(/\.html$/, '');
        if (href === '/' ? path === '/' : (path === href || path.indexOf(href.replace(/\/$/, '') + '/') === 0)) {
          if (!a.classList.contains('btn')) a.setAttribute('aria-current', 'page');
        }
      });
    }
    if (toggle && nav) {
      toggle.addEventListener('click', function () {
        var open = nav.classList.toggle('open');
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
      });
      document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && nav.classList.contains('open')) {
          nav.classList.remove('open'); toggle.setAttribute('aria-expanded', 'false'); toggle.focus();
        }
      });
    }
  }

  function init() {
    initNav();
    renderTiles($('[data-benchmark-tiles]'), $('[data-tiles-state]'));
    renderBenchmarkPage();
    renderMethodologyVersion();
    var y = $('[data-year]'); if (y) y.textContent = String(new Date().getFullYear());
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
