package com.lucilla.settlement.model.basket;

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
import com.lucilla.settlement.model.holding.Holding;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CreationResult extends DamlRecord<CreationResult> {
  public static final String _packageId = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public final Holding.ContractId mintedShares;

  public final BasketReceipt.ContractId receipt;

  public CreationResult(Holding.ContractId mintedShares, BasketReceipt.ContractId receipt) {
    this.mintedShares = mintedShares;
    this.receipt = receipt;
  }

  public static ValueDecoder<CreationResult> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      Holding.ContractId mintedShares =
          new Holding.ContractId(fields$.get(0).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected mintedShares to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      BasketReceipt.ContractId receipt =
          new BasketReceipt.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected receipt to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new CreationResult(mintedShares, receipt);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("mintedShares", this.mintedShares.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("receipt", this.receipt.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<CreationResult> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("mintedShares", "receipt"), name -> {
          switch (name) {
            case "mintedShares": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            case "receipt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.basket.BasketReceipt.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new CreationResult(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static CreationResult fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("mintedShares", apply(JsonLfEncoders::contractId, mintedShares)),
        JsonLfEncoders.Field.of("receipt", apply(JsonLfEncoders::contractId, receipt)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof CreationResult)) {
      return false;
    }
    CreationResult other = (CreationResult) object;
    return Objects.equals(this.mintedShares, other.mintedShares) &&
        Objects.equals(this.receipt, other.receipt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.mintedShares, this.receipt);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.basket.CreationResult(%s, %s)",
        this.mintedShares, this.receipt);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<CreationResult> get() {
      return jsonDecoder();
    }
  }
}
