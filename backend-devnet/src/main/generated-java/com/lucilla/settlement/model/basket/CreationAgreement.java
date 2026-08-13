package com.lucilla.settlement.model.basket;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

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

public final class CreationAgreement extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "Basket", "CreationAgreement");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("7eca29e115ad24f98fd4190f21ac6d7440ce8f3211675421f555856febed4e5c", "Basket", "CreationAgreement");

  public static final String PACKAGE_ID = "7eca29e115ad24f98fd4190f21ac6d7440ce8f3211675421f555856febed4e5c";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<CreationAgreement, ProcessCreation, CreationResult> CHOICE_ProcessCreation = 
      Choice.create("ProcessCreation", value$ -> value$.toValue(), value$ ->
        ProcessCreation.valueDecoder().decode(value$), value$ -> CreationResult.valueDecoder()
        .decode(value$), new ProcessCreation.JsonDecoder$().get(),
        new CreationResult.JsonDecoder$().get(), ProcessCreation::jsonEncoder,
        CreationResult::jsonEncoder);

  public static final Choice<CreationAgreement, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, CreationAgreement> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(CreationAgreement.PACKAGE_ID, CreationAgreement.PACKAGE_NAME, CreationAgreement.PACKAGE_VERSION),
        "com.lucilla.settlement.model.basket.CreationAgreement", TEMPLATE_ID, ContractId::new,
        v -> CreationAgreement.templateValueDecoder().decode(v), CreationAgreement::fromJson,
        Contract::new, List.of(CHOICE_ProcessCreation, CHOICE_Archive));

  public final String administrator;

  public final String ap;

  public final String auditor;

  public final String basketId;

  public final String cashInstrument;

  public final List<Component> components;

  public final BigDecimal shares;

  public final List<Holding.ContractId> componentHoldingCids;

  public final Optional<String> feeReceiver;

  public final Optional<BigDecimal> fee;

  public final Optional<Holding.ContractId> feeHoldingCid;

  public CreationAgreement(String administrator, String ap, String auditor, String basketId,
      String cashInstrument, List<Component> components, BigDecimal shares,
      List<Holding.ContractId> componentHoldingCids, Optional<String> feeReceiver,
      Optional<BigDecimal> fee, Optional<Holding.ContractId> feeHoldingCid) {
    this.administrator = administrator;
    this.ap = ap;
    this.auditor = auditor;
    this.basketId = basketId;
    this.cashInstrument = cashInstrument;
    this.components = components;
    this.shares = shares;
    this.componentHoldingCids = componentHoldingCids;
    this.feeReceiver = feeReceiver;
    this.fee = fee;
    this.feeHoldingCid = feeHoldingCid;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(CreationAgreement.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseProcessCreation} instead
   */
  @Deprecated
  public Update<Exercised<CreationResult>> createAndExerciseProcessCreation(ProcessCreation arg) {
    return createAnd().exerciseProcessCreation(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseProcessCreation} instead
   */
  @Deprecated
  public Update<Exercised<CreationResult>> createAndExerciseProcessCreation() {
    return createAndExerciseProcessCreation(new ProcessCreation());
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

  public static Update<Created<ContractId>> create(String administrator, String ap, String auditor,
      String basketId, String cashInstrument, List<Component> components, BigDecimal shares,
      List<Holding.ContractId> componentHoldingCids, Optional<String> feeReceiver,
      Optional<BigDecimal> fee, Optional<Holding.ContractId> feeHoldingCid) {
    return new CreationAgreement(administrator, ap, auditor, basketId, cashInstrument, components,
        shares, componentHoldingCids, feeReceiver, fee, feeHoldingCid).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, CreationAgreement> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<CreationAgreement> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(11);
    fields.add(new DamlRecord.Field("administrator", new Party(this.administrator)));
    fields.add(new DamlRecord.Field("ap", new Party(this.ap)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("basketId", new Text(this.basketId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("components", this.components.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new DamlRecord.Field("shares", new Numeric(this.shares)));
    fields.add(new DamlRecord.Field("componentHoldingCids", this.componentHoldingCids.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new DamlRecord.Field("feeReceiver", DamlOptional.of(this.feeReceiver.map(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("fee", DamlOptional.of(this.fee.map(v$0 -> new Numeric(v$0)))));
    fields.add(new DamlRecord.Field("feeHoldingCid", DamlOptional.of(this.feeHoldingCid.map(v$0 -> v$0.toValue()))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<CreationAgreement> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(11,3, recordValue$);
      String administrator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String ap = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String basketId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      List<Component> components = PrimitiveValueDecoders.fromList(Component.valueDecoder())
          .decode(fields$.get(5).getValue());
      BigDecimal shares = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      List<Holding.ContractId> componentHoldingCids = PrimitiveValueDecoders.fromList(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected componentHoldingCids to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(7).getValue());
      Optional<String> feeReceiver = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromParty).decode(fields$.get(8).getValue());
      Optional<BigDecimal> fee = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromNumeric).decode(fields$.get(9).getValue());
      Optional<Holding.ContractId> feeHoldingCid = PrimitiveValueDecoders.fromOptional(v$0 ->
              new Holding.ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected feeHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(10).getValue());
      return new CreationAgreement(administrator, ap, auditor, basketId, cashInstrument, components,
          shares, componentHoldingCids, feeReceiver, fee, feeHoldingCid);
    } ;
  }

  public static JsonLfDecoder<CreationAgreement> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("administrator", "ap", "auditor", "basketId", "cashInstrument", "components", "shares", "componentHoldingCids", "feeReceiver", "fee", "feeHoldingCid"), name -> {
          switch (name) {
            case "administrator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "ap": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "basketId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "components": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(new com.lucilla.settlement.model.basket.Component.JsonDecoder$().get()));
            case "shares": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "componentHoldingCids": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new)));
            case "feeReceiver": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party), java.util.Optional.empty());
            case "fee": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10)), java.util.Optional.empty());
            case "feeHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new)), java.util.Optional.empty());
            default: return null;
          }
        }
        , (Object[] args) -> new CreationAgreement(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10])));
  }

  public static CreationAgreement fromJson(String json) throws JsonLfDecoder.Error {
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
        JsonLfEncoders.Field.of("componentHoldingCids", apply(JsonLfEncoders.list(JsonLfEncoders::contractId), componentHoldingCids)),
        JsonLfEncoders.Field.of("feeReceiver", apply(JsonLfEncoders.optional(JsonLfEncoders::party), feeReceiver)),
        JsonLfEncoders.Field.of("fee", apply(JsonLfEncoders.optional(JsonLfEncoders::numeric), fee)),
        JsonLfEncoders.Field.of("feeHoldingCid", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), feeHoldingCid)));
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
    if (!(object instanceof CreationAgreement)) {
      return false;
    }
    CreationAgreement other = (CreationAgreement) object;
    return Objects.equals(this.administrator, other.administrator) &&
        Objects.equals(this.ap, other.ap) && Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.basketId, other.basketId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.components, other.components) &&
        Objects.equals(this.shares, other.shares) &&
        Objects.equals(this.componentHoldingCids, other.componentHoldingCids) &&
        Objects.equals(this.feeReceiver, other.feeReceiver) &&
        Objects.equals(this.fee, other.fee) &&
        Objects.equals(this.feeHoldingCid, other.feeHoldingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.administrator, this.ap, this.auditor, this.basketId,
        this.cashInstrument, this.components, this.shares, this.componentHoldingCids,
        this.feeReceiver, this.fee, this.feeHoldingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.basket.CreationAgreement(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.administrator, this.ap, this.auditor, this.basketId, this.cashInstrument,
        this.components, this.shares, this.componentHoldingCids, this.feeReceiver, this.fee,
        this.feeHoldingCid);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<CreationAgreement> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, CreationAgreement, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<CreationAgreement> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, CreationAgreement> {
    public Contract(ContractId id, CreationAgreement data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, CreationAgreement> getCompanion() {
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
    default Update<Exercised<CreationResult>> exerciseProcessCreation(ProcessCreation arg) {
      return makeExerciseCmd(CHOICE_ProcessCreation, arg);
    }

    default Update<Exercised<CreationResult>> exerciseProcessCreation() {
      return exerciseProcessCreation(new ProcessCreation());
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, CreationAgreement, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<CreationAgreement> get() {
      return jsonDecoder();
    }
  }
}
