package com.lucilla.settlement.model.marketonclose;

import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.holding.Holding;
import com.lucilla.settlement.model.marketonclose.ordercommitment.DeclareTokenHolding;
import com.lucilla.settlement.model.marketonclose.ordercommitment.ReserveHolding;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.Arrays;

public abstract class OrderCommitment extends Variant<OrderCommitment> {
  public static final String _packageId = "d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be";

  public OrderCommitment() {
  }

  public abstract com.daml.ledger.javaapi.data.Variant toValue();

  public static ValueDecoder<OrderCommitment> valueDecoder() {
    return value$ -> {
      com.daml.ledger.javaapi.data.Variant variant$ = value$.asVariant().orElseThrow(() -> new IllegalArgumentException("Expected Variant to build an instance of the Variant com.lucilla.settlement.model.marketonclose.OrderCommitment"));
      if ("ReserveHolding".equals(variant$.getConstructor())) {
        return valueDecoderReserveHolding().decode(variant$);
      }
      if ("DeclareTokenHolding".equals(variant$.getConstructor())) {
        return valueDecoderDeclareTokenHolding().decode(variant$);
      }
      throw new IllegalArgumentException("Found unknown constructor " + variant$.getConstructor() + " for variant com.lucilla.settlement.model.marketonclose.OrderCommitment, expected one of [ReserveHolding, DeclareTokenHolding]. This could be a failed variant downgrade.");
    } ;
  }

  public static JsonLfDecoder<OrderCommitment> jsonDecoder() {
    return JsonLfDecoders.variant(Arrays.asList("ReserveHolding", "DeclareTokenHolding"), name -> {
          switch (name) {
            case "ReserveHolding": return jsonDecoderReserveHolding();
            case "DeclareTokenHolding": return jsonDecoderDeclareTokenHolding();
            default: return null;
          }
        }
        );
  }

  public static OrderCommitment fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  protected abstract JsonLfEncoders.Field fieldForJsonEncoder();

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.variant(OrderCommitment::fieldForJsonEncoder).apply(this);
  }

  private static ValueDecoder<ReserveHolding> valueDecoderReserveHolding() throws
      IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("ReserveHolding", value$);
      Holding.ContractId body =
          new Holding.ContractId(variantValue$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected body to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new ReserveHolding(body);
    } ;
  }

  private static JsonLfDecoder<ReserveHolding> jsonDecoderReserveHolding() {
    return r -> new ReserveHolding(JsonLfDecoders.contractId(Holding.ContractId::new).decode(r));
  }

  private static ValueDecoder<DeclareTokenHolding> valueDecoderDeclareTokenHolding() throws
      IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("DeclareTokenHolding", value$);
      com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId body =
          new com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId(variantValue$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected body to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new DeclareTokenHolding(body);
    } ;
  }

  private static JsonLfDecoder<DeclareTokenHolding> jsonDecoderDeclareTokenHolding() {
    return r -> new DeclareTokenHolding(JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId::new).decode(r));
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<OrderCommitment> get() {
      return jsonDecoder();
    }
  }
}
