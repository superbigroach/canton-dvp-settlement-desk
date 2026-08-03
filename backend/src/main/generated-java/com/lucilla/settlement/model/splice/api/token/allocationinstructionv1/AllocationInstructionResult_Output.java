package com.lucilla.settlement.model.splice.api.token.allocationinstructionv1;

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
import com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.allocationinstructionresult_output.AllocationInstructionResult_Completed;
import com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.allocationinstructionresult_output.AllocationInstructionResult_Failed;
import com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.allocationinstructionresult_output.AllocationInstructionResult_Pending;
import com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.Arrays;
import java.util.List;

public abstract class AllocationInstructionResult_Output extends Variant<AllocationInstructionResult_Output> {
  public static final String _packageId = "275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520";

  public AllocationInstructionResult_Output() {
  }

  public abstract com.daml.ledger.javaapi.data.Variant toValue();

  public static ValueDecoder<AllocationInstructionResult_Output> valueDecoder() {
    return value$ -> {
      com.daml.ledger.javaapi.data.Variant variant$ = value$.asVariant().orElseThrow(() -> new IllegalArgumentException("Expected Variant to build an instance of the Variant com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstructionResult_Output"));
      if ("AllocationInstructionResult_Pending".equals(variant$.getConstructor())) {
        return valueDecoderAllocationInstructionResult_Pending().decode(variant$);
      }
      if ("AllocationInstructionResult_Completed".equals(variant$.getConstructor())) {
        return valueDecoderAllocationInstructionResult_Completed().decode(variant$);
      }
      if ("AllocationInstructionResult_Failed".equals(variant$.getConstructor())) {
        return valueDecoderAllocationInstructionResult_Failed().decode(variant$);
      }
      throw new IllegalArgumentException("Found unknown constructor " + variant$.getConstructor() + " for variant com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstructionResult_Output, expected one of [AllocationInstructionResult_Pending, AllocationInstructionResult_Completed, AllocationInstructionResult_Failed]. This could be a failed variant downgrade.");
    } ;
  }

  public static JsonLfDecoder<AllocationInstructionResult_Output> jsonDecoder() {
    return JsonLfDecoders.variant(Arrays.asList("AllocationInstructionResult_Pending", "AllocationInstructionResult_Completed", "AllocationInstructionResult_Failed"), name -> {
          switch (name) {
            case "AllocationInstructionResult_Pending": return jsonDecoderAllocationInstructionResult_Pending();
            case "AllocationInstructionResult_Completed": return jsonDecoderAllocationInstructionResult_Completed();
            case "AllocationInstructionResult_Failed": return jsonDecoderAllocationInstructionResult_Failed();
            default: return null;
          }
        }
        );
  }

  public static AllocationInstructionResult_Output fromJson(String json) throws
      JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  protected abstract JsonLfEncoders.Field fieldForJsonEncoder();

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.variant(AllocationInstructionResult_Output::fieldForJsonEncoder).apply(this);
  }

  private static ValueDecoder<AllocationInstructionResult_Failed> valueDecoderAllocationInstructionResult_Failed(
      ) throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ =
          PrimitiveValueDecoders.variantCheck("AllocationInstructionResult_Failed", value$);
      Unit body = PrimitiveValueDecoders.fromUnit.decode(variantValue$);
      return new AllocationInstructionResult_Failed(body);
    } ;
  }

  private static JsonLfDecoder<AllocationInstructionResult_Failed> jsonDecoderAllocationInstructionResult_Failed(
      ) {
    return r -> new AllocationInstructionResult_Failed(JsonLfDecoders.unit.decode(r));
  }

  public static ValueDecoder<AllocationInstructionResult_Completed> valueDecoderAllocationInstructionResult_Completed(
      ) throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ =
          PrimitiveValueDecoders.variantCheck("AllocationInstructionResult_Completed", value$);
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0, recordValue$);
      Allocation.ContractId allocationCid =
          new Allocation.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected allocationCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new AllocationInstructionResult_Completed(allocationCid);
    } ;
  }

  private static JsonLfDecoder<AllocationInstructionResult_Completed> jsonDecoderAllocationInstructionResult_Completed(
      ) {
    return JsonLfDecoders.record(Arrays.asList("allocationCid"), name -> {
          switch (name) {
            case "allocationCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new AllocationInstructionResult_Completed(JsonLfDecoders.cast(args[0])));
  }

  public static ValueDecoder<AllocationInstructionResult_Pending> valueDecoderAllocationInstructionResult_Pending(
      ) throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ =
          PrimitiveValueDecoders.variantCheck("AllocationInstructionResult_Pending", value$);
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0, recordValue$);
      AllocationInstruction.ContractId allocationInstructionCid =
          new AllocationInstruction.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected allocationInstructionCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new AllocationInstructionResult_Pending(allocationInstructionCid);
    } ;
  }

  private static JsonLfDecoder<AllocationInstructionResult_Pending> jsonDecoderAllocationInstructionResult_Pending(
      ) {
    return JsonLfDecoders.record(Arrays.asList("allocationInstructionCid"), name -> {
          switch (name) {
            case "allocationInstructionCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstruction.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new AllocationInstructionResult_Pending(JsonLfDecoders.cast(args[0])));
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AllocationInstructionResult_Output> get() {
      return jsonDecoder();
    }
  }
}
