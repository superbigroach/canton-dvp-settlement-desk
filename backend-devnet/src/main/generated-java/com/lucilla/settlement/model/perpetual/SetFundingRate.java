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
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SetFundingRate extends DamlRecord<SetFundingRate> {
  public static final String _packageId = "87c24b9a3ade1253eebbb4ea1feef8f4b9963f33c7cc6272efb5f79afdef1bb0";

  public final BigDecimal newRate;

  public SetFundingRate(BigDecimal newRate) {
    this.newRate = newRate;
  }

  public static ValueDecoder<SetFundingRate> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      BigDecimal newRate = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(0).getValue());
      return new SetFundingRate(newRate);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("newRate", new Numeric(this.newRate)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<SetFundingRate> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("newRate"), name -> {
          switch (name) {
            case "newRate": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new SetFundingRate(JsonLfDecoders.cast(args[0])));
  }

  public static SetFundingRate fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("newRate", apply(JsonLfEncoders::numeric, newRate)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof SetFundingRate)) {
      return false;
    }
    SetFundingRate other = (SetFundingRate) object;
    return Objects.equals(this.newRate, other.newRate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.newRate);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.perpetual.SetFundingRate(%s)", this.newRate);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<SetFundingRate> get() {
      return jsonDecoder();
    }
  }
}
