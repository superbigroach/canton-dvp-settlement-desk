package com.lucilla.settlement.model.marketonclose.ordercommitment;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.marketonclose.OrderCommitment;
import com.lucilla.settlement.model.splice.api.token.holdingv1.Holding;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class DeclareTokenHolding extends OrderCommitment {
  public static final String _packageId = "16b1d7198cf7c7ec9373fe2d1bdb48ab1770fe7ffcb7281ad87048ebecd45ab4";

  public final Holding.ContractId contractIdValue;

  public DeclareTokenHolding(Holding.ContractId contractIdValue) {
    this.contractIdValue = contractIdValue;
  }

  public Variant toValue() {
    return new Variant("DeclareTokenHolding", this.contractIdValue.toValue());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof DeclareTokenHolding)) {
      return false;
    }
    DeclareTokenHolding other = (DeclareTokenHolding) object;
    return Objects.equals(this.contractIdValue, other.contractIdValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.contractIdValue);
  }

  @Override
  public String toString() {
    return String.format("DeclareTokenHolding(%s)", this.contractIdValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("DeclareTokenHolding",
        apply(JsonLfEncoders::contractId, contractIdValue));
  }
}
