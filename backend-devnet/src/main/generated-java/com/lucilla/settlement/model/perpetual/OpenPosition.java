package com.lucilla.settlement.model.perpetual;

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
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class OpenPosition extends DamlRecord<OpenPosition> {
  public static final String _packageId = "abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1";

  public final String trader;

  public final PositionSide side;

  public final BigDecimal size;

  public final BigDecimal collateral;

  public final Holding.ContractId collateralCid;

  public OpenPosition(String trader, PositionSide side, BigDecimal size, BigDecimal collateral,
      Holding.ContractId collateralCid) {
    this.trader = trader;
    this.side = side;
    this.size = size;
    this.collateral = collateral;
    this.collateralCid = collateralCid;
  }

  public static ValueDecoder<OpenPosition> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0,
          recordValue$);
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      PositionSide side = PositionSide.valueDecoder().decode(fields$.get(1).getValue());
      BigDecimal size = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      BigDecimal collateral = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(3).getValue());
      Holding.ContractId collateralCid =
          new Holding.ContractId(fields$.get(4).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected collateralCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new OpenPosition(trader, side, size, collateral, collateralCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(5);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("side", this.side.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("size", new Numeric(this.size)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("collateral", new Numeric(this.collateral)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("collateralCid", this.collateralCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<OpenPosition> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("trader", "side", "size", "collateral", "collateralCid"), name -> {
          switch (name) {
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "side": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.perpetual.PositionSide.JsonDecoder$().get());
            case "size": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "collateral": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "collateralCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new OpenPosition(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static OpenPosition fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("trader", apply(JsonLfEncoders::party, trader)),
        JsonLfEncoders.Field.of("side", apply(PositionSide::jsonEncoder, side)),
        JsonLfEncoders.Field.of("size", apply(JsonLfEncoders::numeric, size)),
        JsonLfEncoders.Field.of("collateral", apply(JsonLfEncoders::numeric, collateral)),
        JsonLfEncoders.Field.of("collateralCid", apply(JsonLfEncoders::contractId, collateralCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof OpenPosition)) {
      return false;
    }
    OpenPosition other = (OpenPosition) object;
    return Objects.equals(this.trader, other.trader) && Objects.equals(this.side, other.side) &&
        Objects.equals(this.size, other.size) &&
        Objects.equals(this.collateral, other.collateral) &&
        Objects.equals(this.collateralCid, other.collateralCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.trader, this.side, this.size, this.collateral, this.collateralCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.perpetual.OpenPosition(%s, %s, %s, %s, %s)",
        this.trader, this.side, this.size, this.collateral, this.collateralCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<OpenPosition> get() {
      return jsonDecoder();
    }
  }
}
