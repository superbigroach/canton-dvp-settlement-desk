package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Int64;
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

public final class OperatorCommittee extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Governance", "OperatorCommittee");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Governance", "OperatorCommittee");

  public static final String PACKAGE_ID = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public static final Choice<OperatorCommittee, ProposeFixing, FixingProposal.ContractId> CHOICE_ProposeFixing = 
      Choice.create("ProposeFixing", value$ -> value$.toValue(), value$ ->
        ProposeFixing.valueDecoder().decode(value$), value$ ->
        new FixingProposal.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<OperatorCommittee, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, OperatorCommittee> COMPANION = 
      new ContractCompanion.WithoutKey<>(
        "com.lucilla.settlement.model.governance.OperatorCommittee", TEMPLATE_ID,
        TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> OperatorCommittee.templateValueDecoder().decode(v), OperatorCommittee::fromJson,
        Contract::new, List.of(CHOICE_ProposeFixing, CHOICE_Archive));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String admin;

  public final List<String> members;

  public final Long threshold;

  public final String auditor;

  public final String label;

  public OperatorCommittee(String admin, List<String> members, Long threshold, String auditor,
      String label) {
    this.admin = admin;
    this.members = members;
    this.threshold = threshold;
    this.auditor = auditor;
    this.label = label;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(OperatorCommittee.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseProposeFixing} instead
   */
  @Deprecated
  public Update<Exercised<FixingProposal.ContractId>> createAndExerciseProposeFixing(
      ProposeFixing arg) {
    return createAnd().exerciseProposeFixing(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseProposeFixing} instead
   */
  @Deprecated
  public Update<Exercised<FixingProposal.ContractId>> createAndExerciseProposeFixing(
      String proposer, String instrumentId, String cashInstrument, String session, BigDecimal price,
      String rationale) {
    return createAndExerciseProposeFixing(new ProposeFixing(proposer, instrumentId, cashInstrument,
        session, price, rationale));
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

  public static Update<Created<ContractId>> create(String admin, List<String> members,
      Long threshold, String auditor, String label) {
    return new OperatorCommittee(admin, members, threshold, auditor, label).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, OperatorCommittee> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static OperatorCommittee fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<OperatorCommittee> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(5);
    fields.add(new DamlRecord.Field("admin", new Party(this.admin)));
    fields.add(new DamlRecord.Field("members", this.members.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("threshold", new Int64(this.threshold)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("label", new Text(this.label)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<OperatorCommittee> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,0, recordValue$);
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      List<String> members = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(1).getValue());
      Long threshold = PrimitiveValueDecoders.fromInt64.decode(fields$.get(2).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(3).getValue());
      String label = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      return new OperatorCommittee(admin, members, threshold, auditor, label);
    } ;
  }

  public static JsonLfDecoder<OperatorCommittee> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("admin", "members", "threshold", "auditor", "label"), name -> {
          switch (name) {
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "members": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "threshold": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "label": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            default: return null;
          }
        }
        , (Object[] args) -> new OperatorCommittee(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static OperatorCommittee fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)),
        JsonLfEncoders.Field.of("members", apply(JsonLfEncoders.list(JsonLfEncoders::party), members)),
        JsonLfEncoders.Field.of("threshold", apply(JsonLfEncoders::int64, threshold)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("label", apply(JsonLfEncoders::text, label)));
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
    if (!(object instanceof OperatorCommittee)) {
      return false;
    }
    OperatorCommittee other = (OperatorCommittee) object;
    return Objects.equals(this.admin, other.admin) && Objects.equals(this.members, other.members) &&
        Objects.equals(this.threshold, other.threshold) &&
        Objects.equals(this.auditor, other.auditor) && Objects.equals(this.label, other.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.admin, this.members, this.threshold, this.auditor, this.label);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.OperatorCommittee(%s, %s, %s, %s, %s)",
        this.admin, this.members, this.threshold, this.auditor, this.label);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<OperatorCommittee> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, OperatorCommittee, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<OperatorCommittee> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, OperatorCommittee> {
    public Contract(ContractId id, OperatorCommittee data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, OperatorCommittee> getCompanion() {
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
    default Update<Exercised<FixingProposal.ContractId>> exerciseProposeFixing(ProposeFixing arg) {
      return makeExerciseCmd(CHOICE_ProposeFixing, arg);
    }

    default Update<Exercised<FixingProposal.ContractId>> exerciseProposeFixing(String proposer,
        String instrumentId, String cashInstrument, String session, BigDecimal price,
        String rationale) {
      return exerciseProposeFixing(new ProposeFixing(proposer, instrumentId, cashInstrument,
          session, price, rationale));
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, OperatorCommittee, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<OperatorCommittee> get() {
      return jsonDecoder();
    }
  }
}
