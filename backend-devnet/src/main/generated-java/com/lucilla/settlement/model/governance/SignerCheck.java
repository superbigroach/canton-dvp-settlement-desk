package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlOptional;
import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Party;
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
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SignerCheck extends DamlRecord<SignerCheck> {
  public static final String _packageId = "527a2b50430ceabba40484b4518c4d390781e8db6c016ab3ec5528eea36766ea";

  public final String member;

  public final String role;

  public final String protocolRef;

  public final List<String> checksPassed;

  public final Optional<BigDecimal> observedLow;

  public final Optional<BigDecimal> observedHigh;

  public SignerCheck(String member, String role, String protocolRef, List<String> checksPassed,
      Optional<BigDecimal> observedLow, Optional<BigDecimal> observedHigh) {
    this.member = member;
    this.role = role;
    this.protocolRef = protocolRef;
    this.checksPassed = checksPassed;
    this.observedLow = observedLow;
    this.observedHigh = observedHigh;
  }

  public static ValueDecoder<SignerCheck> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(6,2,
          recordValue$);
      String member = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String role = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      String protocolRef = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      List<String> checksPassed = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromText)
          .decode(fields$.get(3).getValue());
      Optional<BigDecimal> observedLow = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromNumeric).decode(fields$.get(4).getValue());
      Optional<BigDecimal> observedHigh = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromNumeric).decode(fields$.get(5).getValue());
      return new SignerCheck(member, role, protocolRef, checksPassed, observedLow, observedHigh);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(6);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("member", new Party(this.member)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("role", new Text(this.role)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("protocolRef", new Text(this.protocolRef)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("checksPassed", this.checksPassed.stream().collect(DamlCollectors.toDamlList(v$0 -> new Text(v$0)))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("observedLow", DamlOptional.of(this.observedLow.map(v$0 -> new Numeric(v$0)))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("observedHigh", DamlOptional.of(this.observedHigh.map(v$0 -> new Numeric(v$0)))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<SignerCheck> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("member", "role", "protocolRef", "checksPassed", "observedLow", "observedHigh"), name -> {
          switch (name) {
            case "member": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "role": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "protocolRef": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "checksPassed": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text));
            case "observedLow": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10)), java.util.Optional.empty());
            case "observedHigh": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10)), java.util.Optional.empty());
            default: return null;
          }
        }
        , (Object[] args) -> new SignerCheck(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5])));
  }

  public static SignerCheck fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("member", apply(JsonLfEncoders::party, member)),
        JsonLfEncoders.Field.of("role", apply(JsonLfEncoders::text, role)),
        JsonLfEncoders.Field.of("protocolRef", apply(JsonLfEncoders::text, protocolRef)),
        JsonLfEncoders.Field.of("checksPassed", apply(JsonLfEncoders.list(JsonLfEncoders::text), checksPassed)),
        JsonLfEncoders.Field.of("observedLow", apply(JsonLfEncoders.optional(JsonLfEncoders::numeric), observedLow)),
        JsonLfEncoders.Field.of("observedHigh", apply(JsonLfEncoders.optional(JsonLfEncoders::numeric), observedHigh)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof SignerCheck)) {
      return false;
    }
    SignerCheck other = (SignerCheck) object;
    return Objects.equals(this.member, other.member) && Objects.equals(this.role, other.role) &&
        Objects.equals(this.protocolRef, other.protocolRef) &&
        Objects.equals(this.checksPassed, other.checksPassed) &&
        Objects.equals(this.observedLow, other.observedLow) &&
        Objects.equals(this.observedHigh, other.observedHigh);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.member, this.role, this.protocolRef, this.checksPassed,
        this.observedLow, this.observedHigh);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.SignerCheck(%s, %s, %s, %s, %s, %s)",
        this.member, this.role, this.protocolRef, this.checksPassed, this.observedLow,
        this.observedHigh);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<SignerCheck> get() {
      return jsonDecoder();
    }
  }
}
