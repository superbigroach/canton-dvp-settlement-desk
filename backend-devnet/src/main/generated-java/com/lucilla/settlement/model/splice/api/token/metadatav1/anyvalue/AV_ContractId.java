package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyContract;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class AV_ContractId extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final AnyContract.ContractId contractIdValue;

  public AV_ContractId(AnyContract.ContractId contractIdValue) {
    this.contractIdValue = contractIdValue;
  }

  public Variant toValue() {
    return new Variant("AV_ContractId", this.contractIdValue.toValue());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_ContractId)) {
      return false;
    }
    AV_ContractId other = (AV_ContractId) object;
    return Objects.equals(this.contractIdValue, other.contractIdValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.contractIdValue);
  }

  @Override
  public String toString() {
    return String.format("AV_ContractId(%s)", this.contractIdValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_ContractId",
        apply(JsonLfEncoders::contractId, contractIdValue));
  }
}
