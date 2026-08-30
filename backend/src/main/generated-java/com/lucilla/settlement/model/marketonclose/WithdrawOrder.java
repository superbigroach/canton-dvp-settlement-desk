package com.lucilla.settlement.model.marketonclose;

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

public class WithdrawOrder extends DamlRecord<WithdrawOrder> {
  public static final String _packageId = "527a2b50430ceabba40484b4518c4d390781e8db6c016ab3ec5528eea36766ea";

  public final String trader;

  public final SealedOrder.ContractId orderCid;

  public WithdrawOrder(String trader, SealedOrder.ContractId orderCid) {
    this.trader = trader;
    this.orderCid = orderCid;
  }

  public static ValueDecoder<WithdrawOrder> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      SealedOrder.ContractId orderCid =
          new SealedOrder.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected orderCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new WithdrawOrder(trader, orderCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("orderCid", this.orderCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<WithdrawOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("trader", "orderCid"), name -> {
          switch (name) {
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "orderCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.marketonclose.SealedOrder.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new WithdrawOrder(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static WithdrawOrder fromJson(String json) throws JsonLfDecoder.Error {
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
    if (!(object instanceof WithdrawOrder)) {
      return false;
    }
    WithdrawOrder other = (WithdrawOrder) object;
    return Objects.equals(this.trader, other.trader) &&
        Objects.equals(this.orderCid, other.orderCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.trader, this.orderCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.WithdrawOrder(%s, %s)",
        this.trader, this.orderCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<WithdrawOrder> get() {
      return jsonDecoder();
    }
  }
}
