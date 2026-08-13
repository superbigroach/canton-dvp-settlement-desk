package com.lucilla.settlement.model.liquiditymandate;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class NoteDisclosure extends DamlRecord<NoteDisclosure> {
  public static final String _packageId = "abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1";

  public final String netSide;

  public final BigDecimal netQuantity;

  public NoteDisclosure(String netSide, BigDecimal netQuantity) {
    this.netSide = netSide;
    this.netQuantity = netQuantity;
  }

  public static ValueDecoder<NoteDisclosure> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0,
          recordValue$);
      String netSide = PrimitiveValueDecoders.fromText.decode(fields$.get(0).getValue());
      BigDecimal netQuantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(1).getValue());
      return new NoteDisclosure(netSide, netQuantity);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(2);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("netSide", new Text(this.netSide)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("netQuantity", new Numeric(this.netQuantity)));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<NoteDisclosure> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("netSide", "netQuantity"), name -> {
          switch (name) {
            case "netSide": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "netQuantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new NoteDisclosure(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static NoteDisclosure fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("netSide", apply(JsonLfEncoders::text, netSide)),
        JsonLfEncoders.Field.of("netQuantity", apply(JsonLfEncoders::numeric, netQuantity)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof NoteDisclosure)) {
      return false;
    }
    NoteDisclosure other = (NoteDisclosure) object;
    return Objects.equals(this.netSide, other.netSide) &&
        Objects.equals(this.netQuantity, other.netQuantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.netSide, this.netQuantity);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.liquiditymandate.NoteDisclosure(%s, %s)",
        this.netSide, this.netQuantity);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<NoteDisclosure> get() {
      return jsonDecoder();
    }
  }
}
