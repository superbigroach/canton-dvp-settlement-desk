package com.lucilla.settlement.model.splice.api.token.transferinstructionv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlOptional;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TransferInstructionView extends DamlRecord<TransferInstructionView> {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final Optional<TransferInstruction.ContractId> originalInstructionCid;

  public final Transfer transfer;

  public final TransferInstructionStatus status;

  public final Metadata meta;

  public TransferInstructionView(Optional<TransferInstruction.ContractId> originalInstructionCid,
      Transfer transfer, TransferInstructionStatus status, Metadata meta) {
    this.originalInstructionCid = originalInstructionCid;
    this.transfer = transfer;
    this.status = status;
    this.meta = meta;
  }

  public static ValueDecoder<TransferInstructionView> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(4,0,
          recordValue$);
      Optional<TransferInstruction.ContractId> originalInstructionCid =
          PrimitiveValueDecoders.fromOptional(v$0 ->
              new TransferInstruction.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected originalInstructionCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      Transfer transfer = Transfer.valueDecoder().decode(fields$.get(1).getValue());
      TransferInstructionStatus status = TransferInstructionStatus.valueDecoder()
          .decode(fields$.get(2).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(3).getValue());
      return new TransferInstructionView(originalInstructionCid, transfer, status, meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(4);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("originalInstructionCid", DamlOptional.of(this.originalInstructionCid.map(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("transfer", this.transfer.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("status", this.status.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TransferInstructionView> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("originalInstructionCid", "transfer", "status", "meta"), name -> {
          switch (name) {
            case "originalInstructionCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstruction.ContractId::new)), java.util.Optional.empty());
            case "transfer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.splice.api.token.transferinstructionv1.Transfer.JsonDecoder$().get());
            case "status": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, new com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionStatus.JsonDecoder$().get());
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new TransferInstructionView(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3])));
  }

  public static TransferInstructionView fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("originalInstructionCid", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), originalInstructionCid)),
        JsonLfEncoders.Field.of("transfer", apply(Transfer::jsonEncoder, transfer)),
        JsonLfEncoders.Field.of("status", apply(TransferInstructionStatus::jsonEncoder, status)),
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
    if (!(object instanceof TransferInstructionView)) {
      return false;
    }
    TransferInstructionView other = (TransferInstructionView) object;
    return Objects.equals(this.originalInstructionCid, other.originalInstructionCid) &&
        Objects.equals(this.transfer, other.transfer) &&
        Objects.equals(this.status, other.status) && Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.originalInstructionCid, this.transfer, this.status, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionView(%s, %s, %s, %s)",
        this.originalInstructionCid, this.transfer, this.status, this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TransferInstructionView> get() {
      return jsonDecoder();
    }
  }
}
