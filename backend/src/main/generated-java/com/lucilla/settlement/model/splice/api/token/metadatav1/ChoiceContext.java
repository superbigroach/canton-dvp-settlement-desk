package com.lucilla.settlement.model.splice.api.token.metadatav1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChoiceContext extends DamlRecord<ChoiceContext> {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final Map<String, AnyValue> values;

  public ChoiceContext(Map<String, AnyValue> values) {
    this.values = values;
  }

  public static ValueDecoder<ChoiceContext> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      Map<String, AnyValue> values = PrimitiveValueDecoders.fromTextMap(AnyValue.valueDecoder())
          .decode(fields$.get(0).getValue());
      return new ChoiceContext(values);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("values", this.values.entrySet()
        .stream()
        .collect(DamlCollectors.toDamlTextMap(Map.Entry::getKey, v$0 -> v$0.getValue().toValue()))
        ));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<ChoiceContext> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("values"), name -> {
          switch (name) {
            case "values": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.textMap(new com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue.JsonDecoder$().get()));
            default: return null;
          }
        }
        , (Object[] args) -> new ChoiceContext(JsonLfDecoders.cast(args[0])));
  }

  public static ChoiceContext fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("values", apply(JsonLfEncoders.textMap(AnyValue::jsonEncoder), values)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof ChoiceContext)) {
      return false;
    }
    ChoiceContext other = (ChoiceContext) object;
    return Objects.equals(this.values, other.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.values);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.metadatav1.ChoiceContext(%s)",
        this.values);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ChoiceContext> get() {
      return jsonDecoder();
    }
  }
}
