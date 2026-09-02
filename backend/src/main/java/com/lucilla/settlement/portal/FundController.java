package com.lucilla.settlement.portal;

import com.lucilla.settlement.auth.AuthException;
import com.lucilla.settlement.auth.CurrentUser;
import com.lucilla.settlement.auth.Principal;
import com.lucilla.settlement.auth.Role;
import com.lucilla.settlement.auth.UserRecord;
import com.lucilla.settlement.auth.UserStore;
import com.lucilla.settlement.benchmarks.SeriesService;
import com.lucilla.settlement.ledger.LedgerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code GET /api/fund/{id}/dashboard} — the fund administrator's view
 * (docs/PRODUCT-PLAN.md §5, {@code types.ts FundDashboard}): NAV series, shares
 * outstanding, the create/redeem log, fee accruals, and licensees.
 *
 * <p>A {@code fund_admin} sees the funds its party administers; an {@code admin} sees all.
 */
@RestController
public class FundController {

    private final LedgerService ledger;
    private final SeriesService series;
    private final ApController ap;
    private final UserStore users;

    public FundController(LedgerService ledger, SeriesService series, ApController ap, UserStore users) {
        this.ledger = ledger;
        this.series = series;
        this.ap = ap;
        this.users = users;
    }

    @GetMapping("/api/fund/{id}/dashboard")
    public Map<String, Object> dashboard(HttpServletRequest req, @PathVariable String id) {
        Principal me = CurrentUser.require(req);
        var b = ap.basketOr404(id);
        String adminLabel = LedgerService.labelOf(b.administrator());
        if (!me.isAdmin()) {
            if (!me.hasParty() || !LedgerService.labelOf(me.party()).equalsIgnoreCase(adminLabel)) {
                throw AuthException.forbidden("fund " + id + " is administered by " + adminLabel
                        + "; your party is " + me.party());
            }
        }
        String administrator = b.administrator();
        Map<String, Object> fund = ap.fundView(b, administrator);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", b.basketId());
        out.put("name", b.description());
        out.put("cash", b.cashInstrument());
        out.put("fund", fund);
        out.put("series", series.series(id));
        out.put("navSeries", out.get("series"));
        out.put("components", fund.get("components"));
        out.put("official", fund.get("official"));
        out.put("indicative", fund.get("indicative"));

        // The fund's share record is issued by its administrator, so every share holding
        // is visible from there — that sum IS the shares outstanding.
        List<Map<String, Object>> holders = new ArrayList<>();
        BigDecimal outstanding = BigDecimal.ZERO;
        for (var h : ledger.holdingsVisibleTo(administrator)) {
            if (!h.instrumentId().equals(id)) continue;
            outstanding = outstanding.add(h.amount());
            holders.add(Map.of("owner", LedgerService.labelOf(h.owner()), "shares", h.amount(),
                    "holdingCid", h.contractId()));
        }
        out.put("sharesOutstanding", outstanding);
        out.put("holders", holders);

        List<Map<String, Object>> log = new ArrayList<>();
        List<Map<String, Object>> feeRows = new ArrayList<>();
        BigDecimal creationFees = BigDecimal.ZERO, redemptionFees = BigDecimal.ZERO;
        BigDecimal created = BigDecimal.ZERO, redeemed = BigDecimal.ZERO;
        Map<String, BigDecimal> marks = new LinkedHashMap<>();
        for (var r : ledger.basketReceiptsVisibleTo(administrator)) {
            if (!r.basketId().equals(id)) continue;
            log.add(ap.receiptView(r, marks));
            boolean creation = "Creation".equalsIgnoreCase(r.action());
            if (creation) created = created.add(r.shares()); else redeemed = redeemed.add(r.shares());
            if (r.fee() != null && r.fee().signum() > 0) {
                if (creation) creationFees = creationFees.add(r.fee()); else redemptionFees = redemptionFees.add(r.fee());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date", r.settledAt().toString().substring(0, 10));
                row.put("kind", creation ? "create" : "redeem");
                row.put("amount", r.fee());
                row.put("ref", r.contractId());
                feeRows.add(row);
            }
        }
        out.put("log", log);
        out.put("createRedeemLog", log);
        Map<String, Object> fees = new LinkedHashMap<>();
        fees.put("accrued", creationFees.add(redemptionFees));
        fees.put("currency", b.cashInstrument());
        fees.put("rows", feeRows);
        fees.put("creation", creationFees);
        fees.put("redemption", redemptionFees);
        fees.put("sharesCreated", created);
        fees.put("sharesRedeemed", redeemed);
        out.put("fees", fees);
        out.put("feeAccruals", fees);

        // Licensees: the parties entitled to reference the NAV. Until a licence register
        // exists this is the fund's APs plus its administrator — the parties that settle
        // against the number — and it is labelled as such rather than invented.
        List<Map<String, Object>> licensees = new ArrayList<>();
        for (String p : b.participants()) {
            licensees.add(licensee(p, "authorised participant"));
        }
        licensees.add(licensee(administrator, "administrator"));
        out.put("licensees", licensees);
        out.put("licenseesNote", "derived from the settlement roster; no licence register exists yet");
        return out;
    }

    private Map<String, Object> licensee(String party, String basis) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<UserRecord> us = users.byParty(party);
        String org = us.stream().map(UserRecord::getOrg).filter(o -> o != null).findFirst().orElse(null);
        m.put("name", org == null ? LedgerService.labelOf(party) : org);
        m.put("kind", basis);
        m.put("party", LedgerService.labelOf(party));
        m.put("basis", basis);
        m.put("org", org);
        m.put("users", us.stream().filter(u -> u.roleEnum() != Role.ADMIN).map(UserRecord::getEmail).toList());
        return m;
    }
}
