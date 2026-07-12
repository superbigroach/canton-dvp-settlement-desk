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
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RunClose extends DamlRecord<RunClose> {
  public static final String _packageId = "a515edc777c604a66696e5991316c6e0500be01c634f1dcd1c118c3a0ad8c9fe";

  public final List<SealedOrder.ContractId> buyOrders;

  public final List<SealedOrder.ContractId> sellOrders;

  public RunClose(List<SealedOrder.ContractId> buyOrders, List<SealedOrder.ContractId> sellOrders) {
    this.buyOrders = buyOrders;
    this.sellOrders = sellOrders;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static RunClose fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<RunClose> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      List<SealedOrder.ContractId> buyOrders = PrimitiveValueDecoders.fromList(v$0 ->
              new SealedOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected buyOrders to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      List<SealedOrder.ContractId> sellOrders = PrimitiveValueDecoders.fromList(v$0 ->
              new SealedOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected sellOrders to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(1).getValue());
      return new RunClose(buyOrders, sellOrders);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("buyOrders", this.buyOrders.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("sellOrders", this.sellOrders.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<RunClose> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("buyOrders", "sellOrders"), name -> {
          switch (name) {
            case "buyOrders": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.marketonclose.SealedOrder.ContractId::new)));
            case "sellOrders": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.marketonclose.SealedOrder.ContractId::new)));
            default: return null;
          }
        }
        , (Object[] args) -> new RunClose(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static RunClose fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("buyOrders", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), buyOrders)),
        JsonLfEncoders.Field.of("sellOrders", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), sellOrders)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof RunClose)) {
      return false;
    }
    RunClose other = (RunClose) object;
    return Objects.equals(this.buyOrders, other.buyOrders) &&
        Objects.equals(this.sellOrders, other.sellOrders);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.buyOrders, this.sellOrders);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.RunClose(%s, %s)",
        this.buyOrders, this.sellOrders);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<RunClose> get() {
      return jsonDecoder();
    }
  }
}
