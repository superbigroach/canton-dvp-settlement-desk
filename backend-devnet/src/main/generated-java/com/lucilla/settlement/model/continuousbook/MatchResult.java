package com.lucilla.settlement.model.continuousbook;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
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

public class MatchResult extends DamlRecord<MatchResult> {
  public static final String _packageId = "504d21e4573fdcb737242ee9149b3e88f1ec7d6bd5a76b5701f4762c36fd8ae4";

  public final ContinuousBook.ContractId book;

  public final List<Execution> executions;

  public final List<TapePrint.ContractId> prints;

  public final Optional<RestingOrder.ContractId> residualAggressor;

  public final List<RestingOrder.ContractId> residualContra;

  public MatchResult(ContinuousBook.ContractId book, List<Execution> executions,
      List<TapePrint.ContractId> prints, Optional<RestingOrder.ContractId> residualAggressor,
      List<RestingOrder.ContractId> residualContra) {
    this.book = book;
    this.executions = executions;
    this.prints = prints;
    this.residualAggressor = residualAggressor;
    this.residualContra = residualContra;
  }

  public static ValueDecoder<MatchResult> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0,
          recordValue$);
      ContinuousBook.ContractId book =
          new ContinuousBook.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected book to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      List<Execution> executions = PrimitiveValueDecoders.fromList(Execution.valueDecoder())
          .decode(fields$.get(1).getValue());
      List<TapePrint.ContractId> prints = PrimitiveValueDecoders.fromList(v$0 ->
              new TapePrint.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected prints to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(2).getValue());
      Optional<RestingOrder.ContractId> residualAggressor = PrimitiveValueDecoders.fromOptional(
            v$0 ->
              new RestingOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected residualAggressor to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(3).getValue());
      List<RestingOrder.ContractId> residualContra = PrimitiveValueDecoders.fromList(v$0 ->
              new RestingOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected residualContra to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(4).getValue());
      return new MatchResult(book, executions, prints, residualAggressor, residualContra);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(5);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("book", this.book.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("executions", this.executions.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("prints", this.prints.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("residualAggressor", DamlOptional.of(this.residualAggressor.map(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("residualContra", this.residualContra.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<MatchResult> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("book", "executions", "prints", "residualAggressor", "residualContra"), name -> {
          switch (name) {
            case "book": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.continuousbook.ContinuousBook.ContractId::new));
            case "executions": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(new com.lucilla.settlement.model.continuousbook.Execution.JsonDecoder$().get()));
            case "prints": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.continuousbook.TapePrint.ContractId::new)));
            case "residualAggressor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.continuousbook.RestingOrder.ContractId::new)), java.util.Optional.empty());
            case "residualContra": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.continuousbook.RestingOrder.ContractId::new)));
            default: return null;
          }
        }
        , (Object[] args) -> new MatchResult(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static MatchResult fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("book", apply(JsonLfEncoders::contractId, book)),
        JsonLfEncoders.Field.of("executions", apply(JsonLfEncoders.list(Execution::jsonEncoder), executions)),
        JsonLfEncoders.Field.of("prints", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), prints)),
        JsonLfEncoders.Field.of("residualAggressor", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), residualAggressor)),
        JsonLfEncoders.Field.of("residualContra", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), residualContra)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof MatchResult)) {
      return false;
    }
    MatchResult other = (MatchResult) object;
    return Objects.equals(this.book, other.book) &&
        Objects.equals(this.executions, other.executions) &&
        Objects.equals(this.prints, other.prints) &&
        Objects.equals(this.residualAggressor, other.residualAggressor) &&
        Objects.equals(this.residualContra, other.residualContra);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.book, this.executions, this.prints, this.residualAggressor,
        this.residualContra);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.continuousbook.MatchResult(%s, %s, %s, %s, %s)",
        this.book, this.executions, this.prints, this.residualAggressor, this.residualContra);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<MatchResult> get() {
      return jsonDecoder();
    }
  }
}
