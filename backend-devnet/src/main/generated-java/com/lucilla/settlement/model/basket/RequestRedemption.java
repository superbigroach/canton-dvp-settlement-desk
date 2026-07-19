package com.lucilla.settlement.model.basket;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RequestRedemption extends DamlRecord<RequestRedemption> {
  public static final String _packageId = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public final String ap;

  public final BigDecimal shares;

  public final Holding.ContractId basketHoldingCid;

  public RequestRedemption(String ap, BigDecimal shares, Holding.ContractId basketHoldingCid) {
    this.ap = ap;
    this.shares = shares;
    this.basketHoldingCid = basketHoldingCid;
  }

  public static ValueDecoder<RequestRedemption> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      String ap = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      BigDecimal shares = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(1).getValue());
      Holding.ContractId basketHoldingCid =
          new Holding.ContractId(fields$.get(2).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected basketHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new RequestRedemption(ap, shares, basketHoldingCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("ap", new Party(this.ap)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("shares", new Numeric(this.shares)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("basketHoldingCid", this.basketHoldingCid.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<RequestRedemption> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("ap", "shares", "basketHoldingCid"), name -> {
          switch (name) {
            case "ap": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "shares": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "basketHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new RequestRedemption(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static RequestRedemption fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(JsonLfEncoders.Field.of("ap", apply(JsonLfEncoders::party, ap)),
        JsonLfEncoders.Field.of("shares", apply(JsonLfEncoders::numeric, shares)),
        JsonLfEncoders.Field.of("basketHoldingCid", apply(JsonLfEncoders::contractId, basketHoldingCid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof RequestRedemption)) {
      return false;
    }
    RequestRedemption other = (RequestRedemption) object;
    return Objects.equals(this.ap, other.ap) && Objects.equals(this.shares, other.shares) &&
        Objects.equals(this.basketHoldingCid, other.basketHoldingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.ap, this.shares, this.basketHoldingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.basket.RequestRedemption(%s, %s, %s)",
        this.ap, this.shares, this.basketHoldingCid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<RequestRedemption> get() {
      return jsonDecoder();
    }
  }
}
