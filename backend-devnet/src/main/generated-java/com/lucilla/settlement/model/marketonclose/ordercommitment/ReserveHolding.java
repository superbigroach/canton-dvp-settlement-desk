package com.lucilla.settlement.model.marketonclose.ordercommitment;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.holding.Holding;
import com.lucilla.settlement.model.marketonclose.OrderCommitment;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class ReserveHolding extends OrderCommitment {
  public static final String _packageId = "504d21e4573fdcb737242ee9149b3e88f1ec7d6bd5a76b5701f4762c36fd8ae4";

  public final Holding.ContractId contractIdValue;

  public ReserveHolding(Holding.ContractId contractIdValue) {
    this.contractIdValue = contractIdValue;
  }

  public Variant toValue() {
    return new Variant("ReserveHolding", this.contractIdValue.toValue());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof ReserveHolding)) {
      return false;
    }
    ReserveHolding other = (ReserveHolding) object;
    return Objects.equals(this.contractIdValue, other.contractIdValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.contractIdValue);
  }

  @Override
  public String toString() {
    return String.format("ReserveHolding(%s)", this.contractIdValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("ReserveHolding",
        apply(JsonLfEncoders::contractId, contractIdValue));
  }
}
