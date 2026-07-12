package com.lucilla.settlement.model.marketonclose;

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
import com.lucilla.settlement.model.holding.Holding;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SubmitOrder extends DamlRecord<SubmitOrder> {
  public static final String _packageId = "a515edc777c604a66696e5991316c6e0500be01c634f1dcd1c118c3a0ad8c9fe";

  public final String trader;

  public final Side side;

  public final BigDecimal quantity;

  public final BigDecimal limitPrice;

  public final Holding.ContractId holdingCid;

  public SubmitOrder(String trader, Side side, BigDecimal quantity, BigDecimal limitPrice,
      Holding.ContractId holdingCid) {
    this.trader = trader;
    this.side = side;
    this.quantity = quantity;
    this.limitPrice = limitPrice;
    this.holdingCid = holdingCid;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static SubmitOrder fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<SubmitOrder> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0,
          recordValue$);
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      Side side = Side.valueDecoder().decode(fields$.get(1).getValue());
      BigDecimal quantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      BigDecimal limitPrice = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(3).getValue());
      Holding.ContractId holdingCid =
          new Holding.ContractId(fields$.get(4).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected holdingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new SubmitOrder(trader, side, quantity, limitPrice, holdingCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(5);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("side", this.side.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("quantity", new Numeric(this.quantity)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("limitPrice", new Numeric(this.limitPrice)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("holdingCid", this.holdingCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<SubmitOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("trader", "side", "quantity", "limitPrice", "holdingCid"), name -> {
          switch (name) {
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "side": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.marketonclose.Side.JsonDecoder$().get());
            case "quantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "limitPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "holdingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new SubmitOrder(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static SubmitOrder fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("trader", apply(JsonLfEncoders::party, trader)),
        JsonLfEncoders.Field.of("side", apply(Side::jsonEncoder, side)),
        JsonLfEncoders.Field.of("quantity", apply(JsonLfEncoders::numeric, quantity)),
        JsonLfEncoders.Field.of("limitPrice", apply(JsonLfEncoders::numeric, limitPrice)),
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
    if (!(object instanceof SubmitOrder)) {
      return false;
    }
    SubmitOrder other = (SubmitOrder) object;
    return Objects.equals(this.trader, other.trader) && Objects.equals(this.side, other.side) &&
        Objects.equals(this.quantity, other.quantity) &&
        Objects.equals(this.limitPrice, other.limitPrice) &&
        Objects.equals(this.holdingCid, other.holdingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.trader, this.side, this.quantity, this.limitPrice, this.holdingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.SubmitOrder(%s, %s, %s, %s, %s)",
        this.trader, this.side, this.quantity, this.limitPrice, this.holdingCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<SubmitOrder> get() {
      return jsonDecoder();
    }
  }
}
