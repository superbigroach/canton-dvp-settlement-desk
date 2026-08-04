package com.lucilla.settlement.model.continuousbook;

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

public class Fill extends DamlRecord<Fill> {
  public static final String _packageId = "147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d";

  public final BigDecimal fillQty;

  public final BigDecimal tradePrice;

  public Fill(BigDecimal fillQty, BigDecimal tradePrice) {
    this.fillQty = fillQty;
    this.tradePrice = tradePrice;
  }

  public static ValueDecoder<Fill> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      BigDecimal fillQty = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(0).getValue());
      BigDecimal tradePrice = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(1).getValue());
      return new Fill(fillQty, tradePrice);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("fillQty", new Numeric(this.fillQty)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("tradePrice", new Numeric(this.tradePrice)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Fill> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("fillQty", "tradePrice"), name -> {
          switch (name) {
            case "fillQty": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "tradePrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new Fill(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static Fill fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("fillQty", apply(JsonLfEncoders::numeric, fillQty)),
        JsonLfEncoders.Field.of("tradePrice", apply(JsonLfEncoders::numeric, tradePrice)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof Fill)) {
      return false;
    }
    Fill other = (Fill) object;
    return Objects.equals(this.fillQty, other.fillQty) &&
        Objects.equals(this.tradePrice, other.tradePrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.fillQty, this.tradePrice);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.continuousbook.Fill(%s, %s)", this.fillQty,
        this.tradePrice);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Fill> get() {
      return jsonDecoder();
    }
  }
}
