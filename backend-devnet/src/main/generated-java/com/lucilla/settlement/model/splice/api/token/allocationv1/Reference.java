package com.lucilla.settlement.model.splice.api.token.allocationv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlOptional;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.splice.api.token.metadatav1.AnyContract;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Reference extends DamlRecord<Reference> {
  public static final String _packageId = "93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d";

  public final String id;

  public final Optional<AnyContract.ContractId> cid;

  public Reference(String id, Optional<AnyContract.ContractId> cid) {
    this.id = id;
    this.cid = cid;
  }

  public static ValueDecoder<Reference> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,1,
          recordValue$);
      String id = PrimitiveValueDecoders.fromText.decode(fields$.get(0).getValue());
      Optional<AnyContract.ContractId> cid = PrimitiveValueDecoders.fromOptional(v$0 ->
              new AnyContract.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected cid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(1).getValue());
      return new Reference(id, cid);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("id", new Text(this.id)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("cid", DamlOptional.of(this.cid.map(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Reference> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("id", "cid"), name -> {
          switch (name) {
            case "id": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.metadatav1.AnyContract.ContractId::new)), java.util.Optional.empty());
            default: return null;
          }
        }
        , (Object[] args) -> new Reference(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static Reference fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(JsonLfEncoders.Field.of("id", apply(JsonLfEncoders::text, id)),
        JsonLfEncoders.Field.of("cid", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), cid)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof Reference)) {
      return false;
    }
    Reference other = (Reference) object;
    return Objects.equals(this.id, other.id) && Objects.equals(this.cid, other.cid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.cid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationv1.Reference(%s, %s)",
        this.id, this.cid);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Reference> get() {
      return jsonDecoder();
    }
  }
}
