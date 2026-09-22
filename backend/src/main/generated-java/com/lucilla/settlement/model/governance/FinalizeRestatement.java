package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FinalizeRestatement extends DamlRecord<FinalizeRestatement> {
  public static final String _packageId = "9f697598fdc5fee1bf367e5acd6ca4eb84c7368c987ce1093f58227384f3d0f8";

  public final String finalizer;

  public FinalizeRestatement(String finalizer) {
    this.finalizer = finalizer;
  }

  public static ValueDecoder<FinalizeRestatement> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      String finalizer = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      return new FinalizeRestatement(finalizer);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("finalizer", new Party(this.finalizer)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<FinalizeRestatement> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("finalizer"), name -> {
          switch (name) {
            case "finalizer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            default: return null;
          }
        }
        , (Object[] args) -> new FinalizeRestatement(JsonLfDecoders.cast(args[0])));
  }

  public static FinalizeRestatement fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("finalizer", apply(JsonLfEncoders::party, finalizer)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof FinalizeRestatement)) {
      return false;
    }
    FinalizeRestatement other = (FinalizeRestatement) object;
    return Objects.equals(this.finalizer, other.finalizer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.finalizer);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.FinalizeRestatement(%s)",
        this.finalizer);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<FinalizeRestatement> get() {
      return jsonDecoder();
    }
  }
}
