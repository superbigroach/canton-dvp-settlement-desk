package com.lucilla.settlement.signing;

import com.lucilla.settlement.auth.CurrentUser;
import com.lucilla.settlement.auth.Principal;
import com.lucilla.settlement.events.FixingEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** {@code /api/proposals} — the signer portal's routes (docs/PRODUCT-PLAN.md §5). */
@RestController
public class ProposalController {

    private final ProposalService proposals;

    public ProposalController(ProposalService proposals) {
        this.proposals = proposals;
    }

    /** {@code ?status=open|all&mine=true} — proposals for my instruments, with my conditions and what I did. */
    @GetMapping("/api/proposals")
    public List<Map<String, Object>> list(HttpServletRequest req,
            @RequestParam(required = false, defaultValue = "open") String status,
            @RequestParam(required = false, defaultValue = "true") boolean mine) {
        Principal me = CurrentUser.requireParty(req);
        return proposals.list(me, status, mine);
    }

    @GetMapping("/api/proposals/{cid}")
    public Map<String, Object> one(HttpServletRequest req, @PathVariable String cid) {
        Principal me = CurrentUser.requireParty(req);
        return proposals.one(me, cid).orElseThrow(() ->
                new com.lucilla.settlement.web.ApiExceptionHandler.NotFound("no open proposal " + cid
                        + " is visible to " + me.party()));
    }

    public record ConfirmRequest(@NotEmpty List<String> checks, Evidence evidence) {
        public record Evidence(BigDecimal low, BigDecimal high) {
        }
    }

    @PostMapping("/api/proposals/{cid}/confirm")
    public Map<String, Object> confirm(HttpServletRequest req, @PathVariable String cid,
            @RequestBody ConfirmRequest body) {
        Principal me = CurrentUser.requireParty(req);
        if (body == null || body.checks() == null || body.checks().isEmpty()) {
            throw new IllegalArgumentException("checks must name at least one condition you verified");
        }
        BigDecimal low = body.evidence() == null ? null : body.evidence().low();
        BigDecimal high = body.evidence() == null ? null : body.evidence().high();
        return proposals.confirm(me, cid, body.checks(), low, high);
    }

    public record RefuseRequest(String condition, String reason) {
    }

    @PostMapping("/api/proposals/{cid}/refuse")
    public Map<String, Object> refuse(HttpServletRequest req, @PathVariable String cid,
            @RequestBody RefuseRequest body) {
        Principal me = CurrentUser.requireParty(req);
        return proposals.refuse(me, cid, body == null ? null : body.condition(),
                body == null ? null : body.reason());
    }

    /** The message log for one proposal, newest first — whichever of its cids the caller holds. */
    @GetMapping("/api/proposals/{cid}/events")
    public List<FixingEvent> events(HttpServletRequest req, @PathVariable String cid) {
        CurrentUser.require(req);
        return proposals.eventsOf(cid);
    }
}
