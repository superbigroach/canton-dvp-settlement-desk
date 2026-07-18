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

public class PublishImbalance extends DamlRecord<PublishImbalance> {
  public static final String _packageId = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public final List<SealedOrder.ContractId> restingOrders;

  public PublishImbalance(List<SealedOrder.ContractId> restingOrders) {
    this.restingOrders = restingOrders;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static PublishImbalance fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<PublishImbalance> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      List<SealedOrder.ContractId> restingOrders = PrimitiveValueDecoders.fromList(v$0 ->
              new SealedOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected restingOrders to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      return new PublishImbalance(restingOrders);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("restingOrders", this.restingOrders.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<PublishImbalance> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("restingOrders"), name -> {
          switch (name) {
            case "restingOrders": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.marketonclose.SealedOrder.ContractId::new)));
            default: return null;
          }
        }
        , (Object[] args) -> new PublishImbalance(JsonLfDecoders.cast(args[0])));
  }

  public static PublishImbalance fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("restingOrders", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), restingOrders)));
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
    return Objects.equals(this.restingOrders, other.restingOrders);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.restingOrders);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.PublishImbalance(%s)",
        this.restingOrders);
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
