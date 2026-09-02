package com.lucilla.settlement.events;

import java.util.List;

/** The audit export as CSV — one row per event, RFC 4180 quoting. */
public final class EventsCsv {

    private EventsCsv() {}

    public static final String HEADER =
            "id,ts,kind,instrument,proposalCid,rootCid,actor,seat,condition,reason,price,tier,ledgerCid";

    public static String render(List<FixingEvent> events) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (FixingEvent e : events) {
            sb.append(e.id()).append(',')
                    .append(q(e.ts())).append(',')
                    .append(q(e.kind())).append(',')
                    .append(q(e.instrument())).append(',')
                    .append(q(e.proposalCid())).append(',')
                    .append(q(e.rootCid())).append(',')
                    .append(q(e.actor())).append(',')
                    .append(q(e.seat())).append(',')
                    .append(q(e.condition())).append(',')
                    .append(q(e.reason())).append(',')
                    .append(e.price() == null ? "" : e.price().stripTrailingZeros().toPlainString()).append(',')
                    .append(e.tier() == null ? "" : e.tier()).append(',')
                    .append(q(e.ledgerCid())).append('\n');
        }
        return sb.toString();
    }

    static String q(String s) {
        if (s == null) return "";
        if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0) return s;
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
