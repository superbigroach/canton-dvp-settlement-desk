package com.lucilla.settlement.model.tokensettlement;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Int64;
import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.PackageVersion;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Template;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Timestamp;
import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.Choice;
import com.daml.ledger.javaapi.data.codegen.ContractCompanion;
import com.daml.ledger.javaapi.data.codegen.ContractTypeCompanion;
import com.daml.ledger.javaapi.data.codegen.Created;
import com.daml.ledger.javaapi.data.codegen.Exercised;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.Update;
import com.daml.ledger.javaapi.data.codegen.ValueDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoder;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfReader;
import com.lucilla.settlement.model.da.internal.template.Archive;
import com.lucilla.settlement.model.settlement.SettlementBatch;
import com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AuctionCross extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#canton-dvp-settlement-desk", "TokenSettlement", "AuctionCross");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("16b1d7198cf7c7ec9373fe2d1bdb48ab1770fe7ffcb7281ad87048ebecd45ab4", "TokenSettlement", "AuctionCross");

  public static final String PACKAGE_ID = "16b1d7198cf7c7ec9373fe2d1bdb48ab1770fe7ffcb7281ad87048ebecd45ab4";

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<AuctionCross, AuctionCross_Settle, SettlementBatch.ContractId> CHOICE_AuctionCross_Settle = 
      Choice.create("AuctionCross_Settle", value$ -> value$.toValue(), value$ ->
        AuctionCross_Settle.valueDecoder().decode(value$), value$ ->
        new SettlementBatch.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new AuctionCross_Settle.JsonDecoder$().get(),
        JsonLfDecoders.contractId(SettlementBatch.ContractId::new),
        AuctionCross_Settle::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<AuctionCross, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<AuctionCross, AuctionCross_Abort, Unit> CHOICE_AuctionCross_Abort = 
      Choice.create("AuctionCross_Abort", value$ -> value$.toValue(), value$ ->
        AuctionCross_Abort.valueDecoder().decode(value$), value$ -> PrimitiveValueDecoders.fromUnit
        .decode(value$), new AuctionCross_Abort.JsonDecoder$().get(), JsonLfDecoders.unit,
        AuctionCross_Abort::jsonEncoder, JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, AuctionCross> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(AuctionCross.PACKAGE_ID, AuctionCross.PACKAGE_NAME, AuctionCross.PACKAGE_VERSION),
        "com.lucilla.settlement.model.tokensettlement.AuctionCross", TEMPLATE_ID, ContractId::new,
        v -> AuctionCross.templateValueDecoder().decode(v), AuctionCross::fromJson, Contract::new,
        List.of(CHOICE_AuctionCross_Settle, CHOICE_Archive, CHOICE_AuctionCross_Abort));

  public final String operator;

  public final String auditor;

  public final String settlementId;

  public final InstrumentId assetInstrument;

  public final InstrumentId cashInstrument;

  public final BigDecimal closingPrice;

  public final Long matchCount;

  public final BigDecimal crossedQty;

  public final List<String> participants;

  public final Instant createdAt;

  public final Instant allocateBefore;

  public final Instant settleBefore;

  public AuctionCross(String operator, String auditor, String settlementId,
      InstrumentId assetInstrument, InstrumentId cashInstrument, BigDecimal closingPrice,
      Long matchCount, BigDecimal crossedQty, List<String> participants, Instant createdAt,
      Instant allocateBefore, Instant settleBefore) {
    this.operator = operator;
    this.auditor = auditor;
    this.settlementId = settlementId;
    this.assetInstrument = assetInstrument;
    this.cashInstrument = cashInstrument;
    this.closingPrice = closingPrice;
    this.matchCount = matchCount;
    this.crossedQty = crossedQty;
    this.participants = participants;
    this.createdAt = createdAt;
    this.allocateBefore = allocateBefore;
    this.settleBefore = settleBefore;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(AuctionCross.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAuctionCross_Settle} instead
   */
  @Deprecated
  public Update<Exercised<SettlementBatch.ContractId>> createAndExerciseAuctionCross_Settle(
      AuctionCross_Settle arg) {
    return createAnd().exerciseAuctionCross_Settle(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAuctionCross_Settle} instead
   */
  @Deprecated
  public Update<Exercised<SettlementBatch.ContractId>> createAndExerciseAuctionCross_Settle(
      List<MatchSettleInstruction> legs) {
    return createAndExerciseAuctionCross_Settle(new AuctionCross_Settle(legs));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseArchive} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseArchive(Archive arg) {
    return createAnd().exerciseArchive(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseArchive} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseArchive() {
    return createAndExerciseArchive(new Archive());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAuctionCross_Abort} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseAuctionCross_Abort(AuctionCross_Abort arg) {
    return createAnd().exerciseAuctionCross_Abort(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAuctionCross_Abort} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseAuctionCross_Abort(
      List<MatchAbortInstruction> pairs) {
    return createAndExerciseAuctionCross_Abort(new AuctionCross_Abort(pairs));
  }

  public static Update<Created<ContractId>> create(String operator, String auditor,
      String settlementId, InstrumentId assetInstrument, InstrumentId cashInstrument,
      BigDecimal closingPrice, Long matchCount, BigDecimal crossedQty, List<String> participants,
      Instant createdAt, Instant allocateBefore, Instant settleBefore) {
    return new AuctionCross(operator, auditor, settlementId, assetInstrument, cashInstrument,
        closingPrice, matchCount, crossedQty, participants, createdAt, allocateBefore,
        settleBefore).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, AuctionCross> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<AuctionCross> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(12);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("settlementId", new Text(this.settlementId)));
    fields.add(new DamlRecord.Field("assetInstrument", this.assetInstrument.toValue()));
    fields.add(new DamlRecord.Field("cashInstrument", this.cashInstrument.toValue()));
    fields.add(new DamlRecord.Field("closingPrice", new Numeric(this.closingPrice)));
    fields.add(new DamlRecord.Field("matchCount", new Int64(this.matchCount)));
    fields.add(new DamlRecord.Field("crossedQty", new Numeric(this.crossedQty)));
    fields.add(new DamlRecord.Field("participants", this.participants.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("createdAt", Timestamp.fromInstant(this.createdAt)));
    fields.add(new DamlRecord.Field("allocateBefore", Timestamp.fromInstant(this.allocateBefore)));
    fields.add(new DamlRecord.Field("settleBefore", Timestamp.fromInstant(this.settleBefore)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<AuctionCross> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(12,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String settlementId = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      InstrumentId assetInstrument = InstrumentId.valueDecoder().decode(fields$.get(3).getValue());
      InstrumentId cashInstrument = InstrumentId.valueDecoder().decode(fields$.get(4).getValue());
      BigDecimal closingPrice = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(5).getValue());
      Long matchCount = PrimitiveValueDecoders.fromInt64.decode(fields$.get(6).getValue());
      BigDecimal crossedQty = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(7).getValue());
      List<String> participants = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(8).getValue());
      Instant createdAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(9).getValue());
      Instant allocateBefore = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(10).getValue());
      Instant settleBefore = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(11).getValue());
      return new AuctionCross(operator, auditor, settlementId, assetInstrument, cashInstrument,
          closingPrice, matchCount, crossedQty, participants, createdAt, allocateBefore,
          settleBefore);
    } ;
  }

  public static JsonLfDecoder<AuctionCross> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "settlementId", "assetInstrument", "cashInstrument", "closingPrice", "matchCount", "crossedQty", "participants", "createdAt", "allocateBefore", "settleBefore"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "settlementId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "assetInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, new com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId.JsonDecoder$().get());
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, new com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId.JsonDecoder$().get());
            case "closingPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "matchCount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "crossedQty": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "participants": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "createdAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "allocateBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "settleBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(11, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new AuctionCross(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10]), JsonLfDecoders.cast(args[11])));
  }

  public static AuctionCross fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("settlementId", apply(JsonLfEncoders::text, settlementId)),
        JsonLfEncoders.Field.of("assetInstrument", apply(InstrumentId::jsonEncoder, assetInstrument)),
        JsonLfEncoders.Field.of("cashInstrument", apply(InstrumentId::jsonEncoder, cashInstrument)),
        JsonLfEncoders.Field.of("closingPrice", apply(JsonLfEncoders::numeric, closingPrice)),
        JsonLfEncoders.Field.of("matchCount", apply(JsonLfEncoders::int64, matchCount)),
        JsonLfEncoders.Field.of("crossedQty", apply(JsonLfEncoders::numeric, crossedQty)),
        JsonLfEncoders.Field.of("participants", apply(JsonLfEncoders.list(JsonLfEncoders::party), participants)),
        JsonLfEncoders.Field.of("createdAt", apply(JsonLfEncoders::timestamp, createdAt)),
        JsonLfEncoders.Field.of("allocateBefore", apply(JsonLfEncoders::timestamp, allocateBefore)),
        JsonLfEncoders.Field.of("settleBefore", apply(JsonLfEncoders::timestamp, settleBefore)));
  }

  public static ContractFilter<Contract> contractFilter() {
    return ContractFilter.of(COMPANION);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof AuctionCross)) {
      return false;
    }
    AuctionCross other = (AuctionCross) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.settlementId, other.settlementId) &&
        Objects.equals(this.assetInstrument, other.assetInstrument) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.closingPrice, other.closingPrice) &&
        Objects.equals(this.matchCount, other.matchCount) &&
        Objects.equals(this.crossedQty, other.crossedQty) &&
        Objects.equals(this.participants, other.participants) &&
        Objects.equals(this.createdAt, other.createdAt) &&
        Objects.equals(this.allocateBefore, other.allocateBefore) &&
        Objects.equals(this.settleBefore, other.settleBefore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.settlementId, this.assetInstrument,
        this.cashInstrument, this.closingPrice, this.matchCount, this.crossedQty, this.participants,
        this.createdAt, this.allocateBefore, this.settleBefore);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokensettlement.AuctionCross(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.settlementId, this.assetInstrument, this.cashInstrument,
        this.closingPrice, this.matchCount, this.crossedQty, this.participants, this.createdAt,
        this.allocateBefore, this.settleBefore);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<AuctionCross> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, AuctionCross, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<AuctionCross> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, AuctionCross> {
    public Contract(ContractId id, AuctionCross data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, AuctionCross> getCompanion() {
      return COMPANION;
    }

    public static Contract fromIdAndRecord(String contractId, DamlRecord record$,
        Set<String> signatories, Set<String> observers) {
      return COMPANION.fromIdAndRecord(contractId, record$, signatories, observers);
    }

    public static Contract fromCreatedEvent(CreatedEvent event) {
      return COMPANION.fromCreatedEvent(event);
    }
  }

  public interface Exercises<Cmd> extends com.daml.ledger.javaapi.data.codegen.Exercises.Archivable<Cmd> {
    default Update<Exercised<SettlementBatch.ContractId>> exerciseAuctionCross_Settle(
        AuctionCross_Settle arg) {
      return makeExerciseCmd(CHOICE_AuctionCross_Settle, arg);
    }

    default Update<Exercised<SettlementBatch.ContractId>> exerciseAuctionCross_Settle(
        List<MatchSettleInstruction> legs) {
      return exerciseAuctionCross_Settle(new AuctionCross_Settle(legs));
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<Unit>> exerciseAuctionCross_Abort(AuctionCross_Abort arg) {
      return makeExerciseCmd(CHOICE_AuctionCross_Abort, arg);
    }

    default Update<Exercised<Unit>> exerciseAuctionCross_Abort(List<MatchAbortInstruction> pairs) {
      return exerciseAuctionCross_Abort(new AuctionCross_Abort(pairs));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, AuctionCross, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<AuctionCross> get() {
      return jsonDecoder();
    }
  }
}
