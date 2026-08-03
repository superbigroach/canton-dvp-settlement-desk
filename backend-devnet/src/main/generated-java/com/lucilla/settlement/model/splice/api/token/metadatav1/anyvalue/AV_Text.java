package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Objects;

public class AV_Text extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final String stringValue;

  public AV_Text(String stringValue) {
    this.stringValue = stringValue;
  }

  public Variant toValue() {
    return new Variant("AV_Text", new Text(this.stringValue));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_Text)) {
      return false;
    }
    AV_Text other = (AV_Text) object;
    return Objects.equals(this.stringValue, other.stringValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.stringValue);
  }

  @Override
  public String toString() {
    return String.format("AV_Text(%s)", this.stringValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_Text", apply(JsonLfEncoders::text, stringValue));
  }
}
