package com.lucilla.settlement.model.splice.api.token.allocationv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId;
import com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TransferLeg extends DamlRecord<TransferLeg> {
  public static final String _packageId = "93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d";

  public final String sender;

  public final String receiver;

  public final BigDecimal amount;

  public final InstrumentId instrumentId;

  public final Metadata meta;

  public TransferLeg(String sender, String receiver, BigDecimal amount, InstrumentId instrumentId,
      Metadata meta) {
    this.sender = sender;
    this.receiver = receiver;
    this.amount = amount;
    this.instrumentId = instrumentId;
    this.meta = meta;
  }

  public static ValueDecoder<TransferLeg> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0,
          recordValue$);
      String sender = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String receiver = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      BigDecimal amount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      InstrumentId instrumentId = InstrumentId.valueDecoder().decode(fields$.get(3).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(4).getValue());
      return new TransferLeg(sender, receiver, amount, instrumentId, meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(5);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("sender", new Party(this.sender)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("receiver", new Party(this.receiver)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("amount", new Numeric(this.amount)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("instrumentId", this.instrumentId.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<TransferLeg> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("sender", "receiver", "amount", "instrumentId", "meta"), name -> {
          switch (name) {
            case "sender": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "receiver": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "amount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, new com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId.JsonDecoder$().get());
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new TransferLeg(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static TransferLeg fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("sender", apply(JsonLfEncoders::party, sender)),
        JsonLfEncoders.Field.of("receiver", apply(JsonLfEncoders::party, receiver)),
        JsonLfEncoders.Field.of("amount", apply(JsonLfEncoders::numeric, amount)),
        JsonLfEncoders.Field.of("instrumentId", apply(InstrumentId::jsonEncoder, instrumentId)),
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
    if (!(object instanceof TransferLeg)) {
      return false;
    }
    TransferLeg other = (TransferLeg) object;
    return Objects.equals(this.sender, other.sender) &&
        Objects.equals(this.receiver, other.receiver) &&
        Objects.equals(this.amount, other.amount) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.sender, this.receiver, this.amount, this.instrumentId, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.allocationv1.TransferLeg(%s, %s, %s, %s, %s)",
        this.sender, this.receiver, this.amount, this.instrumentId, this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TransferLeg> get() {
      return jsonDecoder();
    }
  }
}
