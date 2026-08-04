package com.lucilla.settlement.model.tokenstandarddvp;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TokenStandardDvp_Abort extends DamlRecord<TokenStandardDvp_Abort> {
  public static final String _packageId = "147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d";

  public final List<Allocation.ContractId> allocations;

  public TokenStandardDvp_Abort(List<Allocation.ContractId> allocations) {
    this.allocations = allocations;
  }

  public static ValueDecoder<TokenStandardDvp_Abort> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0,
          recordValue$);
      List<Allocation.ContractId> allocations = PrimitiveValueDecoders.fromList(v$0 ->
              new Allocation.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected allocations to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(0).getValue());
      return new TokenStandardDvp_Abort(allocations);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(1);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("allocations", this.allocations.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TokenStandardDvp_Abort> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("allocations"), name -> {
          switch (name) {
            case "allocations": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation.ContractId::new)));
            default: return null;
          }
        }
        , (Object[] args) -> new TokenStandardDvp_Abort(JsonLfDecoders.cast(args[0])));
  }

  public static TokenStandardDvp_Abort fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("allocations", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), allocations)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TokenStandardDvp_Abort)) {
      return false;
    }
    TokenStandardDvp_Abort other = (TokenStandardDvp_Abort) object;
    return Objects.equals(this.allocations, other.allocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.allocations);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokenstandarddvp.TokenStandardDvp_Abort(%s)",
        this.allocations);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TokenStandardDvp_Abort> get() {
      return jsonDecoder();
    }
  }
}
