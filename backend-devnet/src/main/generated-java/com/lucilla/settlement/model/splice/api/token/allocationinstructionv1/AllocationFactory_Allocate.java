package com.lucilla.settlement.model.splice.api.token.allocationinstructionv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
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
import com.lucilla.settlement.model.splice.api.token.allocationv1.AllocationSpecification;
import com.lucilla.settlement.model.splice.api.token.holdingv1.Holding;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AllocationFactory_Allocate extends DamlRecord<AllocationFactory_Allocate> {
  public static final String _packageId = "275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520";

  public final String expectedAdmin;

  public final AllocationSpecification allocation;

  public final Instant requestedAt;

  public final List<Holding.ContractId> inputHoldingCids;

  public final ExtraArgs extraArgs;

  public AllocationFactory_Allocate(String expectedAdmin, AllocationSpecification allocation,
      Instant requestedAt, List<Holding.ContractId> inputHoldingCids, ExtraArgs extraArgs) {
    this.expectedAdmin = expectedAdmin;
    this.allocation = allocation;
    this.requestedAt = requestedAt;
    this.inputHoldingCids = inputHoldingCids;
    this.extraArgs = extraArgs;
  }

  public static ValueDecoder<AllocationFactory_Allocate> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0,
          recordValue$);
      String expectedAdmin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      AllocationSpecification allocation = AllocationSpecification.valueDecoder()
          .decode(fields$.get(1).getValue());
      Instant requestedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(2).getValue());
      List<Holding.ContractId> inputHoldingCids = PrimitiveValueDecoders.fromList(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected inputHoldingCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(3).getValue());
      ExtraArgs extraArgs = ExtraArgs.valueDecoder().decode(fields$.get(4).getValue());
      return new AllocationFactory_Allocate(expectedAdmin, allocation, requestedAt,
          inputHoldingCids, extraArgs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(5);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("expectedAdmin", new Party(this.expectedAdmin)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("allocation", this.allocation.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("requestedAt", Timestamp.fromInstant(this.requestedAt)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("inputHoldingCids", this.inputHoldingCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("extraArgs", this.extraArgs.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AllocationFactory_Allocate> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("expectedAdmin", "allocation", "requestedAt", "inputHoldingCids", "extraArgs"), name -> {
          switch (name) {
            case "expectedAdmin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "allocation": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.splice.api.token.allocationv1.AllocationSpecification.JsonDecoder$().get());
            case "requestedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "inputHoldingCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId::new)));
            case "extraArgs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, new com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new AllocationFactory_Allocate(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static AllocationFactory_Allocate fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("expectedAdmin", apply(JsonLfEncoders::party, expectedAdmin)),
        JsonLfEncoders.Field.of("allocation", apply(AllocationSpecification::jsonEncoder, allocation)),
        JsonLfEncoders.Field.of("requestedAt", apply(JsonLfEncoders::timestamp, requestedAt)),
        JsonLfEncoders.Field.of("inputHoldingCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), inputHoldingCids)),
        JsonLfEncoders.Field.of("extraArgs", apply(ExtraArgs::jsonEncoder, extraArgs)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AllocationFactory_Allocate)) {
      return false;
    }
    AllocationFactory_Allocate other = (AllocationFactory_Allocate) object;
    return Objects.equals(this.expectedAdmin, other.expectedAdmin) &&
        Objects.equals(this.allocation, other.allocation) &&
        Objects.equals(this.requestedAt, other.requestedAt) &&
        Objects.equals(this.inputHoldingCids, other.inputHoldingCids) &&
        Objects.equals(this.extraArgs, other.extraArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expectedAdmin, this.allocation, this.requestedAt,
        this.inputHoldingCids, this.extraArgs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationFactory_Allocate(%s, %s, %s, %s, %s)",
        this.expectedAdmin, this.allocation, this.requestedAt, this.inputHoldingCids,
        this.extraArgs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AllocationFactory_Allocate> get() {
      return jsonDecoder();
    }
  }
}
