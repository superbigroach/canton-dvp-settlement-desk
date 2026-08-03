package com.lucilla.settlement.model.splice.api.token.allocationv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Party;
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
import com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SettlementInfo extends DamlRecord<SettlementInfo> {
  public static final String _packageId = "93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d";

  public final String executor;

  public final Reference settlementRef;

  public final Instant requestedAt;

  public final Instant allocateBefore;

  public final Instant settleBefore;

  public final Metadata meta;

  public SettlementInfo(String executor, Reference settlementRef, Instant requestedAt,
      Instant allocateBefore, Instant settleBefore, Metadata meta) {
    this.executor = executor;
    this.settlementRef = settlementRef;
    this.requestedAt = requestedAt;
    this.allocateBefore = allocateBefore;
    this.settleBefore = settleBefore;
    this.meta = meta;
  }

  public static ValueDecoder<SettlementInfo> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(6,0,
          recordValue$);
      String executor = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      Reference settlementRef = Reference.valueDecoder().decode(fields$.get(1).getValue());
      Instant requestedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(2).getValue());
      Instant allocateBefore = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(3).getValue());
      Instant settleBefore = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(4).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(5).getValue());
      return new SettlementInfo(executor, settlementRef, requestedAt, allocateBefore, settleBefore,
          meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(6);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("executor", new Party(this.executor)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("settlementRef", this.settlementRef.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("requestedAt", Timestamp.fromInstant(this.requestedAt)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("allocateBefore", Timestamp.fromInstant(this.allocateBefore)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("settleBefore", Timestamp.fromInstant(this.settleBefore)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<SettlementInfo> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("executor", "settlementRef", "requestedAt", "allocateBefore", "settleBefore", "meta"), name -> {
          switch (name) {
            case "executor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "settlementRef": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.splice.api.token.allocationv1.Reference.JsonDecoder$().get());
            case "requestedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "allocateBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "settleBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new SettlementInfo(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5])));
  }

  public static SettlementInfo fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("executor", apply(JsonLfEncoders::party, executor)),
        JsonLfEncoders.Field.of("settlementRef", apply(Reference::jsonEncoder, settlementRef)),
        JsonLfEncoders.Field.of("requestedAt", apply(JsonLfEncoders::timestamp, requestedAt)),
        JsonLfEncoders.Field.of("allocateBefore", apply(JsonLfEncoders::timestamp, allocateBefore)),
        JsonLfEncoders.Field.of("settleBefore", apply(JsonLfEncoders::timestamp, settleBefore)),
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
    if (!(object instanceof SettlementInfo)) {
      return false;
    }
    SettlementInfo other = (SettlementInfo) object;
    return Objects.equals(this.executor, other.executor) &&
        Objects.equals(this.settlementRef, other.settlementRef) &&
        Objects.equals(this.requestedAt, other.requestedAt) &&
        Objects.equals(this.allocateBefore, other.allocateBefore) &&
        Objects.equals(this.settleBefore, other.settleBefore) &&
        Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.executor, this.settlementRef, this.requestedAt, this.allocateBefore,
        this.settleBefore, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationv1.SettlementInfo(%s, %s, %s, %s, %s, %s)",
        this.executor, this.settlementRef, this.requestedAt, this.allocateBefore, this.settleBefore,
        this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<SettlementInfo> get() {
      return jsonDecoder();
    }
  }
}
