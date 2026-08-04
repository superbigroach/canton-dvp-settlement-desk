package com.lucilla.settlement.model.continuousbook;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Bool;
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
import java.lang.Long;
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

public final class ContinuousBook extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#canton-dvp-settlement-desk", "ContinuousBook", "ContinuousBook");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d", "ContinuousBook", "ContinuousBook");

  public static final String PACKAGE_ID = "147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d";

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<ContinuousBook, PlaceOrder, Tuple2<ContractId, RestingOrder.ContractId>> CHOICE_PlaceOrder = 
      Choice.create("PlaceOrder", value$ -> value$.toValue(), value$ -> PlaceOrder.valueDecoder()
        .decode(value$), value$ ->
        Tuple2.<com.lucilla.settlement.model.continuousbook.ContinuousBook.ContractId,
        com.lucilla.settlement.model.continuousbook.RestingOrder.ContractId>valueDecoder(v$0 ->
          new ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        v$1 ->
          new RestingOrder.ContractId(v$1.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
        .decode(value$), new PlaceOrder.JsonDecoder$().get(),
        new Tuple2.JsonDecoder$().get(JsonLfDecoders.contractId(ContractId::new), JsonLfDecoders.contractId(RestingOrder.ContractId::new)),
        PlaceOrder::jsonEncoder,
        _x0 -> _x0.jsonEncoder(JsonLfEncoders::contractId, JsonLfEncoders::contractId));

  public static final Choice<ContinuousBook, MatchOrder, MatchResult> CHOICE_MatchOrder = 
      Choice.create("MatchOrder", value$ -> value$.toValue(), value$ -> MatchOrder.valueDecoder()
        .decode(value$), value$ -> MatchResult.valueDecoder().decode(value$),
        new MatchOrder.JsonDecoder$().get(), new MatchResult.JsonDecoder$().get(),
        MatchOrder::jsonEncoder, MatchResult::jsonEncoder);

  public static final Choice<ContinuousBook, KillOrder, Tuple2<ContractId, Holding.ContractId>> CHOICE_KillOrder = 
      Choice.create("KillOrder", value$ -> value$.toValue(), value$ -> KillOrder.valueDecoder()
        .decode(value$), value$ ->
        Tuple2.<com.lucilla.settlement.model.continuousbook.ContinuousBook.ContractId,
        com.lucilla.settlement.model.holding.Holding.ContractId>valueDecoder(v$0 ->
          new ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        v$1 ->
          new Holding.ContractId(v$1.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
        .decode(value$), new KillOrder.JsonDecoder$().get(),
        new Tuple2.JsonDecoder$().get(JsonLfDecoders.contractId(ContractId::new), JsonLfDecoders.contractId(Holding.ContractId::new)),
        KillOrder::jsonEncoder,
        _x0 -> _x0.jsonEncoder(JsonLfEncoders::contractId, JsonLfEncoders::contractId));

  public static final Choice<ContinuousBook, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<ContinuousBook, OpenSession, ContractId> CHOICE_OpenSession = 
      Choice.create("OpenSession", value$ -> value$.toValue(), value$ -> OpenSession.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new OpenSession.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        OpenSession::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<ContinuousBook, CancelOrder, Tuple2<ContractId, Holding.ContractId>> CHOICE_CancelOrder = 
      Choice.create("CancelOrder", value$ -> value$.toValue(), value$ -> CancelOrder.valueDecoder()
        .decode(value$), value$ ->
        Tuple2.<com.lucilla.settlement.model.continuousbook.ContinuousBook.ContractId,
        com.lucilla.settlement.model.holding.Holding.ContractId>valueDecoder(v$0 ->
          new ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        v$1 ->
          new Holding.ContractId(v$1.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
        .decode(value$), new CancelOrder.JsonDecoder$().get(),
        new Tuple2.JsonDecoder$().get(JsonLfDecoders.contractId(ContractId::new), JsonLfDecoders.contractId(Holding.ContractId::new)),
        CancelOrder::jsonEncoder,
        _x0 -> _x0.jsonEncoder(JsonLfEncoders::contractId, JsonLfEncoders::contractId));

  public static final Choice<ContinuousBook, CloseSession, ContractId> CHOICE_CloseSession = 
      Choice.create("CloseSession", value$ -> value$.toValue(), value$ ->
        CloseSession.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new CloseSession.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        CloseSession::jsonEncoder, JsonLfEncoders::contractId);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, ContinuousBook> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(ContinuousBook.PACKAGE_ID, ContinuousBook.PACKAGE_NAME, ContinuousBook.PACKAGE_VERSION),
        "com.lucilla.settlement.model.continuousbook.ContinuousBook", TEMPLATE_ID, ContractId::new,
        v -> ContinuousBook.templateValueDecoder().decode(v), ContinuousBook::fromJson,
        Contract::new, List.of(CHOICE_CloseSession, CHOICE_MatchOrder, CHOICE_CancelOrder,
        CHOICE_OpenSession, CHOICE_PlaceOrder, CHOICE_Archive, CHOICE_KillOrder));

  public final String operator;

  public final String auditor;

  public final List<String> participants;

  public final String instrumentId;

  public final String cashInstrument;

  public final BigDecimal referencePrice;

  public final BigDecimal bandFraction;

  public final Long nextSeq;

  public final Long liveCount;

  public final Boolean isOpen;

  public ContinuousBook(String operator, String auditor, List<String> participants,
      String instrumentId, String cashInstrument, BigDecimal referencePrice,
      BigDecimal bandFraction, Long nextSeq, Long liveCount, Boolean isOpen) {
    this.operator = operator;
    this.auditor = auditor;
    this.participants = participants;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.referencePrice = referencePrice;
    this.bandFraction = bandFraction;
    this.nextSeq = nextSeq;
    this.liveCount = liveCount;
    this.isOpen = isOpen;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(ContinuousBook.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePlaceOrder} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, RestingOrder.ContractId>>> createAndExercisePlaceOrder(
      PlaceOrder arg) {
    return createAnd().exercisePlaceOrder(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePlaceOrder} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, RestingOrder.ContractId>>> createAndExercisePlaceOrder(
      String trader, BookSide side, BigDecimal quantity, Optional<BigDecimal> limitPrice,
      TimeInForce timeInForce, Holding.ContractId holdingCid) {
    return createAndExercisePlaceOrder(new PlaceOrder(trader, side, quantity, limitPrice,
        timeInForce, holdingCid));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseMatchOrder} instead
   */
  @Deprecated
  public Update<Exercised<MatchResult>> createAndExerciseMatchOrder(MatchOrder arg) {
    return createAnd().exerciseMatchOrder(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseMatchOrder} instead
   */
  @Deprecated
  public Update<Exercised<MatchResult>> createAndExerciseMatchOrder(
      RestingOrder.ContractId aggressorCid, List<RestingOrder.ContractId> contraCids) {
    return createAndExerciseMatchOrder(new MatchOrder(aggressorCid, contraCids));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseKillOrder} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, Holding.ContractId>>> createAndExerciseKillOrder(
      KillOrder arg) {
    return createAnd().exerciseKillOrder(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseKillOrder} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, Holding.ContractId>>> createAndExerciseKillOrder(
      RestingOrder.ContractId orderCid) {
    return createAndExerciseKillOrder(new KillOrder(orderCid));
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseOpenSession} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseOpenSession(OpenSession arg) {
    return createAnd().exerciseOpenSession(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseOpenSession} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseOpenSession() {
    return createAndExerciseOpenSession(new OpenSession());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCancelOrder} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, Holding.ContractId>>> createAndExerciseCancelOrder(
      CancelOrder arg) {
    return createAnd().exerciseCancelOrder(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCancelOrder} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, Holding.ContractId>>> createAndExerciseCancelOrder(
      String trader, RestingOrder.ContractId orderCid) {
    return createAndExerciseCancelOrder(new CancelOrder(trader, orderCid));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCloseSession} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseCloseSession(CloseSession arg) {
    return createAnd().exerciseCloseSession(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCloseSession} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseCloseSession() {
    return createAndExerciseCloseSession(new CloseSession());
  }

  public static Update<Created<ContractId>> create(String operator, String auditor,
      List<String> participants, String instrumentId, String cashInstrument,
      BigDecimal referencePrice, BigDecimal bandFraction, Long nextSeq, Long liveCount,
      Boolean isOpen) {
    return new ContinuousBook(operator, auditor, participants, instrumentId, cashInstrument,
        referencePrice, bandFraction, nextSeq, liveCount, isOpen).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, ContinuousBook> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<ContinuousBook> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(10);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("participants", this.participants.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("referencePrice", new Numeric(this.referencePrice)));
    fields.add(new DamlRecord.Field("bandFraction", new Numeric(this.bandFraction)));
    fields.add(new DamlRecord.Field("nextSeq", new Int64(this.nextSeq)));
    fields.add(new DamlRecord.Field("liveCount", new Int64(this.liveCount)));
    fields.add(new DamlRecord.Field("isOpen", Bool.of(this.isOpen)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<ContinuousBook> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(10,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      List<String> participants = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(2).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      BigDecimal referencePrice = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(5).getValue());
      BigDecimal bandFraction = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(6).getValue());
      Long nextSeq = PrimitiveValueDecoders.fromInt64.decode(fields$.get(7).getValue());
      Long liveCount = PrimitiveValueDecoders.fromInt64.decode(fields$.get(8).getValue());
      Boolean isOpen = PrimitiveValueDecoders.fromBool.decode(fields$.get(9).getValue());
      return new ContinuousBook(operator, auditor, participants, instrumentId, cashInstrument,
          referencePrice, bandFraction, nextSeq, liveCount, isOpen);
    } ;
  }

  public static JsonLfDecoder<ContinuousBook> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "participants", "instrumentId", "cashInstrument", "referencePrice", "bandFraction", "nextSeq", "liveCount", "isOpen"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "participants": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "referencePrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "bandFraction": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "nextSeq": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "liveCount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "isOpen": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.bool);
            default: return null;
          }
        }
        , (Object[] args) -> new ContinuousBook(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9])));
  }

  public static ContinuousBook fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("participants", apply(JsonLfEncoders.list(JsonLfEncoders::party), participants)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("referencePrice", apply(JsonLfEncoders::numeric, referencePrice)),
        JsonLfEncoders.Field.of("bandFraction", apply(JsonLfEncoders::numeric, bandFraction)),
        JsonLfEncoders.Field.of("nextSeq", apply(JsonLfEncoders::int64, nextSeq)),
        JsonLfEncoders.Field.of("liveCount", apply(JsonLfEncoders::int64, liveCount)),
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
    if (!(object instanceof ContinuousBook)) {
      return false;
    }
    ContinuousBook other = (ContinuousBook) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.participants, other.participants) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.referencePrice, other.referencePrice) &&
        Objects.equals(this.bandFraction, other.bandFraction) &&
        Objects.equals(this.nextSeq, other.nextSeq) &&
        Objects.equals(this.liveCount, other.liveCount) &&
        Objects.equals(this.isOpen, other.isOpen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.participants, this.instrumentId,
        this.cashInstrument, this.referencePrice, this.bandFraction, this.nextSeq, this.liveCount,
        this.isOpen);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.continuousbook.ContinuousBook(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.participants, this.instrumentId, this.cashInstrument,
        this.referencePrice, this.bandFraction, this.nextSeq, this.liveCount, this.isOpen);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<ContinuousBook> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, ContinuousBook, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<ContinuousBook> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ContinuousBook> {
    public Contract(ContractId id, ContinuousBook data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, ContinuousBook> getCompanion() {
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
    default Update<Exercised<Tuple2<ContractId, RestingOrder.ContractId>>> exercisePlaceOrder(
        PlaceOrder arg) {
      return makeExerciseCmd(CHOICE_PlaceOrder, arg);
    }

    default Update<Exercised<Tuple2<ContractId, RestingOrder.ContractId>>> exercisePlaceOrder(
        String trader, BookSide side, BigDecimal quantity, Optional<BigDecimal> limitPrice,
        TimeInForce timeInForce, Holding.ContractId holdingCid) {
      return exercisePlaceOrder(new PlaceOrder(trader, side, quantity, limitPrice, timeInForce,
          holdingCid));
    }

    default Update<Exercised<MatchResult>> exerciseMatchOrder(MatchOrder arg) {
      return makeExerciseCmd(CHOICE_MatchOrder, arg);
    }

    default Update<Exercised<MatchResult>> exerciseMatchOrder(RestingOrder.ContractId aggressorCid,
        List<RestingOrder.ContractId> contraCids) {
      return exerciseMatchOrder(new MatchOrder(aggressorCid, contraCids));
    }

    default Update<Exercised<Tuple2<ContractId, Holding.ContractId>>> exerciseKillOrder(
        KillOrder arg) {
      return makeExerciseCmd(CHOICE_KillOrder, arg);
    }

    default Update<Exercised<Tuple2<ContractId, Holding.ContractId>>> exerciseKillOrder(
        RestingOrder.ContractId orderCid) {
      return exerciseKillOrder(new KillOrder(orderCid));
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<ContractId>> exerciseOpenSession(OpenSession arg) {
      return makeExerciseCmd(CHOICE_OpenSession, arg);
    }

    default Update<Exercised<ContractId>> exerciseOpenSession() {
      return exerciseOpenSession(new OpenSession());
    }

    default Update<Exercised<Tuple2<ContractId, Holding.ContractId>>> exerciseCancelOrder(
        CancelOrder arg) {
      return makeExerciseCmd(CHOICE_CancelOrder, arg);
    }

    default Update<Exercised<Tuple2<ContractId, Holding.ContractId>>> exerciseCancelOrder(
        String trader, RestingOrder.ContractId orderCid) {
      return exerciseCancelOrder(new CancelOrder(trader, orderCid));
    }

    default Update<Exercised<ContractId>> exerciseCloseSession(CloseSession arg) {
      return makeExerciseCmd(CHOICE_CloseSession, arg);
    }

    default Update<Exercised<ContractId>> exerciseCloseSession() {
      return exerciseCloseSession(new CloseSession());
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, ContinuousBook, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ContinuousBook> get() {
      return jsonDecoder();
    }
  }
}
