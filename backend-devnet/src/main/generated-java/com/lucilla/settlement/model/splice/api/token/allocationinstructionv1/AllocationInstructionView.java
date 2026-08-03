package com.lucilla.settlement.model.splice.api.token.allocationinstructionv1;

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
import com.lucilla.settlement.model.splice.api.token.allocationv1.AllocationSpecification;
import com.lucilla.settlement.model.splice.api.token.holdingv1.Holding;
import com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AllocationInstructionView extends DamlRecord<AllocationInstructionView> {
  public static final String _packageId = "275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520";

  public final Optional<AllocationInstruction.ContractId> originalInstructionCid;

  public final AllocationSpecification allocation;

  public final Map<String, String> pendingActions;

  public final Instant requestedAt;

  public final List<Holding.ContractId> inputHoldingCids;

  public final Metadata meta;

  public AllocationInstructionView(
      Optional<AllocationInstruction.ContractId> originalInstructionCid,
      AllocationSpecification allocation, Map<String, String> pendingActions, Instant requestedAt,
      List<Holding.ContractId> inputHoldingCids, Metadata meta) {
    this.originalInstructionCid = originalInstructionCid;
    this.allocation = allocation;
    this.pendingActions = pendingActions;
    this.requestedAt = requestedAt;
    this.inputHoldingCids = inputHoldingCids;
    this.meta = meta;
  }

  public static ValueDecoder<AllocationInstructionView> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(6,0,
          recordValue$);
      Optional<AllocationInstruction.ContractId> originalInstructionCid =
          PrimitiveValueDecoders.fromOptional(v$0 ->
              new AllocationInstruction.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected originalInstructionCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      AllocationSpecification allocation = AllocationSpecification.valueDecoder()
          .decode(fields$.get(1).getValue());
      Map<String, String> pendingActions = PrimitiveValueDecoders.fromGenMap(
            PrimitiveValueDecoders.fromParty, PrimitiveValueDecoders.fromText)
          .decode(fields$.get(2).getValue());
      Instant requestedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(3).getValue());
      List<Holding.ContractId> inputHoldingCids = PrimitiveValueDecoders.fromList(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected inputHoldingCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(4).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(5).getValue());
      return new AllocationInstructionView(originalInstructionCid, allocation, pendingActions,
          requestedAt, inputHoldingCids, meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(6);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("originalInstructionCid", DamlOptional.of(this.originalInstructionCid.map(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("allocation", this.allocation.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("pendingActions", this.pendingActions.entrySet()
        .stream()
        .collect(DamlCollectors.toDamlGenMap(v$0 -> new Party(v$0.getKey()), v$0 -> new Text(v$0.getValue())))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("requestedAt", Timestamp.fromInstant(this.requestedAt)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("inputHoldingCids", this.inputHoldingCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AllocationInstructionView> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("originalInstructionCid", "allocation", "pendingActions", "requestedAt", "inputHoldingCids", "meta"), name -> {
          switch (name) {
            case "originalInstructionCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstruction.ContractId::new)), java.util.Optional.empty());
            case "allocation": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.splice.api.token.allocationv1.AllocationSpecification.JsonDecoder$().get());
            case "pendingActions": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.genMap(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text));
            case "requestedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "inputHoldingCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId::new)));
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new AllocationInstructionView(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5])));
  }

  public static AllocationInstructionView fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("originalInstructionCid", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), originalInstructionCid)),
        JsonLfEncoders.Field.of("allocation", apply(AllocationSpecification::jsonEncoder, allocation)),
        JsonLfEncoders.Field.of("pendingActions", apply(JsonLfEncoders.genMap(JsonLfEncoders::party, JsonLfEncoders::text), pendingActions)),
        JsonLfEncoders.Field.of("requestedAt", apply(JsonLfEncoders::timestamp, requestedAt)),
        JsonLfEncoders.Field.of("inputHoldingCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), inputHoldingCids)),
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
    if (!(object instanceof AllocationInstructionView)) {
      return false;
    }
    AllocationInstructionView other = (AllocationInstructionView) object;
    return Objects.equals(this.originalInstructionCid, other.originalInstructionCid) &&
        Objects.equals(this.allocation, other.allocation) &&
        Objects.equals(this.pendingActions, other.pendingActions) &&
        Objects.equals(this.requestedAt, other.requestedAt) &&
        Objects.equals(this.inputHoldingCids, other.inputHoldingCids) &&
        Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.originalInstructionCid, this.allocation, this.pendingActions,
        this.requestedAt, this.inputHoldingCids, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstructionView(%s, %s, %s, %s, %s, %s)",
        this.originalInstructionCid, this.allocation, this.pendingActions, this.requestedAt,
        this.inputHoldingCids, this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AllocationInstructionView> get() {
      return jsonDecoder();
    }
  }
}
