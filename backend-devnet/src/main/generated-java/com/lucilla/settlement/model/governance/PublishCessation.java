package com.lucilla.settlement.model.governance;

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

public class PublishCessation extends DamlRecord<PublishCessation> {
  public static final String _packageId = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public final String instrumentId;

  public final String session;

  public final Instant finalStrike;

  public final Optional<String> successor;

  public final String reason;

  public final List<String> notifyTo;

  public PublishCessation(String instrumentId, String session, Instant finalStrike,
      Optional<String> successor, String reason, List<String> notifyTo) {
    this.instrumentId = instrumentId;
    this.session = session;
    this.finalStrike = finalStrike;
    this.successor = successor;
    this.reason = reason;
    this.notifyTo = notifyTo;
  }

  public static ValueDecoder<PublishCessation> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(6,0,
          recordValue$);
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(0).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      Instant finalStrike = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(2).getValue());
      Optional<String> successor = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromText).decode(fields$.get(3).getValue());
      String reason = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      List<String> notifyTo = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(5).getValue());
      return new PublishCessation(instrumentId, session, finalStrike, successor, reason, notifyTo);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(6);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("session", new Text(this.session)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("finalStrike", Timestamp.fromInstant(this.finalStrike)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("successor", DamlOptional.of(this.successor.map(v$0 -> new Text(v$0)))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("reason", new Text(this.reason)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("notifyTo", this.notifyTo.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<PublishCessation> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("instrumentId", "session", "finalStrike", "successor", "reason", "notifyTo"), name -> {
          switch (name) {
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "finalStrike": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "successor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text), java.util.Optional.empty());
            case "reason": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "notifyTo": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            default: return null;
          }
        }
        , (Object[] args) -> new PublishCessation(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5])));
  }

  public static PublishCessation fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("finalStrike", apply(JsonLfEncoders::timestamp, finalStrike)),
        JsonLfEncoders.Field.of("successor", apply(JsonLfEncoders.optional(JsonLfEncoders::text), successor)),
        JsonLfEncoders.Field.of("reason", apply(JsonLfEncoders::text, reason)),
        JsonLfEncoders.Field.of("notifyTo", apply(JsonLfEncoders.list(JsonLfEncoders::party), notifyTo)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof PublishCessation)) {
      return false;
    }
    PublishCessation other = (PublishCessation) object;
    return Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.session, other.session) &&
        Objects.equals(this.finalStrike, other.finalStrike) &&
        Objects.equals(this.successor, other.successor) &&
        Objects.equals(this.reason, other.reason) && Objects.equals(this.notifyTo, other.notifyTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.instrumentId, this.session, this.finalStrike, this.successor,
        this.reason, this.notifyTo);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.PublishCessation(%s, %s, %s, %s, %s, %s)",
        this.instrumentId, this.session, this.finalStrike, this.successor, this.reason,
        this.notifyTo);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PublishCessation> get() {
      return jsonDecoder();
    }
  }
}
