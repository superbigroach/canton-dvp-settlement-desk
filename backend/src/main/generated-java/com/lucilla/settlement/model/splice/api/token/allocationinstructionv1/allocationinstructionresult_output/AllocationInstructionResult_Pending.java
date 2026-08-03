package com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.allocationinstructionresult_output;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstruction;
import com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstructionResult_Output;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Objects;

public class AllocationInstructionResult_Pending extends AllocationInstructionResult_Output {
  public static final String _packageId = "275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520";

  public final AllocationInstruction.ContractId allocationInstructionCid;

  public AllocationInstructionResult_Pending(
      AllocationInstruction.ContractId allocationInstructionCid) {
    this.allocationInstructionCid = allocationInstructionCid;
  }

  public Variant toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(1);
    fields.add(new DamlRecord.Field("allocationInstructionCid", this.allocationInstructionCid.toValue()));
    return new Variant("AllocationInstructionResult_Pending", new DamlRecord(fields));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AllocationInstructionResult_Pending)) {
      return false;
    }
    AllocationInstructionResult_Pending other = (AllocationInstructionResult_Pending) object;
    return Objects.equals(this.allocationInstructionCid, other.allocationInstructionCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.allocationInstructionCid);
  }

  @Override
  public String toString() {
    return String.format("AllocationInstructionResult_Pending(%s)", this.allocationInstructionCid);
  }

  private JsonLfEncoder jsonEncoderAllocationInstructionResult_Pending() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("allocationInstructionCid", apply(JsonLfEncoders::contractId, allocationInstructionCid)));
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AllocationInstructionResult_Pending",
        this.jsonEncoderAllocationInstructionResult_Pending());
  }
}
