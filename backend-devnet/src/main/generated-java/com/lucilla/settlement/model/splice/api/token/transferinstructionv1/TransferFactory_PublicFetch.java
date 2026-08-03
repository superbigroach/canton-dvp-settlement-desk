package com.lucilla.settlement.model.splice.api.token.transferinstructionv1;

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

public class TransferFactory_PublicFetch extends DamlRecord<TransferFactory_PublicFetch> {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final String expectedAdmin;

  public final String actor;

  public TransferFactory_PublicFetch(String expectedAdmin, String actor) {
    this.expectedAdmin = expectedAdmin;
    this.actor = actor;
  }

  public static ValueDecoder<TransferFactory_PublicFetch> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String expectedAdmin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String actor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      return new TransferFactory_PublicFetch(expectedAdmin, actor);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("expectedAdmin", new Party(this.expectedAdmin)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("actor", new Party(this.actor)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TransferFactory_PublicFetch> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("expectedAdmin", "actor"), name -> {
          switch (name) {
            case "expectedAdmin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "actor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            default: return null;
          }
        }
        , (Object[] args) -> new TransferFactory_PublicFetch(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static TransferFactory_PublicFetch fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("expectedAdmin", apply(JsonLfEncoders::party, expectedAdmin)),
        JsonLfEncoders.Field.of("actor", apply(JsonLfEncoders::party, actor)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TransferFactory_PublicFetch)) {
      return false;
    }
    TransferFactory_PublicFetch other = (TransferFactory_PublicFetch) object;
    return Objects.equals(this.expectedAdmin, other.expectedAdmin) &&
        Objects.equals(this.actor, other.actor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expectedAdmin, this.actor);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferFactory_PublicFetch(%s, %s)",
        this.expectedAdmin, this.actor);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TransferFactory_PublicFetch> get() {
      return jsonDecoder();
    }
  }
}
