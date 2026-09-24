# DevNet IP allowlisting: who can do it, and what to send

**Egress IP to allowlist: `34.71.67.227`.** This is the GCP static address
`crossdesk-devnet-validator-ip`, in us-central1. It is for **DevNet only**. TestNet and
MainNet will each need a different IP.

## How the process works (verified from primary sources, 2026-09-15)

| Fact | Source | Verified? |
|---|---|---|
| DevNet is *"open to any node but still requires the validator's egress IP to be added to the allowlist maintained by the SV node operators."* | Splice 0.8.1 docs, `docs.dev.sync.global/validator_operator/validator_onboarding.html` | ✅ fetched |
| *"Provide your sponsoring SV with the egress IP … Only one IP may be provided per network … Wait for super validators to adopt the new IP allowlist. This usually takes between 2-7 days."* | same page | ✅ fetched |
| The DevNet onboarding **secret** is self-serve (`POST https://sv.sv-1.dev.global.canton.network.sync.global/api/sv/v0/devnet/onboard/validator/prepare`, valid 1 h). **Only the IP needs a human.** | same page + `validator_compose.html` | ✅ fetched |
| The Canton Foundation **Validator Request** form (`sync.global/validator-request/` → `canton.foundation/apply-to-set-up-a-validator-node/`) asks for the *"Name and email of contact at one of the Super Validators who will sponsor your onboarding process"* and accepts "N/A". Submissions go to the public `tokenomics@lists.sync.global` list. Questions: `operations@canton.foundation` | the form page | ✅ fetched |
| That form is for *MainNet approval* (TestNet requires it too). DevNet itself needs no approval, only the IP | onboarding page ("Joining TestNet requires that you have been approved to join MainNet") | ✅ fetched |
| **A Validator Request for Lucilla, Inc. (CrossDesk) was SUBMITTED on 15 Sep**, sponsor N/A. Its confirmation page links a **"DevNet IP Whitelisting" form**: `https://wkf.ms/4wX6y4Z` → `https://forms.monday.com/forms/1479820bd708e384e8d872719bc48f8f` | `BUSINESS/3-MEETINGS/07_NODERS_APPSFACTORY/02_ACCEPTED_COHORT1_2026-09-15.md` | ✅ the redirect and the form URL resolve (HTTP 200). ⚠️ The form's fields render in JavaScript and were **not read**. ✅ **SUBMITTED 24 Sep 2026** (company "Lucilla, Inc. (CrossDesk)", DevNet IP 34.71.67.227 only, sponsor Canton Foundation, confirmation e-mail s.borjas@lucilla.ca). Validator approval itself landed 23 Sep: tokenomics-announce #388 |
| DevNet SVs today: C7, Cumberland (×2), DA-Helm-Test-Node, Digital-Asset (×2), Five-North, **Global-Synchronizer-Foundation**, Liberty-City-Ventures, MPC-Holding, Orb-1-LP, Proof-Group, SV-Nodeops, Tradeweb (all on 0.8.1) | `docs.dev.global.canton.network.sync.global/versions` (feeds `sync.global/sv-network/`) | ✅ fetched |
| **Noders is not a DevNet SV** (absent from `canton-foundation/configs` `configs/DevNet/approved-sv-id-values.yaml`). Noders cannot allowlist us; they can only forward or introduce | that YAML + the list above | ✅ fetched |
| GSF also runs the Slack Connect channel `#validator-operations` (`daholdings.slack.com/archives/C08AP9QR7K4`); *"ask your SV sponsor to send you an invitation"* | onboarding page | ✅ fetched; ⚠️ we have no invite |
| Canton Foundation validator Telegram group `https://t.me/+opCW61DMR3lhODA5` | founder's notes, 15 Sep | ⚠️ **unverified** (not opened) |
| Named people at any SV who handle DevNet allowlisting | — | ❌ **none found in public sources. Do not invent contacts.** Ask in the channels above |

## The path, in order

1. **Submit the DevNet IP Whitelisting form** (the monday.com link above). It is the one
   channel the Foundation itself pointed us to. Use the message below as the free text.
2. **Post in the Canton Foundation validator Telegram group**, and ask Noders/AppsFactory to
   forward the request to an SV contact (the Telegram message below).
3. **If nothing moves in 5 business days:** email `operations@canton.foundation` with the
   message below, and quote the Validator Request submission date.
4. **Every day from day 2, run `03-verify-allowlist.sh` from the VM.** The request goes to
   ~14 SVs, and the check shows which of them have adopted our IP. Chase specifically the
   ones still BLOCKED.

---

## Ready-to-send message (form free text / email)

> **Subject:** DevNet validator IP allowlist request — Lucilla, Inc. (CrossDesk) — 34.71.67.227
>
> Hello,
>
> We'd like to run our own validator on **DevNet**. Please add our egress IP to the SV
> allowlist:
>
> - **Egress IP (DevNet only): 34.71.67.227** (static, Google Cloud us-central1)
> - Organisation: Lucilla, Inc. (CrossDesk), incorporated in Delaware, US
> - Contact: Sebastián Borjas, s.borjas@lucilla.ca
> - Deployment: Splice 0.8.1 validator (Docker Compose). We will use the DevNet self-serve
>   onboarding secret once our IP is visible to the SVs. Planned party hint:
>   `crossdesk-validator-1`
> - Purpose: CrossDesk is a sealed-auction and atomic DvP settlement desk on Canton. It won
>   Best Financial Application at HackCanton S2, and it is in the AppsFactory accelerator,
>   Cohort 1. On DevNet we settle real CIP-56 assets (BitSafe CBTC) and test in-kind fund
>   creation and redemption.
> - We submitted the Canton Foundation Validator Request on 15 Sep 2026 (sponsor: N/A).
>
> If an SV is able to sponsor us, or if there is a better channel for this, we'd be
> grateful for a pointer. We'll run the Scan and sequencer reachability checks from the
> docs and can report which SVs have picked up the change.
>
> Thank you,
> Sebastián Borjas, Lucilla, Inc.

---

## Telegram message (Noders <> CrossDesk / AppsFactory group)

> Hi all 👋 A quick ask as we start Cohort 1. The HackCanton devnet node no longer
> accepts our user, so CrossDesk is standing up its **own DevNet validator** (Splice 0.8.1,
> GCP).
>
> DevNet needs our egress IP on the **Super Validator allowlist**: **34.71.67.227** (DevNet
> only). We've submitted the Canton Foundation Validator Request and are filing their DevNet
> IP whitelisting form. Allowlisting usually takes 2–7 days.
>
> Could anyone here **sponsor it, or forward it to a contact at a DevNet SV**? That would
> be GSF, Digital Asset, Cumberland, C7, Five North, Proof Group, SV-Nodeops, Tradeweb, MPCH,
> LCV or Orb1. An intro is plenty. We'll confirm with the docs' Scan/sequencer check which
> SVs have picked it up.
>
> In the meantime, everything already runs green on Splice LocalNet: sealed auction,
> committee NAV, in-kind create/redeem, and atomic DvP with a must-fail case. The day the IP
> is live, it moves to DevNet with a config switch. Thanks! 🙏
