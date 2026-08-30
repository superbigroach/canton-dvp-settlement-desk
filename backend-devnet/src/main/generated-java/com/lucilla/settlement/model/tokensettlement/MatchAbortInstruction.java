package com.lucilla.settlement.model.tokensettlement;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MatchAbortInstruction extends DamlRecord<MatchAbortInstruction> {
  public static final String _packageId = "527a2b50430ceabba40484b4518c4d390781e8db6c016ab3ec5528eea36766ea";

  public final MatchSettlement.ContractId matchCid;

  public final List<Allocation.ContractId> allocations;

  public final ExtraArgs extraArgs;

  public MatchAbortInstruction(MatchSettlement.ContractId matchCid,
      List<Allocation.ContractId> allocations, ExtraArgs extraArgs) {
    this.matchCid = matchCid;
    this.allocations = allocations;
    this.extraArgs = extraArgs;
  }

  public static ValueDecoder<MatchAbortInstruction> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      MatchSettlement.ContractId matchCid =
          new MatchSettlement.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected matchCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      List<Allocation.ContractId> allocations = PrimitiveValueDecoders.fromList(v$0 ->
              new Allocation.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected allocations to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(1).getValue());
      ExtraArgs extraArgs = ExtraArgs.valueDecoder().decode(fields$.get(2).getValue());
      return new MatchAbortInstruction(matchCid, allocations, extraArgs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("matchCid", this.matchCid.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("allocations", this.allocations.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("extraArgs", this.extraArgs.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<MatchAbortInstruction> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("matchCid", "allocations", "extraArgs"), name -> {
          switch (name) {
            case "matchCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.tokensettlement.MatchSettlement.ContractId::new));
            case "allocations": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation.ContractId::new)));
            case "extraArgs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, new com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new MatchAbortInstruction(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static MatchAbortInstruction fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("matchCid", apply(JsonLfEncoders::contractId, matchCid)),
        JsonLfEncoders.Field.of("allocations", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), allocations)),
        JsonLfEncoders.Field.of("extraArgs", apply(ExtraArgs::jsonEncoder, extraArgs)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof MatchAbortInstruction)) {
      return false;
    }
    MatchAbortInstruction other = (MatchAbortInstruction) object;
    return Objects.equals(this.matchCid, other.matchCid) &&
        Objects.equals(this.allocations, other.allocations) &&
        Objects.equals(this.extraArgs, other.extraArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.matchCid, this.allocations, this.extraArgs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokensettlement.MatchAbortInstruction(%s, %s, %s)",
        this.matchCid, this.allocations, this.extraArgs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<MatchAbortInstruction> get() {
      return jsonDecoder();
    }
  }
}
