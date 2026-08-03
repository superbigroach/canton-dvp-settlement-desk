package com.lucilla.settlement.model.splice.api.token.metadatav1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Text;
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

public class Metadata extends DamlRecord<Metadata> {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final Map<String, String> values;

  public Metadata(Map<String, String> values) {
    this.values = values;
  }

  public static ValueDecoder<Metadata> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      Map<String, String> values = PrimitiveValueDecoders.fromTextMap(
            PrimitiveValueDecoders.fromText).decode(fields$.get(0).getValue());
      return new Metadata(values);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("values", this.values.entrySet()
        .stream()
        .collect(DamlCollectors.toDamlTextMap(Map.Entry::getKey, v$0 -> new Text(v$0.getValue())))
        ));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Metadata> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("values"), name -> {
          switch (name) {
            case "values": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.textMap(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text));
            default: return null;
          }
        }
        , (Object[] args) -> new Metadata(JsonLfDecoders.cast(args[0])));
  }

  public static Metadata fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("values", apply(JsonLfEncoders.textMap(JsonLfEncoders::text), values)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof Metadata)) {
      return false;
    }
    Metadata other = (Metadata) object;
    return Objects.equals(this.values, other.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.values);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata(%s)",
        this.values);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Metadata> get() {
      return jsonDecoder();
    }
  }
}
