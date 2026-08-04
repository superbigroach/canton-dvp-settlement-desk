package com.lucilla.settlement.model.continuousbook;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Numeric;
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
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Execution extends DamlRecord<Execution> {
  public static final String _packageId = "147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d";

  public final BigDecimal price;

  public final BigDecimal quantity;

  public final BigDecimal cashAmount;

  public final String buyer;

  public final String seller;

  public final String aggressor;

  public final String maker;

  public Execution(BigDecimal price, BigDecimal quantity, BigDecimal cashAmount, String buyer,
      String seller, String aggressor, String maker) {
    this.price = price;
    this.quantity = quantity;
    this.cashAmount = cashAmount;
    this.buyer = buyer;
    this.seller = seller;
    this.aggressor = aggressor;
    this.maker = maker;
  }

  public static ValueDecoder<Execution> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(7,0,
          recordValue$);
      BigDecimal price = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(0).getValue());
      BigDecimal quantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(1).getValue());
      BigDecimal cashAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      String buyer = PrimitiveValueDecoders.fromParty.decode(fields$.get(3).getValue());
      String seller = PrimitiveValueDecoders.fromParty.decode(fields$.get(4).getValue());
      String aggressor = PrimitiveValueDecoders.fromParty.decode(fields$.get(5).getValue());
      String maker = PrimitiveValueDecoders.fromParty.decode(fields$.get(6).getValue());
      return new Execution(price, quantity, cashAmount, buyer, seller, aggressor, maker);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(7);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("price", new Numeric(this.price)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("quantity", new Numeric(this.quantity)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashAmount", new Numeric(this.cashAmount)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("buyer", new Party(this.buyer)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("seller", new Party(this.seller)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("aggressor", new Party(this.aggressor)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("maker", new Party(this.maker)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Execution> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("price", "quantity", "cashAmount", "buyer", "seller", "aggressor", "maker"), name -> {
          switch (name) {
            case "price": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "quantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "cashAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "buyer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "seller": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "aggressor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "maker": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            default: return null;
          }
        }
        , (Object[] args) -> new Execution(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6])));
  }

  public static Execution fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("price", apply(JsonLfEncoders::numeric, price)),
        JsonLfEncoders.Field.of("quantity", apply(JsonLfEncoders::numeric, quantity)),
        JsonLfEncoders.Field.of("cashAmount", apply(JsonLfEncoders::numeric, cashAmount)),
        JsonLfEncoders.Field.of("buyer", apply(JsonLfEncoders::party, buyer)),
        JsonLfEncoders.Field.of("seller", apply(JsonLfEncoders::party, seller)),
        JsonLfEncoders.Field.of("aggressor", apply(JsonLfEncoders::party, aggressor)),
        JsonLfEncoders.Field.of("maker", apply(JsonLfEncoders::party, maker)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof Execution)) {
      return false;
    }
    Execution other = (Execution) object;
    return Objects.equals(this.price, other.price) &&
        Objects.equals(this.quantity, other.quantity) &&
        Objects.equals(this.cashAmount, other.cashAmount) &&
        Objects.equals(this.buyer, other.buyer) && Objects.equals(this.seller, other.seller) &&
        Objects.equals(this.aggressor, other.aggressor) && Objects.equals(this.maker, other.maker);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.price, this.quantity, this.cashAmount, this.buyer, this.seller,
        this.aggressor, this.maker);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.continuousbook.Execution(%s, %s, %s, %s, %s, %s, %s)",
        this.price, this.quantity, this.cashAmount, this.buyer, this.seller, this.aggressor,
        this.maker);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Execution> get() {
      return jsonDecoder();
    }
  }
}
