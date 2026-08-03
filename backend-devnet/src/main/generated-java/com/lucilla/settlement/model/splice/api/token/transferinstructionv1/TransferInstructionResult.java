package com.lucilla.settlement.model.splice.api.token.transferinstructionv1;

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

public class TransferInstructionResult extends DamlRecord<TransferInstructionResult> {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final TransferInstructionResult_Output output;

  public final List<Holding.ContractId> senderChangeCids;

  public final Metadata meta;

  public TransferInstructionResult(TransferInstructionResult_Output output,
      List<Holding.ContractId> senderChangeCids, Metadata meta) {
    this.output = output;
    this.senderChangeCids = senderChangeCids;
    this.meta = meta;
  }

  public static ValueDecoder<TransferInstructionResult> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      TransferInstructionResult_Output output = TransferInstructionResult_Output.valueDecoder()
          .decode(fields$.get(0).getValue());
      List<Holding.ContractId> senderChangeCids = PrimitiveValueDecoders.fromList(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected senderChangeCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(1).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(2).getValue());
      return new TransferInstructionResult(output, senderChangeCids, meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("output", this.output.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("senderChangeCids", this.senderChangeCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TransferInstructionResult> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("output", "senderChangeCids", "meta"), name -> {
          switch (name) {
            case "output": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, new com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionResult_Output.JsonDecoder$().get());
            case "senderChangeCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId::new)));
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new TransferInstructionResult(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static TransferInstructionResult fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("output", apply(TransferInstructionResult_Output::jsonEncoder, output)),
        JsonLfEncoders.Field.of("senderChangeCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), senderChangeCids)),
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
    if (!(object instanceof TransferInstructionResult)) {
      return false;
    }
    TransferInstructionResult other = (TransferInstructionResult) object;
    return Objects.equals(this.output, other.output) &&
        Objects.equals(this.senderChangeCids, other.senderChangeCids) &&
        Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.output, this.senderChangeCids, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionResult(%s, %s, %s)",
        this.output, this.senderChangeCids, this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TransferInstructionResult> get() {
      return jsonDecoder();
    }
  }
}
