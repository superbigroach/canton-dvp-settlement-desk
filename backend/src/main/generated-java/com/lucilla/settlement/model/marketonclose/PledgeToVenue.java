package com.lucilla.settlement.model.marketonclose;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PledgeToVenue extends DamlRecord<PledgeToVenue> {
  public static final String _packageId = "698224dbdf62d308e4973c1fca97d735a0f9802f145920fca7e2e45e4cafc507";

  public final BigDecimal fillQty;

  public final BigDecimal closingPrice;

  public PledgeToVenue(BigDecimal fillQty, BigDecimal closingPrice) {
    this.fillQty = fillQty;
    this.closingPrice = closingPrice;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static PledgeToVenue fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<PledgeToVenue> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      BigDecimal fillQty = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(0).getValue());
      BigDecimal closingPrice = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(1).getValue());
      return new PledgeToVenue(fillQty, closingPrice);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("fillQty", new Numeric(this.fillQty)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("closingPrice", new Numeric(this.closingPrice)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<PledgeToVenue> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("fillQty", "closingPrice"), name -> {
          switch (name) {
            case "fillQty": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "closingPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new PledgeToVenue(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static PledgeToVenue fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("fillQty", apply(JsonLfEncoders::numeric, fillQty)),
        JsonLfEncoders.Field.of("closingPrice", apply(JsonLfEncoders::numeric, closingPrice)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof PledgeToVenue)) {
      return false;
    }
    PledgeToVenue other = (PledgeToVenue) object;
    return Objects.equals(this.fillQty, other.fillQty) &&
        Objects.equals(this.closingPrice, other.closingPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.fillQty, this.closingPrice);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.PledgeToVenue(%s, %s)",
        this.fillQty, this.closingPrice);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PledgeToVenue> get() {
      return jsonDecoder();
    }
  }
}
