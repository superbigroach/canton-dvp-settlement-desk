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

public final class BasketDefinition extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Basket", "BasketDefinition");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Basket", "BasketDefinition");

  public static final String PACKAGE_ID = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public static final Choice<BasketDefinition, RequestCreation, CreationOrder.ContractId> CHOICE_RequestCreation = 
      Choice.create("RequestCreation", value$ -> value$.toValue(), value$ ->
        RequestCreation.valueDecoder().decode(value$), value$ ->
        new CreationOrder.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<BasketDefinition, RequestRedemption, RedemptionOrder.ContractId> CHOICE_RequestRedemption = 
      Choice.create("RequestRedemption", value$ -> value$.toValue(), value$ ->
        RequestRedemption.valueDecoder().decode(value$), value$ ->
        new RedemptionOrder.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<BasketDefinition, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, BasketDefinition> COMPANION = 
      new ContractCompanion.WithoutKey<>("com.lucilla.settlement.model.basket.BasketDefinition",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> BasketDefinition.templateValueDecoder().decode(v), BasketDefinition::fromJson,
        Contract::new, List.of(CHOICE_RequestCreation, CHOICE_RequestRedemption, CHOICE_Archive));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String administrator;

  public final String auditor;

  public final String basketId;

  public final String description;

  public final String cashInstrument;

  public final List<Component> components;

  public final List<String> participants;

  public BasketDefinition(String administrator, String auditor, String basketId, String description,
      String cashInstrument, List<Component> components, List<String> participants) {
    this.administrator = administrator;
    this.auditor = auditor;
    this.basketId = basketId;
    this.description = description;
    this.cashInstrument = cashInstrument;
    this.components = components;
    this.participants = participants;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(BasketDefinition.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRequestCreation} instead
   */
  @Deprecated
  public Update<Exercised<CreationOrder.ContractId>> createAndExerciseRequestCreation(
      RequestCreation arg) {
    return createAnd().exerciseRequestCreation(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRequestCreation} instead
   */
  @Deprecated
  public Update<Exercised<CreationOrder.ContractId>> createAndExerciseRequestCreation(String ap,
      BigDecimal shares, List<Holding.ContractId> componentHoldingCids) {
    return createAndExerciseRequestCreation(new RequestCreation(ap, shares, componentHoldingCids));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRequestRedemption} instead
   */
  @Deprecated
  public Update<Exercised<RedemptionOrder.ContractId>> createAndExerciseRequestRedemption(
      RequestRedemption arg) {
    return createAnd().exerciseRequestRedemption(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRequestRedemption} instead
   */
  @Deprecated
  public Update<Exercised<RedemptionOrder.ContractId>> createAndExerciseRequestRedemption(String ap,
      BigDecimal shares, Holding.ContractId basketHoldingCid) {
    return createAndExerciseRequestRedemption(new RequestRedemption(ap, shares, basketHoldingCid));
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

  public static Update<Created<ContractId>> create(String administrator, String auditor,
      String basketId, String description, String cashInstrument, List<Component> components,
      List<String> participants) {
    return new BasketDefinition(administrator, auditor, basketId, description, cashInstrument,
        components, participants).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, BasketDefinition> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static BasketDefinition fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<BasketDefinition> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(7);
    fields.add(new DamlRecord.Field("administrator", new Party(this.administrator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("basketId", new Text(this.basketId)));
    fields.add(new DamlRecord.Field("description", new Text(this.description)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("components", this.components.stream().collect(DamlCollectors.toDamlList(v$0 -> v$0.toValue()))));
    fields.add(new DamlRecord.Field("participants", this.participants.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<BasketDefinition> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(7,0, recordValue$);
      String administrator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String basketId = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String description = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      List<Component> components = PrimitiveValueDecoders.fromList(Component.valueDecoder())
          .decode(fields$.get(5).getValue());
      List<String> participants = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(6).getValue());
      return new BasketDefinition(administrator, auditor, basketId, description, cashInstrument,
          components, participants);
    } ;
  }

  public static JsonLfDecoder<BasketDefinition> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("administrator", "auditor", "basketId", "description", "cashInstrument", "components", "participants"), name -> {
          switch (name) {
            case "administrator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "basketId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "description": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "components": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(new com.lucilla.settlement.model.basket.Component.JsonDecoder$().get()));
            case "participants": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            default: return null;
          }
        }
        , (Object[] args) -> new BasketDefinition(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6])));
  }

  public static BasketDefinition fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("administrator", apply(JsonLfEncoders::party, administrator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("basketId", apply(JsonLfEncoders::text, basketId)),
        JsonLfEncoders.Field.of("description", apply(JsonLfEncoders::text, description)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("components", apply(JsonLfEncoders.list(Component::jsonEncoder), components)),
        JsonLfEncoders.Field.of("participants", apply(JsonLfEncoders.list(JsonLfEncoders::party), participants)));
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
    if (!(object instanceof BasketDefinition)) {
      return false;
    }
    BasketDefinition other = (BasketDefinition) object;
    return Objects.equals(this.administrator, other.administrator) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.basketId, other.basketId) &&
        Objects.equals(this.description, other.description) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.components, other.components) &&
        Objects.equals(this.participants, other.participants);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.administrator, this.auditor, this.basketId, this.description,
        this.cashInstrument, this.components, this.participants);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.basket.BasketDefinition(%s, %s, %s, %s, %s, %s, %s)",
        this.administrator, this.auditor, this.basketId, this.description, this.cashInstrument,
        this.components, this.participants);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<BasketDefinition> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, BasketDefinition, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<BasketDefinition> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, BasketDefinition> {
    public Contract(ContractId id, BasketDefinition data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, BasketDefinition> getCompanion() {
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
    default Update<Exercised<CreationOrder.ContractId>> exerciseRequestCreation(
        RequestCreation arg) {
      return makeExerciseCmd(CHOICE_RequestCreation, arg);
    }

    default Update<Exercised<CreationOrder.ContractId>> exerciseRequestCreation(String ap,
        BigDecimal shares, List<Holding.ContractId> componentHoldingCids) {
      return exerciseRequestCreation(new RequestCreation(ap, shares, componentHoldingCids));
    }

    default Update<Exercised<RedemptionOrder.ContractId>> exerciseRequestRedemption(
        RequestRedemption arg) {
      return makeExerciseCmd(CHOICE_RequestRedemption, arg);
    }

    default Update<Exercised<RedemptionOrder.ContractId>> exerciseRequestRedemption(String ap,
        BigDecimal shares, Holding.ContractId basketHoldingCid) {
      return exerciseRequestRedemption(new RequestRedemption(ap, shares, basketHoldingCid));
    }

    default Update<Exercised<Unit>> exerciseArchive(
        com.lucilla.settlement.model.da.internal.template.Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new com.lucilla.settlement.model.da.internal.template.Archive());
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, BasketDefinition, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<BasketDefinition> get() {
      return jsonDecoder();
    }
  }
}
