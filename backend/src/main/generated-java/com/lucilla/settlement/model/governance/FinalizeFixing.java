package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
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

public class FinalizeFixing extends DamlRecord<FinalizeFixing> {
  public static final String _packageId = "9f697598fdc5fee1bf367e5acd6ca4eb84c7368c987ce1093f58227384f3d0f8";

  public final String finalizer;

  public final FixingSeries.ContractId series;

  public final List<String> publishTo;

  public FinalizeFixing(String finalizer, FixingSeries.ContractId series, List<String> publishTo) {
    this.finalizer = finalizer;
    this.series = series;
    this.publishTo = publishTo;
  }

  public static ValueDecoder<FinalizeFixing> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      String finalizer = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      FixingSeries.ContractId series =
          new FixingSeries.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected series to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      List<String> publishTo = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(2).getValue());
      return new FinalizeFixing(finalizer, series, publishTo);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("finalizer", new Party(this.finalizer)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("series", this.series.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("publishTo", this.publishTo.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<FinalizeFixing> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("finalizer", "series", "publishTo"), name -> {
          switch (name) {
            case "finalizer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "series": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.governance.FixingSeries.ContractId::new));
            case "publishTo": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            default: return null;
          }
        }
        , (Object[] args) -> new FinalizeFixing(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static FinalizeFixing fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("finalizer", apply(JsonLfEncoders::party, finalizer)),
        JsonLfEncoders.Field.of("series", apply(JsonLfEncoders::contractId, series)),
        JsonLfEncoders.Field.of("publishTo", apply(JsonLfEncoders.list(JsonLfEncoders::party), publishTo)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof FinalizeFixing)) {
      return false;
    }
    FinalizeFixing other = (FinalizeFixing) object;
    return Objects.equals(this.finalizer, other.finalizer) &&
        Objects.equals(this.series, other.series) &&
        Objects.equals(this.publishTo, other.publishTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.finalizer, this.series, this.publishTo);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.FinalizeFixing(%s, %s, %s)",
        this.finalizer, this.series, this.publishTo);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<FinalizeFixing> get() {
      return jsonDecoder();
    }
  }
}
