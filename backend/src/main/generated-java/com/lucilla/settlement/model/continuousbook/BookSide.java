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

public enum BookSide implements DamlEnum<BookSide> {
  BID,

  ASK;

  private static final com.daml.ledger.javaapi.data.DamlEnum[] __values$ = {new com.daml.ledger.javaapi.data.DamlEnum("Bid"), new com.daml.ledger.javaapi.data.DamlEnum("Ask")};

  private static final Map<String, BookSide> __enums$ = __buildEnumsMap$();

  private static final Map<String, BookSide> __buildEnumsMap$() {
    Map<String, BookSide> m = new HashMap<String, BookSide>();
    m.put("Bid", BID);
    m.put("Ask", ASK);
    return m;
  }

  public static final ValueDecoder<BookSide> valueDecoder() {
    return value$ -> {
      String constructor$ = value$.asEnum().orElseThrow(() -> new IllegalArgumentException("Expected DamlEnum to build an instance of the Enum com.lucilla.settlement.model.continuousbook.BookSide")).getConstructor();
      if (!__enums$.containsKey(constructor$)) throw new IllegalArgumentException("Found unknown constructor " + constructor$ + " for enum com.lucilla.settlement.model.continuousbook.BookSide, expected one of [Bid, Ask]. This could be a failed enum downgrade.");
      return __enums$.get(constructor$);
    } ;
  }

  public final com.daml.ledger.javaapi.data.DamlEnum toValue() {
    return __values$[ordinal()];
  }

  public static JsonLfDecoder<BookSide> jsonDecoder() {
    return JsonLfDecoders.enumeration(__enums$);
  }

  public static BookSide fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public String getConstructor() {
    return toValue().getConstructor();
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.enumeration((BookSide e$) -> e$.getConstructor()).apply(this);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<BookSide> get() {
      return jsonDecoder();
    }
  }
}
