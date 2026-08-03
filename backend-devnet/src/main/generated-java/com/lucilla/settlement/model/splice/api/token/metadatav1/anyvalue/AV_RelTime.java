package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.da.time.types.RelTime;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class AV_RelTime extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final RelTime relTimeValue;

  public AV_RelTime(RelTime relTimeValue) {
    this.relTimeValue = relTimeValue;
  }

  public Variant toValue() {
    return new Variant("AV_RelTime", this.relTimeValue.toValue());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_RelTime)) {
      return false;
    }
    AV_RelTime other = (AV_RelTime) object;
    return Objects.equals(this.relTimeValue, other.relTimeValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.relTimeValue);
  }

  @Override
  public String toString() {
    return String.format("AV_RelTime(%s)", this.relTimeValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_RelTime", apply(RelTime::jsonEncoder, relTimeValue));
  }
}
