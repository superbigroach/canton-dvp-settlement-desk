package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Timestamp;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.Instant;
import java.util.Objects;

public class AV_Time extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final Instant instantValue;

  public AV_Time(Instant instantValue) {
    this.instantValue = instantValue;
  }

  public Variant toValue() {
    return new Variant("AV_Time", Timestamp.fromInstant(this.instantValue));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_Time)) {
      return false;
    }
    AV_Time other = (AV_Time) object;
    return Objects.equals(this.instantValue, other.instantValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.instantValue);
  }

  @Override
  public String toString() {
    return String.format("AV_Time(%s)", this.instantValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_Time", apply(JsonLfEncoders::timestamp, instantValue));
  }
}
