package com.lucilla.settlement.model.marketonclose;

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
import com.lucilla.settlement.model.holding.Holding;
import com.lucilla.settlement.model.tokensettlement.MatchSettlement;
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

public final class SealedOrder extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "MarketOnClose", "SealedOrder");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be", "MarketOnClose", "SealedOrder");

  public static final String PACKAGE_ID = "d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 0, 0});

  public static final Choice<SealedOrder, VenueCancel, Unit> CHOICE_VenueCancel = 
      Choice.create("VenueCancel", value$ -> value$.toValue(), value$ -> VenueCancel.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new VenueCancel.JsonDecoder$().get(), JsonLfDecoders.unit, VenueCancel::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<SealedOrder, PledgeToVenue, Holding.ContractId> CHOICE_PledgeToVenue = 
      Choice.create("PledgeToVenue", value$ -> value$.toValue(), value$ ->
        PledgeToVenue.valueDecoder().decode(value$), value$ ->
        new Holding.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new PledgeToVenue.JsonDecoder$().get(), JsonLfDecoders.contractId(Holding.ContractId::new),
        PledgeToVenue::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<SealedOrder, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<SealedOrder, CoSignSettlement, MatchSettlement.ContractId> CHOICE_CoSignSettlement = 
      Choice.create("CoSignSettlement", value$ -> value$.toValue(), value$ ->
        CoSignSettlement.valueDecoder().decode(value$), value$ ->
        new MatchSettlement.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new CoSignSettlement.JsonDecoder$().get(),
        JsonLfDecoders.contractId(MatchSettlement.ContractId::new), CoSignSettlement::jsonEncoder,
        JsonLfEncoders::contractId);

  public static final Choice<SealedOrder, Cancel, Optional<Holding.ContractId>> CHOICE_Cancel = 
      Choice.create("Cancel", value$ -> value$.toValue(), value$ -> Cancel.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromOptional(v$0 ->
            new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
        .decode(value$), new Cancel.JsonDecoder$().get(),
        JsonLfDecoders.optional(JsonLfDecoders.contractId(Holding.ContractId::new)),
        Cancel::jsonEncoder, JsonLfEncoders.optional(JsonLfEncoders::contractId));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, SealedOrder> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(SealedOrder.PACKAGE_ID, SealedOrder.PACKAGE_NAME, SealedOrder.PACKAGE_VERSION),
        "com.lucilla.settlement.model.marketonclose.SealedOrder", TEMPLATE_ID, ContractId::new,
        v -> SealedOrder.templateValueDecoder().decode(v), SealedOrder::fromJson, Contract::new,
        List.of(CHOICE_Cancel, CHOICE_CoSignSettlement, CHOICE_VenueCancel, CHOICE_Archive,
        CHOICE_PledgeToVenue));

  public final String operator;

  public final String auditor;

  public final String trader;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final Side side;

  public final BigDecimal quantity;

  public final Optional<BigDecimal> limitPrice;

  public final OrderBacking backing;

  public final Long seqNo;

  public SealedOrder(String operator, String auditor, String trader, String instrumentId,
      String cashInstrument, String session, Side side, BigDecimal quantity,
      Optional<BigDecimal> limitPrice, OrderBacking backing, Long seqNo) {
    this.operator = operator;
    this.auditor = auditor;
    this.trader = trader;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.side = side;
    this.quantity = quantity;
    this.limitPrice = limitPrice;
    this.backing = backing;
    this.seqNo = seqNo;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(SealedOrder.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseVenueCancel} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseVenueCancel(VenueCancel arg) {
    return createAnd().exerciseVenueCancel(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseVenueCancel} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseVenueCancel() {
    return createAndExerciseVenueCancel(new VenueCancel());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePledgeToVenue} instead
   */
  @Deprecated
  public Update<Exercised<Holding.ContractId>> createAndExercisePledgeToVenue(PledgeToVenue arg) {
    return createAnd().exercisePledgeToVenue(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePledgeToVenue} instead
   */
  @Deprecated
  public Update<Exercised<Holding.ContractId>> createAndExercisePledgeToVenue(BigDecimal fillQty,
      BigDecimal closingPrice) {
    return createAndExercisePledgeToVenue(new PledgeToVenue(fillQty, closingPrice));
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCoSignSettlement} instead
   */
  @Deprecated
  public Update<Exercised<MatchSettlement.ContractId>> createAndExerciseCoSignSettlement(
      CoSignSettlement arg) {
    return createAnd().exerciseCoSignSettlement(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCoSignSettlement} instead
   */
  @Deprecated
  public Update<Exercised<MatchSettlement.ContractId>> createAndExerciseCoSignSettlement(
      MatchSettlement.ContractId settlementCid, List<String> currentSigners,
      List<ContractId> alsoSign) {
    return createAndExerciseCoSignSettlement(new CoSignSettlement(settlementCid, currentSigners,
        alsoSign));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCancel} instead
   */
  @Deprecated
  public Update<Exercised<Optional<Holding.ContractId>>> createAndExerciseCancel(Cancel arg) {
    return createAnd().exerciseCancel(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCancel} instead
   */
  @Deprecated
  public Update<Exercised<Optional<Holding.ContractId>>> createAndExerciseCancel() {
    return createAndExerciseCancel(new Cancel());
  }

  public static Update<Created<ContractId>> create(String operator, String auditor, String trader,
      String instrumentId, String cashInstrument, String session, Side side, BigDecimal quantity,
      Optional<BigDecimal> limitPrice, OrderBacking backing, Long seqNo) {
    return new SealedOrder(operator, auditor, trader, instrumentId, cashInstrument, session, side,
        quantity, limitPrice, backing, seqNo).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, SealedOrder> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<SealedOrder> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(11);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("side", this.side.toValue()));
    fields.add(new DamlRecord.Field("quantity", new Numeric(this.quantity)));
    fields.add(new DamlRecord.Field("limitPrice", DamlOptional.of(this.limitPrice.map(v$0 -> new Numeric(v$0)))));
    fields.add(new DamlRecord.Field("backing", this.backing.toValue()));
    fields.add(new DamlRecord.Field("seqNo", new Int64(this.seqNo)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<SealedOrder> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(11,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      Side side = Side.valueDecoder().decode(fields$.get(6).getValue());
      BigDecimal quantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(7).getValue());
      Optional<BigDecimal> limitPrice = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromNumeric).decode(fields$.get(8).getValue());
      OrderBacking backing = OrderBacking.valueDecoder().decode(fields$.get(9).getValue());
      Long seqNo = PrimitiveValueDecoders.fromInt64.decode(fields$.get(10).getValue());
      return new SealedOrder(operator, auditor, trader, instrumentId, cashInstrument, session, side,
          quantity, limitPrice, backing, seqNo);
    } ;
  }

  public static JsonLfDecoder<SealedOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "trader", "instrumentId", "cashInstrument", "session", "side", "quantity", "limitPrice", "backing", "seqNo"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "side": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, new com.lucilla.settlement.model.marketonclose.Side.JsonDecoder$().get());
            case "quantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "limitPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10)), java.util.Optional.empty());
            case "backing": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, new com.lucilla.settlement.model.marketonclose.OrderBacking.JsonDecoder$().get());
            case "seqNo": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            default: return null;
          }
        }
        , (Object[] args) -> new SealedOrder(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10])));
  }

  public static SealedOrder fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("trader", apply(JsonLfEncoders::party, trader)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("side", apply(Side::jsonEncoder, side)),
        JsonLfEncoders.Field.of("quantity", apply(JsonLfEncoders::numeric, quantity)),
        JsonLfEncoders.Field.of("limitPrice", apply(JsonLfEncoders.optional(JsonLfEncoders::numeric), limitPrice)),
        JsonLfEncoders.Field.of("backing", apply(OrderBacking::jsonEncoder, backing)),
        JsonLfEncoders.Field.of("seqNo", apply(JsonLfEncoders::int64, seqNo)));
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
    if (!(object instanceof SealedOrder)) {
      return false;
    }
    SealedOrder other = (SealedOrder) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) && Objects.equals(this.trader, other.trader) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) && Objects.equals(this.side, other.side) &&
        Objects.equals(this.quantity, other.quantity) &&
        Objects.equals(this.limitPrice, other.limitPrice) &&
        Objects.equals(this.backing, other.backing) && Objects.equals(this.seqNo, other.seqNo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.trader, this.instrumentId,
        this.cashInstrument, this.session, this.side, this.quantity, this.limitPrice, this.backing,
        this.seqNo);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.SealedOrder(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.trader, this.instrumentId, this.cashInstrument,
        this.session, this.side, this.quantity, this.limitPrice, this.backing, this.seqNo);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<SealedOrder> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, SealedOrder, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<SealedOrder> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, SealedOrder> {
    public Contract(ContractId id, SealedOrder data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, SealedOrder> getCompanion() {
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
    default Update<Exercised<Unit>> exerciseVenueCancel(VenueCancel arg) {
      return makeExerciseCmd(CHOICE_VenueCancel, arg);
    }

    default Update<Exercised<Unit>> exerciseVenueCancel() {
      return exerciseVenueCancel(new VenueCancel());
    }

    default Update<Exercised<Holding.ContractId>> exercisePledgeToVenue(PledgeToVenue arg) {
      return makeExerciseCmd(CHOICE_PledgeToVenue, arg);
    }

    default Update<Exercised<Holding.ContractId>> exercisePledgeToVenue(BigDecimal fillQty,
        BigDecimal closingPrice) {
      return exercisePledgeToVenue(new PledgeToVenue(fillQty, closingPrice));
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<MatchSettlement.ContractId>> exerciseCoSignSettlement(
        CoSignSettlement arg) {
      return makeExerciseCmd(CHOICE_CoSignSettlement, arg);
    }

    default Update<Exercised<MatchSettlement.ContractId>> exerciseCoSignSettlement(
        MatchSettlement.ContractId settlementCid, List<String> currentSigners,
        List<ContractId> alsoSign) {
      return exerciseCoSignSettlement(new CoSignSettlement(settlementCid, currentSigners,
          alsoSign));
    }

    default Update<Exercised<Optional<Holding.ContractId>>> exerciseCancel(Cancel arg) {
      return makeExerciseCmd(CHOICE_Cancel, arg);
    }

    default Update<Exercised<Optional<Holding.ContractId>>> exerciseCancel() {
      return exerciseCancel(new Cancel());
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, SealedOrder, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<SealedOrder> get() {
      return jsonDecoder();
    }
  }
}
