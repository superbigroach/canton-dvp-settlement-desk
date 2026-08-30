package com.lucilla.settlement.model.perpetual;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Bool;
import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlOptional;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.PackageVersion;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Template;
import com.daml.ledger.javaapi.data.Text;
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
import java.lang.Boolean;
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
import java.util.Optional;
import java.util.Set;

public final class PerpMarket extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "Perpetual", "PerpMarket");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b", "Perpetual", "PerpMarket");

  public static final String PACKAGE_ID = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<PerpMarket, CloseMarket, ContractId> CHOICE_CloseMarket = 
      Choice.create("CloseMarket", value$ -> value$.toValue(), value$ -> CloseMarket.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new CloseMarket.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        CloseMarket::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<PerpMarket, SetIndex, ContractId> CHOICE_SetIndex = 
      Choice.create("SetIndex", value$ -> value$.toValue(), value$ -> SetIndex.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new SetIndex.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        SetIndex::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<PerpMarket, SetFundingRate, ContractId> CHOICE_SetFundingRate = 
      Choice.create("SetFundingRate", value$ -> value$.toValue(), value$ ->
        SetFundingRate.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new SetFundingRate.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        SetFundingRate::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<PerpMarket, FundInsurance, ContractId> CHOICE_FundInsurance = 
      Choice.create("FundInsurance", value$ -> value$.toValue(), value$ ->
        FundInsurance.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new FundInsurance.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        FundInsurance::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<PerpMarket, SetPoolAndInterest, ContractId> CHOICE_SetPoolAndInterest = 
      Choice.create("SetPoolAndInterest", value$ -> value$.toValue(), value$ ->
        SetPoolAndInterest.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new SetPoolAndInterest.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        SetPoolAndInterest::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<PerpMarket, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<PerpMarket, DeriveFunding, ContractId> CHOICE_DeriveFunding = 
      Choice.create("DeriveFunding", value$ -> value$.toValue(), value$ ->
        DeriveFunding.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new DeriveFunding.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        DeriveFunding::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<PerpMarket, ReopenMarket, ContractId> CHOICE_ReopenMarket = 
      Choice.create("ReopenMarket", value$ -> value$.toValue(), value$ ->
        ReopenMarket.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new ReopenMarket.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        ReopenMarket::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<PerpMarket, OpenPosition, Tuple2<ContractId, PerpPosition.ContractId>> CHOICE_OpenPosition = 
      Choice.create("OpenPosition", value$ -> value$.toValue(), value$ ->
        OpenPosition.valueDecoder().decode(value$), value$ ->
        Tuple2.<com.lucilla.settlement.model.perpetual.PerpMarket.ContractId,
        com.lucilla.settlement.model.perpetual.PerpPosition.ContractId>valueDecoder(v$0 ->
          new ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        v$1 ->
          new PerpPosition.ContractId(v$1.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
        .decode(value$), new OpenPosition.JsonDecoder$().get(),
        new Tuple2.JsonDecoder$().get(JsonLfDecoders.contractId(ContractId::new), JsonLfDecoders.contractId(PerpPosition.ContractId::new)),
        OpenPosition::jsonEncoder,
        _x0 -> _x0.jsonEncoder(JsonLfEncoders::contractId, JsonLfEncoders::contractId));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, PerpMarket> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(PerpMarket.PACKAGE_ID, PerpMarket.PACKAGE_NAME, PerpMarket.PACKAGE_VERSION),
        "com.lucilla.settlement.model.perpetual.PerpMarket", TEMPLATE_ID, ContractId::new,
        v -> PerpMarket.templateValueDecoder().decode(v), PerpMarket::fromJson, Contract::new,
        List.of(CHOICE_ReopenMarket, CHOICE_SetFundingRate, CHOICE_SetIndex, CHOICE_Archive,
        CHOICE_SetPoolAndInterest, CHOICE_CloseMarket, CHOICE_FundInsurance, CHOICE_DeriveFunding,
        CHOICE_OpenPosition));

  public final String operator;

  public final String auditor;

  public final List<String> participants;

  public final String instrumentId;

  public final String cashInstrument;

  public final BigDecimal indexPrice;

  public final BigDecimal fundingRate;

  public final BigDecimal fundingRateCap;

  public final BigDecimal maxLeverage;

  public final BigDecimal maintenanceMarginBps;

  public final BigDecimal openLong;

  public final BigDecimal openShort;

  public final Optional<Holding.ContractId> insurance;

  public final Boolean isOpen;

  public PerpMarket(String operator, String auditor, List<String> participants, String instrumentId,
      String cashInstrument, BigDecimal indexPrice, BigDecimal fundingRate,
      BigDecimal fundingRateCap, BigDecimal maxLeverage, BigDecimal maintenanceMarginBps,
      BigDecimal openLong, BigDecimal openShort, Optional<Holding.ContractId> insurance,
      Boolean isOpen) {
    this.operator = operator;
    this.auditor = auditor;
    this.participants = participants;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.indexPrice = indexPrice;
    this.fundingRate = fundingRate;
    this.fundingRateCap = fundingRateCap;
    this.maxLeverage = maxLeverage;
    this.maintenanceMarginBps = maintenanceMarginBps;
    this.openLong = openLong;
    this.openShort = openShort;
    this.insurance = insurance;
    this.isOpen = isOpen;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(PerpMarket.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCloseMarket} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseCloseMarket(CloseMarket arg) {
    return createAnd().exerciseCloseMarket(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCloseMarket} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseCloseMarket() {
    return createAndExerciseCloseMarket(new CloseMarket());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSetIndex} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseSetIndex(SetIndex arg) {
    return createAnd().exerciseSetIndex(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSetIndex} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseSetIndex(BigDecimal newIndex) {
    return createAndExerciseSetIndex(new SetIndex(newIndex));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSetFundingRate} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseSetFundingRate(SetFundingRate arg) {
    return createAnd().exerciseSetFundingRate(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSetFundingRate} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseSetFundingRate(BigDecimal newRate) {
    return createAndExerciseSetFundingRate(new SetFundingRate(newRate));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseFundInsurance} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseFundInsurance(FundInsurance arg) {
    return createAnd().exerciseFundInsurance(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseFundInsurance} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseFundInsurance(Holding.ContractId poolCid) {
    return createAndExerciseFundInsurance(new FundInsurance(poolCid));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSetPoolAndInterest} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseSetPoolAndInterest(SetPoolAndInterest arg) {
    return createAnd().exerciseSetPoolAndInterest(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSetPoolAndInterest} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseSetPoolAndInterest(
      Optional<Holding.ContractId> newPool, PositionSide closedSide, BigDecimal closedSize) {
    return createAndExerciseSetPoolAndInterest(new SetPoolAndInterest(newPool, closedSide,
        closedSize));
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseDeriveFunding} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseDeriveFunding(DeriveFunding arg) {
    return createAnd().exerciseDeriveFunding(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseDeriveFunding} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseDeriveFunding(BigDecimal perpMark) {
    return createAndExerciseDeriveFunding(new DeriveFunding(perpMark));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseReopenMarket} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseReopenMarket(ReopenMarket arg) {
    return createAnd().exerciseReopenMarket(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseReopenMarket} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseReopenMarket() {
    return createAndExerciseReopenMarket(new ReopenMarket());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseOpenPosition} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, PerpPosition.ContractId>>> createAndExerciseOpenPosition(
      OpenPosition arg) {
    return createAnd().exerciseOpenPosition(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseOpenPosition} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, PerpPosition.ContractId>>> createAndExerciseOpenPosition(
      String trader, PositionSide side, BigDecimal size, BigDecimal collateral,
      Holding.ContractId collateralCid) {
    return createAndExerciseOpenPosition(new OpenPosition(trader, side, size, collateral,
        collateralCid));
  }

  public static Update<Created<ContractId>> create(String operator, String auditor,
      List<String> participants, String instrumentId, String cashInstrument, BigDecimal indexPrice,
      BigDecimal fundingRate, BigDecimal fundingRateCap, BigDecimal maxLeverage,
      BigDecimal maintenanceMarginBps, BigDecimal openLong, BigDecimal openShort,
      Optional<Holding.ContractId> insurance, Boolean isOpen) {
    return new PerpMarket(operator, auditor, participants, instrumentId, cashInstrument, indexPrice,
        fundingRate, fundingRateCap, maxLeverage, maintenanceMarginBps, openLong, openShort,
        insurance, isOpen).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, PerpMarket> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<PerpMarket> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(14);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("participants", this.participants.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("indexPrice", new Numeric(this.indexPrice)));
    fields.add(new DamlRecord.Field("fundingRate", new Numeric(this.fundingRate)));
    fields.add(new DamlRecord.Field("fundingRateCap", new Numeric(this.fundingRateCap)));
    fields.add(new DamlRecord.Field("maxLeverage", new Numeric(this.maxLeverage)));
    fields.add(new DamlRecord.Field("maintenanceMarginBps", new Numeric(this.maintenanceMarginBps)));
    fields.add(new DamlRecord.Field("openLong", new Numeric(this.openLong)));
    fields.add(new DamlRecord.Field("openShort", new Numeric(this.openShort)));
    fields.add(new DamlRecord.Field("insurance", DamlOptional.of(this.insurance.map(v$0 -> v$0.toValue()))));
    fields.add(new DamlRecord.Field("isOpen", Bool.of(this.isOpen)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<PerpMarket> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(14,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      List<String> participants = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(2).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      BigDecimal indexPrice = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(5).getValue());
      BigDecimal fundingRate = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      BigDecimal fundingRateCap = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(7).getValue());
      BigDecimal maxLeverage = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(8).getValue());
      BigDecimal maintenanceMarginBps = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(9).getValue());
      BigDecimal openLong = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(10).getValue());
      BigDecimal openShort = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(11).getValue());
      Optional<Holding.ContractId> insurance = PrimitiveValueDecoders.fromOptional(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected insurance to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(12).getValue());
      Boolean isOpen = PrimitiveValueDecoders.fromBool.decode(fields$.get(13).getValue());
      return new PerpMarket(operator, auditor, participants, instrumentId, cashInstrument,
          indexPrice, fundingRate, fundingRateCap, maxLeverage, maintenanceMarginBps, openLong,
          openShort, insurance, isOpen);
    } ;
  }

  public static JsonLfDecoder<PerpMarket> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "participants", "instrumentId", "cashInstrument", "indexPrice", "fundingRate", "fundingRateCap", "maxLeverage", "maintenanceMarginBps", "openLong", "openShort", "insurance", "isOpen"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "participants": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "indexPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "fundingRate": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "fundingRateCap": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "maxLeverage": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "maintenanceMarginBps": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "openLong": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "openShort": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(11, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "insurance": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(12, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new)), java.util.Optional.empty());
            case "isOpen": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(13, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.bool);
            default: return null;
          }
        }
        , (Object[] args) -> new PerpMarket(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10]), JsonLfDecoders.cast(args[11]), JsonLfDecoders.cast(args[12]), JsonLfDecoders.cast(args[13])));
  }

  public static PerpMarket fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("participants", apply(JsonLfEncoders.list(JsonLfEncoders::party), participants)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("indexPrice", apply(JsonLfEncoders::numeric, indexPrice)),
        JsonLfEncoders.Field.of("fundingRate", apply(JsonLfEncoders::numeric, fundingRate)),
        JsonLfEncoders.Field.of("fundingRateCap", apply(JsonLfEncoders::numeric, fundingRateCap)),
        JsonLfEncoders.Field.of("maxLeverage", apply(JsonLfEncoders::numeric, maxLeverage)),
        JsonLfEncoders.Field.of("maintenanceMarginBps", apply(JsonLfEncoders::numeric, maintenanceMarginBps)),
        JsonLfEncoders.Field.of("openLong", apply(JsonLfEncoders::numeric, openLong)),
        JsonLfEncoders.Field.of("openShort", apply(JsonLfEncoders::numeric, openShort)),
        JsonLfEncoders.Field.of("insurance", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), insurance)),
        JsonLfEncoders.Field.of("isOpen", apply(JsonLfEncoders::bool, isOpen)));
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
    if (!(object instanceof PerpMarket)) {
      return false;
    }
    PerpMarket other = (PerpMarket) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.participants, other.participants) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.indexPrice, other.indexPrice) &&
        Objects.equals(this.fundingRate, other.fundingRate) &&
        Objects.equals(this.fundingRateCap, other.fundingRateCap) &&
        Objects.equals(this.maxLeverage, other.maxLeverage) &&
        Objects.equals(this.maintenanceMarginBps, other.maintenanceMarginBps) &&
        Objects.equals(this.openLong, other.openLong) &&
        Objects.equals(this.openShort, other.openShort) &&
        Objects.equals(this.insurance, other.insurance) &&
        Objects.equals(this.isOpen, other.isOpen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.participants, this.instrumentId,
        this.cashInstrument, this.indexPrice, this.fundingRate, this.fundingRateCap,
        this.maxLeverage, this.maintenanceMarginBps, this.openLong, this.openShort, this.insurance,
        this.isOpen);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.perpetual.PerpMarket(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.participants, this.instrumentId, this.cashInstrument,
        this.indexPrice, this.fundingRate, this.fundingRateCap, this.maxLeverage,
        this.maintenanceMarginBps, this.openLong, this.openShort, this.insurance, this.isOpen);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<PerpMarket> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PerpMarket, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<PerpMarket> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, PerpMarket> {
    public Contract(ContractId id, PerpMarket data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, PerpMarket> getCompanion() {
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
    default Update<Exercised<ContractId>> exerciseCloseMarket(CloseMarket arg) {
      return makeExerciseCmd(CHOICE_CloseMarket, arg);
    }

    default Update<Exercised<ContractId>> exerciseCloseMarket() {
      return exerciseCloseMarket(new CloseMarket());
    }

    default Update<Exercised<ContractId>> exerciseSetIndex(SetIndex arg) {
      return makeExerciseCmd(CHOICE_SetIndex, arg);
    }

    default Update<Exercised<ContractId>> exerciseSetIndex(BigDecimal newIndex) {
      return exerciseSetIndex(new SetIndex(newIndex));
    }

    default Update<Exercised<ContractId>> exerciseSetFundingRate(SetFundingRate arg) {
      return makeExerciseCmd(CHOICE_SetFundingRate, arg);
    }

    default Update<Exercised<ContractId>> exerciseSetFundingRate(BigDecimal newRate) {
      return exerciseSetFundingRate(new SetFundingRate(newRate));
    }

    default Update<Exercised<ContractId>> exerciseFundInsurance(FundInsurance arg) {
      return makeExerciseCmd(CHOICE_FundInsurance, arg);
    }

    default Update<Exercised<ContractId>> exerciseFundInsurance(Holding.ContractId poolCid) {
      return exerciseFundInsurance(new FundInsurance(poolCid));
    }

    default Update<Exercised<ContractId>> exerciseSetPoolAndInterest(SetPoolAndInterest arg) {
      return makeExerciseCmd(CHOICE_SetPoolAndInterest, arg);
    }

    default Update<Exercised<ContractId>> exerciseSetPoolAndInterest(
        Optional<Holding.ContractId> newPool, PositionSide closedSide, BigDecimal closedSize) {
      return exerciseSetPoolAndInterest(new SetPoolAndInterest(newPool, closedSide, closedSize));
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<ContractId>> exerciseDeriveFunding(DeriveFunding arg) {
      return makeExerciseCmd(CHOICE_DeriveFunding, arg);
    }

    default Update<Exercised<ContractId>> exerciseDeriveFunding(BigDecimal perpMark) {
      return exerciseDeriveFunding(new DeriveFunding(perpMark));
    }

    default Update<Exercised<ContractId>> exerciseReopenMarket(ReopenMarket arg) {
      return makeExerciseCmd(CHOICE_ReopenMarket, arg);
    }

    default Update<Exercised<ContractId>> exerciseReopenMarket() {
      return exerciseReopenMarket(new ReopenMarket());
    }

    default Update<Exercised<Tuple2<ContractId, PerpPosition.ContractId>>> exerciseOpenPosition(
        OpenPosition arg) {
      return makeExerciseCmd(CHOICE_OpenPosition, arg);
    }

    default Update<Exercised<Tuple2<ContractId, PerpPosition.ContractId>>> exerciseOpenPosition(
        String trader, PositionSide side, BigDecimal size, BigDecimal collateral,
        Holding.ContractId collateralCid) {
      return exerciseOpenPosition(new OpenPosition(trader, side, size, collateral, collateralCid));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, PerpMarket, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<PerpMarket> get() {
      return jsonDecoder();
    }
  }
}
