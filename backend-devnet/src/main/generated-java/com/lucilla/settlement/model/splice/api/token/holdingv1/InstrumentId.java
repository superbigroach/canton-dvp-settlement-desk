package com.lucilla.settlement.model.splice.api.token.holdingv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Party;
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
import java.util.Objects;

public class InstrumentId extends DamlRecord<InstrumentId> {
  public static final String _packageId = "718a0f77e505a8de22f188bd4c87fe74101274e9d4cb1bfac7d09aec7158d35b";

  public final String admin;

  public final String id;

  public InstrumentId(String admin, String id) {
    this.admin = admin;
    this.id = id;
  }

  public static ValueDecoder<InstrumentId> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String id = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      return new InstrumentId(admin, id);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("admin", new Party(this.admin)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("id", new Text(this.id)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<InstrumentId> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("admin", "id"), name -> {
          switch (name) {
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "id": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            default: return null;
          }
        }
        , (Object[] args) -> new InstrumentId(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static InstrumentId fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)),
        JsonLfEncoders.Field.of("id", apply(JsonLfEncoders::text, id)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof InstrumentId)) {
      return false;
    }
    InstrumentId other = (InstrumentId) object;
    return Objects.equals(this.admin, other.admin) && Objects.equals(this.id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.admin, this.id);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId(%s, %s)",
        this.admin, this.id);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<InstrumentId> get() {
      return jsonDecoder();
    }
  }
}
