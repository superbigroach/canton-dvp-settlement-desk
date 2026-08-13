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

public class AuctionCross_Settle extends DamlRecord<AuctionCross_Settle> {
  public static final String _packageId = "7eca29e115ad24f98fd4190f21ac6d7440ce8f3211675421f555856febed4e5c";

  public final List<MatchSettleInstruction> legs;

  public AuctionCross_Settle(List<MatchSettleInstruction> legs) {
    this.legs = legs;
  }

  public static ValueDecoder<AuctionCross_Settle> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      List<MatchSettleInstruction> legs = PrimitiveValueDecoders.fromList(
            MatchSettleInstruction.valueDecoder()).decode(fields$.get(0).getValue());
      return new AuctionCross_Settle(legs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("legs", this.legs.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AuctionCross_Settle> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("legs"), name -> {
          switch (name) {
            case "legs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(new com.lucilla.settlement.model.tokensettlement.MatchSettleInstruction.JsonDecoder$().get()));
            default: return null;
          }
        }
        , (Object[] args) -> new AuctionCross_Settle(JsonLfDecoders.cast(args[0])));
  }

  public static AuctionCross_Settle fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("legs", apply(JsonLfEncoders.list(MatchSettleInstruction::jsonEncoder), legs)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AuctionCross_Settle)) {
      return false;
    }
    AuctionCross_Settle other = (AuctionCross_Settle) object;
    return Objects.equals(this.legs, other.legs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.legs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokensettlement.AuctionCross_Settle(%s)",
        this.legs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AuctionCross_Settle> get() {
      return jsonDecoder();
    }
  }
}
