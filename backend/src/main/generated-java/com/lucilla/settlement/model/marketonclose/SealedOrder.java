package com.lucilla.settlement.model.marketonclose;

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

public final class SealedOrder extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("12f056257e4f6e96f8abcaafbc3d7261e58f3fcddcba133f3033b91190110371", "MarketOnClose", "SealedOrder");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("12f056257e4f6e96f8abcaafbc3d7261e58f3fcddcba133f3033b91190110371", "MarketOnClose", "SealedOrder");

  public static final String PACKAGE_ID = "12f056257e4f6e96f8abcaafbc3d7261e58f3fcddcba133f3033b91190110371";

  public static final Choice<SealedOrder, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<SealedOrder, VenueCancel, Unit> CHOICE_VenueCancel = 
      Choice.create("VenueCancel", value$ -> value$.toValue(), value$ -> VenueCancel.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<SealedOrder, PledgeToVenue, Holding.ContractId> CHOICE_PledgeToVenue = 
      Choice.create("PledgeToVenue", value$ -> value$.toValue(), value$ ->
        PledgeToVenue.valueDecoder().decode(value$), value$ ->
        new Holding.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<SealedOrder, Cancel, Holding.ContractId> CHOICE_Cancel = 
      Choice.create("Cancel", value$ -> value$.toValue(), value$ -> Cancel.valueDecoder()
        .decode(value$), value$ ->
        new Holding.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, SealedOrder> COMPANION = 
      new ContractCompanion.WithoutKey<>("com.lucilla.settlement.model.marketonclose.SealedOrder",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> SealedOrder.templateValueDecoder().decode(v), SealedOrder::fromJson, Contract::new,
        List.of(CHOICE_Archive, CHOICE_VenueCancel, CHOICE_PledgeToVenue, CHOICE_Cancel));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String operator;

  public final String auditor;

  public final String trader;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final Side side;

  public final BigDecimal quantity;

  public final BigDecimal limitPrice;

  public final Holding.ContractId holdingCid;

  public SealedOrder(String operator, String auditor, String trader, String instrumentId,
      String cashInstrument, String session, Side side, BigDecimal quantity, BigDecimal limitPrice,
      Holding.ContractId holdingCid) {
    this.operator = operator;
    this.auditor = auditor;
    this.trader = trader;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.side = side;
    this.quantity = quantity;
    this.limitPrice = limitPrice;
    this.holdingCid = holdingCid;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(SealedOrder.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCancel} instead
   */
  @Deprecated
  public Update<Exercised<Holding.ContractId>> createAndExerciseCancel(Cancel arg) {
    return createAnd().exerciseCancel(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCancel} instead
   */
  @Deprecated
  public Update<Exercised<Holding.ContractId>> createAndExerciseCancel() {
    return createAndExerciseCancel(new Cancel());
  }

  public static Update<Created<ContractId>> create(String operator, String auditor, String trader,
      String instrumentId, String cashInstrument, String session, Side side, BigDecimal quantity,
      BigDecimal limitPrice, Holding.ContractId holdingCid) {
    return new SealedOrder(operator, auditor, trader, instrumentId, cashInstrument, session, side,
        quantity, limitPrice, holdingCid).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, SealedOrder> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static SealedOrder fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<SealedOrder> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(10);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("side", this.side.toValue()));
    fields.add(new DamlRecord.Field("quantity", new Numeric(this.quantity)));
    fields.add(new DamlRecord.Field("limitPrice", new Numeric(this.limitPrice)));
    fields.add(new DamlRecord.Field("holdingCid", this.holdingCid.toValue()));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<SealedOrder> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(10,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      Side side = Side.valueDecoder().decode(fields$.get(6).getValue());
      BigDecimal quantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(7).getValue());
      BigDecimal limitPrice = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(8).getValue());
      Holding.ContractId holdingCid =
          new Holding.ContractId(fields$.get(9).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected holdingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new SealedOrder(operator, auditor, trader, instrumentId, cashInstrument, session, side,
          quantity, limitPrice, holdingCid);
    } ;
  }

  public static JsonLfDecoder<SealedOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "trader", "instrumentId", "cashInstrument", "session", "side", "quantity", "limitPrice", "holdingCid"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "side": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, new com.lucilla.settlement.model.marketonclose.Side.JsonDecoder$().get());
            case "quantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "limitPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "holdingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new SealedOrder(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9])));
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
        JsonLfEncoders.Field.of("limitPrice", apply(JsonLfEncoders::numeric, limitPrice)),
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
        Objects.equals(this.holdingCid, other.holdingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.trader, this.instrumentId,
        this.cashInstrument, this.session, this.side, this.quantity, this.limitPrice,
        this.holdingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.SealedOrder(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.trader, this.instrumentId, this.cashInstrument,
        this.session, this.side, this.quantity, this.limitPrice, this.holdingCid);
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
    public Contract(ContractId id, SealedOrder data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, SealedOrder> getCompanion() {
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

    default Update<Exercised<Holding.ContractId>> exerciseCancel(Cancel arg) {
      return makeExerciseCmd(CHOICE_Cancel, arg);
    }

    default Update<Exercised<Holding.ContractId>> exerciseCancel() {
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
