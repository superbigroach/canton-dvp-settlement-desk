package com.lucilla.settlement.model.splice.api.token.allocationrequestv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.splice.api.token.allocationv1.SettlementInfo;
import com.lucilla.settlement.model.splice.api.token.allocationv1.TransferLeg;
import com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AllocationRequestView extends DamlRecord<AllocationRequestView> {
  public static final String _packageId = "6fe848530b2404017c4a12874c956ad7d5c8a419ee9b040f96b5c13172d2e193";

  public final SettlementInfo settlement;

  public final Map<String, TransferLeg> transferLegs;

  public final Metadata meta;

  public AllocationRequestView(SettlementInfo settlement, Map<String, TransferLeg> transferLegs,
      Metadata meta) {
    this.settlement = settlement;
    this.transferLegs = transferLegs;
    this.meta = meta;
  }

  public static ValueDecoder<AllocationRequestView> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      SettlementInfo settlement = SettlementInfo.valueDecoder().decode(fields$.get(0).getValue());
      Map<String, TransferLeg> transferLegs = PrimitiveValueDecoders.fromTextMap(
            TransferLeg.valueDecoder()).decode(fields$.get(1).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(2).getValue());
      return new AllocationRequestView(settlement, transferLegs, meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("settlement", this.settlement.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("transferLegs", this.transferLegs.entrySet()
        .stream()
        .collect(DamlCollectors.toDamlTextMap(Map.Entry::getKey, v$0 -> v$0.getValue().toValue()))
        ));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AllocationRequestView> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("settlement", "transferLegs", "meta"), name -> {
          switch (name) {
            case "settlement": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, new com.lucilla.settlement.model.splice.api.token.allocationv1.SettlementInfo.JsonDecoder$().get());
            case "transferLegs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.textMap(new com.lucilla.settlement.model.splice.api.token.allocationv1.TransferLeg.JsonDecoder$().get()));
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new AllocationRequestView(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static AllocationRequestView fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("settlement", apply(SettlementInfo::jsonEncoder, settlement)),
        JsonLfEncoders.Field.of("transferLegs", apply(JsonLfEncoders.textMap(TransferLeg::jsonEncoder), transferLegs)),
        JsonLfEncoders.Field.of("meta", apply(Metadata::jsonEncoder, meta)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AllocationRequestView)) {
      return false;
    }
    AllocationRequestView other = (AllocationRequestView) object;
    return Objects.equals(this.settlement, other.settlement) &&
        Objects.equals(this.transferLegs, other.transferLegs) &&
        Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.settlement, this.transferLegs, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationrequestv1.AllocationRequestView(%s, %s, %s)",
        this.settlement, this.transferLegs, this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AllocationRequestView> get() {
      return jsonDecoder();
    }
  }
}
