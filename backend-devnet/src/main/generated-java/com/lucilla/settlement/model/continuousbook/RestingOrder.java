package com.lucilla.settlement.model.continuousbook;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlOptional;
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

public final class RestingOrder extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "ContinuousBook", "RestingOrder");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("504d21e4573fdcb737242ee9149b3e88f1ec7d6bd5a76b5701f4762c36fd8ae4", "ContinuousBook", "RestingOrder");

  public static final String PACKAGE_ID = "504d21e4573fdcb737242ee9149b3e88f1ec7d6bd5a76b5701f4762c36fd8ae4";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<RestingOrder, Fill, Tuple2<Holding.ContractId, Optional<ContractId>>> CHOICE_Fill = 
      Choice.create("Fill", value$ -> value$.toValue(), value$ -> Fill.valueDecoder()
        .decode(value$), value$ -> Tuple2.<com.lucilla.settlement.model.holding.Holding.ContractId,
        java.util.Optional<com.lucilla.settlement.model.continuousbook.RestingOrder.ContractId>>valueDecoder(v$0 ->
          new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        PrimitiveValueDecoders.fromOptional(v$1 ->
            new ContractId(v$1.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue())))
        .decode(value$), new Fill.JsonDecoder$().get(),
        new Tuple2.JsonDecoder$().get(JsonLfDecoders.contractId(Holding.ContractId::new), JsonLfDecoders.optional(JsonLfDecoders.contractId(ContractId::new))),
        Fill::jsonEncoder,
        _x0 -> _x0.jsonEncoder(JsonLfEncoders::contractId, JsonLfEncoders.optional(JsonLfEncoders::contractId)));

  public static final Choice<RestingOrder, Release, Holding.ContractId> CHOICE_Release = 
      Choice.create("Release", value$ -> value$.toValue(), value$ -> Release.valueDecoder()
        .decode(value$), value$ ->
        new Holding.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new Release.JsonDecoder$().get(), JsonLfDecoders.contractId(Holding.ContractId::new),
        Release::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<RestingOrder, VenueRelease, Holding.ContractId> CHOICE_VenueRelease = 
      Choice.create("VenueRelease", value$ -> value$.toValue(), value$ ->
        VenueRelease.valueDecoder().decode(value$), value$ ->
        new Holding.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new VenueRelease.JsonDecoder$().get(), JsonLfDecoders.contractId(Holding.ContractId::new),
        VenueRelease::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<RestingOrder, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, RestingOrder> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(RestingOrder.PACKAGE_ID, RestingOrder.PACKAGE_NAME, RestingOrder.PACKAGE_VERSION),
        "com.lucilla.settlement.model.continuousbook.RestingOrder", TEMPLATE_ID, ContractId::new,
        v -> RestingOrder.templateValueDecoder().decode(v), RestingOrder::fromJson, Contract::new,
        List.of(CHOICE_Fill, CHOICE_Release, CHOICE_VenueRelease, CHOICE_Archive));

  public final String operator;

  public final String auditor;

  public final String trader;

  public final String instrumentId;

  public final String cashInstrument;

  public final BookSide side;

  public final BigDecimal quantity;

  public final Optional<BigDecimal> limitPrice;

  public final TimeInForce timeInForce;

  public final Long seqNo;

  public final Holding.ContractId holdingCid;

  public RestingOrder(String operator, String auditor, String trader, String instrumentId,
      String cashInstrument, BookSide side, BigDecimal quantity, Optional<BigDecimal> limitPrice,
      TimeInForce timeInForce, Long seqNo, Holding.ContractId holdingCid) {
    this.operator = operator;
    this.auditor = auditor;
    this.trader = trader;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.side = side;
    this.quantity = quantity;
    this.limitPrice = limitPrice;
    this.timeInForce = timeInForce;
    this.seqNo = seqNo;
    this.holdingCid = holdingCid;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(RestingOrder.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseFill} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<Holding.ContractId, Optional<ContractId>>>> createAndExerciseFill(
      Fill arg) {
    return createAnd().exerciseFill(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseFill} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<Holding.ContractId, Optional<ContractId>>>> createAndExerciseFill(
      BigDecimal fillQty, BigDecimal tradePrice) {
    return createAndExerciseFill(new Fill(fillQty, tradePrice));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRelease} instead
   */
  @Deprecated
  public Update<Exercised<Holding.ContractId>> createAndExerciseRelease(Release arg) {
    return createAnd().exerciseRelease(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRelease} instead
   */
  @Deprecated
  public Update<Exercised<Holding.ContractId>> createAndExerciseRelease() {
    return createAndExerciseRelease(new Release());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseVenueRelease} instead
   */
  @Deprecated
  public Update<Exercised<Holding.ContractId>> createAndExerciseVenueRelease(VenueRelease arg) {
    return createAnd().exerciseVenueRelease(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseVenueRelease} instead
   */
  @Deprecated
  public Update<Exercised<Holding.ContractId>> createAndExerciseVenueRelease() {
    return createAndExerciseVenueRelease(new VenueRelease());
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

  public static Update<Created<ContractId>> create(String operator, String auditor, String trader,
      String instrumentId, String cashInstrument, BookSide side, BigDecimal quantity,
      Optional<BigDecimal> limitPrice, TimeInForce timeInForce, Long seqNo,
      Holding.ContractId holdingCid) {
    return new RestingOrder(operator, auditor, trader, instrumentId, cashInstrument, side, quantity,
        limitPrice, timeInForce, seqNo, holdingCid).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, RestingOrder> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<RestingOrder> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(11);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("side", this.side.toValue()));
    fields.add(new DamlRecord.Field("quantity", new Numeric(this.quantity)));
    fields.add(new DamlRecord.Field("limitPrice", DamlOptional.of(this.limitPrice.map(v$0 -> new Numeric(v$0)))));
    fields.add(new DamlRecord.Field("timeInForce", this.timeInForce.toValue()));
    fields.add(new DamlRecord.Field("seqNo", new Int64(this.seqNo)));
    fields.add(new DamlRecord.Field("holdingCid", this.holdingCid.toValue()));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<RestingOrder> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(11,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      BookSide side = BookSide.valueDecoder().decode(fields$.get(5).getValue());
      BigDecimal quantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      Optional<BigDecimal> limitPrice = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromNumeric).decode(fields$.get(7).getValue());
      TimeInForce timeInForce = TimeInForce.valueDecoder().decode(fields$.get(8).getValue());
      Long seqNo = PrimitiveValueDecoders.fromInt64.decode(fields$.get(9).getValue());
      Holding.ContractId holdingCid =
          new Holding.ContractId(fields$.get(10).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected holdingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new RestingOrder(operator, auditor, trader, instrumentId, cashInstrument, side,
          quantity, limitPrice, timeInForce, seqNo, holdingCid);
    } ;
  }

  public static JsonLfDecoder<RestingOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "trader", "instrumentId", "cashInstrument", "side", "quantity", "limitPrice", "timeInForce", "seqNo", "holdingCid"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "side": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, new com.lucilla.settlement.model.continuousbook.BookSide.JsonDecoder$().get());
            case "quantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "limitPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10)), java.util.Optional.empty());
            case "timeInForce": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, new com.lucilla.settlement.model.continuousbook.TimeInForce.JsonDecoder$().get());
            case "seqNo": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "holdingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new RestingOrder(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10])));
  }

  public static RestingOrder fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("trader", apply(JsonLfEncoders::party, trader)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("side", apply(BookSide::jsonEncoder, side)),
        JsonLfEncoders.Field.of("quantity", apply(JsonLfEncoders::numeric, quantity)),
        JsonLfEncoders.Field.of("limitPrice", apply(JsonLfEncoders.optional(JsonLfEncoders::numeric), limitPrice)),
        JsonLfEncoders.Field.of("timeInForce", apply(TimeInForce::jsonEncoder, timeInForce)),
        JsonLfEncoders.Field.of("seqNo", apply(JsonLfEncoders::int64, seqNo)),
        JsonLfEncoders.Field.of("holdingCid", apply(JsonLfEncoders::contractId, holdingCid)));
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
    if (!(object instanceof RestingOrder)) {
      return false;
    }
    RestingOrder other = (RestingOrder) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) && Objects.equals(this.trader, other.trader) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.side, other.side) && Objects.equals(this.quantity, other.quantity) &&
        Objects.equals(this.limitPrice, other.limitPrice) &&
        Objects.equals(this.timeInForce, other.timeInForce) &&
        Objects.equals(this.seqNo, other.seqNo) &&
        Objects.equals(this.holdingCid, other.holdingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.trader, this.instrumentId,
        this.cashInstrument, this.side, this.quantity, this.limitPrice, this.timeInForce,
        this.seqNo, this.holdingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.continuousbook.RestingOrder(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.trader, this.instrumentId, this.cashInstrument, this.side,
        this.quantity, this.limitPrice, this.timeInForce, this.seqNo, this.holdingCid);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<RestingOrder> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, RestingOrder, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<RestingOrder> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, RestingOrder> {
    public Contract(ContractId id, RestingOrder data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, RestingOrder> getCompanion() {
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
    default Update<Exercised<Tuple2<Holding.ContractId, Optional<ContractId>>>> exerciseFill(
        Fill arg) {
      return makeExerciseCmd(CHOICE_Fill, arg);
    }

    default Update<Exercised<Tuple2<Holding.ContractId, Optional<ContractId>>>> exerciseFill(
        BigDecimal fillQty, BigDecimal tradePrice) {
      return exerciseFill(new Fill(fillQty, tradePrice));
    }

    default Update<Exercised<Holding.ContractId>> exerciseRelease(Release arg) {
      return makeExerciseCmd(CHOICE_Release, arg);
    }

    default Update<Exercised<Holding.ContractId>> exerciseRelease() {
      return exerciseRelease(new Release());
    }

    default Update<Exercised<Holding.ContractId>> exerciseVenueRelease(VenueRelease arg) {
      return makeExerciseCmd(CHOICE_VenueRelease, arg);
    }

    default Update<Exercised<Holding.ContractId>> exerciseVenueRelease() {
      return exerciseVenueRelease(new VenueRelease());
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, RestingOrder, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<RestingOrder> get() {
      return jsonDecoder();
    }
  }
}
