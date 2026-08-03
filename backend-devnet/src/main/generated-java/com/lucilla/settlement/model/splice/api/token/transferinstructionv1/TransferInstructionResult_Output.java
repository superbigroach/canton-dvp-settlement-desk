package com.lucilla.settlement.model.splice.api.token.transferinstructionv1;

import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.splice.api.token.holdingv1.Holding;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionresult_output.TransferInstructionResult_Completed;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionresult_output.TransferInstructionResult_Failed;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionresult_output.TransferInstructionResult_Pending;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.Arrays;
import java.util.List;

public abstract class TransferInstructionResult_Output extends Variant<TransferInstructionResult_Output> {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public TransferInstructionResult_Output() {
  }

  public abstract com.daml.ledger.javaapi.data.Variant toValue();

  public static ValueDecoder<TransferInstructionResult_Output> valueDecoder() {
    return value$ -> {
      com.daml.ledger.javaapi.data.Variant variant$ = value$.asVariant().orElseThrow(() -> new IllegalArgumentException("Expected Variant to build an instance of the Variant com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionResult_Output"));
      if ("TransferInstructionResult_Pending".equals(variant$.getConstructor())) {
        return valueDecoderTransferInstructionResult_Pending().decode(variant$);
      }
      if ("TransferInstructionResult_Completed".equals(variant$.getConstructor())) {
        return valueDecoderTransferInstructionResult_Completed().decode(variant$);
      }
      if ("TransferInstructionResult_Failed".equals(variant$.getConstructor())) {
        return valueDecoderTransferInstructionResult_Failed().decode(variant$);
      }
      throw new IllegalArgumentException("Found unknown constructor " + variant$.getConstructor() + " for variant com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionResult_Output, expected one of [TransferInstructionResult_Pending, TransferInstructionResult_Completed, TransferInstructionResult_Failed]. This could be a failed variant downgrade.");
    } ;
  }

  public static JsonLfDecoder<TransferInstructionResult_Output> jsonDecoder() {
    return JsonLfDecoders.variant(Arrays.asList("TransferInstructionResult_Pending", "TransferInstructionResult_Completed", "TransferInstructionResult_Failed"), name -> {
          switch (name) {
            case "TransferInstructionResult_Pending": return jsonDecoderTransferInstructionResult_Pending();
            case "TransferInstructionResult_Completed": return jsonDecoderTransferInstructionResult_Completed();
            case "TransferInstructionResult_Failed": return jsonDecoderTransferInstructionResult_Failed();
            default: return null;
          }
        }
        );
  }

  public static TransferInstructionResult_Output fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  protected abstract JsonLfEncoders.Field fieldForJsonEncoder();

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.variant(TransferInstructionResult_Output::fieldForJsonEncoder).apply(this);
  }

  private static ValueDecoder<TransferInstructionResult_Failed> valueDecoderTransferInstructionResult_Failed(
      ) throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("TransferInstructionResult_Failed",
          value$);
      Unit body = PrimitiveValueDecoders.fromUnit.decode(variantValue$);
      return new TransferInstructionResult_Failed(body);
    } ;
  }

  private static JsonLfDecoder<TransferInstructionResult_Failed> jsonDecoderTransferInstructionResult_Failed(
      ) {
    return r -> new TransferInstructionResult_Failed(JsonLfDecoders.unit.decode(r));
  }

  public static ValueDecoder<TransferInstructionResult_Pending> valueDecoderTransferInstructionResult_Pending(
      ) throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = PrimitiveValueDecoders.variantCheck("TransferInstructionResult_Pending",
          value$);
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0, recordValue$);
      TransferInstruction.ContractId transferInstructionCid =
          new TransferInstruction.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected transferInstructionCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new TransferInstructionResult_Pending(transferInstructionCid);
    } ;
  }

  private static JsonLfDecoder<TransferInstructionResult_Pending> jsonDecoderTransferInstructionResult_Pending(
      ) {
    return JsonLfDecoders.record(Arrays.asList("transferInstructionCid"), name -> {
          switch (name) {
            case "transferInstructionCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstruction.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new TransferInstructionResult_Pending(JsonLfDecoders.cast(args[0])));
  }

  public static ValueDecoder<TransferInstructionResult_Completed> valueDecoderTransferInstructionResult_Completed(
      ) throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ =
          PrimitiveValueDecoders.variantCheck("TransferInstructionResult_Completed", value$);
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0, recordValue$);
      List<Holding.ContractId> receiverHoldingCids = PrimitiveValueDecoders.fromList(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected receiverHoldingCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      return new TransferInstructionResult_Completed(receiverHoldingCids);
    } ;
  }

  private static JsonLfDecoder<TransferInstructionResult_Completed> jsonDecoderTransferInstructionResult_Completed(
      ) {
    return JsonLfDecoders.record(Arrays.asList("receiverHoldingCids"), name -> {
          switch (name) {
            case "receiverHoldingCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId::new)));
            default: return null;
          }
        }
        , (Object[] args) -> new TransferInstructionResult_Completed(JsonLfDecoders.cast(args[0])));
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TransferInstructionResult_Output> get() {
      return jsonDecoder();
    }
  }
}
