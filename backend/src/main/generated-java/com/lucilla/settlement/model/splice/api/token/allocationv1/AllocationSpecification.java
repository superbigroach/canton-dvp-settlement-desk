package com.lucilla.settlement.model.splice.api.token.allocationv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AllocationSpecification extends DamlRecord<AllocationSpecification> {
  public static final String _packageId = "93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d";

  public final SettlementInfo settlement;

  public final String transferLegId;

  public final TransferLeg transferLeg;

  public AllocationSpecification(SettlementInfo settlement, String transferLegId,
      TransferLeg transferLeg) {
    this.settlement = settlement;
    this.transferLegId = transferLegId;
    this.transferLeg = transferLeg;
  }

  public static ValueDecoder<AllocationSpecification> valueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(3,0,
          recordValue$);
      SettlementInfo settlement = SettlementInfo.valueDecoder().decode(fields$.get(0).getValue());
      String transferLegId = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      TransferLeg transferLeg = TransferLeg.valueDecoder().decode(fields$.get(2).getValue());
      return new AllocationSpecification(settlement, transferLegId, transferLeg);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(3);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("settlement", this.settlement.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("transferLegId", new Text(this.transferLegId)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("transferLeg", this.transferLeg.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<AllocationSpecification> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("settlement", "transferLegId", "transferLeg"), name -> {
          switch (name) {
            case "settlement": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, new com.lucilla.settlement.model.splice.api.token.allocationv1.SettlementInfo.JsonDecoder$().get());
            case "transferLegId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "transferLeg": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, new com.lucilla.settlement.model.splice.api.token.allocationv1.TransferLeg.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new AllocationSpecification(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2])));
  }

  public static AllocationSpecification fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("settlement", apply(SettlementInfo::jsonEncoder, settlement)),
        JsonLfEncoders.Field.of("transferLegId", apply(JsonLfEncoders::text, transferLegId)),
        JsonLfEncoders.Field.of("transferLeg", apply(TransferLeg::jsonEncoder, transferLeg)));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AllocationSpecification)) {
      return false;
    }
    AllocationSpecification other = (AllocationSpecification) object;
    return Objects.equals(this.settlement, other.settlement) &&
        Objects.equals(this.transferLegId, other.transferLegId) &&
        Objects.equals(this.transferLeg, other.transferLeg);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.settlement, this.transferLegId, this.transferLeg);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationv1.AllocationSpecification(%s, %s, %s)",
        this.settlement, this.transferLegId, this.transferLeg);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AllocationSpecification> get() {
      return jsonDecoder();
    }
  }
}
