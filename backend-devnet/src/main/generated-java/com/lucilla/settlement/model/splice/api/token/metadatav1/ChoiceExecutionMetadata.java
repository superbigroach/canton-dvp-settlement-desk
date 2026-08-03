package com.lucilla.settlement.model.splice.api.token.metadatav1;

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
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ChoiceExecutionMetadata extends DamlRecord<ChoiceExecutionMetadata> {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public final Metadata meta;

  public ChoiceExecutionMetadata(Metadata meta) {
    this.meta = meta;
  }

  public static ValueDecoder<ChoiceExecutionMetadata> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(0).getValue());
      return new ChoiceExecutionMetadata(meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<ChoiceExecutionMetadata> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("meta"), name -> {
          switch (name) {
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new ChoiceExecutionMetadata(JsonLfDecoders.cast(args[0])));
  }

  public static ChoiceExecutionMetadata fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
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
    if (!(object instanceof ChoiceExecutionMetadata)) {
      return false;
    }
    ChoiceExecutionMetadata other = (ChoiceExecutionMetadata) object;
    return Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.metadatav1.ChoiceExecutionMetadata(%s)",
        this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ChoiceExecutionMetadata> get() {
      return jsonDecoder();
    }
  }
}
