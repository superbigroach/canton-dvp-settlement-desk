package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Timestamp;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExtendCessation extends DamlRecord<ExtendCessation> {
  public static final String _packageId = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public final Instant newFinalStrike;

  public final String note;

  public ExtendCessation(Instant newFinalStrike, String note) {
    this.newFinalStrike = newFinalStrike;
    this.note = note;
  }

  public static ValueDecoder<ExtendCessation> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      Instant newFinalStrike = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(0).getValue());
      String note = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      return new ExtendCessation(newFinalStrike, note);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("newFinalStrike", Timestamp.fromInstant(this.newFinalStrike)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("note", new Text(this.note)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<ExtendCessation> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("newFinalStrike", "note"), name -> {
          switch (name) {
            case "newFinalStrike": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "note": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            default: return null;
          }
        }
        , (Object[] args) -> new ExtendCessation(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static ExtendCessation fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("newFinalStrike", apply(JsonLfEncoders::timestamp, newFinalStrike)),
        JsonLfEncoders.Field.of("note", apply(JsonLfEncoders::text, note)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof ExtendCessation)) {
      return false;
    }
    ExtendCessation other = (ExtendCessation) object;
    return Objects.equals(this.newFinalStrike, other.newFinalStrike) &&
        Objects.equals(this.note, other.note);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.newFinalStrike, this.note);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.ExtendCessation(%s, %s)",
        this.newFinalStrike, this.note);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ExtendCessation> get() {
      return jsonDecoder();
    }
  }
}
