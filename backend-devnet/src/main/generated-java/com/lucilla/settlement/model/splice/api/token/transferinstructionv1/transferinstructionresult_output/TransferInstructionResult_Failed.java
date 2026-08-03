package com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionresult_output;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionResult_Output;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class TransferInstructionResult_Failed extends TransferInstructionResult_Output {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final Unit unitValue;

  public TransferInstructionResult_Failed(Unit unitValue) {
    this.unitValue = unitValue;
  }

  public Variant toValue() {
    return new Variant("TransferInstructionResult_Failed", Unit.getInstance());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TransferInstructionResult_Failed)) {
      return false;
    }
    TransferInstructionResult_Failed other = (TransferInstructionResult_Failed) object;
    return Objects.equals(this.unitValue, other.unitValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.unitValue);
  }

  @Override
  public String toString() {
    return String.format("TransferInstructionResult_Failed(%s)", this.unitValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("TransferInstructionResult_Failed",
        apply(JsonLfEncoders::unit, unitValue));
  }
}
