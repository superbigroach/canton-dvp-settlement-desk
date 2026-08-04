package com.lucilla.settlement.model.marketonclose;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Timestamp;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RunCloseTokenStandard extends DamlRecord<RunCloseTokenStandard> {
  public static final String _packageId = "147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d";

  public final List<SealedOrder.ContractId> buyOrders;

  public final List<SealedOrder.ContractId> sellOrders;

  public final String assetAdmin;

  public final String cashAdmin;

  public final String settlementId;

  public final Instant allocateBefore;

  public final Instant settleBefore;

  public RunCloseTokenStandard(List<SealedOrder.ContractId> buyOrders,
      List<SealedOrder.ContractId> sellOrders, String assetAdmin, String cashAdmin,
      String settlementId, Instant allocateBefore, Instant settleBefore) {
    this.buyOrders = buyOrders;
    this.sellOrders = sellOrders;
    this.assetAdmin = assetAdmin;
    this.cashAdmin = cashAdmin;
    this.settlementId = settlementId;
    this.allocateBefore = allocateBefore;
    this.settleBefore = settleBefore;
  }

  public static ValueDecoder<RunCloseTokenStandard> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(7,0,
          recordValue$);
      List<SealedOrder.ContractId> buyOrders = PrimitiveValueDecoders.fromList(v$0 ->
              new SealedOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected buyOrders to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      List<SealedOrder.ContractId> sellOrders = PrimitiveValueDecoders.fromList(v$0 ->
              new SealedOrder.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected sellOrders to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(1).getValue());
      String assetAdmin = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String cashAdmin = PrimitiveValueDecoders.fromParty.decode(fields$.get(3).getValue());
      String settlementId = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      Instant allocateBefore = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(5).getValue());
      Instant settleBefore = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(6).getValue());
      return new RunCloseTokenStandard(buyOrders, sellOrders, assetAdmin, cashAdmin, settlementId,
          allocateBefore, settleBefore);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(7);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("buyOrders", this.buyOrders.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("sellOrders", this.sellOrders.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("assetAdmin", new Party(this.assetAdmin)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cashAdmin", new Party(this.cashAdmin)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("settlementId", new Text(this.settlementId)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("allocateBefore", Timestamp.fromInstant(this.allocateBefore)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("settleBefore", Timestamp.fromInstant(this.settleBefore)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<RunCloseTokenStandard> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("buyOrders", "sellOrders", "assetAdmin", "cashAdmin", "settlementId", "allocateBefore", "settleBefore"), name -> {
          switch (name) {
            case "buyOrders": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.marketonclose.SealedOrder.ContractId::new)));
            case "sellOrders": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.marketonclose.SealedOrder.ContractId::new)));
            case "assetAdmin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "cashAdmin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "settlementId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "allocateBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "settleBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new RunCloseTokenStandard(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6])));
  }

  public static RunCloseTokenStandard fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("buyOrders", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), buyOrders)),
        JsonLfEncoders.Field.of("sellOrders", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), sellOrders)),
        JsonLfEncoders.Field.of("assetAdmin", apply(JsonLfEncoders::party, assetAdmin)),
        JsonLfEncoders.Field.of("cashAdmin", apply(JsonLfEncoders::party, cashAdmin)),
        JsonLfEncoders.Field.of("settlementId", apply(JsonLfEncoders::text, settlementId)),
        JsonLfEncoders.Field.of("allocateBefore", apply(JsonLfEncoders::timestamp, allocateBefore)),
        JsonLfEncoders.Field.of("settleBefore", apply(JsonLfEncoders::timestamp, settleBefore)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof RunCloseTokenStandard)) {
      return false;
    }
    RunCloseTokenStandard other = (RunCloseTokenStandard) object;
    return Objects.equals(this.buyOrders, other.buyOrders) &&
        Objects.equals(this.sellOrders, other.sellOrders) &&
        Objects.equals(this.assetAdmin, other.assetAdmin) &&
        Objects.equals(this.cashAdmin, other.cashAdmin) &&
        Objects.equals(this.settlementId, other.settlementId) &&
        Objects.equals(this.allocateBefore, other.allocateBefore) &&
        Objects.equals(this.settleBefore, other.settleBefore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.buyOrders, this.sellOrders, this.assetAdmin, this.cashAdmin,
        this.settlementId, this.allocateBefore, this.settleBefore);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.RunCloseTokenStandard(%s, %s, %s, %s, %s, %s, %s)",
        this.buyOrders, this.sellOrders, this.assetAdmin, this.cashAdmin, this.settlementId,
        this.allocateBefore, this.settleBefore);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<RunCloseTokenStandard> get() {
      return jsonDecoder();
    }
  }
}
