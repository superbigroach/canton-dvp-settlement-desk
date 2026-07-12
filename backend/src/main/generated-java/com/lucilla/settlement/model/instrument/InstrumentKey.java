package com.lucilla.settlement.model.instrument;

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
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class InstrumentKey extends DamlRecord<InstrumentKey> {
  public static final String _packageId = "12f056257e4f6e96f8abcaafbc3d7261e58f3fcddcba133f3033b91190110371";

  public final String issuer;

  public final String depository;

  public final String id;

  public final String version;

  public InstrumentKey(String issuer, String depository, String id, String version) {
    this.issuer = issuer;
    this.depository = depository;
    this.id = id;
    this.version = version;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static InstrumentKey fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<InstrumentKey> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(4,0,
          recordValue$);
      String issuer = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String depository = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String id = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String version = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      return new InstrumentKey(issuer, depository, id, version);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(4);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("issuer", new Party(this.issuer)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("depository", new Party(this.depository)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("id", new Text(this.id)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("version", new Text(this.version)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<InstrumentKey> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("issuer", "depository", "id", "version"), name -> {
          switch (name) {
            case "issuer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "depository": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "id": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "version": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            default: return null;
          }
        }
        , (Object[] args) -> new InstrumentKey(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3])));
  }

  public static InstrumentKey fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("issuer", apply(JsonLfEncoders::party, issuer)),
        JsonLfEncoders.Field.of("depository", apply(JsonLfEncoders::party, depository)),
        JsonLfEncoders.Field.of("id", apply(JsonLfEncoders::text, id)),
        JsonLfEncoders.Field.of("version", apply(JsonLfEncoders::text, version)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof InstrumentKey)) {
      return false;
    }
    InstrumentKey other = (InstrumentKey) object;
    return Objects.equals(this.issuer, other.issuer) &&
        Objects.equals(this.depository, other.depository) && Objects.equals(this.id, other.id) &&
        Objects.equals(this.version, other.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.issuer, this.depository, this.id, this.version);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.instrument.InstrumentKey(%s, %s, %s, %s)",
        this.issuer, this.depository, this.id, this.version);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<InstrumentKey> get() {
      return jsonDecoder();
    }
  }
}
