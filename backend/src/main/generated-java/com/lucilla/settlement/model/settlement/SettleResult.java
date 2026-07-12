package com.lucilla.settlement.model.settlement;

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
import com.lucilla.settlement.model.holding.Holding;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SettleResult extends DamlRecord<SettleResult> {
  public static final String _packageId = "a515edc777c604a66696e5991316c6e0500be01c634f1dcd1c118c3a0ad8c9fe";

  public final Holding.ContractId deliveredAsset;

  public final Holding.ContractId deliveredCash;

  public final SettlementReceipt.ContractId receipt;

  public SettleResult(Holding.ContractId deliveredAsset, Holding.ContractId deliveredCash,
      SettlementReceipt.ContractId receipt) {
    this.deliveredAsset = deliveredAsset;
    this.deliveredCash = deliveredCash;
    this.receipt = receipt;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static SettleResult fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<SettleResult> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      Holding.ContractId deliveredAsset =
          new Holding.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected deliveredAsset to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Holding.ContractId deliveredCash =
          new Holding.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected deliveredCash to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      SettlementReceipt.ContractId receipt =
          new SettlementReceipt.ContractId(fields$.get(2).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected receipt to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new SettleResult(deliveredAsset, deliveredCash, receipt);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("deliveredAsset", this.deliveredAsset.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("deliveredCash", this.deliveredCash.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("receipt", this.receipt.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<SettleResult> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("deliveredAsset", "deliveredCash", "receipt"), name -> {
          switch (name) {
            case "deliveredAsset": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            case "deliveredCash": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            case "receipt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.settlement.SettlementReceipt.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new SettleResult(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static SettleResult fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("deliveredAsset", apply(JsonLfEncoders::contractId, deliveredAsset)),
        JsonLfEncoders.Field.of("deliveredCash", apply(JsonLfEncoders::contractId, deliveredCash)),
        JsonLfEncoders.Field.of("receipt", apply(JsonLfEncoders::contractId, receipt)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof SettleResult)) {
      return false;
    }
    SettleResult other = (SettleResult) object;
    return Objects.equals(this.deliveredAsset, other.deliveredAsset) &&
        Objects.equals(this.deliveredCash, other.deliveredCash) &&
        Objects.equals(this.receipt, other.receipt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.deliveredAsset, this.deliveredCash, this.receipt);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.settlement.SettleResult(%s, %s, %s)",
        this.deliveredAsset, this.deliveredCash, this.receipt);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<SettleResult> get() {
      return jsonDecoder();
    }
  }
}
