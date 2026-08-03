package com.lucilla.settlement.model.splice.api.token.holdingv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlOptional;
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
import com.lucilla.settlement.model.da.time.types.RelTime;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Lock extends DamlRecord<Lock> {
  public static final String _packageId = "718a0f77e505a8de22f188bd4c87fe74101274e9d4cb1bfac7d09aec7158d35b";

  public final List<String> holders;

  public final Optional<Instant> expiresAt;

  public final Optional<RelTime> expiresAfter;

  public final Optional<String> context;

  public Lock(List<String> holders, Optional<Instant> expiresAt, Optional<RelTime> expiresAfter,
      Optional<String> context) {
    this.holders = holders;
    this.expiresAt = expiresAt;
    this.expiresAfter = expiresAfter;
    this.context = context;
  }

  public static ValueDecoder<Lock> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(4,3,
          recordValue$);
      List<String> holders = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(0).getValue());
      Optional<Instant> expiresAt = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromTimestamp).decode(fields$.get(1).getValue());
      Optional<RelTime> expiresAfter = PrimitiveValueDecoders.fromOptional(RelTime.valueDecoder())
          .decode(fields$.get(2).getValue());
      Optional<String> context = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromText).decode(fields$.get(3).getValue());
      return new Lock(holders, expiresAt, expiresAfter, context);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(4);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("holders", this.holders.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("expiresAt", DamlOptional.of(this.expiresAt.map(v$0 -> Timestamp.fromInstant(v$0)))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("expiresAfter", DamlOptional.of(this.expiresAfter.map(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("context", DamlOptional.of(this.context.map(v$0 -> new Text(v$0)))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Lock> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("holders", "expiresAt", "expiresAfter", "context"), name -> {
          switch (name) {
            case "holders": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "expiresAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp), java.util.Optional.empty());
            case "expiresAfter": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(new com.lucilla.settlement.model.da.time.types.RelTime.JsonDecoder$().get()), java.util.Optional.empty());
            case "context": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text), java.util.Optional.empty());
            default: return null;
          }
        }
        , (Object[] args) -> new Lock(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3])));
  }

  public static Lock fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("holders", apply(JsonLfEncoders.list(JsonLfEncoders::party), holders)),
        JsonLfEncoders.Field.of("expiresAt", apply(JsonLfEncoders.optional(JsonLfEncoders::timestamp), expiresAt)),
        JsonLfEncoders.Field.of("expiresAfter", apply(JsonLfEncoders.optional(RelTime::jsonEncoder), expiresAfter)),
        JsonLfEncoders.Field.of("context", apply(JsonLfEncoders.optional(JsonLfEncoders::text), context)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof Lock)) {
      return false;
    }
    Lock other = (Lock) object;
    return Objects.equals(this.holders, other.holders) &&
        Objects.equals(this.expiresAt, other.expiresAt) &&
        Objects.equals(this.expiresAfter, other.expiresAfter) &&
        Objects.equals(this.context, other.context);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.holders, this.expiresAt, this.expiresAfter, this.context);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.holdingv1.Lock(%s, %s, %s, %s)",
        this.holders, this.expiresAt, this.expiresAfter, this.context);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Lock> get() {
      return jsonDecoder();
    }
  }
}
