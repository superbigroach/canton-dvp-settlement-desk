package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Date;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Advance extends DamlRecord<Advance> {
  public static final String _packageId = "9f697598fdc5fee1bf367e5acd6ca4eb84c7368c987ce1093f58227384f3d0f8";

  public final LocalDate asOfDate;

  public Advance(LocalDate asOfDate) {
    this.asOfDate = asOfDate;
  }

  public static ValueDecoder<Advance> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      LocalDate asOfDate = PrimitiveValueDecoders.fromDate.decode(fields$.get(0).getValue());
      return new Advance(asOfDate);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("asOfDate", new Date((int) this.asOfDate.toEpochDay())));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Advance> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("asOfDate"), name -> {
          switch (name) {
            case "asOfDate": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.date);
            default: return null;
          }
        }
        , (Object[] args) -> new Advance(JsonLfDecoders.cast(args[0])));
  }

  public static Advance fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("asOfDate", apply(JsonLfEncoders::date, asOfDate)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof Advance)) {
      return false;
    }
    Advance other = (Advance) object;
    return Objects.equals(this.asOfDate, other.asOfDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.asOfDate);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.Advance(%s)", this.asOfDate);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Advance> get() {
      return jsonDecoder();
    }
  }
}
