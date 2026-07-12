package com.lucilla.settlement.model.agent;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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

public class InitiateDvP extends DamlRecord<InitiateDvP> {
  public static final String _packageId = "12f056257e4f6e96f8abcaafbc3d7261e58f3fcddcba133f3033b91190110371";

  public final String counterparty;

  public final String auditor;

  public final Holding.ContractId assetHoldingCid;

  public final Holding.ContractId cashHoldingCid;

  public final BigDecimal assetAmount;

  public final String cashInstrument;

  public final BigDecimal cashAmount;

  public InitiateDvP(String counterparty, String auditor, Holding.ContractId assetHoldingCid,
      Holding.ContractId cashHoldingCid, BigDecimal assetAmount, String cashInstrument,
      BigDecimal cashAmount) {
    this.counterparty = counterparty;
    this.auditor = auditor;
    this.assetHoldingCid = assetHoldingCid;
    this.cashHoldingCid = cashHoldingCid;
    this.assetAmount = assetAmount;
    this.cashInstrument = cashInstrument;
    this.cashAmount = cashAmount;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static InitiateDvP fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<InitiateDvP> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(7,0,
          recordValue$);
      String counterparty = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      Holding.ContractId assetHoldingCid =
          new Holding.ContractId(fields$.get(2).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected assetHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Holding.ContractId cashHoldingCid =
          new Holding.ContractId(fields$.get(3).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected cashHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      BigDecimal assetAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(4).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      BigDecimal cashAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      return new InitiateDvP(counterparty, auditor, assetHoldingCid, cashHoldingCid, assetAmount,
          cashInstrument, cashAmount);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(7);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("counterparty", new Party(this.counterparty)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("assetHoldingCid", this.assetHoldingCid.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashHoldingCid", this.cashHoldingCid.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("assetAmount", new Numeric(this.assetAmount)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashAmount", new Numeric(this.cashAmount)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<InitiateDvP> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("counterparty", "auditor", "assetHoldingCid", "cashHoldingCid", "assetAmount", "cashInstrument", "cashAmount"), name -> {
          switch (name) {
            case "counterparty": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "assetHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            case "cashHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            case "assetAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new InitiateDvP(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6])));
  }

  public static InitiateDvP fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("counterparty", apply(JsonLfEncoders::party, counterparty)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("assetHoldingCid", apply(JsonLfEncoders::contractId, assetHoldingCid)),
        JsonLfEncoders.Field.of("cashHoldingCid", apply(JsonLfEncoders::contractId, cashHoldingCid)),
        JsonLfEncoders.Field.of("assetAmount", apply(JsonLfEncoders::numeric, assetAmount)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("cashAmount", apply(JsonLfEncoders::numeric, cashAmount)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof InitiateDvP)) {
      return false;
    }
    InitiateDvP other = (InitiateDvP) object;
    return Objects.equals(this.counterparty, other.counterparty) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.assetHoldingCid, other.assetHoldingCid) &&
        Objects.equals(this.cashHoldingCid, other.cashHoldingCid) &&
        Objects.equals(this.assetAmount, other.assetAmount) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.cashAmount, other.cashAmount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.counterparty, this.auditor, this.assetHoldingCid, this.cashHoldingCid,
        this.assetAmount, this.cashInstrument, this.cashAmount);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.agent.InitiateDvP(%s, %s, %s, %s, %s, %s, %s)",
        this.counterparty, this.auditor, this.assetHoldingCid, this.cashHoldingCid,
        this.assetAmount, this.cashInstrument, this.cashAmount);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<InitiateDvP> get() {
      return jsonDecoder();
    }
  }
}
