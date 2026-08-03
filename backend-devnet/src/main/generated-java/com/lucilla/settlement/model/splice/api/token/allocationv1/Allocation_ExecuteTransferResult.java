package com.lucilla.settlement.model.splice.api.token.allocationv1;

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
import com.lucilla.settlement.model.splice.api.token.holdingv1.Holding;
import com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Allocation_ExecuteTransferResult extends DamlRecord<Allocation_ExecuteTransferResult> {
  public static final String _packageId = "93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d";

  public final List<Holding.ContractId> senderHoldingCids;

  public final List<Holding.ContractId> receiverHoldingCids;

  public final Metadata meta;

  public Allocation_ExecuteTransferResult(List<Holding.ContractId> senderHoldingCids,
      List<Holding.ContractId> receiverHoldingCids, Metadata meta) {
    this.senderHoldingCids = senderHoldingCids;
    this.receiverHoldingCids = receiverHoldingCids;
    this.meta = meta;
  }

  public static ValueDecoder<Allocation_ExecuteTransferResult> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      List<Holding.ContractId> senderHoldingCids = PrimitiveValueDecoders.fromList(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected senderHoldingCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      List<Holding.ContractId> receiverHoldingCids = PrimitiveValueDecoders.fromList(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected receiverHoldingCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(1).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(2).getValue());
      return new Allocation_ExecuteTransferResult(senderHoldingCids, receiverHoldingCids, meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("senderHoldingCids", this.senderHoldingCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("receiverHoldingCids", this.receiverHoldingCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Allocation_ExecuteTransferResult> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("senderHoldingCids", "receiverHoldingCids", "meta"), name -> {
          switch (name) {
            case "senderHoldingCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId::new)));
            case "receiverHoldingCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId::new)));
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new Allocation_ExecuteTransferResult(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static Allocation_ExecuteTransferResult fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("senderHoldingCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), senderHoldingCids)),
        JsonLfEncoders.Field.of("receiverHoldingCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), receiverHoldingCids)),
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
    if (!(object instanceof Allocation_ExecuteTransferResult)) {
      return false;
    }
    Allocation_ExecuteTransferResult other = (Allocation_ExecuteTransferResult) object;
    return Objects.equals(this.senderHoldingCids, other.senderHoldingCids) &&
        Objects.equals(this.receiverHoldingCids, other.receiverHoldingCids) &&
        Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.senderHoldingCids, this.receiverHoldingCids, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation_ExecuteTransferResult(%s, %s, %s)",
        this.senderHoldingCids, this.receiverHoldingCids, this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Allocation_ExecuteTransferResult> get() {
      return jsonDecoder();
    }
  }
}
