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
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TransferFactory_Transfer extends DamlRecord<TransferFactory_Transfer> {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final String expectedAdmin;

  public final Transfer transfer;

  public final ExtraArgs extraArgs;

  public TransferFactory_Transfer(String expectedAdmin, Transfer transfer, ExtraArgs extraArgs) {
    this.expectedAdmin = expectedAdmin;
    this.transfer = transfer;
    this.extraArgs = extraArgs;
  }

  public static ValueDecoder<TransferFactory_Transfer> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      String expectedAdmin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      Transfer transfer = Transfer.valueDecoder().decode(fields$.get(1).getValue());
      ExtraArgs extraArgs = ExtraArgs.valueDecoder().decode(fields$.get(2).getValue());
      return new TransferFactory_Transfer(expectedAdmin, transfer, extraArgs);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("expectedAdmin", new Party(this.expectedAdmin)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("transfer", this.transfer.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("extraArgs", this.extraArgs.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TransferFactory_Transfer> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("expectedAdmin", "transfer", "extraArgs"), name -> {
          switch (name) {
            case "expectedAdmin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "transfer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, new com.lucilla.settlement.model.splice.api.token.transferinstructionv1.Transfer.JsonDecoder$().get());
            case "extraArgs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, new com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new TransferFactory_Transfer(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static TransferFactory_Transfer fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("expectedAdmin", apply(JsonLfEncoders::party, expectedAdmin)),
        JsonLfEncoders.Field.of("transfer", apply(Transfer::jsonEncoder, transfer)),
        JsonLfEncoders.Field.of("extraArgs", apply(ExtraArgs::jsonEncoder, extraArgs)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof TransferFactory_Transfer)) {
      return false;
    }
    TransferFactory_Transfer other = (TransferFactory_Transfer) object;
    return Objects.equals(this.expectedAdmin, other.expectedAdmin) &&
        Objects.equals(this.transfer, other.transfer) &&
        Objects.equals(this.extraArgs, other.extraArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expectedAdmin, this.transfer, this.extraArgs);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferFactory_Transfer(%s, %s, %s)",
        this.expectedAdmin, this.transfer, this.extraArgs);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TransferFactory_Transfer> get() {
      return jsonDecoder();
    }
  }
}
