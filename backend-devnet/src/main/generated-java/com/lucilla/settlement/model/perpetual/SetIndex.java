package com.lucilla.settlement.model.perpetual;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Numeric;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SetIndex extends DamlRecord<SetIndex> {
  public static final String _packageId = "7eca29e115ad24f98fd4190f21ac6d7440ce8f3211675421f555856febed4e5c";

  public final BigDecimal newIndex;

  public SetIndex(BigDecimal newIndex) {
    this.newIndex = newIndex;
  }

  public static ValueDecoder<SetIndex> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      BigDecimal newIndex = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(0).getValue());
      return new SetIndex(newIndex);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("newIndex", new Numeric(this.newIndex)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<SetIndex> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("newIndex"), name -> {
          switch (name) {
            case "newIndex": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new SetIndex(JsonLfDecoders.cast(args[0])));
  }

  public static SetIndex fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("newIndex", apply(JsonLfEncoders::numeric, newIndex)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof SetIndex)) {
      return false;
    }
    SetIndex other = (SetIndex) object;
    return Objects.equals(this.newIndex, other.newIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.newIndex);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.perpetual.SetIndex(%s)", this.newIndex);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<SetIndex> get() {
      return jsonDecoder();
    }
  }
}
