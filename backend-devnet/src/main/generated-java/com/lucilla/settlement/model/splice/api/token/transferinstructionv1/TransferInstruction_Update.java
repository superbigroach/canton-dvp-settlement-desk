package com.lucilla.settlement.model.splice.api.token.transferinstructionv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
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

public class TransferInstruction_Update extends DamlRecord<TransferInstruction_Update> {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final List<String> extraActors;

  public final ExtraArgs extraArgs;

  public TransferInstruction_Update(List<String> extraActors, ExtraArgs extraArgs) {
    this.extraActors = extraActors;
    this.extraArgs = extraArgs;
  }

  public static ValueDecoder<TransferInstruction_Update> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      List<String> extraActors = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(0).getValue());
      ExtraArgs extraArgs = ExtraArgs.valueDecoder().decode(fields$.get(1).getValue());
      return new TransferInstruction_Update(extraActors, extraArgs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("extraActors", this.extraActors.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("extraArgs", this.extraArgs.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TransferInstruction_Update> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("extraActors", "extraArgs"), name -> {
          switch (name) {
            case "extraActors": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "extraArgs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new TransferInstruction_Update(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static TransferInstruction_Update fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("extraActors", apply(JsonLfEncoders.list(JsonLfEncoders::party), extraActors)),
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
    if (!(object instanceof TransferInstruction_Update)) {
      return false;
    }
    TransferInstruction_Update other = (TransferInstruction_Update) object;
    return Objects.equals(this.extraActors, other.extraActors) &&
        Objects.equals(this.extraArgs, other.extraArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.extraActors, this.extraArgs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstruction_Update(%s, %s)",
        this.extraActors, this.extraArgs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TransferInstruction_Update> get() {
      return jsonDecoder();
    }
  }
}
