package com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.allocationinstructionresult_output;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstructionResult_Output;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class AllocationInstructionResult_Failed extends AllocationInstructionResult_Output {
  public static final String _packageId = "275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520";

  public final Unit unitValue;

  public AllocationInstructionResult_Failed(Unit unitValue) {
    this.unitValue = unitValue;
  }

  public Variant toValue() {
    return new Variant("AllocationInstructionResult_Failed", Unit.getInstance());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AllocationInstructionResult_Failed)) {
      return false;
    }
    AllocationInstructionResult_Failed other = (AllocationInstructionResult_Failed) object;
    return Objects.equals(this.unitValue, other.unitValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.unitValue);
  }

  @Override
  public String toString() {
    return String.format("AllocationInstructionResult_Failed(%s)", this.unitValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AllocationInstructionResult_Failed",
        apply(JsonLfEncoders::unit, unitValue));
  }
}
