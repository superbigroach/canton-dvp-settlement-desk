package com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionstatus;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionStatus;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class TransferPendingInternalWorkflow extends TransferInstructionStatus {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final Map<String, String> pendingActions;

  public TransferPendingInternalWorkflow(Map<String, String> pendingActions) {
    this.pendingActions = pendingActions;
  }

  public Variant toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(1);
    fields.add(new DamlRecord.Field("pendingActions", this.pendingActions.entrySet().stream()
        .collect(DamlCollectors.toDamlGenMap(v$0 -> new Party(v$0.getKey()), v$0 -> new Text(v$0.getValue())))));
    return new Variant("TransferPendingInternalWorkflow", new DamlRecord(fields));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TransferPendingInternalWorkflow)) {
      return false;
    }
    TransferPendingInternalWorkflow other = (TransferPendingInternalWorkflow) object;
    return Objects.equals(this.pendingActions, other.pendingActions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.pendingActions);
  }

  @Override
  public String toString() {
    return String.format("TransferPendingInternalWorkflow(%s)", this.pendingActions);
  }

  private JsonLfEncoder jsonEncoderTransferPendingInternalWorkflow() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("pendingActions", apply(JsonLfEncoders.genMap(JsonLfEncoders::party, JsonLfEncoders::text), pendingActions)));
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("TransferPendingInternalWorkflow",
        this.jsonEncoderTransferPendingInternalWorkflow());
  }
}
