package com.lucilla.settlement.model.liquiditymandate;

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

public class AdmitParticipant extends DamlRecord<AdmitParticipant> {
  public static final String _packageId = "147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d";

  public final String participant;

  public AdmitParticipant(String participant) {
    this.participant = participant;
  }

  public static ValueDecoder<AdmitParticipant> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      String participant = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      return new AdmitParticipant(participant);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("participant", new Party(this.participant)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AdmitParticipant> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("participant"), name -> {
          switch (name) {
            case "participant": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            default: return null;
          }
        }
        , (Object[] args) -> new AdmitParticipant(JsonLfDecoders.cast(args[0])));
  }

  public static AdmitParticipant fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("participant", apply(JsonLfEncoders::party, participant)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AdmitParticipant)) {
      return false;
    }
    AdmitParticipant other = (AdmitParticipant) object;
    return Objects.equals(this.participant, other.participant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.participant);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.liquiditymandate.AdmitParticipant(%s)",
        this.participant);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AdmitParticipant> get() {
      return jsonDecoder();
    }
  }
}
