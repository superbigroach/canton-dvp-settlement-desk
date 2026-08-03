package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Date;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.util.Objects;

public class AV_Date extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final LocalDate localDateValue;

  public AV_Date(LocalDate localDateValue) {
    this.localDateValue = localDateValue;
  }

  public Variant toValue() {
    return new Variant("AV_Date", new Date((int) this.localDateValue.toEpochDay()));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_Date)) {
      return false;
    }
    AV_Date other = (AV_Date) object;
    return Objects.equals(this.localDateValue, other.localDateValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.localDateValue);
  }

  @Override
  public String toString() {
    return String.format("AV_Date(%s)", this.localDateValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_Date", apply(JsonLfEncoders::date, localDateValue));
  }
}
