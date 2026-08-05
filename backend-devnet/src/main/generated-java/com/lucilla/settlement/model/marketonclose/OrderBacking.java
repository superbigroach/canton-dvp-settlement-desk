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
import com.lucilla.settlement.model.marketonclose.orderbacking.DeclaredToken;
import com.lucilla.settlement.model.marketonclose.orderbacking.ReservedHolding;
import com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.Arrays;

public abstract class OrderBacking extends Variant<OrderBacking> {
  public static final String _packageId = "d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be";

  public OrderBacking() {
  }

  public abstract com.daml.ledger.javaapi.data.Variant toValue();

  public static ValueDecoder<OrderBacking> valueDecoder() {
    return value$ -> {
      com.daml.ledger.javaapi.data.Variant variant$ = value$.asVariant().orElseThrow(() -> new IllegalArgumentException("Expected Variant to build an instance of the Variant com.lucilla.settlement.model.marketonclose.OrderBacking"));
      if ("ReservedHolding".equals(variant$.getConstructor())) {
        return valueDecoderReservedHolding().decode(variant$);
      }
      if ("DeclaredToken".equals(variant$.getConstructor())) {
        return valueDecoderDeclaredToken().decode(variant$);
      }
      throw new IllegalArgumentException("Found unknown constructor " + variant$.getConstructor() + " for variant com.lucilla.settlement.model.marketonclose.OrderBacking, expected one of [ReservedHolding, DeclaredToken]. This could be a failed variant downgrade.");
    } ;
  }

  public static JsonLfDecoder<OrderBacking> jsonDecoder() {
    return JsonLfDecoders.variant(Arrays.asList("ReservedHolding", "DeclaredToken"), name -> {
          switch (name) {
            case "ReservedHolding": return jsonDecoderReservedHolding();
            case "DeclaredToken": return jsonDecoderDeclaredToken();
            default: return null;
          }
        }
        );
  }

  public static OrderBacking fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  protected abstract JsonLfEncoders.Field fieldForJsonEncoder();

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.variant(OrderBacking::fieldForJsonEncoder).apply(this);
  }

  private static ValueDecoder<ReservedHolding> valueDecoderReservedHolding() throws
      IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("ReservedHolding", value$);
      Holding.ContractId body =
          new Holding.ContractId(variantValue$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected body to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new ReservedHolding(body);
    } ;
  }

  private static JsonLfDecoder<ReservedHolding> jsonDecoderReservedHolding() {
    return r -> new ReservedHolding(JsonLfDecoders.contractId(Holding.ContractId::new).decode(r));
  }

  private static ValueDecoder<DeclaredToken> valueDecoderDeclaredToken() throws
      IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("DeclaredToken", value$);
      InstrumentId body = InstrumentId.valueDecoder().decode(variantValue$);
      return new DeclaredToken(body);
    } ;
  }

  private static JsonLfDecoder<DeclaredToken> jsonDecoderDeclaredToken() {
    return r -> new DeclaredToken(new InstrumentId.JsonDecoder$().get().decode(r));
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<OrderBacking> get() {
      return jsonDecoder();
    }
  }
}
