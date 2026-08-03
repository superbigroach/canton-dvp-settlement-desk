package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Bool;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class AV_Bool extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final Boolean booleanValue;

  public AV_Bool(Boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  public Variant toValue() {
    return new Variant("AV_Bool", Bool.of(this.booleanValue));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_Bool)) {
      return false;
    }
    AV_Bool other = (AV_Bool) object;
    return Objects.equals(this.booleanValue, other.booleanValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.booleanValue);
  }

  @Override
  public String toString() {
    return String.format("AV_Bool(%s)", this.booleanValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_Bool", apply(JsonLfEncoders::bool, booleanValue));
  }
}
