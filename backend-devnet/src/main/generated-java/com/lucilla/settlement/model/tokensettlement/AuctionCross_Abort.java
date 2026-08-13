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

public class AuctionCross_Abort extends DamlRecord<AuctionCross_Abort> {
  public static final String _packageId = "7eca29e115ad24f98fd4190f21ac6d7440ce8f3211675421f555856febed4e5c";

  public final List<MatchAbortInstruction> pairs;

  public AuctionCross_Abort(List<MatchAbortInstruction> pairs) {
    this.pairs = pairs;
  }

  public static ValueDecoder<AuctionCross_Abort> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      List<MatchAbortInstruction> pairs = PrimitiveValueDecoders.fromList(
            MatchAbortInstruction.valueDecoder()).decode(fields$.get(0).getValue());
      return new AuctionCross_Abort(pairs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("pairs", this.pairs.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AuctionCross_Abort> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("pairs"), name -> {
          switch (name) {
            case "pairs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(new com.lucilla.settlement.model.tokensettlement.MatchAbortInstruction.JsonDecoder$().get()));
            default: return null;
          }
        }
        , (Object[] args) -> new AuctionCross_Abort(JsonLfDecoders.cast(args[0])));
  }

  public static AuctionCross_Abort fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("pairs", apply(JsonLfEncoders.list(MatchAbortInstruction::jsonEncoder), pairs)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AuctionCross_Abort)) {
      return false;
    }
    AuctionCross_Abort other = (AuctionCross_Abort) object;
    return Objects.equals(this.pairs, other.pairs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.pairs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokensettlement.AuctionCross_Abort(%s)",
        this.pairs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AuctionCross_Abort> get() {
      return jsonDecoder();
    }
  }
}
