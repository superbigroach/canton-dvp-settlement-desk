package com.lucilla.settlement.model.splice.api.token.holdingv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlOptional;
import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HoldingView extends DamlRecord<HoldingView> {
  public static final String _packageId = "718a0f77e505a8de22f188bd4c87fe74101274e9d4cb1bfac7d09aec7158d35b";

  public final String owner;

  public final InstrumentId instrumentId;

  public final BigDecimal amount;

  public final Optional<Lock> lock;

  public final Metadata meta;

  public HoldingView(String owner, InstrumentId instrumentId, BigDecimal amount,
      Optional<Lock> lock, Metadata meta) {
    this.owner = owner;
    this.instrumentId = instrumentId;
    this.amount = amount;
    this.lock = lock;
    this.meta = meta;
  }

  public static ValueDecoder<HoldingView> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0,
          recordValue$);
      String owner = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      InstrumentId instrumentId = InstrumentId.valueDecoder().decode(fields$.get(1).getValue());
      BigDecimal amount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      Optional<Lock> lock = PrimitiveValueDecoders.fromOptional(Lock.valueDecoder())
          .decode(fields$.get(3).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(4).getValue());
      return new HoldingView(owner, instrumentId, amount, lock, meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(5);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("owner", new Party(this.owner)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("instrumentId", this.instrumentId.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("amount", new Numeric(this.amount)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("lock", DamlOptional.of(this.lock.map(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<HoldingView> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("owner", "instrumentId", "amount", "lock", "meta"), name -> {
          switch (name) {
            case "owner": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId.JsonDecoder$().get());
            case "amount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "lock": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(new com.lucilla.settlement.model.splice.api.token.holdingv1.Lock.JsonDecoder$().get()), java.util.Optional.empty());
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new HoldingView(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static HoldingView fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("owner", apply(JsonLfEncoders::party, owner)),
        JsonLfEncoders.Field.of("instrumentId", apply(InstrumentId::jsonEncoder, instrumentId)),
        JsonLfEncoders.Field.of("amount", apply(JsonLfEncoders::numeric, amount)),
        JsonLfEncoders.Field.of("lock", apply(JsonLfEncoders.optional(Lock::jsonEncoder), lock)),
        JsonLfEncoders.Field.of("meta", apply(Metadata::jsonEncoder, meta)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof HoldingView)) {
      return false;
    }
    HoldingView other = (HoldingView) object;
    return Objects.equals(this.owner, other.owner) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.amount, other.amount) && Objects.equals(this.lock, other.lock) &&
        Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.owner, this.instrumentId, this.amount, this.lock, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.holdingv1.HoldingView(%s, %s, %s, %s, %s)",
        this.owner, this.instrumentId, this.amount, this.lock, this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<HoldingView> get() {
      return jsonDecoder();
    }
  }
}
