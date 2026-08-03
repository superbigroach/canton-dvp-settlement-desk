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
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionstatus.TransferPendingInternalWorkflow;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionstatus.TransferPendingReceiverAcceptance;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class TransferInstructionStatus extends Variant<TransferInstructionStatus> {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public TransferInstructionStatus() {
  }

  public abstract com.daml.ledger.javaapi.data.Variant toValue();

  public static ValueDecoder<TransferInstructionStatus> valueDecoder() {
    return value$ -> {
      com.daml.ledger.javaapi.data.Variant variant$ = value$.asVariant().orElseThrow(() -> new IllegalArgumentException("Expected Variant to build an instance of the Variant com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionStatus"));
      if ("TransferPendingReceiverAcceptance".equals(variant$.getConstructor())) {
        return valueDecoderTransferPendingReceiverAcceptance().decode(variant$);
      }
      if ("TransferPendingInternalWorkflow".equals(variant$.getConstructor())) {
        return valueDecoderTransferPendingInternalWorkflow().decode(variant$);
      }
      throw new IllegalArgumentException("Found unknown constructor " + variant$.getConstructor() + " for variant com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionStatus, expected one of [TransferPendingReceiverAcceptance, TransferPendingInternalWorkflow]. This could be a failed variant downgrade.");
    } ;
  }

  public static JsonLfDecoder<TransferInstructionStatus> jsonDecoder() {
    return JsonLfDecoders.variant(Arrays.asList("TransferPendingReceiverAcceptance", "TransferPendingInternalWorkflow"), name -> {
          switch (name) {
            case "TransferPendingReceiverAcceptance": return jsonDecoderTransferPendingReceiverAcceptance();
            case "TransferPendingInternalWorkflow": return jsonDecoderTransferPendingInternalWorkflow();
            default: return null;
          }
        }
        );
  }

  public static TransferInstructionStatus fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  protected abstract JsonLfEncoders.Field fieldForJsonEncoder();

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.variant(TransferInstructionStatus::fieldForJsonEncoder).apply(this);
  }

  private static ValueDecoder<TransferPendingReceiverAcceptance> valueDecoderTransferPendingReceiverAcceptance(
      ) throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("TransferPendingReceiverAcceptance",
          value$);
      Unit body = PrimitiveValueDecoders.fromUnit.decode(variantValue$);
      return new TransferPendingReceiverAcceptance(body);
    } ;
  }

  private static JsonLfDecoder<TransferPendingReceiverAcceptance> jsonDecoderTransferPendingReceiverAcceptance(
      ) {
    return r -> new TransferPendingReceiverAcceptance(JsonLfDecoders.unit.decode(r));
  }

  public static ValueDecoder<TransferPendingInternalWorkflow> valueDecoderTransferPendingInternalWorkflow(
      ) throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = PrimitiveValueDecoders.variantCheck("TransferPendingInternalWorkflow",
          value$);
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0, recordValue$);
      Map<String, String> pendingActions = PrimitiveValueDecoders.fromGenMap(
            PrimitiveValueDecoders.fromParty, PrimitiveValueDecoders.fromText)
          .decode(fields$.get(0).getValue());
      return new TransferPendingInternalWorkflow(pendingActions);
    } ;
  }

  private static JsonLfDecoder<TransferPendingInternalWorkflow> jsonDecoderTransferPendingInternalWorkflow(
      ) {
    return JsonLfDecoders.record(Arrays.asList("pendingActions"), name -> {
          switch (name) {
            case "pendingActions": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.genMap(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text));
            default: return null;
          }
        }
        , (Object[] args) -> new TransferPendingInternalWorkflow(JsonLfDecoders.cast(args[0])));
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TransferInstructionStatus> get() {
      return jsonDecoder();
    }
  }
}
