package com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionresult_output;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstruction;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionResult_Output;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Objects;

public class TransferInstructionResult_Pending extends TransferInstructionResult_Output {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final TransferInstruction.ContractId transferInstructionCid;

  public TransferInstructionResult_Pending(TransferInstruction.ContractId transferInstructionCid) {
    this.transferInstructionCid = transferInstructionCid;
  }

  public Variant toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(1);
    fields.add(new DamlRecord.Field("transferInstructionCid", this.transferInstructionCid.toValue()));
    return new Variant("TransferInstructionResult_Pending", new DamlRecord(fields));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TransferInstructionResult_Pending)) {
      return false;
    }
    TransferInstructionResult_Pending other = (TransferInstructionResult_Pending) object;
    return Objects.equals(this.transferInstructionCid, other.transferInstructionCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.transferInstructionCid);
  }

  @Override
  public String toString() {
    return String.format("TransferInstructionResult_Pending(%s)", this.transferInstructionCid);
  }

  private JsonLfEncoder jsonEncoderTransferInstructionResult_Pending() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("transferInstructionCid", apply(JsonLfEncoders::contractId, transferInstructionCid)));
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("TransferInstructionResult_Pending",
        this.jsonEncoderTransferInstructionResult_Pending());
  }
}
