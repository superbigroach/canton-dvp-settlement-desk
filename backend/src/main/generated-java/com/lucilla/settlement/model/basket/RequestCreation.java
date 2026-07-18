package com.lucilla.settlement.model.basket;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Numeric;
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
import com.lucilla.settlement.model.holding.Holding;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RequestCreation extends DamlRecord<RequestCreation> {
  public static final String _packageId = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public final String ap;

  public final BigDecimal shares;

  public final List<Holding.ContractId> componentHoldingCids;

  public RequestCreation(String ap, BigDecimal shares,
      List<Holding.ContractId> componentHoldingCids) {
    this.ap = ap;
    this.shares = shares;
    this.componentHoldingCids = componentHoldingCids;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static RequestCreation fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<RequestCreation> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      String ap = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      BigDecimal shares = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(1).getValue());
      List<Holding.ContractId> componentHoldingCids = PrimitiveValueDecoders.fromList(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected componentHoldingCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(2).getValue());
      return new RequestCreation(ap, shares, componentHoldingCids);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("ap", new Party(this.ap)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("shares", new Numeric(this.shares)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("componentHoldingCids", this.componentHoldingCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<RequestCreation> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("ap", "shares", "componentHoldingCids"), name -> {
          switch (name) {
            case "ap": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "shares": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "componentHoldingCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new)));
            default: return null;
          }
        }
        , (Object[] args) -> new RequestCreation(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static RequestCreation fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(JsonLfEncoders.Field.of("ap", apply(JsonLfEncoders::party, ap)),
        JsonLfEncoders.Field.of("shares", apply(JsonLfEncoders::numeric, shares)),
        JsonLfEncoders.Field.of("componentHoldingCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), componentHoldingCids)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof RequestCreation)) {
      return false;
    }
    RequestCreation other = (RequestCreation) object;
    return Objects.equals(this.ap, other.ap) && Objects.equals(this.shares, other.shares) &&
        Objects.equals(this.componentHoldingCids, other.componentHoldingCids);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.ap, this.shares, this.componentHoldingCids);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.basket.RequestCreation(%s, %s, %s)", this.ap,
        this.shares, this.componentHoldingCids);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<RequestCreation> get() {
      return jsonDecoder();
    }
  }
}
