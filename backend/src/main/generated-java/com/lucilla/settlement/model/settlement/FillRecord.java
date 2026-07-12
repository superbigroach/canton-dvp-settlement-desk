package com.lucilla.settlement.model.settlement;

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

public class FillRecord extends DamlRecord<FillRecord> {
  public static final String _packageId = "85b662c2e7d0a4d42bea5a2232989bd04641057da99c8bda47d7f7b912ef699c";

  public final String seller;

  public final String buyer;

  public final BigDecimal quantity;

  public final BigDecimal price;

  public final SettlementReceipt.ContractId receipt;

  public FillRecord(String seller, String buyer, BigDecimal quantity, BigDecimal price,
      SettlementReceipt.ContractId receipt) {
    this.seller = seller;
    this.buyer = buyer;
    this.quantity = quantity;
    this.price = price;
    this.receipt = receipt;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static FillRecord fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<FillRecord> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0,
          recordValue$);
      String seller = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String buyer = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      BigDecimal quantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      BigDecimal price = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(3).getValue());
      SettlementReceipt.ContractId receipt =
          new SettlementReceipt.ContractId(fields$.get(4).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected receipt to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new FillRecord(seller, buyer, quantity, price, receipt);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(5);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("seller", new Party(this.seller)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("buyer", new Party(this.buyer)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("quantity", new Numeric(this.quantity)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("price", new Numeric(this.price)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("receipt", this.receipt.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<FillRecord> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("seller", "buyer", "quantity", "price", "receipt"), name -> {
          switch (name) {
            case "seller": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "buyer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "quantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "price": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "receipt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.settlement.SettlementReceipt.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new FillRecord(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static FillRecord fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("seller", apply(JsonLfEncoders::party, seller)),
        JsonLfEncoders.Field.of("buyer", apply(JsonLfEncoders::party, buyer)),
        JsonLfEncoders.Field.of("quantity", apply(JsonLfEncoders::numeric, quantity)),
        JsonLfEncoders.Field.of("price", apply(JsonLfEncoders::numeric, price)),
        JsonLfEncoders.Field.of("receipt", apply(JsonLfEncoders::contractId, receipt)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof FillRecord)) {
      return false;
    }
    FillRecord other = (FillRecord) object;
    return Objects.equals(this.seller, other.seller) && Objects.equals(this.buyer, other.buyer) &&
        Objects.equals(this.quantity, other.quantity) && Objects.equals(this.price, other.price) &&
        Objects.equals(this.receipt, other.receipt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.seller, this.buyer, this.quantity, this.price, this.receipt);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.settlement.FillRecord(%s, %s, %s, %s, %s)",
        this.seller, this.buyer, this.quantity, this.price, this.receipt);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<FillRecord> get() {
      return jsonDecoder();
    }
  }
}
