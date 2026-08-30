package com.lucilla.settlement.model.perpetual;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlOptional;
import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.holding.Holding;
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

public class SetPoolAndInterest extends DamlRecord<SetPoolAndInterest> {
  public static final String _packageId = "527a2b50430ceabba40484b4518c4d390781e8db6c016ab3ec5528eea36766ea";

  public final Optional<Holding.ContractId> newPool;

  public final PositionSide closedSide;

  public final BigDecimal closedSize;

  public SetPoolAndInterest(Optional<Holding.ContractId> newPool, PositionSide closedSide,
      BigDecimal closedSize) {
    this.newPool = newPool;
    this.closedSide = closedSide;
    this.closedSize = closedSize;
  }

  public static ValueDecoder<SetPoolAndInterest> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      Optional<Holding.ContractId> newPool = PrimitiveValueDecoders.fromOptional(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected newPool to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      PositionSide closedSide = PositionSide.valueDecoder().decode(fields$.get(1).getValue());
      BigDecimal closedSize = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      return new SetPoolAndInterest(newPool, closedSide, closedSize);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("newPool", DamlOptional.of(this.newPool.map(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("closedSide", this.closedSide.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("closedSize", new Numeric(this.closedSize)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<SetPoolAndInterest> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("newPool", "closedSide", "closedSize"), name -> {
          switch (name) {
            case "newPool": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new)), java.util.Optional.empty());
            case "closedSide": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.perpetual.PositionSide.JsonDecoder$().get());
            case "closedSize": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new SetPoolAndInterest(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static SetPoolAndInterest fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("newPool", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), newPool)),
        JsonLfEncoders.Field.of("closedSide", apply(PositionSide::jsonEncoder, closedSide)),
        JsonLfEncoders.Field.of("closedSize", apply(JsonLfEncoders::numeric, closedSize)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof SetPoolAndInterest)) {
      return false;
    }
    SetPoolAndInterest other = (SetPoolAndInterest) object;
    return Objects.equals(this.newPool, other.newPool) &&
        Objects.equals(this.closedSide, other.closedSide) &&
        Objects.equals(this.closedSize, other.closedSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.newPool, this.closedSide, this.closedSize);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.perpetual.SetPoolAndInterest(%s, %s, %s)",
        this.newPool, this.closedSide, this.closedSize);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<SetPoolAndInterest> get() {
      return jsonDecoder();
    }
  }
}
