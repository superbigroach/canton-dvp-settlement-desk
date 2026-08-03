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
import com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TransferFactoryView extends DamlRecord<TransferFactoryView> {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final String admin;

  public final Metadata meta;

  public TransferFactoryView(String admin, Metadata meta) {
    this.admin = admin;
    this.meta = meta;
  }

  public static ValueDecoder<TransferFactoryView> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(1).getValue());
      return new TransferFactoryView(admin, meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("admin", new Party(this.admin)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TransferFactoryView> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("admin", "meta"), name -> {
          switch (name) {
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new TransferFactoryView(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static TransferFactoryView fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)),
        JsonLfEncoders.Field.of("meta", apply(Metadata::jsonEncoder, meta)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TransferFactoryView)) {
      return false;
    }
    TransferFactoryView other = (TransferFactoryView) object;
    return Objects.equals(this.admin, other.admin) && Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.admin, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferFactoryView(%s, %s)",
        this.admin, this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TransferFactoryView> get() {
      return jsonDecoder();
    }
  }
}
