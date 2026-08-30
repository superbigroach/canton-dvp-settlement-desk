package com.lucilla.settlement.model.perpetual;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
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
import com.lucilla.settlement.model.da.types.Tuple2;
import com.lucilla.settlement.model.holding.Holding;
import java.lang.Deprecated;
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
import java.util.Set;

public final class PerpPosition extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "Perpetual", "PerpPosition");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b", "Perpetual", "PerpPosition");

  public static final String PACKAGE_ID = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<PerpPosition, ClosePosition, Tuple2<PerpMarket.ContractId, BigDecimal>> CHOICE_ClosePosition = 
      Choice.create("ClosePosition", value$ -> value$.toValue(), value$ ->
        ClosePosition.valueDecoder().decode(value$), value$ ->
        Tuple2.<com.lucilla.settlement.model.perpetual.PerpMarket.ContractId,
        java.math.BigDecimal>valueDecoder(v$0 ->
          new PerpMarket.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        PrimitiveValueDecoders.fromNumeric).decode(value$), new ClosePosition.JsonDecoder$().get(),
        new Tuple2.JsonDecoder$().get(JsonLfDecoders.contractId(PerpMarket.ContractId::new), JsonLfDecoders.numeric(10)),
        ClosePosition::jsonEncoder,
        _x0 -> _x0.jsonEncoder(JsonLfEncoders::contractId, JsonLfEncoders::numeric));

  public static final Choice<PerpPosition, AddCollateral, ContractId> CHOICE_AddCollateral = 
      Choice.create("AddCollateral", value$ -> value$.toValue(), value$ ->
        AddCollateral.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new AddCollateral.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        AddCollateral::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<PerpPosition, Liquidate, Tuple2<PerpMarket.ContractId, BigDecimal>> CHOICE_Liquidate = 
      Choice.create("Liquidate", value$ -> value$.toValue(), value$ -> Liquidate.valueDecoder()
        .decode(value$), value$ ->
        Tuple2.<com.lucilla.settlement.model.perpetual.PerpMarket.ContractId,
        java.math.BigDecimal>valueDecoder(v$0 ->
          new PerpMarket.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        PrimitiveValueDecoders.fromNumeric).decode(value$), new Liquidate.JsonDecoder$().get(),
        new Tuple2.JsonDecoder$().get(JsonLfDecoders.contractId(PerpMarket.ContractId::new), JsonLfDecoders.numeric(10)),
        Liquidate::jsonEncoder,
        _x0 -> _x0.jsonEncoder(JsonLfEncoders::contractId, JsonLfEncoders::numeric));

  public static final Choice<PerpPosition, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<PerpPosition, ApplyFunding, ContractId> CHOICE_ApplyFunding = 
      Choice.create("ApplyFunding", value$ -> value$.toValue(), value$ ->
        ApplyFunding.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new ApplyFunding.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        ApplyFunding::jsonEncoder, JsonLfEncoders::contractId);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, PerpPosition> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(PerpPosition.PACKAGE_ID, PerpPosition.PACKAGE_NAME, PerpPosition.PACKAGE_VERSION),
        "com.lucilla.settlement.model.perpetual.PerpPosition", TEMPLATE_ID, ContractId::new,
        v -> PerpPosition.templateValueDecoder().decode(v), PerpPosition::fromJson, Contract::new,
        List.of(CHOICE_Liquidate, CHOICE_ApplyFunding, CHOICE_ClosePosition, CHOICE_AddCollateral,
        CHOICE_Archive));

  public final String operator;

  public final String auditor;

  public final String trader;

  public final String instrumentId;

  public final String cashInstrument;

  public final PositionSide side;

  public final BigDecimal size;

  public final BigDecimal entryPrice;

  public final BigDecimal collateral;

  public final Holding.ContractId collateralCid;

  public final Instant openedAt;

  public final Instant lastFundingAt;

  public final BigDecimal maintenanceMarginBps;

  public PerpPosition(String operator, String auditor, String trader, String instrumentId,
      String cashInstrument, PositionSide side, BigDecimal size, BigDecimal entryPrice,
      BigDecimal collateral, Holding.ContractId collateralCid, Instant openedAt,
      Instant lastFundingAt, BigDecimal maintenanceMarginBps) {
    this.operator = operator;
    this.auditor = auditor;
    this.trader = trader;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.side = side;
    this.size = size;
    this.entryPrice = entryPrice;
    this.collateral = collateral;
    this.collateralCid = collateralCid;
    this.openedAt = openedAt;
    this.lastFundingAt = lastFundingAt;
    this.maintenanceMarginBps = maintenanceMarginBps;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(PerpPosition.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseClosePosition} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<PerpMarket.ContractId, BigDecimal>>> createAndExerciseClosePosition(
      ClosePosition arg) {
    return createAnd().exerciseClosePosition(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseClosePosition} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<PerpMarket.ContractId, BigDecimal>>> createAndExerciseClosePosition(
      PerpMarket.ContractId marketCid) {
    return createAndExerciseClosePosition(new ClosePosition(marketCid));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAddCollateral} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseAddCollateral(AddCollateral arg) {
    return createAnd().exerciseAddCollateral(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAddCollateral} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseAddCollateral(BigDecimal extra,
      Holding.ContractId extraCid) {
    return createAndExerciseAddCollateral(new AddCollateral(extra, extraCid));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseLiquidate} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<PerpMarket.ContractId, BigDecimal>>> createAndExerciseLiquidate(
      Liquidate arg) {
    return createAnd().exerciseLiquidate(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseLiquidate} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<PerpMarket.ContractId, BigDecimal>>> createAndExerciseLiquidate(
      PerpMarket.ContractId marketCid) {
    return createAndExerciseLiquidate(new Liquidate(marketCid));
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseApplyFunding} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseApplyFunding(ApplyFunding arg) {
    return createAnd().exerciseApplyFunding(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseApplyFunding} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseApplyFunding(
      PerpMarket.ContractId marketCid) {
    return createAndExerciseApplyFunding(new ApplyFunding(marketCid));
  }

  public static Update<Created<ContractId>> create(String operator, String auditor, String trader,
      String instrumentId, String cashInstrument, PositionSide side, BigDecimal size,
      BigDecimal entryPrice, BigDecimal collateral, Holding.ContractId collateralCid,
      Instant openedAt, Instant lastFundingAt, BigDecimal maintenanceMarginBps) {
    return new PerpPosition(operator, auditor, trader, instrumentId, cashInstrument, side, size,
        entryPrice, collateral, collateralCid, openedAt, lastFundingAt,
        maintenanceMarginBps).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, PerpPosition> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<PerpPosition> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(13);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("side", this.side.toValue()));
    fields.add(new DamlRecord.Field("size", new Numeric(this.size)));
    fields.add(new DamlRecord.Field("entryPrice", new Numeric(this.entryPrice)));
    fields.add(new DamlRecord.Field("collateral", new Numeric(this.collateral)));
    fields.add(new DamlRecord.Field("collateralCid", this.collateralCid.toValue()));
    fields.add(new DamlRecord.Field("openedAt", Timestamp.fromInstant(this.openedAt)));
    fields.add(new DamlRecord.Field("lastFundingAt", Timestamp.fromInstant(this.lastFundingAt)));
    fields.add(new DamlRecord.Field("maintenanceMarginBps", new Numeric(this.maintenanceMarginBps)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<PerpPosition> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(13,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      PositionSide side = PositionSide.valueDecoder().decode(fields$.get(5).getValue());
      BigDecimal size = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      BigDecimal entryPrice = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(7).getValue());
      BigDecimal collateral = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(8).getValue());
      Holding.ContractId collateralCid =
          new Holding.ContractId(fields$.get(9).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected collateralCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Instant openedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(10).getValue());
      Instant lastFundingAt = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(11).getValue());
      BigDecimal maintenanceMarginBps = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(12).getValue());
      return new PerpPosition(operator, auditor, trader, instrumentId, cashInstrument, side, size,
          entryPrice, collateral, collateralCid, openedAt, lastFundingAt, maintenanceMarginBps);
    } ;
  }

  public static JsonLfDecoder<PerpPosition> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "trader", "instrumentId", "cashInstrument", "side", "size", "entryPrice", "collateral", "collateralCid", "openedAt", "lastFundingAt", "maintenanceMarginBps"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "side": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, new com.lucilla.settlement.model.perpetual.PositionSide.JsonDecoder$().get());
            case "size": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "entryPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "collateral": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "collateralCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            case "openedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "lastFundingAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(11, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "maintenanceMarginBps": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(12, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new PerpPosition(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10]), JsonLfDecoders.cast(args[11]), JsonLfDecoders.cast(args[12])));
  }

  public static PerpPosition fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("trader", apply(JsonLfEncoders::party, trader)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("side", apply(PositionSide::jsonEncoder, side)),
        JsonLfEncoders.Field.of("size", apply(JsonLfEncoders::numeric, size)),
        JsonLfEncoders.Field.of("entryPrice", apply(JsonLfEncoders::numeric, entryPrice)),
        JsonLfEncoders.Field.of("collateral", apply(JsonLfEncoders::numeric, collateral)),
        JsonLfEncoders.Field.of("collateralCid", apply(JsonLfEncoders::contractId, collateralCid)),
        JsonLfEncoders.Field.of("openedAt", apply(JsonLfEncoders::timestamp, openedAt)),
        JsonLfEncoders.Field.of("lastFundingAt", apply(JsonLfEncoders::timestamp, lastFundingAt)),
        JsonLfEncoders.Field.of("maintenanceMarginBps", apply(JsonLfEncoders::numeric, maintenanceMarginBps)));
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
    if (!(object instanceof PerpPosition)) {
      return false;
    }
    PerpPosition other = (PerpPosition) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) && Objects.equals(this.trader, other.trader) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.side, other.side) && Objects.equals(this.size, other.size) &&
        Objects.equals(this.entryPrice, other.entryPrice) &&
        Objects.equals(this.collateral, other.collateral) &&
        Objects.equals(this.collateralCid, other.collateralCid) &&
        Objects.equals(this.openedAt, other.openedAt) &&
        Objects.equals(this.lastFundingAt, other.lastFundingAt) &&
        Objects.equals(this.maintenanceMarginBps, other.maintenanceMarginBps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.trader, this.instrumentId,
        this.cashInstrument, this.side, this.size, this.entryPrice, this.collateral,
        this.collateralCid, this.openedAt, this.lastFundingAt, this.maintenanceMarginBps);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.perpetual.PerpPosition(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.trader, this.instrumentId, this.cashInstrument, this.side,
        this.size, this.entryPrice, this.collateral, this.collateralCid, this.openedAt,
        this.lastFundingAt, this.maintenanceMarginBps);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<PerpPosition> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PerpPosition, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<PerpPosition> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, PerpPosition> {
    public Contract(ContractId id, PerpPosition data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, PerpPosition> getCompanion() {
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
    default Update<Exercised<Tuple2<PerpMarket.ContractId, BigDecimal>>> exerciseClosePosition(
        ClosePosition arg) {
      return makeExerciseCmd(CHOICE_ClosePosition, arg);
    }

    default Update<Exercised<Tuple2<PerpMarket.ContractId, BigDecimal>>> exerciseClosePosition(
        PerpMarket.ContractId marketCid) {
      return exerciseClosePosition(new ClosePosition(marketCid));
    }

    default Update<Exercised<ContractId>> exerciseAddCollateral(AddCollateral arg) {
      return makeExerciseCmd(CHOICE_AddCollateral, arg);
    }

    default Update<Exercised<ContractId>> exerciseAddCollateral(BigDecimal extra,
        Holding.ContractId extraCid) {
      return exerciseAddCollateral(new AddCollateral(extra, extraCid));
    }

    default Update<Exercised<Tuple2<PerpMarket.ContractId, BigDecimal>>> exerciseLiquidate(
        Liquidate arg) {
      return makeExerciseCmd(CHOICE_Liquidate, arg);
    }

    default Update<Exercised<Tuple2<PerpMarket.ContractId, BigDecimal>>> exerciseLiquidate(
        PerpMarket.ContractId marketCid) {
      return exerciseLiquidate(new Liquidate(marketCid));
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<ContractId>> exerciseApplyFunding(ApplyFunding arg) {
      return makeExerciseCmd(CHOICE_ApplyFunding, arg);
    }

    default Update<Exercised<ContractId>> exerciseApplyFunding(PerpMarket.ContractId marketCid) {
      return exerciseApplyFunding(new ApplyFunding(marketCid));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PerpPosition, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PerpPosition> get() {
      return jsonDecoder();
    }
  }
}
