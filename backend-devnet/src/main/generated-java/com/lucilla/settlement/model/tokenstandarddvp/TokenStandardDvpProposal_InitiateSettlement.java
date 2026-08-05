package com.lucilla.settlement.model.tokenstandarddvp;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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

public class TokenStandardDvpProposal_InitiateSettlement extends DamlRecord<TokenStandardDvpProposal_InitiateSettlement> {
  public static final String _packageId = "d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be";

  public final Instant allocateBefore;

  public final Instant settleBefore;

  public TokenStandardDvpProposal_InitiateSettlement(Instant allocateBefore, Instant settleBefore) {
    this.allocateBefore = allocateBefore;
    this.settleBefore = settleBefore;
  }

  public static ValueDecoder<TokenStandardDvpProposal_InitiateSettlement> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      Instant allocateBefore = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(0).getValue());
      Instant settleBefore = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(1).getValue());
      return new TokenStandardDvpProposal_InitiateSettlement(allocateBefore, settleBefore);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("allocateBefore", Timestamp.fromInstant(this.allocateBefore)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("settleBefore", Timestamp.fromInstant(this.settleBefore)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TokenStandardDvpProposal_InitiateSettlement> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("allocateBefore", "settleBefore"), name -> {
          switch (name) {
            case "allocateBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "settleBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new TokenStandardDvpProposal_InitiateSettlement(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static TokenStandardDvpProposal_InitiateSettlement fromJson(String json) throws
      JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("allocateBefore", apply(JsonLfEncoders::timestamp, allocateBefore)),
        JsonLfEncoders.Field.of("settleBefore", apply(JsonLfEncoders::timestamp, settleBefore)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TokenStandardDvpProposal_InitiateSettlement)) {
      return false;
    }
    TokenStandardDvpProposal_InitiateSettlement other = (TokenStandardDvpProposal_InitiateSettlement) object;
    return Objects.equals(this.allocateBefore, other.allocateBefore) &&
        Objects.equals(this.settleBefore, other.settleBefore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.allocateBefore, this.settleBefore);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokenstandarddvp.TokenStandardDvpProposal_InitiateSettlement(%s, %s)",
        this.allocateBefore, this.settleBefore);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TokenStandardDvpProposal_InitiateSettlement> get() {
      return jsonDecoder();
    }
  }
}
