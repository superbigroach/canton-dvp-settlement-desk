package com.lucilla.settlement.model.perpetual;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Numeric;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AddCollateral extends DamlRecord<AddCollateral> {
  public static final String _packageId = "d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be";

  public final BigDecimal extra;

  public final Holding.ContractId extraCid;

  public AddCollateral(BigDecimal extra, Holding.ContractId extraCid) {
    this.extra = extra;
    this.extraCid = extraCid;
  }

  public static ValueDecoder<AddCollateral> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      BigDecimal extra = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(0).getValue());
      Holding.ContractId extraCid =
          new Holding.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected extraCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new AddCollateral(extra, extraCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("extra", new Numeric(this.extra)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("extraCid", this.extraCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AddCollateral> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("extra", "extraCid"), name -> {
          switch (name) {
            case "extra": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "extraCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new AddCollateral(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static AddCollateral fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("extra", apply(JsonLfEncoders::numeric, extra)),
        JsonLfEncoders.Field.of("extraCid", apply(JsonLfEncoders::contractId, extraCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AddCollateral)) {
      return false;
    }
    AddCollateral other = (AddCollateral) object;
    return Objects.equals(this.extra, other.extra) && Objects.equals(this.extraCid, other.extraCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.extra, this.extraCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.perpetual.AddCollateral(%s, %s)", this.extra,
        this.extraCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AddCollateral> get() {
      return jsonDecoder();
    }
  }
}
