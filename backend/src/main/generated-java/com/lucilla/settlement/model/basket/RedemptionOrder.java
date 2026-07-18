package com.lucilla.settlement.model.basket;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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

public final class RedemptionOrder extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Basket", "RedemptionOrder");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Basket", "RedemptionOrder");

  public static final String PACKAGE_ID = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public static final Choice<RedemptionOrder, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<RedemptionOrder, RejectRedemption, Unit> CHOICE_RejectRedemption = 
      Choice.create("RejectRedemption", value$ -> value$.toValue(), value$ ->
        RejectRedemption.valueDecoder().decode(value$), value$ -> PrimitiveValueDecoders.fromUnit
        .decode(value$));

  public static final Choice<RedemptionOrder, CancelRedemption, Unit> CHOICE_CancelRedemption = 
      Choice.create("CancelRedemption", value$ -> value$.toValue(), value$ ->
        CancelRedemption.valueDecoder().decode(value$), value$ -> PrimitiveValueDecoders.fromUnit
        .decode(value$));

  public static final Choice<RedemptionOrder, ApproveRedemption, RedemptionAgreement.ContractId> CHOICE_ApproveRedemption = 
      Choice.create("ApproveRedemption", value$ -> value$.toValue(), value$ ->
        ApproveRedemption.valueDecoder().decode(value$), value$ ->
        new RedemptionAgreement.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, RedemptionOrder> COMPANION = 
      new ContractCompanion.WithoutKey<>("com.lucilla.settlement.model.basket.RedemptionOrder",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> RedemptionOrder.templateValueDecoder().decode(v), RedemptionOrder::fromJson,
        Contract::new, List.of(CHOICE_Archive, CHOICE_RejectRedemption, CHOICE_CancelRedemption,
        CHOICE_ApproveRedemption));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String administrator;

  public final String ap;

  public final String auditor;

  public final String basketId;

  public final String cashInstrument;

  public final List<Component> components;

  public final BigDecimal shares;

  public final Holding.ContractId basketHoldingCid;

  public RedemptionOrder(String administrator, String ap, String auditor, String basketId,
      String cashInstrument, List<Component> components, BigDecimal shares,
      Holding.ContractId basketHoldingCid) {
    this.administrator = administrator;
    this.ap = ap;
    this.auditor = auditor;
    this.basketId = basketId;
    this.cashInstrument = cashInstrument;
    this.components = components;
    this.shares = shares;
    this.basketHoldingCid = basketHoldingCid;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(RedemptionOrder.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRejectRedemption} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseRejectRedemption(RejectRedemption arg) {
    return createAnd().exerciseRejectRedemption(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRejectRedemption} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseRejectRedemption() {
    return createAndExerciseRejectRedemption(new RejectRedemption());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCancelRedemption} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseCancelRedemption(CancelRedemption arg) {
    return createAnd().exerciseCancelRedemption(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseCancelRedemption} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseCancelRedemption() {
    return createAndExerciseCancelRedemption(new CancelRedemption());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseApproveRedemption} instead
   */
  @Deprecated
  public Update<Exercised<RedemptionAgreement.ContractId>> createAndExerciseApproveRedemption(
      ApproveRedemption arg) {
    return createAnd().exerciseApproveRedemption(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseApproveRedemption} instead
   */
  @Deprecated
  public Update<Exercised<RedemptionAgreement.ContractId>> createAndExerciseApproveRedemption(
      List<Holding.ContractId> custodyHoldingCids) {
    return createAndExerciseApproveRedemption(new ApproveRedemption(custodyHoldingCids));
  }

  public static Update<Created<ContractId>> create(String administrator, String ap, String auditor,
      String basketId, String cashInstrument, List<Component> components, BigDecimal shares,
      Holding.ContractId basketHoldingCid) {
    return new RedemptionOrder(administrator, ap, auditor, basketId, cashInstrument, components,
        shares, basketHoldingCid).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, RedemptionOrder> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static RedemptionOrder fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<RedemptionOrder> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(8);
    fields.add(new DamlRecord.Field("administrator", new Party(this.administrator)));
    fields.add(new DamlRecord.Field("ap", new Party(this.ap)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("basketId", new Text(this.basketId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("components", this.components.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new DamlRecord.Field("shares", new Numeric(this.shares)));
    fields.add(new DamlRecord.Field("basketHoldingCid", this.basketHoldingCid.toValue()));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<RedemptionOrder> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(8,0, recordValue$);
      String administrator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String ap = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String basketId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      List<Component> components = PrimitiveValueDecoders.fromList(Component.valueDecoder())
          .decode(fields$.get(5).getValue());
      BigDecimal shares = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      Holding.ContractId basketHoldingCid =
          new Holding.ContractId(fields$.get(7).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected basketHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new RedemptionOrder(administrator, ap, auditor, basketId, cashInstrument, components,
          shares, basketHoldingCid);
    } ;
  }

  public static JsonLfDecoder<RedemptionOrder> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("administrator", "ap", "auditor", "basketId", "cashInstrument", "components", "shares", "basketHoldingCid"), name -> {
          switch (name) {
            case "administrator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "ap": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "basketId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "components": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(new com.lucilla.settlement.model.basket.Component.JsonDecoder$().get()));
            case "shares": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "basketHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new RedemptionOrder(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7])));
  }

  public static RedemptionOrder fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("administrator", apply(JsonLfEncoders::party, administrator)),
        JsonLfEncoders.Field.of("ap", apply(JsonLfEncoders::party, ap)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("basketId", apply(JsonLfEncoders::text, basketId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("components", apply(JsonLfEncoders.list(Component::jsonEncoder), components)),
        JsonLfEncoders.Field.of("shares", apply(JsonLfEncoders::numeric, shares)),
        JsonLfEncoders.Field.of("basketHoldingCid", apply(JsonLfEncoders::contractId, basketHoldingCid)));
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
    if (!(object instanceof RedemptionOrder)) {
      return false;
    }
    RedemptionOrder other = (RedemptionOrder) object;
    return Objects.equals(this.administrator, other.administrator) &&
        Objects.equals(this.ap, other.ap) && Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.basketId, other.basketId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.components, other.components) &&
        Objects.equals(this.shares, other.shares) &&
        Objects.equals(this.basketHoldingCid, other.basketHoldingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.administrator, this.ap, this.auditor, this.basketId,
        this.cashInstrument, this.components, this.shares, this.basketHoldingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.basket.RedemptionOrder(%s, %s, %s, %s, %s, %s, %s, %s)",
        this.administrator, this.ap, this.auditor, this.basketId, this.cashInstrument,
        this.components, this.shares, this.basketHoldingCid);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<RedemptionOrder> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, RedemptionOrder, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<RedemptionOrder> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, RedemptionOrder> {
    public Contract(ContractId id, RedemptionOrder data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, RedemptionOrder> getCompanion() {
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

    default Update<Exercised<Unit>> exerciseRejectRedemption(RejectRedemption arg) {
      return makeExerciseCmd(CHOICE_RejectRedemption, arg);
    }

    default Update<Exercised<Unit>> exerciseRejectRedemption() {
      return exerciseRejectRedemption(new RejectRedemption());
    }

    default Update<Exercised<Unit>> exerciseCancelRedemption(CancelRedemption arg) {
      return makeExerciseCmd(CHOICE_CancelRedemption, arg);
    }

    default Update<Exercised<Unit>> exerciseCancelRedemption() {
      return exerciseCancelRedemption(new CancelRedemption());
    }

    default Update<Exercised<RedemptionAgreement.ContractId>> exerciseApproveRedemption(
        ApproveRedemption arg) {
      return makeExerciseCmd(CHOICE_ApproveRedemption, arg);
    }

    default Update<Exercised<RedemptionAgreement.ContractId>> exerciseApproveRedemption(
        List<Holding.ContractId> custodyHoldingCids) {
      return exerciseApproveRedemption(new ApproveRedemption(custodyHoldingCids));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, RedemptionOrder, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<RedemptionOrder> get() {
      return jsonDecoder();
    }
  }
}
