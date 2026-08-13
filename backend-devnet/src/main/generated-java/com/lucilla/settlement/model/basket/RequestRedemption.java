package com.lucilla.settlement.model.basket;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlOptional;
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
import java.util.Optional;

public class RequestRedemption extends DamlRecord<RequestRedemption> {
  public static final String _packageId = "abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1";

  public final String ap;

  public final BigDecimal shares;

  public final Holding.ContractId basketHoldingCid;

  public final Optional<Holding.ContractId> feeHoldingCid;

  public RequestRedemption(String ap, BigDecimal shares, Holding.ContractId basketHoldingCid,
      Optional<Holding.ContractId> feeHoldingCid) {
    this.ap = ap;
    this.shares = shares;
    this.basketHoldingCid = basketHoldingCid;
    this.feeHoldingCid = feeHoldingCid;
  }

  public static ValueDecoder<RequestRedemption> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(4,1,
          recordValue$);
      String ap = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      BigDecimal shares = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(1).getValue());
      Holding.ContractId basketHoldingCid =
          new Holding.ContractId(fields$.get(2).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected basketHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Optional<Holding.ContractId> feeHoldingCid = PrimitiveValueDecoders.fromOptional(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected feeHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(3).getValue());
      return new RequestRedemption(ap, shares, basketHoldingCid, feeHoldingCid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(4);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("ap", new Party(this.ap)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("shares", new Numeric(this.shares)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("basketHoldingCid", this.basketHoldingCid.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("feeHoldingCid", DamlOptional.of(this.feeHoldingCid.map(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<RequestRedemption> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("ap", "shares", "basketHoldingCid", "feeHoldingCid"), name -> {
          switch (name) {
            case "ap": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "shares": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "basketHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            case "feeHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new)), java.util.Optional.empty());
            default: return null;
          }
        }
        , (Object[] args) -> new RequestRedemption(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3])));
  }

  public static RequestRedemption fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(JsonLfEncoders.Field.of("ap", apply(JsonLfEncoders::party, ap)),
        JsonLfEncoders.Field.of("shares", apply(JsonLfEncoders::numeric, shares)),
        JsonLfEncoders.Field.of("basketHoldingCid", apply(JsonLfEncoders::contractId, basketHoldingCid)),
        JsonLfEncoders.Field.of("feeHoldingCid", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), feeHoldingCid)));
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
        Objects.equals(this.basketHoldingCid, other.basketHoldingCid) &&
        Objects.equals(this.feeHoldingCid, other.feeHoldingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.ap, this.shares, this.basketHoldingCid, this.feeHoldingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.basket.RequestRedemption(%s, %s, %s, %s)",
        this.ap, this.shares, this.basketHoldingCid, this.feeHoldingCid);
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
