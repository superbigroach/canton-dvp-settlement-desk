package com.lucilla.settlement.model.splice.api.token.transferinstructionv1;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Timestamp;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.DamlRecord;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.splice.api.token.holdingv1.Holding;
import com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId;
import com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Transfer extends DamlRecord<Transfer> {
  public static final String _packageId = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public final String sender;

  public final String receiver;

  public final BigDecimal amount;

  public final InstrumentId instrumentId;

  public final Instant requestedAt;

  public final Instant executeBefore;

  public final List<Holding.ContractId> inputHoldingCids;

  public final Metadata meta;

  public Transfer(String sender, String receiver, BigDecimal amount, InstrumentId instrumentId,
      Instant requestedAt, Instant executeBefore, List<Holding.ContractId> inputHoldingCids,
      Metadata meta) {
    this.sender = sender;
    this.receiver = receiver;
    this.amount = amount;
    this.instrumentId = instrumentId;
    this.requestedAt = requestedAt;
    this.executeBefore = executeBefore;
    this.inputHoldingCids = inputHoldingCids;
    this.meta = meta;
  }

  public static ValueDecoder<Transfer> valueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<com.daml.ledger.javaapi.data.DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(8,0,
          recordValue$);
      String sender = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String receiver = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      BigDecimal amount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(2).getValue());
      InstrumentId instrumentId = InstrumentId.valueDecoder().decode(fields$.get(3).getValue());
      Instant requestedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(4).getValue());
      Instant executeBefore = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(5).getValue());
      List<Holding.ContractId> inputHoldingCids = PrimitiveValueDecoders.fromList(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected inputHoldingCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(6).getValue());
      Metadata meta = Metadata.valueDecoder().decode(fields$.get(7).getValue());
      return new Transfer(sender, receiver, amount, instrumentId, requestedAt, executeBefore,
          inputHoldingCids, meta);
    } ;
  }

  public com.daml.ledger.javaapi.data.DamlRecord toValue() {
    ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field> fields = new ArrayList<com.daml.ledger.javaapi.data.DamlRecord.Field>(8);
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("sender", new Party(this.sender)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("receiver", new Party(this.receiver)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("amount", new Numeric(this.amount)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("instrumentId", this.instrumentId.toValue()));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("requestedAt", Timestamp.fromInstant(this.requestedAt)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("executeBefore", Timestamp.fromInstant(this.executeBefore)));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("inputHoldingCids", this.inputHoldingCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new com.daml.ledger.javaapi.data.DamlRecord.Field("meta", this.meta.toValue()));
    return new com.daml.ledger.javaapi.data.DamlRecord(fields);
  }

  public static JsonLfDecoder<Transfer> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("sender", "receiver", "amount", "instrumentId", "requestedAt", "executeBefore", "inputHoldingCids", "meta"), name -> {
          switch (name) {
            case "sender": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "receiver": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "amount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, new com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId.JsonDecoder$().get());
            case "requestedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "executeBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "inputHoldingCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.splice.api.token.holdingv1.Holding.ContractId::new)));
            case "meta": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, new com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata.JsonDecoder$().get());
            default: return null;
          }
        }
        , (Object[] args) -> new Transfer(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7])));
  }

  public static Transfer fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("sender", apply(JsonLfEncoders::party, sender)),
        JsonLfEncoders.Field.of("receiver", apply(JsonLfEncoders::party, receiver)),
        JsonLfEncoders.Field.of("amount", apply(JsonLfEncoders::numeric, amount)),
        JsonLfEncoders.Field.of("instrumentId", apply(InstrumentId::jsonEncoder, instrumentId)),
        JsonLfEncoders.Field.of("requestedAt", apply(JsonLfEncoders::timestamp, requestedAt)),
        JsonLfEncoders.Field.of("executeBefore", apply(JsonLfEncoders::timestamp, executeBefore)),
        JsonLfEncoders.Field.of("inputHoldingCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), inputHoldingCids)),
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
    if (!(object instanceof Transfer)) {
      return false;
    }
    Transfer other = (Transfer) object;
    return Objects.equals(this.sender, other.sender) &&
        Objects.equals(this.receiver, other.receiver) &&
        Objects.equals(this.amount, other.amount) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.requestedAt, other.requestedAt) &&
        Objects.equals(this.executeBefore, other.executeBefore) &&
        Objects.equals(this.inputHoldingCids, other.inputHoldingCids) &&
        Objects.equals(this.meta, other.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.sender, this.receiver, this.amount, this.instrumentId,
        this.requestedAt, this.executeBefore, this.inputHoldingCids, this.meta);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.splice.api.token.transferinstructionv1.Transfer(%s, %s, %s, %s, %s, %s, %s, %s)",
        this.sender, this.receiver, this.amount, this.instrumentId, this.requestedAt,
        this.executeBefore, this.inputHoldingCids, this.meta);
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Transfer> get() {
      return jsonDecoder();
    }
  }
}
