package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Timestamp;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ProposeAccruingFixing extends DamlRecord<ProposeAccruingFixing> {
  public static final String _packageId = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public final String proposer;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal price;

  public final String rationale;

  public final BigDecimal ratePerAnnum;

  public final String dayCount;

  public final Instant accrualFrom;

  public ProposeAccruingFixing(String proposer, String instrumentId, String cashInstrument,
      String session, BigDecimal price, String rationale, BigDecimal ratePerAnnum, String dayCount,
      Instant accrualFrom) {
    this.proposer = proposer;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.price = price;
    this.rationale = rationale;
    this.ratePerAnnum = ratePerAnnum;
    this.dayCount = dayCount;
    this.accrualFrom = accrualFrom;
  }

  public static ValueDecoder<ProposeAccruingFixing> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(9,0,
          recordValue$);
      String proposer = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      BigDecimal price = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(4).getValue());
      String rationale = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      BigDecimal ratePerAnnum = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(6).getValue());
      String dayCount = PrimitiveValueDecoders.fromText.decode(fields$.get(7).getValue());
      Instant accrualFrom = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(8).getValue());
      return new ProposeAccruingFixing(proposer, instrumentId, cashInstrument, session, price,
          rationale, ratePerAnnum, dayCount, accrualFrom);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(9);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("proposer", new Party(this.proposer)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("session", new Text(this.session)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("price", new Numeric(this.price)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("rationale", new Text(this.rationale)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("ratePerAnnum", new Numeric(this.ratePerAnnum)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("dayCount", new Text(this.dayCount)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("accrualFrom", Timestamp.fromInstant(this.accrualFrom)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<ProposeAccruingFixing> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("proposer", "instrumentId", "cashInstrument", "session", "price", "rationale", "ratePerAnnum", "dayCount", "accrualFrom"), name -> {
          switch (name) {
            case "proposer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "price": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "rationale": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "ratePerAnnum": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "dayCount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "accrualFrom": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new ProposeAccruingFixing(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8])));
  }

  public static ProposeAccruingFixing fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("proposer", apply(JsonLfEncoders::party, proposer)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("price", apply(JsonLfEncoders::numeric, price)),
        JsonLfEncoders.Field.of("rationale", apply(JsonLfEncoders::text, rationale)),
        JsonLfEncoders.Field.of("ratePerAnnum", apply(JsonLfEncoders::numeric, ratePerAnnum)),
        JsonLfEncoders.Field.of("dayCount", apply(JsonLfEncoders::text, dayCount)),
        JsonLfEncoders.Field.of("accrualFrom", apply(JsonLfEncoders::timestamp, accrualFrom)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof ProposeAccruingFixing)) {
      return false;
    }
    ProposeAccruingFixing other = (ProposeAccruingFixing) object;
    return Objects.equals(this.proposer, other.proposer) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) && Objects.equals(this.price, other.price) &&
        Objects.equals(this.rationale, other.rationale) &&
        Objects.equals(this.ratePerAnnum, other.ratePerAnnum) &&
        Objects.equals(this.dayCount, other.dayCount) &&
        Objects.equals(this.accrualFrom, other.accrualFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.proposer, this.instrumentId, this.cashInstrument, this.session,
        this.price, this.rationale, this.ratePerAnnum, this.dayCount, this.accrualFrom);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.ProposeAccruingFixing(%s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.proposer, this.instrumentId, this.cashInstrument, this.session, this.price,
        this.rationale, this.ratePerAnnum, this.dayCount, this.accrualFrom);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ProposeAccruingFixing> get() {
      return jsonDecoder();
    }
  }
}
