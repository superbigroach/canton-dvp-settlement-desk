package com.lucilla.settlement.model.tokenstandarddvp;

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

public class TokenStandardDvpProposal_Accept extends DamlRecord<TokenStandardDvpProposal_Accept> {
  public static final String _packageId = "cd6202b647482a998c93612fd615750e35250bcfb57272e00d9198ebe014161a";

  public final String approver;

  public TokenStandardDvpProposal_Accept(String approver) {
    this.approver = approver;
  }

  public static ValueDecoder<TokenStandardDvpProposal_Accept> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      String approver = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      return new TokenStandardDvpProposal_Accept(approver);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("approver", new Party(this.approver)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TokenStandardDvpProposal_Accept> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("approver"), name -> {
          switch (name) {
            case "approver": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            default: return null;
          }
        }
        , (Object[] args) -> new TokenStandardDvpProposal_Accept(JsonLfDecoders.cast(args[0])));
  }

  public static TokenStandardDvpProposal_Accept fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("approver", apply(JsonLfEncoders::party, approver)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TokenStandardDvpProposal_Accept)) {
      return false;
    }
    TokenStandardDvpProposal_Accept other = (TokenStandardDvpProposal_Accept) object;
    return Objects.equals(this.approver, other.approver);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.approver);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokenstandarddvp.TokenStandardDvpProposal_Accept(%s)",
        this.approver);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TokenStandardDvpProposal_Accept> get() {
      return jsonDecoder();
    }
  }
}
