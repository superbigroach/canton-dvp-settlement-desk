package com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Map;
import java.util.Objects;

public class AV_Map extends AnyValue {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final Map<String, AnyValue> mapValue;

  public AV_Map(Map<String, AnyValue> mapValue) {
    this.mapValue = mapValue;
  }

  public Variant toValue() {
    return new Variant("AV_Map", this.mapValue.entrySet().stream()
        .collect(DamlCollectors.toDamlTextMap(Map.Entry::getKey, v$0 -> v$0.getValue().toValue()))
        );
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AV_Map)) {
      return false;
    }
    AV_Map other = (AV_Map) object;
    return Objects.equals(this.mapValue, other.mapValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.mapValue);
  }

  @Override
  public String toString() {
    return String.format("AV_Map(%s)", this.mapValue);
  }

  @Override
  protected JsonLfEncoders.Field fieldForJsonEncoder() {
    return JsonLfEncoders.Field.of("AV_Map",
        apply(JsonLfEncoders.textMap(AnyValue::jsonEncoder), mapValue));
  }
}
