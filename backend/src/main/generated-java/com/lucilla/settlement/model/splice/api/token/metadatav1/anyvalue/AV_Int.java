package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Int64;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class AV_Int extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final Long longValue;

  public AV_Int(Long longValue) {
    this.longValue = longValue;
  }

  public Variant toValue() {
    return new Variant("AV_Int", new Int64(this.longValue));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_Int)) {
      return false;
    }
    AV_Int other = (AV_Int) object;
    return Objects.equals(this.longValue, other.longValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.longValue);
  }

  @Override
  public String toString() {
    return String.format("AV_Int(%s)", this.longValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_Int", apply(JsonLfEncoders::int64, longValue));
  }
}
