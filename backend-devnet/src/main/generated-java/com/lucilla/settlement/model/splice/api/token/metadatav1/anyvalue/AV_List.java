package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.Objects;

public class AV_List extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final List<AnyValue> listValue;

  public AV_List(List<AnyValue> listValue) {
    this.listValue = listValue;
  }

  public Variant toValue() {
    return new Variant("AV_List", this.listValue.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue())));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_List)) {
      return false;
    }
    AV_List other = (AV_List) object;
    return Objects.equals(this.listValue, other.listValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.listValue);
  }

  @Override
  public String toString() {
    return String.format("AV_List(%s)", this.listValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_List",
        apply(JsonLfEncoders.list(AnyValue::jsonEncoder), listValue));
  }
}
