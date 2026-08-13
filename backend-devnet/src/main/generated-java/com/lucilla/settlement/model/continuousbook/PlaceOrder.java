package com.lucilla.settlement.model.continuousbook;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlOptional;
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
import com.lucilla.settlement.model.holding.Holding;
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

public class PlaceOrder extends DamlRecord<PlaceOrder> {
  public static final String _packageId = "abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1";

  public final String trader;

  public final BookSide side;

  public final BigDecimal quantity;

  public final Optional<BigDecimal> limitPrice;

  public final TimeInForce timeInForce;

  public final Holding.ContractId holdingCid;

  public PlaceOrder(String trader, BookSide side, BigDecimal quantity,
      Optional<BigDecimal> limitPrice, TimeInForce timeInForce, Holding.ContractId holdingCid) {
    this.trader = trader;
    this.side = side;
    this.quantity = quantity;
    this.limitPrice = limitPrice;
    this.timeInForce = timeInForce;
    this.holdingCid = holdingCid;
  }

  public static ValueDecoder<PlaceOrder> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(6,0,
          recordValue$);
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      BookSide side = BookSide.valueDecoder().decode(fields$.get(1).getValue());
      BigDecimal quantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      Optional<BigDecimal> limitPrice = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromNumeric).decode(fields$.get(3).getValue());
      TimeInForce timeInForce = TimeInForce.valueDecoder().decode(fields$.get(4).getValue());
      Holding.ContractId holdingCid =
          new Holding.ContractId(fields$.get(5).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected holdingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new PlaceOrder(trader, side, quantity, limitPrice, timeInForce, holdingCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(6);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("side", this.side.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("quantity", new Numeric(this.quantity)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("limitPrice", DamlOptional.of(this.limitPrice.map(v$0 -> new Numeric(v$0)))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("timeInForce", this.timeInForce.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("holdingCid", this.holdingCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<PlaceOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("trader", "side", "quantity", "limitPrice", "timeInForce", "holdingCid"), name -> {
          switch (name) {
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "side": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.continuousbook.BookSide.JsonDecoder$().get());
            case "quantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "limitPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10)), java.util.Optional.empty());
            case "timeInForce": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, new com.lucilla.settlement.model.continuousbook.TimeInForce.JsonDecoder$().get());
            case "holdingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new PlaceOrder(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5])));
  }

  public static PlaceOrder fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("trader", apply(JsonLfEncoders::party, trader)),
        JsonLfEncoders.Field.of("side", apply(BookSide::jsonEncoder, side)),
        JsonLfEncoders.Field.of("quantity", apply(JsonLfEncoders::numeric, quantity)),
        JsonLfEncoders.Field.of("limitPrice", apply(JsonLfEncoders.optional(JsonLfEncoders::numeric), limitPrice)),
        JsonLfEncoders.Field.of("timeInForce", apply(TimeInForce::jsonEncoder, timeInForce)),
        JsonLfEncoders.Field.of("holdingCid", apply(JsonLfEncoders::contractId, holdingCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof PlaceOrder)) {
      return false;
    }
    PlaceOrder other = (PlaceOrder) object;
    return Objects.equals(this.trader, other.trader) && Objects.equals(this.side, other.side) &&
        Objects.equals(this.quantity, other.quantity) &&
        Objects.equals(this.limitPrice, other.limitPrice) &&
        Objects.equals(this.timeInForce, other.timeInForce) &&
        Objects.equals(this.holdingCid, other.holdingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.trader, this.side, this.quantity, this.limitPrice, this.timeInForce,
        this.holdingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.continuousbook.PlaceOrder(%s, %s, %s, %s, %s, %s)",
        this.trader, this.side, this.quantity, this.limitPrice, this.timeInForce, this.holdingCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PlaceOrder> get() {
      return jsonDecoder();
    }
  }
}
