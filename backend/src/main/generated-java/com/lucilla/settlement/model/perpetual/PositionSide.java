package com.lucilla.settlement.model.perpetual;

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

public enum PositionSide implements DamlEnum<PositionSide> {
  LONG,

  SHORT;

  private static final com.daml.ledger.javaapi.data.DamlEnum[] __values$ = {new com.daml.ledger.javaapi.data.DamlEnum("Long"), new com.daml.ledger.javaapi.data.DamlEnum("Short")};

  private static final Map<String, PositionSide> __enums$ = __buildEnumsMap$();

  private static final Map<String, PositionSide> __buildEnumsMap$() {
    Map<String, PositionSide> m = new HashMap<String, PositionSide>();
    m.put("Long", LONG);
    m.put("Short", SHORT);
    return m;
  }

  public static final ValueDecoder<PositionSide> valueDecoder() {
    return value$ -> {
      String constructor$ = value$.asEnum().orElseThrow(() -> new IllegalArgumentException("Expected DamlEnum to build an instance of the Enum com.lucilla.settlement.model.perpetual.PositionSide")).getConstructor();
      if (!__enums$.containsKey(constructor$)) throw new IllegalArgumentException("Found unknown constructor " + constructor$ + " for enum com.lucilla.settlement.model.perpetual.PositionSide, expected one of [Long, Short]. This could be a failed enum downgrade.");
      return __enums$.get(constructor$);
    } ;
  }

  public final com.daml.ledger.javaapi.data.DamlEnum toValue() {
    return __values$[ordinal()];
  }

  public static JsonLfDecoder<PositionSide> jsonDecoder() {
    return JsonLfDecoders.enumeration(__enums$);
  }

  public static PositionSide fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public String getConstructor() {
    return toValue().getConstructor();
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.enumeration((PositionSide e$) -> e$.getConstructor()).apply(this);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PositionSide> get() {
      return jsonDecoder();
    }
  }
}
