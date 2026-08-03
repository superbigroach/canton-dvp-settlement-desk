package com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionresult_output;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.holdingv1.Holding;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionResult_Output;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransferInstructionResult_Completed extends TransferInstructionResult_Output {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final List<Holding.ContractId> receiverHoldingCids;

  public TransferInstructionResult_Completed(List<Holding.ContractId> receiverHoldingCids) {
    this.receiverHoldingCids = receiverHoldingCids;
  }

  public Variant toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(1);
    fields.add(new DamlRecord.Field("receiverHoldingCids", this.receiverHoldingCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new Variant("TransferInstructionResult_Completed", new DamlRecord(fields));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TransferInstructionResult_Completed)) {
      return false;
    }
    TransferInstructionResult_Completed other = (TransferInstructionResult_Completed) object;
    return Objects.equals(this.receiverHoldingCids, other.receiverHoldingCids);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.receiverHoldingCids);
  }

  @Override
  public String toString() {
    return String.format("TransferInstructionResult_Completed(%s)", this.receiverHoldingCids);
  }

  private JsonLfEncoder jsonEncoderTransferInstructionResult_Completed() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("receiverHoldingCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), receiverHoldingCids)));
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("TransferInstructionResult_Completed",
        this.jsonEncoderTransferInstructionResult_Completed());
  }
}
