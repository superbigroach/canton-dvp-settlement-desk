package com.lucilla.settlement.model.marketonclose;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.liquiditymandate.LiquidityMandate;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PublishImbalance extends DamlRecord<PublishImbalance> {
  public static final String _packageId = "d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be";

  public final List<SealedOrder.ContractId> restingOrders;

  public final LiquidityMandate.ContractId mandateCid;

  public PublishImbalance(List<SealedOrder.ContractId> restingOrders,
      LiquidityMandate.ContractId mandateCid) {
    this.restingOrders = restingOrders;
    this.mandateCid = mandateCid;
  }

  public static ValueDecoder<PublishImbalance> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      List<SealedOrder.ContractId> restingOrders = PrimitiveValueDecoders.fromList(v$0 ->
              new SealedOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected restingOrders to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      LiquidityMandate.ContractId mandateCid =
          new LiquidityMandate.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected mandateCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new PublishImbalance(restingOrders, mandateCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("restingOrders", this.restingOrders.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("mandateCid", this.mandateCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<PublishImbalance> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("restingOrders", "mandateCid"), name -> {
          switch (name) {
            case "restingOrders": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.marketonclose.SealedOrder.ContractId::new)));
            case "mandateCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.liquiditymandate.LiquidityMandate.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new PublishImbalance(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static PublishImbalance fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("restingOrders", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), restingOrders)),
        JsonLfEncoders.Field.of("mandateCid", apply(JsonLfEncoders::contractId, mandateCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof PublishImbalance)) {
      return false;
    }
    PublishImbalance other = (PublishImbalance) object;
    return Objects.equals(this.restingOrders, other.restingOrders) &&
        Objects.equals(this.mandateCid, other.mandateCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.restingOrders, this.mandateCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.PublishImbalance(%s, %s)",
        this.restingOrders, this.mandateCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PublishImbalance> get() {
      return jsonDecoder();
    }
  }
}
