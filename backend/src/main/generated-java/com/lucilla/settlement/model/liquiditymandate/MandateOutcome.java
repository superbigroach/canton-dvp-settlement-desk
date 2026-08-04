package com.lucilla.settlement.model.liquiditymandate;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlOptional;
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
import java.util.Optional;

public class MandateOutcome extends DamlRecord<MandateOutcome> {
  public static final String _packageId = "147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d";

  public final MandatePerformance.ContractId performance;

  public final Optional<LiquidityMandate.ContractId> mandate;

  public final MandateTerms.ContractId terms;

  public MandateOutcome(MandatePerformance.ContractId performance,
      Optional<LiquidityMandate.ContractId> mandate, MandateTerms.ContractId terms) {
    this.performance = performance;
    this.mandate = mandate;
    this.terms = terms;
  }

  public static ValueDecoder<MandateOutcome> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      MandatePerformance.ContractId performance =
          new MandatePerformance.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected performance to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Optional<LiquidityMandate.ContractId> mandate = PrimitiveValueDecoders.fromOptional(v$0 ->
              new LiquidityMandate.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected mandate to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(1).getValue());
      MandateTerms.ContractId terms =
          new MandateTerms.ContractId(fields$.get(2).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected terms to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new MandateOutcome(performance, mandate, terms);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("performance", this.performance.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("mandate", DamlOptional.of(this.mandate.map(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("terms", this.terms.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<MandateOutcome> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("performance", "mandate", "terms"), name -> {
          switch (name) {
            case "performance": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.liquiditymandate.MandatePerformance.ContractId::new));
            case "mandate": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.liquiditymandate.LiquidityMandate.ContractId::new)), java.util.Optional.empty());
            case "terms": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.liquiditymandate.MandateTerms.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new MandateOutcome(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static MandateOutcome fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("performance", apply(JsonLfEncoders::contractId, performance)),
        JsonLfEncoders.Field.of("mandate", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), mandate)),
        JsonLfEncoders.Field.of("terms", apply(JsonLfEncoders::contractId, terms)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof MandateOutcome)) {
      return false;
    }
    MandateOutcome other = (MandateOutcome) object;
    return Objects.equals(this.performance, other.performance) &&
        Objects.equals(this.mandate, other.mandate) && Objects.equals(this.terms, other.terms);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.performance, this.mandate, this.terms);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.liquiditymandate.MandateOutcome(%s, %s, %s)",
        this.performance, this.mandate, this.terms);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<MandateOutcome> get() {
      return jsonDecoder();
    }
  }
}
