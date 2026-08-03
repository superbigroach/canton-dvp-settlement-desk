package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.Objects;

public class AV_Decimal extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final BigDecimal bigDecimalValue;

  public AV_Decimal(BigDecimal bigDecimalValue) {
    this.bigDecimalValue = bigDecimalValue;
  }

  public Variant toValue() {
    return new Variant("AV_Decimal", new Numeric(this.bigDecimalValue));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_Decimal)) {
      return false;
    }
    AV_Decimal other = (AV_Decimal) object;
    return Objects.equals(this.bigDecimalValue, other.bigDecimalValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.bigDecimalValue);
  }

  @Override
  public String toString() {
    return String.format("AV_Decimal(%s)", this.bigDecimalValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_Decimal", apply(JsonLfEncoders::numeric, bigDecimalValue));
  }
}
