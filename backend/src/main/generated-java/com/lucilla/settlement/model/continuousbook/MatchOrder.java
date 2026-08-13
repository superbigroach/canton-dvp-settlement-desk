package com.lucilla.settlement.model.continuousbook;

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

public class MatchOrder extends DamlRecord<MatchOrder> {
  public static final String _packageId = "abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1";

  public final RestingOrder.ContractId aggressorCid;

  public final List<RestingOrder.ContractId> contraCids;

  public MatchOrder(RestingOrder.ContractId aggressorCid,
      List<RestingOrder.ContractId> contraCids) {
    this.aggressorCid = aggressorCid;
    this.contraCids = contraCids;
  }

  public static ValueDecoder<MatchOrder> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      RestingOrder.ContractId aggressorCid =
          new RestingOrder.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected aggressorCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      List<RestingOrder.ContractId> contraCids = PrimitiveValueDecoders.fromList(v$0 ->
              new RestingOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected contraCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(1).getValue());
      return new MatchOrder(aggressorCid, contraCids);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("aggressorCid", this.aggressorCid.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("contraCids", this.contraCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<MatchOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("aggressorCid", "contraCids"), name -> {
          switch (name) {
            case "aggressorCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.continuousbook.RestingOrder.ContractId::new));
            case "contraCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.continuousbook.RestingOrder.ContractId::new)));
            default: return null;
          }
        }
        , (Object[] args) -> new MatchOrder(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static MatchOrder fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("aggressorCid", apply(JsonLfEncoders::contractId, aggressorCid)),
        JsonLfEncoders.Field.of("contraCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), contraCids)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof MatchOrder)) {
      return false;
    }
    MatchOrder other = (MatchOrder) object;
    return Objects.equals(this.aggressorCid, other.aggressorCid) &&
        Objects.equals(this.contraCids, other.contraCids);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.aggressorCid, this.contraCids);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.continuousbook.MatchOrder(%s, %s)",
        this.aggressorCid, this.contraCids);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<MatchOrder> get() {
      return jsonDecoder();
    }
  }
}
