package com.lucilla.settlement.model.basket;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlOptional;
import com.daml.ledger.javaapi.data.Numeric;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Component extends DamlRecord<Component> {
  public static final String _packageId = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public final String instrumentId;

  public final BigDecimal unitsPerShare;

  public final Optional<String> expectedIssuer;

  public Component(String instrumentId, BigDecimal unitsPerShare, Optional<String> expectedIssuer) {
    this.instrumentId = instrumentId;
    this.unitsPerShare = unitsPerShare;
    this.expectedIssuer = expectedIssuer;
  }

  public static ValueDecoder<Component> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,1,
          recordValue$);
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(0).getValue());
      BigDecimal unitsPerShare = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(1).getValue());
      Optional<String> expectedIssuer = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromParty).decode(fields$.get(2).getValue());
      return new Component(instrumentId, unitsPerShare, expectedIssuer);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("unitsPerShare", new Numeric(this.unitsPerShare)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("expectedIssuer", DamlOptional.of(this.expectedIssuer.map(v$0 -> new Party(v$0)))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Component> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("instrumentId", "unitsPerShare", "expectedIssuer"), name -> {
          switch (name) {
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "unitsPerShare": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "expectedIssuer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party), java.util.Optional.empty());
            default: return null;
          }
        }
        , (Object[] args) -> new Component(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static Component fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("unitsPerShare", apply(JsonLfEncoders::numeric, unitsPerShare)),
        JsonLfEncoders.Field.of("expectedIssuer", apply(JsonLfEncoders.optional(JsonLfEncoders::party), expectedIssuer)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof Component)) {
      return false;
    }
    Component other = (Component) object;
    return Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.unitsPerShare, other.unitsPerShare) &&
        Objects.equals(this.expectedIssuer, other.expectedIssuer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.instrumentId, this.unitsPerShare, this.expectedIssuer);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.basket.Component(%s, %s, %s)",
        this.instrumentId, this.unitsPerShare, this.expectedIssuer);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Component> get() {
      return jsonDecoder();
    }
  }
}
