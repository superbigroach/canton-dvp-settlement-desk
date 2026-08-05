package com.lucilla.settlement.model.marketonclose;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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

public class ClearOrder extends DamlRecord<ClearOrder> {
  public static final String _packageId = "16b1d7198cf7c7ec9373fe2d1bdb48ab1770fe7ffcb7281ad87048ebecd45ab4";

  public final SealedOrder.ContractId orderCid;

  public ClearOrder(SealedOrder.ContractId orderCid) {
    this.orderCid = orderCid;
  }

  public static ValueDecoder<ClearOrder> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      SealedOrder.ContractId orderCid =
          new SealedOrder.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected orderCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new ClearOrder(orderCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("orderCid", this.orderCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<ClearOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("orderCid"), name -> {
          switch (name) {
            case "orderCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.marketonclose.SealedOrder.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new ClearOrder(JsonLfDecoders.cast(args[0])));
  }

  public static ClearOrder fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
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
    if (!(object instanceof ClearOrder)) {
      return false;
    }
    ClearOrder other = (ClearOrder) object;
    return Objects.equals(this.orderCid, other.orderCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.orderCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.ClearOrder(%s)",
        this.orderCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ClearOrder> get() {
      return jsonDecoder();
    }
  }
}
