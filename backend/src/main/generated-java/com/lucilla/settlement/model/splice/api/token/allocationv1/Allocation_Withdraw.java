package com.lucilla.settlement.model.splice.api.token.allocationv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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

public class Allocation_Withdraw extends DamlRecord<Allocation_Withdraw> {
  public static final String _packageId = "93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d";

  public final ExtraArgs extraArgs;

  public Allocation_Withdraw(ExtraArgs extraArgs) {
    this.extraArgs = extraArgs;
  }

  public static ValueDecoder<Allocation_Withdraw> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      ExtraArgs extraArgs = ExtraArgs.valueDecoder().decode(fields$.get(0).getValue());
      return new Allocation_Withdraw(extraArgs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("extraArgs", this.extraArgs.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Allocation_Withdraw> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("extraArgs"), name -> {
          switch (name) {
            case "extraArgs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, new com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new Allocation_Withdraw(JsonLfDecoders.cast(args[0])));
  }

  public static Allocation_Withdraw fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
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
    if (!(object instanceof Allocation_Withdraw)) {
      return false;
    }
    Allocation_Withdraw other = (Allocation_Withdraw) object;
    return Objects.equals(this.extraArgs, other.extraArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.extraArgs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation_Withdraw(%s)",
        this.extraArgs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Allocation_Withdraw> get() {
      return jsonDecoder();
    }
  }
}
