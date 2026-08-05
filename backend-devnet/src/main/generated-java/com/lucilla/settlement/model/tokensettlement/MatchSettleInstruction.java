package com.lucilla.settlement.model.tokensettlement;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MatchSettleInstruction extends DamlRecord<MatchSettleInstruction> {
  public static final String _packageId = "16b1d7198cf7c7ec9373fe2d1bdb48ab1770fe7ffcb7281ad87048ebecd45ab4";

  public final MatchSettlement.ContractId matchCid;

  public final Allocation.ContractId assetAllocation;

  public final Allocation.ContractId cashAllocation;

  public final Map<String, ExtraArgs> legArgs;

  public MatchSettleInstruction(MatchSettlement.ContractId matchCid,
      Allocation.ContractId assetAllocation, Allocation.ContractId cashAllocation,
      Map<String, ExtraArgs> legArgs) {
    this.matchCid = matchCid;
    this.assetAllocation = assetAllocation;
    this.cashAllocation = cashAllocation;
    this.legArgs = legArgs;
  }

  public static ValueDecoder<MatchSettleInstruction> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(4,0,
          recordValue$);
      MatchSettlement.ContractId matchCid =
          new MatchSettlement.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected matchCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Allocation.ContractId assetAllocation =
          new Allocation.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected assetAllocation to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Allocation.ContractId cashAllocation =
          new Allocation.ContractId(fields$.get(2).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected cashAllocation to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Map<String, ExtraArgs> legArgs = PrimitiveValueDecoders.fromTextMap(ExtraArgs.valueDecoder())
          .decode(fields$.get(3).getValue());
      return new MatchSettleInstruction(matchCid, assetAllocation, cashAllocation, legArgs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(4);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("matchCid", this.matchCid.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("assetAllocation", this.assetAllocation.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashAllocation", this.cashAllocation.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("legArgs", this.legArgs.entrySet()
        .stream()
        .collect(DamlCollectors.toDamlTextMap(Map.Entry::getKey, v$0 -> v$0.getValue().toValue()))
        ));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<MatchSettleInstruction> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("matchCid", "assetAllocation", "cashAllocation", "legArgs"), name -> {
          switch (name) {
            case "matchCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.tokensettlement.MatchSettlement.ContractId::new));
            case "assetAllocation": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation.ContractId::new));
            case "cashAllocation": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation.ContractId::new));
            case "legArgs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.textMap(new com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs.JsonDecoder$().get()));
            default: return null;
          }
        }
        , (Object[] args) -> new MatchSettleInstruction(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3])));
  }

  public static MatchSettleInstruction fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("matchCid", apply(JsonLfEncoders::contractId, matchCid)),
        JsonLfEncoders.Field.of("assetAllocation", apply(JsonLfEncoders::contractId, assetAllocation)),
        JsonLfEncoders.Field.of("cashAllocation", apply(JsonLfEncoders::contractId, cashAllocation)),
        JsonLfEncoders.Field.of("legArgs", apply(JsonLfEncoders.textMap(ExtraArgs::jsonEncoder), legArgs)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof MatchSettleInstruction)) {
      return false;
    }
    MatchSettleInstruction other = (MatchSettleInstruction) object;
    return Objects.equals(this.matchCid, other.matchCid) &&
        Objects.equals(this.assetAllocation, other.assetAllocation) &&
        Objects.equals(this.cashAllocation, other.cashAllocation) &&
        Objects.equals(this.legArgs, other.legArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.matchCid, this.assetAllocation, this.cashAllocation, this.legArgs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokensettlement.MatchSettleInstruction(%s, %s, %s, %s)",
        this.matchCid, this.assetAllocation, this.cashAllocation, this.legArgs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<MatchSettleInstruction> get() {
      return jsonDecoder();
    }
  }
}
