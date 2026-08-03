package com.lucilla.settlement.model.splice.api.token.metadatav1;

import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.Variant;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.da.time.types.RelTime;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_Bool;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_ContractId;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_Date;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_Decimal;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_Int;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_List;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_Map;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_Party;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_RelTime;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_Text;
import com.lucilla.settlement.model.splice.api.token.metadatav1.anyvalue.AV_Time;
import java.lang.Boolean;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.String;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AnyValue extends Variant<AnyValue> {
  public static final String _packageId = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public AnyValue() {
  }

  public abstract com.daml.ledger.javaapi.data.Variant toValue();

  public static ValueDecoder<AnyValue> valueDecoder() {
    return value$ -> {
      com.daml.ledger.javaapi.data.Variant variant$ = value$.asVariant().orElseThrow(() -> new IllegalArgumentException("Expected Variant to build an instance of the Variant com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue"));
      if ("AV_Text".equals(variant$.getConstructor())) {
        return valueDecoderAV_Text().decode(variant$);
      }
      if ("AV_Int".equals(variant$.getConstructor())) {
        return valueDecoderAV_Int().decode(variant$);
      }
      if ("AV_Decimal".equals(variant$.getConstructor())) {
        return valueDecoderAV_Decimal().decode(variant$);
      }
      if ("AV_Bool".equals(variant$.getConstructor())) {
        return valueDecoderAV_Bool().decode(variant$);
      }
      if ("AV_Date".equals(variant$.getConstructor())) {
        return valueDecoderAV_Date().decode(variant$);
      }
      if ("AV_Time".equals(variant$.getConstructor())) {
        return valueDecoderAV_Time().decode(variant$);
      }
      if ("AV_RelTime".equals(variant$.getConstructor())) {
        return valueDecoderAV_RelTime().decode(variant$);
      }
      if ("AV_Party".equals(variant$.getConstructor())) {
        return valueDecoderAV_Party().decode(variant$);
      }
      if ("AV_ContractId".equals(variant$.getConstructor())) {
        return valueDecoderAV_ContractId().decode(variant$);
      }
      if ("AV_List".equals(variant$.getConstructor())) {
        return valueDecoderAV_List().decode(variant$);
      }
      if ("AV_Map".equals(variant$.getConstructor())) {
        return valueDecoderAV_Map().decode(variant$);
      }
      throw new IllegalArgumentException("Found unknown constructor " + variant$.getConstructor() + " for variant com.lucilla.settlement.model.splice.api.token.metadatav1.AnyValue, expected one of [AV_Text, AV_Int, AV_Decimal, AV_Bool, AV_Date, AV_Time, AV_RelTime, AV_Party, AV_ContractId, AV_List, AV_Map]. This could be a failed variant downgrade.");
    } ;
  }

  public static JsonLfDecoder<AnyValue> jsonDecoder() {
    return JsonLfDecoders.variant(Arrays.asList("AV_Text", "AV_Int", "AV_Decimal", "AV_Bool", "AV_Date", "AV_Time", "AV_RelTime", "AV_Party", "AV_ContractId", "AV_List", "AV_Map"), name -> {
          switch (name) {
            case "AV_Text": return jsonDecoderAV_Text();
            case "AV_Int": return jsonDecoderAV_Int();
            case "AV_Decimal": return jsonDecoderAV_Decimal();
            case "AV_Bool": return jsonDecoderAV_Bool();
            case "AV_Date": return jsonDecoderAV_Date();
            case "AV_Time": return jsonDecoderAV_Time();
            case "AV_RelTime": return jsonDecoderAV_RelTime();
            case "AV_Party": return jsonDecoderAV_Party();
            case "AV_ContractId": return jsonDecoderAV_ContractId();
            case "AV_List": return jsonDecoderAV_List();
            case "AV_Map": return jsonDecoderAV_Map();
            default: return null;
          }
        }
        );
  }

  public static AnyValue fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  protected abstract JsonLfEncoders.Field fieldForJsonEncoder();

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.variant(AnyValue::fieldForJsonEncoder).apply(this);
  }

  private static ValueDecoder<AV_Text> valueDecoderAV_Text() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_Text", value$);
      String body = PrimitiveValueDecoders.fromText.decode(variantValue$);
      return new AV_Text(body);
    } ;
  }

  private static JsonLfDecoder<AV_Text> jsonDecoderAV_Text() {
    return r -> new AV_Text(JsonLfDecoders.text.decode(r));
  }

  private static ValueDecoder<AV_Int> valueDecoderAV_Int() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_Int", value$);
      Long body = PrimitiveValueDecoders.fromInt64.decode(variantValue$);
      return new AV_Int(body);
    } ;
  }

  private static JsonLfDecoder<AV_Int> jsonDecoderAV_Int() {
    return r -> new AV_Int(JsonLfDecoders.int64.decode(r));
  }

  private static ValueDecoder<AV_Decimal> valueDecoderAV_Decimal() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_Decimal", value$);
      BigDecimal body = PrimitiveValueDecoders.fromNumeric.decode(variantValue$);
      return new AV_Decimal(body);
    } ;
  }

  private static JsonLfDecoder<AV_Decimal> jsonDecoderAV_Decimal() {
    return r -> new AV_Decimal(JsonLfDecoders.numeric(10).decode(r));
  }

  private static ValueDecoder<AV_Bool> valueDecoderAV_Bool() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_Bool", value$);
      Boolean body = PrimitiveValueDecoders.fromBool.decode(variantValue$);
      return new AV_Bool(body);
    } ;
  }

  private static JsonLfDecoder<AV_Bool> jsonDecoderAV_Bool() {
    return r -> new AV_Bool(JsonLfDecoders.bool.decode(r));
  }

  private static ValueDecoder<AV_Date> valueDecoderAV_Date() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_Date", value$);
      LocalDate body = PrimitiveValueDecoders.fromDate.decode(variantValue$);
      return new AV_Date(body);
    } ;
  }

  private static JsonLfDecoder<AV_Date> jsonDecoderAV_Date() {
    return r -> new AV_Date(JsonLfDecoders.date.decode(r));
  }

  private static ValueDecoder<AV_Time> valueDecoderAV_Time() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_Time", value$);
      Instant body = PrimitiveValueDecoders.fromTimestamp.decode(variantValue$);
      return new AV_Time(body);
    } ;
  }

  private static JsonLfDecoder<AV_Time> jsonDecoderAV_Time() {
    return r -> new AV_Time(JsonLfDecoders.timestamp.decode(r));
  }

  private static ValueDecoder<AV_RelTime> valueDecoderAV_RelTime() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_RelTime", value$);
      RelTime body = RelTime.valueDecoder().decode(variantValue$);
      return new AV_RelTime(body);
    } ;
  }

  private static JsonLfDecoder<AV_RelTime> jsonDecoderAV_RelTime() {
    return r -> new AV_RelTime(new RelTime.JsonDecoder$().get().decode(r));
  }

  private static ValueDecoder<AV_Party> valueDecoderAV_Party() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_Party", value$);
      String body = PrimitiveValueDecoders.fromParty.decode(variantValue$);
      return new AV_Party(body);
    } ;
  }

  private static JsonLfDecoder<AV_Party> jsonDecoderAV_Party() {
    return r -> new AV_Party(JsonLfDecoders.party.decode(r));
  }

  private static ValueDecoder<AV_ContractId> valueDecoderAV_ContractId() throws
      IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_ContractId", value$);
      AnyContract.ContractId body =
          new AnyContract.ContractId(variantValue$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected body to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new AV_ContractId(body);
    } ;
  }

  private static JsonLfDecoder<AV_ContractId> jsonDecoderAV_ContractId() {
    return r -> new AV_ContractId(JsonLfDecoders.contractId(AnyContract.ContractId::new).decode(r));
  }

  private static ValueDecoder<AV_List> valueDecoderAV_List() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_List", value$);
      List<AnyValue> body = PrimitiveValueDecoders.fromList(AnyValue.valueDecoder())
          .decode(variantValue$);
      return new AV_List(body);
    } ;
  }

  private static JsonLfDecoder<AV_List> jsonDecoderAV_List() {
    return r -> new AV_List(JsonLfDecoders.list(new JsonDecoder$().get()).decode(r));
  }

  private static ValueDecoder<AV_Map> valueDecoderAV_Map() throws IllegalArgumentException {
    return value$ -> {
      Value variantValue$ = PrimitiveValueDecoders.variantCheck("AV_Map", value$);
      Map<String, AnyValue> body = PrimitiveValueDecoders.fromTextMap(AnyValue.valueDecoder())
          .decode(variantValue$);
      return new AV_Map(body);
    } ;
  }

  private static JsonLfDecoder<AV_Map> jsonDecoderAV_Map() {
    return r -> new AV_Map(JsonLfDecoders.textMap(new JsonDecoder$().get()).decode(r));
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AnyValue> get() {
      return jsonDecoder();
    }
  }
}
