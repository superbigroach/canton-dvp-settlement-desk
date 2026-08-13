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
import com.lucilla.settlement.model.holding.Holding;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FundInsurance extends DamlRecord<FundInsurance> {
  public static final String _packageId = "7eca29e115ad24f98fd4190f21ac6d7440ce8f3211675421f555856febed4e5c";

  public final Holding.ContractId poolCid;

  public FundInsurance(Holding.ContractId poolCid) {
    this.poolCid = poolCid;
  }

  public static ValueDecoder<FundInsurance> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      Holding.ContractId poolCid =
          new Holding.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected poolCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new FundInsurance(poolCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("poolCid", this.poolCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<FundInsurance> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("poolCid"), name -> {
          switch (name) {
            case "poolCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new FundInsurance(JsonLfDecoders.cast(args[0])));
  }

  public static FundInsurance fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("poolCid", apply(JsonLfEncoders::contractId, poolCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof FundInsurance)) {
      return false;
    }
    FundInsurance other = (FundInsurance) object;
    return Objects.equals(this.poolCid, other.poolCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.poolCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.perpetual.FundInsurance(%s)", this.poolCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<FundInsurance> get() {
      return jsonDecoder();
    }
  }
}
