package com.lucilla.settlement.model.holding;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Disclose extends DamlRecord<Disclose> {
  public static final String _packageId = "85b662c2e7d0a4d42bea5a2232989bd04641057da99c8bda47d7f7b912ef699c";

  public final String newObserver;

  public Disclose(String newObserver) {
    this.newObserver = newObserver;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static Disclose fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<Disclose> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      String newObserver = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      return new Disclose(newObserver);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("newObserver", new Party(this.newObserver)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Disclose> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("newObserver"), name -> {
          switch (name) {
            case "newObserver": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            default: return null;
          }
        }
        , (Object[] args) -> new Disclose(JsonLfDecoders.cast(args[0])));
  }

  public static Disclose fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("newObserver", apply(JsonLfEncoders::party, newObserver)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof Disclose)) {
      return false;
    }
    Disclose other = (Disclose) object;
    return Objects.equals(this.newObserver, other.newObserver);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.newObserver);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.holding.Disclose(%s)", this.newObserver);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Disclose> get() {
      return jsonDecoder();
    }
  }
}
