package com.lucilla.settlement.model.marketonclose;

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
  public static final Identifier TEMPLATE_ID = new Identifier("85b662c2e7d0a4d42bea5a2232989bd04641057da99c8bda47d7f7b912ef699c", "MarketOnClose", "ClosingAuction");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("85b662c2e7d0a4d42bea5a2232989bd04641057da99c8bda47d7f7b912ef699c", "MarketOnClose", "ClosingAuction");

  public static final String PACKAGE_ID = "85b662c2e7d0a4d42bea5a2232989bd04641057da99c8bda47d7f7b912ef699c";

  public static final Choice<ClosingAuction, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<ClosingAuction, RunClose, SettlementBatch.ContractId> CHOICE_RunClose = 
      Choice.create("RunClose", value$ -> value$.toValue(), value$ -> RunClose.valueDecoder()
        .decode(value$), value$ ->
        new SettlementBatch.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<ClosingAuction, SubmitOrder, SealedOrder.ContractId> CHOICE_SubmitOrder = 
      Choice.create("SubmitOrder", value$ -> value$.toValue(), value$ -> SubmitOrder.valueDecoder()
        .decode(value$), value$ ->
        new SealedOrder.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<ClosingAuction, CloseBidding, ContractId> CHOICE_CloseBidding = 
      Choice.create("CloseBidding", value$ -> value$.toValue(), value$ ->
        CloseBidding.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, ClosingAuction> COMPANION = 
      new ContractCompanion.WithoutKey<>(
        "com.lucilla.settlement.model.marketonclose.ClosingAuction", TEMPLATE_ID,
        TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> ClosingAuction.templateValueDecoder().decode(v), ClosingAuction::fromJson,
        Contract::new, List.of(CHOICE_Archive, CHOICE_RunClose, CHOICE_SubmitOrder,
        CHOICE_CloseBidding));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String operator;

  public final String auditor;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal referencePrice;

  public final List<String> participants;

  public final Boolean isOpen;

  public ClosingAuction(String operator, String auditor, String instrumentId, String cashInstrument,
      String session, BigDecimal referencePrice, List<String> participants, Boolean isOpen) {
    this.operator = operator;
    this.auditor = auditor;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.referencePrice = referencePrice;
    this.participants = participants;
    this.isOpen = isOpen;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(ClosingAuction.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseArchive} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseArchive(
      com.lucilla.settlement.model.da.internal.template.Archive arg) {
    return createAnd().exerciseArchive(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseArchive} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseArchive() {
    return createAndExerciseArchive(new com.lucilla.settlement.model.da.internal.template.Archive());
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

  public static Update<Created<ContractId>> create(String operator, String auditor,
      String instrumentId, String cashInstrument, String session, BigDecimal referencePrice,
      List<String> participants, Boolean isOpen) {
    return new ClosingAuction(operator, auditor, instrumentId, cashInstrument, session,
        referencePrice, participants, isOpen).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, ClosingAuction> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static ClosingAuction fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<ClosingAuction> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(8);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("referencePrice", new Numeric(this.referencePrice)));
    fields.add(new DamlRecord.Field("participants", this.participants.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("isOpen", Bool.of(this.isOpen)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<ClosingAuction> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(8,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      BigDecimal referencePrice = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(5).getValue());
      List<String> participants = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(6).getValue());
      Boolean isOpen = PrimitiveValueDecoders.fromBool.decode(fields$.get(7).getValue());
      return new ClosingAuction(operator, auditor, instrumentId, cashInstrument, session,
          referencePrice, participants, isOpen);
    } ;
  }

  public static JsonLfDecoder<ClosingAuction> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "instrumentId", "cashInstrument", "session", "referencePrice", "participants", "isOpen"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "referencePrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "participants": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "isOpen": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.bool);
            default: return null;
          }
        }
        , (Object[] args) -> new ClosingAuction(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7])));
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
        Objects.equals(this.isOpen, other.isOpen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.instrumentId, this.cashInstrument,
        this.session, this.referencePrice, this.participants, this.isOpen);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.ClosingAuction(%s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.instrumentId, this.cashInstrument, this.session,
        this.referencePrice, this.participants, this.isOpen);
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
    public Contract(ContractId id, ClosingAuction data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, ClosingAuction> getCompanion() {
      return COMPANION;
    }

    public static Contract fromIdAndRecord(String contractId, DamlRecord record$,
        Optional<String> agreementText, Set<String> signatories, Set<String> observers) {
      return COMPANION.fromIdAndRecord(contractId, record$, agreementText, signatories, observers);
    }

    public static Contract fromCreatedEvent(CreatedEvent event) {
      return COMPANION.fromCreatedEvent(event);
    }
  }

  public interface Exercises<Cmd> extends com.daml.ledger.javaapi.data.codegen.Exercises.Archive<Cmd> {
    default Update<Exercised<Unit>> exerciseArchive(
        com.lucilla.settlement.model.da.internal.template.Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new com.lucilla.settlement.model.da.internal.template.Archive());
    }

    default Update<Exercised<SettlementBatch.ContractId>> exerciseRunClose(RunClose arg) {
      return makeExerciseCmd(CHOICE_RunClose, arg);
    }

    default Update<Exercised<SettlementBatch.ContractId>> exerciseRunClose(
        List<SealedOrder.ContractId> buyOrders, List<SealedOrder.ContractId> sellOrders) {
      return exerciseRunClose(new RunClose(buyOrders, sellOrders));
    }

    default Update<Exercised<SealedOrder.ContractId>> exerciseSubmitOrder(SubmitOrder arg) {
      return makeExerciseCmd(CHOICE_SubmitOrder, arg);
    }

    default Update<Exercised<SealedOrder.ContractId>> exerciseSubmitOrder(String trader, Side side,
        BigDecimal quantity, BigDecimal limitPrice, Holding.ContractId holdingCid) {
      return exerciseSubmitOrder(new SubmitOrder(trader, side, quantity, limitPrice, holdingCid));
    }

    default Update<Exercised<ContractId>> exerciseCloseBidding(CloseBidding arg) {
      return makeExerciseCmd(CHOICE_CloseBidding, arg);
    }

    default Update<Exercised<ContractId>> exerciseCloseBidding() {
      return exerciseCloseBidding(new CloseBidding());
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
