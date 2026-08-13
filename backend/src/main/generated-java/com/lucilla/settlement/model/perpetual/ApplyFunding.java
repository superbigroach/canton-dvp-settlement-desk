package com.lucilla.settlement.model.perpetual;

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

public class ApplyFunding extends DamlRecord<ApplyFunding> {
  public static final String _packageId = "87c24b9a3ade1253eebbb4ea1feef8f4b9963f33c7cc6272efb5f79afdef1bb0";

  public final PerpMarket.ContractId marketCid;

  public ApplyFunding(PerpMarket.ContractId marketCid) {
    this.marketCid = marketCid;
  }

  public static ValueDecoder<ApplyFunding> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      PerpMarket.ContractId marketCid =
          new PerpMarket.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected marketCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new ApplyFunding(marketCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("marketCid", this.marketCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<ApplyFunding> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("marketCid"), name -> {
          switch (name) {
            case "marketCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.perpetual.PerpMarket.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new ApplyFunding(JsonLfDecoders.cast(args[0])));
  }

  public static ApplyFunding fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("marketCid", apply(JsonLfEncoders::contractId, marketCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof ApplyFunding)) {
      return false;
    }
    ApplyFunding other = (ApplyFunding) object;
    return Objects.equals(this.marketCid, other.marketCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.marketCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.perpetual.ApplyFunding(%s)", this.marketCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ApplyFunding> get() {
      return jsonDecoder();
    }
  }
}
