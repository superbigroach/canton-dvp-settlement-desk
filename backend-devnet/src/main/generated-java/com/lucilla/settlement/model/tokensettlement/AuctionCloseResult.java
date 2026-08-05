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
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AuctionCloseResult extends DamlRecord<AuctionCloseResult> {
  public static final String _packageId = "d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be";

  public final AuctionCross.ContractId cross;

  public final List<MatchSettlement.ContractId> matches;

  public final List<AuctionAllocationRequest.ContractId> requests;

  public AuctionCloseResult(AuctionCross.ContractId cross, List<MatchSettlement.ContractId> matches,
      List<AuctionAllocationRequest.ContractId> requests) {
    this.cross = cross;
    this.matches = matches;
    this.requests = requests;
  }

  public static ValueDecoder<AuctionCloseResult> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      AuctionCross.ContractId cross =
          new AuctionCross.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected cross to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      List<MatchSettlement.ContractId> matches = PrimitiveValueDecoders.fromList(v$0 ->
              new MatchSettlement.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected matches to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(1).getValue());
      List<AuctionAllocationRequest.ContractId> requests = PrimitiveValueDecoders.fromList(v$0 ->
              new AuctionAllocationRequest.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected requests to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(2).getValue());
      return new AuctionCloseResult(cross, matches, requests);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cross", this.cross.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("matches", this.matches.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("requests", this.requests.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AuctionCloseResult> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("cross", "matches", "requests"), name -> {
          switch (name) {
            case "cross": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.tokensettlement.AuctionCross.ContractId::new));
            case "matches": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.tokensettlement.MatchSettlement.ContractId::new)));
            case "requests": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.tokensettlement.AuctionAllocationRequest.ContractId::new)));
            default: return null;
          }
        }
        , (Object[] args) -> new AuctionCloseResult(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static AuctionCloseResult fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("cross", apply(JsonLfEncoders::contractId, cross)),
        JsonLfEncoders.Field.of("matches", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), matches)),
        JsonLfEncoders.Field.of("requests", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), requests)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AuctionCloseResult)) {
      return false;
    }
    AuctionCloseResult other = (AuctionCloseResult) object;
    return Objects.equals(this.cross, other.cross) && Objects.equals(this.matches, other.matches) &&
        Objects.equals(this.requests, other.requests);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.cross, this.matches, this.requests);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokensettlement.AuctionCloseResult(%s, %s, %s)",
        this.cross, this.matches, this.requests);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AuctionCloseResult> get() {
      return jsonDecoder();
    }
  }
}
