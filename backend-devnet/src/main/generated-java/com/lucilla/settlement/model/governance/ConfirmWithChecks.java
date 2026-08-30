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

public class ConfirmWithChecks extends DamlRecord<ConfirmWithChecks> {
  public static final String _packageId = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public final String member;

  public final SignerCheck check;

  public ConfirmWithChecks(String member, SignerCheck check) {
    this.member = member;
    this.check = check;
  }

  public static ValueDecoder<ConfirmWithChecks> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String member = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      SignerCheck check = SignerCheck.valueDecoder().decode(fields$.get(1).getValue());
      return new ConfirmWithChecks(member, check);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("member", new Party(this.member)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("check", this.check.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<ConfirmWithChecks> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("member", "check"), name -> {
          switch (name) {
            case "member": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "check": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.governance.SignerCheck.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new ConfirmWithChecks(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static ConfirmWithChecks fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("member", apply(JsonLfEncoders::party, member)),
        JsonLfEncoders.Field.of("check", apply(SignerCheck::jsonEncoder, check)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof ConfirmWithChecks)) {
      return false;
    }
    ConfirmWithChecks other = (ConfirmWithChecks) object;
    return Objects.equals(this.member, other.member) && Objects.equals(this.check, other.check);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.member, this.check);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.ConfirmWithChecks(%s, %s)",
        this.member, this.check);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ConfirmWithChecks> get() {
      return jsonDecoder();
    }
  }
}
