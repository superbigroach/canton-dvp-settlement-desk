package com.lucilla.settlement.model.splice.api.token.allocationrequestv1;

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
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AllocationRequest_Reject extends DamlRecord<AllocationRequest_Reject> {
  public static final String _packageId = "6fe848530b2404017c4a12874c956ad7d5c8a419ee9b040f96b5c13172d2e193";

  public final String actor;

  public final ExtraArgs extraArgs;

  public AllocationRequest_Reject(String actor, ExtraArgs extraArgs) {
    this.actor = actor;
    this.extraArgs = extraArgs;
  }

  public static ValueDecoder<AllocationRequest_Reject> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String actor = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      ExtraArgs extraArgs = ExtraArgs.valueDecoder().decode(fields$.get(1).getValue());
      return new AllocationRequest_Reject(actor, extraArgs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("actor", new Party(this.actor)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("extraArgs", this.extraArgs.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AllocationRequest_Reject> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("actor", "extraArgs"), name -> {
          switch (name) {
            case "actor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "extraArgs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new AllocationRequest_Reject(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static AllocationRequest_Reject fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("actor", apply(JsonLfEncoders::party, actor)),
        JsonLfEncoders.Field.of("extraArgs", apply(ExtraArgs::jsonEncoder, extraArgs)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AllocationRequest_Reject)) {
      return false;
    }
    AllocationRequest_Reject other = (AllocationRequest_Reject) object;
    return Objects.equals(this.actor, other.actor) &&
        Objects.equals(this.extraArgs, other.extraArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.actor, this.extraArgs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationrequestv1.AllocationRequest_Reject(%s, %s)",
        this.actor, this.extraArgs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AllocationRequest_Reject> get() {
      return jsonDecoder();
    }
  }
}
