package com.lucilla.settlement.model.continuousbook;

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

public enum TimeInForce implements DamlEnum<TimeInForce> {
  GTC,

  IOC,

  FOK,

  AON;

  private static final com.daml.ledger.javaapi.data.DamlEnum[] __values$ = {new com.daml.ledger.javaapi.data.DamlEnum("GTC"), new com.daml.ledger.javaapi.data.DamlEnum("IOC"), new com.daml.ledger.javaapi.data.DamlEnum("FOK"), new com.daml.ledger.javaapi.data.DamlEnum("AON")};

  private static final Map<String, TimeInForce> __enums$ = __buildEnumsMap$();

  private static final Map<String, TimeInForce> __buildEnumsMap$() {
    Map<String, TimeInForce> m = new HashMap<String, TimeInForce>();
    m.put("GTC", GTC);
    m.put("IOC", IOC);
    m.put("FOK", FOK);
    m.put("AON", AON);
    return m;
  }

  public static final ValueDecoder<TimeInForce> valueDecoder() {
    return value$ -> {
      String constructor$ = value$.asEnum().orElseThrow(() -> new IllegalArgumentException("Expected DamlEnum to build an instance of the Enum com.lucilla.settlement.model.continuousbook.TimeInForce")).getConstructor();
      if (!__enums$.containsKey(constructor$)) throw new IllegalArgumentException("Found unknown constructor " + constructor$ + " for enum com.lucilla.settlement.model.continuousbook.TimeInForce, expected one of [GTC, IOC, FOK, AON]. This could be a failed enum downgrade.");
      return __enums$.get(constructor$);
    } ;
  }

  public final com.daml.ledger.javaapi.data.DamlEnum toValue() {
    return __values$[ordinal()];
  }

  public static JsonLfDecoder<TimeInForce> jsonDecoder() {
    return JsonLfDecoders.enumeration(__enums$);
  }

  public static TimeInForce fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public String getConstructor() {
    return toValue().getConstructor();
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.enumeration((TimeInForce e$) -> e$.getConstructor()).apply(this);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TimeInForce> get() {
      return jsonDecoder();
    }
  }
}
