package com.lucilla.settlement.model.marketonclose;

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
import com.lucilla.settlement.model.governance.NavFixing;
import com.lucilla.settlement.model.holding.Holding;
import com.lucilla.settlement.model.settlement.SettlementBatch;
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

public final class ClosingAuction extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#canton-dvp-settlement-desk", "MarketOnClose", "ClosingAuction");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "MarketOnClose", "ClosingAuction");

  public static final String PACKAGE_ID = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<ClosingAuction, PublishImbalance, ImbalanceDisclosure.ContractId> CHOICE_PublishImbalance = 
      Choice.create("PublishImbalance", value$ -> value$.toValue(), value$ ->
        PublishImbalance.valueDecoder().decode(value$), value$ ->
        new ImbalanceDisclosure.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new PublishImbalance.JsonDecoder$().get(),
        JsonLfDecoders.contractId(ImbalanceDisclosure.ContractId::new),
        PublishImbalance::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<ClosingAuction, CloseBidding, ContractId> CHOICE_CloseBidding = 
      Choice.create("CloseBidding", value$ -> value$.toValue(), value$ ->
        CloseBidding.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new CloseBidding.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        CloseBidding::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<ClosingAuction, SubmitOrder, SealedOrder.ContractId> CHOICE_SubmitOrder = 
      Choice.create("SubmitOrder", value$ -> value$.toValue(), value$ -> SubmitOrder.valueDecoder()
        .decode(value$), value$ ->
        new SealedOrder.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new SubmitOrder.JsonDecoder$().get(),
        JsonLfDecoders.contractId(SealedOrder.ContractId::new), SubmitOrder::jsonEncoder,
        JsonLfEncoders::contractId);

  public static final Choice<ClosingAuction, RunClose, SettlementBatch.ContractId> CHOICE_RunClose = 
      Choice.create("RunClose", value$ -> value$.toValue(), value$ -> RunClose.valueDecoder()
        .decode(value$), value$ ->
        new SettlementBatch.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new RunClose.JsonDecoder$().get(),
        JsonLfDecoders.contractId(SettlementBatch.ContractId::new), RunClose::jsonEncoder,
        JsonLfEncoders::contractId);

  public static final Choice<ClosingAuction, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, ClosingAuction> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(ClosingAuction.PACKAGE_ID, ClosingAuction.PACKAGE_NAME, ClosingAuction.PACKAGE_VERSION),
        "com.lucilla.settlement.model.marketonclose.ClosingAuction", TEMPLATE_ID, ContractId::new,
        v -> ClosingAuction.templateValueDecoder().decode(v), ClosingAuction::fromJson,
        Contract::new, List.of(CHOICE_SubmitOrder, CHOICE_RunClose, CHOICE_Archive,
        CHOICE_PublishImbalance, CHOICE_CloseBidding));

  public final String operator;

  public final String auditor;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal referencePrice;

  public final List<String> participants;

  public final Optional<String> liquidityProvider;

  public final Optional<NavFixing.ContractId> fixingRef;

  public final Boolean isOpen;

  public ClosingAuction(String operator, String auditor, String instrumentId, String cashInstrument,
      String session, BigDecimal referencePrice, List<String> participants,
      Optional<String> liquidityProvider, Optional<NavFixing.ContractId> fixingRef,
      Boolean isOpen) {
    this.operator = operator;
    this.auditor = auditor;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.referencePrice = referencePrice;
    this.participants = participants;
    this.liquidityProvider = liquidityProvider;
    this.fixingRef = fixingRef;
    this.isOpen = isOpen;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(ClosingAuction.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePublishImbalance} instead
   */
  @Deprecated
  public Update<Exercised<ImbalanceDisclosure.ContractId>> createAndExercisePublishImbalance(
      PublishImbalance arg) {
    return createAnd().exercisePublishImbalance(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePublishImbalance} instead
   */
  @Deprecated
  public Update<Exercised<ImbalanceDisclosure.ContractId>> createAndExercisePublishImbalance(
      List<SealedOrder.ContractId> restingOrders) {
    return createAndExercisePublishImbalance(new PublishImbalance(restingOrders));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCloseBidding} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseCloseBidding(CloseBidding arg) {
    return createAnd().exerciseCloseBidding(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCloseBidding} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseCloseBidding() {
    return createAndExerciseCloseBidding(new CloseBidding());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSubmitOrder} instead
   */
  @Deprecated
  public Update<Exercised<SealedOrder.ContractId>> createAndExerciseSubmitOrder(SubmitOrder arg) {
    return createAnd().exerciseSubmitOrder(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSubmitOrder} instead
   */
  @Deprecated
  public Update<Exercised<SealedOrder.ContractId>> createAndExerciseSubmitOrder(String trader,
      Side side, BigDecimal quantity, BigDecimal limitPrice, Holding.ContractId holdingCid) {
    return createAndExerciseSubmitOrder(new SubmitOrder(trader, side, quantity, limitPrice,
        holdingCid));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRunClose} instead
   */
  @Deprecated
  public Update<Exercised<SettlementBatch.ContractId>> createAndExerciseRunClose(RunClose arg) {
    return createAnd().exerciseRunClose(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRunClose} instead
   */
  @Deprecated
  public Update<Exercised<SettlementBatch.ContractId>> createAndExerciseRunClose(
      List<SealedOrder.ContractId> buyOrders, List<SealedOrder.ContractId> sellOrders) {
    return createAndExerciseRunClose(new RunClose(buyOrders, sellOrders));
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

  public static Update<Created<ContractId>> create(String operator, String auditor,
      String instrumentId, String cashInstrument, String session, BigDecimal referencePrice,
      List<String> participants, Optional<String> liquidityProvider,
      Optional<NavFixing.ContractId> fixingRef, Boolean isOpen) {
    return new ClosingAuction(operator, auditor, instrumentId, cashInstrument, session,
        referencePrice, participants, liquidityProvider, fixingRef, isOpen).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, ClosingAuction> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<ClosingAuction> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(10);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("referencePrice", new Numeric(this.referencePrice)));
    fields.add(new DamlRecord.Field("participants", this.participants.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("liquidityProvider", DamlOptional.of(this.liquidityProvider.map(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("fixingRef", DamlOptional.of(this.fixingRef.map(v$0 -> v$0.toValue()))));
    fields.add(new DamlRecord.Field("isOpen", Bool.of(this.isOpen)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<ClosingAuction> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(10,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      BigDecimal referencePrice = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(5).getValue());
      List<String> participants = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(6).getValue());
      Optional<String> liquidityProvider = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromParty).decode(fields$.get(7).getValue());
      Optional<NavFixing.ContractId> fixingRef = PrimitiveValueDecoders.fromOptional(v$0 ->
              new NavFixing.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected fixingRef to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(8).getValue());
      Boolean isOpen = PrimitiveValueDecoders.fromBool.decode(fields$.get(9).getValue());
      return new ClosingAuction(operator, auditor, instrumentId, cashInstrument, session,
          referencePrice, participants, liquidityProvider, fixingRef, isOpen);
    } ;
  }

  public static JsonLfDecoder<ClosingAuction> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "instrumentId", "cashInstrument", "session", "referencePrice", "participants", "liquidityProvider", "fixingRef", "isOpen"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "referencePrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "participants": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "liquidityProvider": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party), java.util.Optional.empty());
            case "fixingRef": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.governance.NavFixing.ContractId::new)), java.util.Optional.empty());
            case "isOpen": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.bool);
            default: return null;
          }
        }
        , (Object[] args) -> new ClosingAuction(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9])));
  }

  public static ClosingAuction fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("referencePrice", apply(JsonLfEncoders::numeric, referencePrice)),
        JsonLfEncoders.Field.of("participants", apply(JsonLfEncoders.list(JsonLfEncoders::party), participants)),
        JsonLfEncoders.Field.of("liquidityProvider", apply(JsonLfEncoders.optional(JsonLfEncoders::party), liquidityProvider)),
        JsonLfEncoders.Field.of("fixingRef", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), fixingRef)),
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
    if (!(object instanceof ClosingAuction)) {
      return false;
    }
    ClosingAuction other = (ClosingAuction) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) &&
        Objects.equals(this.referencePrice, other.referencePrice) &&
        Objects.equals(this.participants, other.participants) &&
        Objects.equals(this.liquidityProvider, other.liquidityProvider) &&
        Objects.equals(this.fixingRef, other.fixingRef) &&
        Objects.equals(this.isOpen, other.isOpen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.instrumentId, this.cashInstrument,
        this.session, this.referencePrice, this.participants, this.liquidityProvider,
        this.fixingRef, this.isOpen);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.ClosingAuction(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.instrumentId, this.cashInstrument, this.session,
        this.referencePrice, this.participants, this.liquidityProvider, this.fixingRef,
        this.isOpen);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<ClosingAuction> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, ClosingAuction, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<ClosingAuction> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ClosingAuction> {
    public Contract(ContractId id, ClosingAuction data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, ClosingAuction> getCompanion() {
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
    default Update<Exercised<ImbalanceDisclosure.ContractId>> exercisePublishImbalance(
        PublishImbalance arg) {
      return makeExerciseCmd(CHOICE_PublishImbalance, arg);
    }

    default Update<Exercised<ImbalanceDisclosure.ContractId>> exercisePublishImbalance(
        List<SealedOrder.ContractId> restingOrders) {
      return exercisePublishImbalance(new PublishImbalance(restingOrders));
    }

    default Update<Exercised<ContractId>> exerciseCloseBidding(CloseBidding arg) {
      return makeExerciseCmd(CHOICE_CloseBidding, arg);
    }

    default Update<Exercised<ContractId>> exerciseCloseBidding() {
      return exerciseCloseBidding(new CloseBidding());
    }

    default Update<Exercised<SealedOrder.ContractId>> exerciseSubmitOrder(SubmitOrder arg) {
      return makeExerciseCmd(CHOICE_SubmitOrder, arg);
    }

    default Update<Exercised<SealedOrder.ContractId>> exerciseSubmitOrder(String trader, Side side,
        BigDecimal quantity, BigDecimal limitPrice, Holding.ContractId holdingCid) {
      return exerciseSubmitOrder(new SubmitOrder(trader, side, quantity, limitPrice, holdingCid));
    }

    default Update<Exercised<SettlementBatch.ContractId>> exerciseRunClose(RunClose arg) {
      return makeExerciseCmd(CHOICE_RunClose, arg);
    }

    default Update<Exercised<SettlementBatch.ContractId>> exerciseRunClose(
        List<SealedOrder.ContractId> buyOrders, List<SealedOrder.ContractId> sellOrders) {
      return exerciseRunClose(new RunClose(buyOrders, sellOrders));
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, ClosingAuction, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ClosingAuction> get() {
      return jsonDecoder();
    }
  }
}
