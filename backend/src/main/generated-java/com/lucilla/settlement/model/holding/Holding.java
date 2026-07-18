package com.lucilla.settlement.model.holding;

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
import com.lucilla.settlement.model.da.types.Tuple2;
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

public final class Holding extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Holding", "Holding");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Holding", "Holding");

  public static final String PACKAGE_ID = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public static final Choice<Holding, Redeem, Unit> CHOICE_Redeem = 
      Choice.create("Redeem", value$ -> value$.toValue(), value$ -> Redeem.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<Holding, Transfer, ContractId> CHOICE_Transfer = 
      Choice.create("Transfer", value$ -> value$.toValue(), value$ -> Transfer.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<Holding, Disclose, ContractId> CHOICE_Disclose = 
      Choice.create("Disclose", value$ -> value$.toValue(), value$ -> Disclose.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<Holding, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<Holding, Split, Tuple2<ContractId, ContractId>> CHOICE_Split = 
      Choice.create("Split", value$ -> value$.toValue(), value$ -> Split.valueDecoder()
        .decode(value$), value$ -> Tuple2.<com.lucilla.settlement.model.holding.Holding.ContractId,
        com.lucilla.settlement.model.holding.Holding.ContractId>valueDecoder(v$0 ->
          new ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        v$1 ->
          new ContractId(v$1.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
        .decode(value$));

  public static final Choice<Holding, Merge, ContractId> CHOICE_Merge = 
      Choice.create("Merge", value$ -> value$.toValue(), value$ -> Merge.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, Holding> COMPANION = 
      new ContractCompanion.WithoutKey<>("com.lucilla.settlement.model.holding.Holding",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> Holding.templateValueDecoder().decode(v), Holding::fromJson, Contract::new,
        List.of(CHOICE_Split, CHOICE_Redeem, CHOICE_Disclose, CHOICE_Merge, CHOICE_Archive,
        CHOICE_Transfer));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String issuer;

  public final String instrumentId;

  public final String owner;

  public final BigDecimal amount;

  public final List<String> disclosedTo;

  public Holding(String issuer, String instrumentId, String owner, BigDecimal amount,
      List<String> disclosedTo) {
    this.issuer = issuer;
    this.instrumentId = instrumentId;
    this.owner = owner;
    this.amount = amount;
    this.disclosedTo = disclosedTo;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(Holding.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRedeem} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseRedeem(Redeem arg) {
    return createAnd().exerciseRedeem(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRedeem} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseRedeem() {
    return createAndExerciseRedeem(new Redeem());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTransfer} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseTransfer(Transfer arg) {
    return createAnd().exerciseTransfer(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTransfer} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseTransfer(String newOwner) {
    return createAndExerciseTransfer(new Transfer(newOwner));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseDisclose} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseDisclose(Disclose arg) {
    return createAnd().exerciseDisclose(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseDisclose} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseDisclose(String newObserver) {
    return createAndExerciseDisclose(new Disclose(newObserver));
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSplit} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, ContractId>>> createAndExerciseSplit(Split arg) {
    return createAnd().exerciseSplit(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSplit} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, ContractId>>> createAndExerciseSplit(
      BigDecimal splitAmount) {
    return createAndExerciseSplit(new Split(splitAmount));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseMerge} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseMerge(Merge arg) {
    return createAnd().exerciseMerge(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseMerge} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseMerge(ContractId otherCid) {
    return createAndExerciseMerge(new Merge(otherCid));
  }

  public static Update<Created<ContractId>> create(String issuer, String instrumentId, String owner,
      BigDecimal amount, List<String> disclosedTo) {
    return new Holding(issuer, instrumentId, owner, amount, disclosedTo).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, Holding> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static Holding fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<Holding> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(5);
    fields.add(new DamlRecord.Field("issuer", new Party(this.issuer)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("owner", new Party(this.owner)));
    fields.add(new DamlRecord.Field("amount", new Numeric(this.amount)));
    fields.add(new DamlRecord.Field("disclosedTo", this.disclosedTo.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<Holding> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0, recordValue$);
      String issuer = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      String owner = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      BigDecimal amount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(3).getValue());
      List<String> disclosedTo = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(4).getValue());
      return new Holding(issuer, instrumentId, owner, amount, disclosedTo);
    } ;
  }

  public static JsonLfDecoder<Holding> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("issuer", "instrumentId", "owner", "amount", "disclosedTo"), name -> {
          switch (name) {
            case "issuer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "owner": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "amount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "disclosedTo": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            default: return null;
          }
        }
        , (Object[] args) -> new Holding(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static Holding fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("issuer", apply(JsonLfEncoders::party, issuer)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("owner", apply(JsonLfEncoders::party, owner)),
        JsonLfEncoders.Field.of("amount", apply(JsonLfEncoders::numeric, amount)),
        JsonLfEncoders.Field.of("disclosedTo", apply(JsonLfEncoders.list(JsonLfEncoders::party), disclosedTo)));
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
    if (!(object instanceof Holding)) {
      return false;
    }
    Holding other = (Holding) object;
    return Objects.equals(this.issuer, other.issuer) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.owner, other.owner) && Objects.equals(this.amount, other.amount) &&
        Objects.equals(this.disclosedTo, other.disclosedTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.issuer, this.instrumentId, this.owner, this.amount, this.disclosedTo);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.holding.Holding(%s, %s, %s, %s, %s)",
        this.issuer, this.instrumentId, this.owner, this.amount, this.disclosedTo);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<Holding> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, Holding, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<Holding> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, Holding> {
    public Contract(ContractId id, Holding data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, Holding> getCompanion() {
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
    default Update<Exercised<Unit>> exerciseRedeem(Redeem arg) {
      return makeExerciseCmd(CHOICE_Redeem, arg);
    }

    default Update<Exercised<Unit>> exerciseRedeem() {
      return exerciseRedeem(new Redeem());
    }

    default Update<Exercised<ContractId>> exerciseTransfer(Transfer arg) {
      return makeExerciseCmd(CHOICE_Transfer, arg);
    }

    default Update<Exercised<ContractId>> exerciseTransfer(String newOwner) {
      return exerciseTransfer(new Transfer(newOwner));
    }

    default Update<Exercised<ContractId>> exerciseDisclose(Disclose arg) {
      return makeExerciseCmd(CHOICE_Disclose, arg);
    }

    default Update<Exercised<ContractId>> exerciseDisclose(String newObserver) {
      return exerciseDisclose(new Disclose(newObserver));
    }

    default Update<Exercised<Unit>> exerciseArchive(
        com.lucilla.settlement.model.da.internal.template.Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new com.lucilla.settlement.model.da.internal.template.Archive());
    }

    default Update<Exercised<Tuple2<ContractId, ContractId>>> exerciseSplit(Split arg) {
      return makeExerciseCmd(CHOICE_Split, arg);
    }

    default Update<Exercised<Tuple2<ContractId, ContractId>>> exerciseSplit(
        BigDecimal splitAmount) {
      return exerciseSplit(new Split(splitAmount));
    }

    default Update<Exercised<ContractId>> exerciseMerge(Merge arg) {
      return makeExerciseCmd(CHOICE_Merge, arg);
    }

    default Update<Exercised<ContractId>> exerciseMerge(ContractId otherCid) {
      return exerciseMerge(new Merge(otherCid));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, Holding, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Holding> get() {
      return jsonDecoder();
    }
  }
}
