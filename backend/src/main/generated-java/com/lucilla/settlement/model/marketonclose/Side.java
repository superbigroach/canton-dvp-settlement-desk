package com.lucilla.settlement.model.marketonclose;

import com.daml.ledger.javaapi.data.codegen.DamlEnum;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;

public enum Side implements DamlEnum<Side> {
  BUY,

  SELL;

  private static final com.daml.ledger.javaapi.data.DamlEnum[] __values$ = {new com.daml.ledger.javaapi.data.DamlEnum("Buy"), new com.daml.ledger.javaapi.data.DamlEnum("Sell")};

  private static final Map<String, Side> __enums$ = __buildEnumsMap$();

  private static final Map<String, Side> __buildEnumsMap$() {
    Map<String, Side> m = new HashMap<String, Side>();
    m.put("Buy", BUY);
    m.put("Sell", SELL);
    return m;
  }

  public static final ValueDecoder<Side> valueDecoder() {
    return value$ -> {
      String constructor$ = value$.asEnum().orElseThrow(() -> new IllegalArgumentException("Expected DamlEnum to build an instance of the Enum com.lucilla.settlement.model.marketonclose.Side")).getConstructor();
      if (!__enums$.containsKey(constructor$)) throw new IllegalArgumentException("Found unknown constructor " + constructor$ + " for enum com.lucilla.settlement.model.marketonclose.Side, expected one of [Buy, Sell]. This could be a failed enum downgrade.");
      return __enums$.get(constructor$);
    } ;
  }

  public final com.daml.ledger.javaapi.data.DamlEnum toValue() {
    return __values$[ordinal()];
  }

  public static JsonLfDecoder<Side> jsonDecoder() {
    return JsonLfDecoders.enumeration(__enums$);
  }

  public static Side fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public String getConstructor() {
    return toValue().getConstructor();
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.enumeration((Side e$) -> e$.getConstructor()).apply(this);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Side> get() {
      return jsonDecoder();
    }
  }
}
