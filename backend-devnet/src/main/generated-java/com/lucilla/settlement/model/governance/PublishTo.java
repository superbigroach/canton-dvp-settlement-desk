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

public class PublishTo extends DamlRecord<PublishTo> {
  public static final String _packageId = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public final String attestor;

  public final String venue;

  public PublishTo(String attestor, String venue) {
    this.attestor = attestor;
    this.venue = venue;
  }

  public static ValueDecoder<PublishTo> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String attestor = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String venue = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      return new PublishTo(attestor, venue);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("attestor", new Party(this.attestor)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("venue", new Party(this.venue)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<PublishTo> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("attestor", "venue"), name -> {
          switch (name) {
            case "attestor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "venue": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            default: return null;
          }
        }
        , (Object[] args) -> new PublishTo(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static PublishTo fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("attestor", apply(JsonLfEncoders::party, attestor)),
        JsonLfEncoders.Field.of("venue", apply(JsonLfEncoders::party, venue)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof PublishTo)) {
      return false;
    }
    PublishTo other = (PublishTo) object;
    return Objects.equals(this.attestor, other.attestor) && Objects.equals(this.venue, other.venue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.attestor, this.venue);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.PublishTo(%s, %s)", this.attestor,
        this.venue);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PublishTo> get() {
      return jsonDecoder();
    }
  }
}
