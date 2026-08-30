package com.lucilla.settlement.model.tokensettlement;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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
import java.util.Objects;

public class MatchSettlement_Settle extends DamlRecord<MatchSettlement_Settle> {
  public static final String _packageId = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public final Allocation.ContractId assetAllocation;

  public final Allocation.ContractId cashAllocation;

  public final ExtraArgs assetArgs;

  public final ExtraArgs cashArgs;

  public MatchSettlement_Settle(Allocation.ContractId assetAllocation,
      Allocation.ContractId cashAllocation, ExtraArgs assetArgs, ExtraArgs cashArgs) {
    this.assetAllocation = assetAllocation;
    this.cashAllocation = cashAllocation;
    this.assetArgs = assetArgs;
    this.cashArgs = cashArgs;
  }

  public static ValueDecoder<MatchSettlement_Settle> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(4,0,
          recordValue$);
      Allocation.ContractId assetAllocation =
          new Allocation.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected assetAllocation to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Allocation.ContractId cashAllocation =
          new Allocation.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected cashAllocation to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      ExtraArgs assetArgs = ExtraArgs.valueDecoder().decode(fields$.get(2).getValue());
      ExtraArgs cashArgs = ExtraArgs.valueDecoder().decode(fields$.get(3).getValue());
      return new MatchSettlement_Settle(assetAllocation, cashAllocation, assetArgs, cashArgs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(4);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("assetAllocation", this.assetAllocation.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashAllocation", this.cashAllocation.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("assetArgs", this.assetArgs.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashArgs", this.cashArgs.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<MatchSettlement_Settle> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("assetAllocation", "cashAllocation", "assetArgs", "cashArgs"), name -> {
          switch (name) {
            case "assetAllocation": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation.ContractId::new));
            case "cashAllocation": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation.ContractId::new));
            case "assetArgs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, new com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs.JsonDecoder$().get());
            case "cashArgs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, new com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new MatchSettlement_Settle(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3])));
  }

  public static MatchSettlement_Settle fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("assetAllocation", apply(JsonLfEncoders::contractId, assetAllocation)),
        JsonLfEncoders.Field.of("cashAllocation", apply(JsonLfEncoders::contractId, cashAllocation)),
        JsonLfEncoders.Field.of("assetArgs", apply(ExtraArgs::jsonEncoder, assetArgs)),
        JsonLfEncoders.Field.of("cashArgs", apply(ExtraArgs::jsonEncoder, cashArgs)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof MatchSettlement_Settle)) {
      return false;
    }
    MatchSettlement_Settle other = (MatchSettlement_Settle) object;
    return Objects.equals(this.assetAllocation, other.assetAllocation) &&
        Objects.equals(this.cashAllocation, other.cashAllocation) &&
        Objects.equals(this.assetArgs, other.assetArgs) &&
        Objects.equals(this.cashArgs, other.cashArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.assetAllocation, this.cashAllocation, this.assetArgs, this.cashArgs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokensettlement.MatchSettlement_Settle(%s, %s, %s, %s)",
        this.assetAllocation, this.cashAllocation, this.assetArgs, this.cashArgs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<MatchSettlement_Settle> get() {
      return jsonDecoder();
    }
  }
}
