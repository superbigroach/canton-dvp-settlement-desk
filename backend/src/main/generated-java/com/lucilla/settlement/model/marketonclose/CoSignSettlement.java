package com.lucilla.settlement.model.marketonclose;

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
import com.lucilla.settlement.model.tokensettlement.MatchSettlement;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CoSignSettlement extends DamlRecord<CoSignSettlement> {
  public static final String _packageId = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public final MatchSettlement.ContractId settlementCid;

  public final List<String> currentSigners;

  public final List<SealedOrder.ContractId> alsoSign;

  public CoSignSettlement(MatchSettlement.ContractId settlementCid, List<String> currentSigners,
      List<SealedOrder.ContractId> alsoSign) {
    this.settlementCid = settlementCid;
    this.currentSigners = currentSigners;
    this.alsoSign = alsoSign;
  }

  public static ValueDecoder<CoSignSettlement> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      MatchSettlement.ContractId settlementCid =
          new MatchSettlement.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected settlementCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      List<String> currentSigners = PrimitiveValueDecoders.fromList(
            PrimitiveValueDecoders.fromParty).decode(fields$.get(1).getValue());
      List<SealedOrder.ContractId> alsoSign = PrimitiveValueDecoders.fromList(v$0 ->
              new SealedOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected alsoSign to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(2).getValue());
      return new CoSignSettlement(settlementCid, currentSigners, alsoSign);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("settlementCid", this.settlementCid.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("currentSigners", this.currentSigners.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("alsoSign", this.alsoSign.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<CoSignSettlement> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("settlementCid", "currentSigners", "alsoSign"), name -> {
          switch (name) {
            case "settlementCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.tokensettlement.MatchSettlement.ContractId::new));
            case "currentSigners": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "alsoSign": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.marketonclose.SealedOrder.ContractId::new)));
            default: return null;
          }
        }
        , (Object[] args) -> new CoSignSettlement(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static CoSignSettlement fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("settlementCid", apply(JsonLfEncoders::contractId, settlementCid)),
        JsonLfEncoders.Field.of("currentSigners", apply(JsonLfEncoders.list(JsonLfEncoders::party), currentSigners)),
        JsonLfEncoders.Field.of("alsoSign", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), alsoSign)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof CoSignSettlement)) {
      return false;
    }
    CoSignSettlement other = (CoSignSettlement) object;
    return Objects.equals(this.settlementCid, other.settlementCid) &&
        Objects.equals(this.currentSigners, other.currentSigners) &&
        Objects.equals(this.alsoSign, other.alsoSign);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.settlementCid, this.currentSigners, this.alsoSign);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.CoSignSettlement(%s, %s, %s)",
        this.settlementCid, this.currentSigners, this.alsoSign);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<CoSignSettlement> get() {
      return jsonDecoder();
    }
  }
}
