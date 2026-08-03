package com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.allocationinstructionresult_output;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstructionResult_Output;
import com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Objects;

public class AllocationInstructionResult_Completed extends AllocationInstructionResult_Output {
  public static final String _packageId = "275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520";

  public final Allocation.ContractId allocationCid;

  public AllocationInstructionResult_Completed(Allocation.ContractId allocationCid) {
    this.allocationCid = allocationCid;
  }

  public Variant toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(1);
    fields.add(new DamlRecord.Field("allocationCid", this.allocationCid.toValue()));
    return new Variant("AllocationInstructionResult_Completed", new DamlRecord(fields));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AllocationInstructionResult_Completed)) {
      return false;
    }
    AllocationInstructionResult_Completed other = (AllocationInstructionResult_Completed) object;
    return Objects.equals(this.allocationCid, other.allocationCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.allocationCid);
  }

  @Override
  public String toString() {
    return String.format("AllocationInstructionResult_Completed(%s)", this.allocationCid);
  }

  private JsonLfEncoder jsonEncoderAllocationInstructionResult_Completed() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("allocationCid", apply(JsonLfEncoders::contractId, allocationCid)));
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AllocationInstructionResult_Completed",
        this.jsonEncoderAllocationInstructionResult_Completed());
  }
}
