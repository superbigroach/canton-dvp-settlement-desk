package com.lucilla.settlement.model.continuousbook;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CancelOrder extends DamlRecord<CancelOrder> {
  public static final String _packageId = "504d21e4573fdcb737242ee9149b3e88f1ec7d6bd5a76b5701f4762c36fd8ae4";

  public final String trader;

  public final RestingOrder.ContractId orderCid;

  public CancelOrder(String trader, RestingOrder.ContractId orderCid) {
    this.trader = trader;
    this.orderCid = orderCid;
  }

  public static ValueDecoder<CancelOrder> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      RestingOrder.ContractId orderCid =
          new RestingOrder.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected orderCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new CancelOrder(trader, orderCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("orderCid", this.orderCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<CancelOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("trader", "orderCid"), name -> {
          switch (name) {
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "orderCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.continuousbook.RestingOrder.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new CancelOrder(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static CancelOrder fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("trader", apply(JsonLfEncoders::party, trader)),
        JsonLfEncoders.Field.of("orderCid", apply(JsonLfEncoders::contractId, orderCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof CancelOrder)) {
      return false;
    }
    CancelOrder other = (CancelOrder) object;
    return Objects.equals(this.trader, other.trader) &&
        Objects.equals(this.orderCid, other.orderCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.trader, this.orderCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.continuousbook.CancelOrder(%s, %s)",
        this.trader, this.orderCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<CancelOrder> get() {
      return jsonDecoder();
    }
  }
}
