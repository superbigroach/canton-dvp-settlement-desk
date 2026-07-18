package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ProposeFixing extends DamlRecord<ProposeFixing> {
  public static final String _packageId = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public final String proposer;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal price;

  public final String rationale;

  public ProposeFixing(String proposer, String instrumentId, String cashInstrument, String session,
      BigDecimal price, String rationale) {
    this.proposer = proposer;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.price = price;
    this.rationale = rationale;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static ProposeFixing fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<ProposeFixing> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(6,0,
          recordValue$);
      String proposer = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      BigDecimal price = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(4).getValue());
      String rationale = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      return new ProposeFixing(proposer, instrumentId, cashInstrument, session, price, rationale);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(6);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("proposer", new Party(this.proposer)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("session", new Text(this.session)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("price", new Numeric(this.price)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("rationale", new Text(this.rationale)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<ProposeFixing> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("proposer", "instrumentId", "cashInstrument", "session", "price", "rationale"), name -> {
          switch (name) {
            case "proposer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "price": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "rationale": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            default: return null;
          }
        }
        , (Object[] args) -> new ProposeFixing(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5])));
  }

  public static ProposeFixing fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("proposer", apply(JsonLfEncoders::party, proposer)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("price", apply(JsonLfEncoders::numeric, price)),
        JsonLfEncoders.Field.of("rationale", apply(JsonLfEncoders::text, rationale)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof ProposeFixing)) {
      return false;
    }
    ProposeFixing other = (ProposeFixing) object;
    return Objects.equals(this.proposer, other.proposer) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) && Objects.equals(this.price, other.price) &&
        Objects.equals(this.rationale, other.rationale);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.proposer, this.instrumentId, this.cashInstrument, this.session,
        this.price, this.rationale);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.ProposeFixing(%s, %s, %s, %s, %s, %s)",
        this.proposer, this.instrumentId, this.cashInstrument, this.session, this.price,
        this.rationale);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ProposeFixing> get() {
      return jsonDecoder();
    }
  }
}
