package com.lucilla.settlement.model.liquiditymandate;

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
import com.lucilla.settlement.model.settlement.SettlementBatch;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RecordPerformance extends DamlRecord<RecordPerformance> {
  public static final String _packageId = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public final SettlementBatch.ContractId batchCid;

  public final MandateTerms.ContractId termsCid;

  public RecordPerformance(SettlementBatch.ContractId batchCid, MandateTerms.ContractId termsCid) {
    this.batchCid = batchCid;
    this.termsCid = termsCid;
  }

  public static ValueDecoder<RecordPerformance> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      SettlementBatch.ContractId batchCid =
          new SettlementBatch.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected batchCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      MandateTerms.ContractId termsCid =
          new MandateTerms.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected termsCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new RecordPerformance(batchCid, termsCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("batchCid", this.batchCid.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("termsCid", this.termsCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<RecordPerformance> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("batchCid", "termsCid"), name -> {
          switch (name) {
            case "batchCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.settlement.SettlementBatch.ContractId::new));
            case "termsCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.liquiditymandate.MandateTerms.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new RecordPerformance(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static RecordPerformance fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("batchCid", apply(JsonLfEncoders::contractId, batchCid)),
        JsonLfEncoders.Field.of("termsCid", apply(JsonLfEncoders::contractId, termsCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof RecordPerformance)) {
      return false;
    }
    RecordPerformance other = (RecordPerformance) object;
    return Objects.equals(this.batchCid, other.batchCid) &&
        Objects.equals(this.termsCid, other.termsCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.batchCid, this.termsCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.liquiditymandate.RecordPerformance(%s, %s)",
        this.batchCid, this.termsCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<RecordPerformance> get() {
      return jsonDecoder();
    }
  }
}
