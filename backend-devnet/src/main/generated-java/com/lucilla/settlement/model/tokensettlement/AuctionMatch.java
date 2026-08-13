package com.lucilla.settlement.model.tokensettlement;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Int64;
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
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AuctionMatch extends DamlRecord<AuctionMatch> {
  public static final String _packageId = "abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1";

  public final String buyer;

  public final String seller;

  public final BigDecimal quantity;

  public final BigDecimal cashAmount;

  public final Long legIndex;

  public AuctionMatch(String buyer, String seller, BigDecimal quantity, BigDecimal cashAmount,
      Long legIndex) {
    this.buyer = buyer;
    this.seller = seller;
    this.quantity = quantity;
    this.cashAmount = cashAmount;
    this.legIndex = legIndex;
  }

  public static ValueDecoder<AuctionMatch> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0,
          recordValue$);
      String buyer = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String seller = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      BigDecimal quantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      BigDecimal cashAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(3).getValue());
      Long legIndex = PrimitiveValueDecoders.fromInt64.decode(fields$.get(4).getValue());
      return new AuctionMatch(buyer, seller, quantity, cashAmount, legIndex);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(5);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("buyer", new Party(this.buyer)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("seller", new Party(this.seller)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("quantity", new Numeric(this.quantity)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashAmount", new Numeric(this.cashAmount)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("legIndex", new Int64(this.legIndex)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AuctionMatch> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("buyer", "seller", "quantity", "cashAmount", "legIndex"), name -> {
          switch (name) {
            case "buyer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "seller": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "quantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "cashAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "legIndex": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            default: return null;
          }
        }
        , (Object[] args) -> new AuctionMatch(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static AuctionMatch fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("buyer", apply(JsonLfEncoders::party, buyer)),
        JsonLfEncoders.Field.of("seller", apply(JsonLfEncoders::party, seller)),
        JsonLfEncoders.Field.of("quantity", apply(JsonLfEncoders::numeric, quantity)),
        JsonLfEncoders.Field.of("cashAmount", apply(JsonLfEncoders::numeric, cashAmount)),
        JsonLfEncoders.Field.of("legIndex", apply(JsonLfEncoders::int64, legIndex)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AuctionMatch)) {
      return false;
    }
    AuctionMatch other = (AuctionMatch) object;
    return Objects.equals(this.buyer, other.buyer) && Objects.equals(this.seller, other.seller) &&
        Objects.equals(this.quantity, other.quantity) &&
        Objects.equals(this.cashAmount, other.cashAmount) &&
        Objects.equals(this.legIndex, other.legIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.buyer, this.seller, this.quantity, this.cashAmount, this.legIndex);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokensettlement.AuctionMatch(%s, %s, %s, %s, %s)",
        this.buyer, this.seller, this.quantity, this.cashAmount, this.legIndex);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AuctionMatch> get() {
      return jsonDecoder();
    }
  }
}
