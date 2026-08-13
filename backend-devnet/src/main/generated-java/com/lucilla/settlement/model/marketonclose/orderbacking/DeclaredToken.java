package com.lucilla.settlement.model.marketonclose.orderbacking;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.marketonclose.OrderBacking;
import com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class DeclaredToken extends OrderBacking {
  public static final String _packageId = "7eca29e115ad24f98fd4190f21ac6d7440ce8f3211675421f555856febed4e5c";

  public final InstrumentId instrumentIdValue;

  public DeclaredToken(InstrumentId instrumentIdValue) {
    this.instrumentIdValue = instrumentIdValue;
  }

  public Variant toValue() {
    return new Variant("DeclaredToken", this.instrumentIdValue.toValue());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof DeclaredToken)) {
      return false;
    }
    DeclaredToken other = (DeclaredToken) object;
    return Objects.equals(this.instrumentIdValue, other.instrumentIdValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.instrumentIdValue);
  }

  @Override
  public String toString() {
    return String.format("DeclaredToken(%s)", this.instrumentIdValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("DeclaredToken",
        apply(InstrumentId::jsonEncoder, instrumentIdValue));
  }
}
