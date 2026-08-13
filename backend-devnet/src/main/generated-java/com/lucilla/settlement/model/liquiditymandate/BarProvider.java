package com.lucilla.settlement.model.liquiditymandate;

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

public class BarProvider extends DamlRecord<BarProvider> {
  public static final String _packageId = "504d21e4573fdcb737242ee9149b3e88f1ec7d6bd5a76b5701f4762c36fd8ae4";

  public final String provider;

  public final MandatePerformance.ContractId performanceCid;

  public BarProvider(String provider, MandatePerformance.ContractId performanceCid) {
    this.provider = provider;
    this.performanceCid = performanceCid;
  }

  public static ValueDecoder<BarProvider> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String provider = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      MandatePerformance.ContractId performanceCid =
          new MandatePerformance.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected performanceCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new BarProvider(provider, performanceCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("provider", new Party(this.provider)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("performanceCid", this.performanceCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<BarProvider> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("provider", "performanceCid"), name -> {
          switch (name) {
            case "provider": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "performanceCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.liquiditymandate.MandatePerformance.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new BarProvider(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static BarProvider fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("provider", apply(JsonLfEncoders::party, provider)),
        JsonLfEncoders.Field.of("performanceCid", apply(JsonLfEncoders::contractId, performanceCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof BarProvider)) {
      return false;
    }
    BarProvider other = (BarProvider) object;
    return Objects.equals(this.provider, other.provider) &&
        Objects.equals(this.performanceCid, other.performanceCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.provider, this.performanceCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.liquiditymandate.BarProvider(%s, %s)",
        this.provider, this.performanceCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<BarProvider> get() {
      return jsonDecoder();
    }
  }
}
