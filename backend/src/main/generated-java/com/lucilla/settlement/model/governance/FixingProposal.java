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

public final class FixingProposal extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Governance", "FixingProposal");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Governance", "FixingProposal");

  public static final String PACKAGE_ID = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public static final Choice<FixingProposal, Confirm, ContractId> CHOICE_Confirm = 
      Choice.create("Confirm", value$ -> value$.toValue(), value$ -> Confirm.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<FixingProposal, FinalizeFixing, NavFixing.ContractId> CHOICE_FinalizeFixing = 
      Choice.create("FinalizeFixing", value$ -> value$.toValue(), value$ ->
        FinalizeFixing.valueDecoder().decode(value$), value$ ->
        new NavFixing.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<FixingProposal, WithdrawFixing, Unit> CHOICE_WithdrawFixing = 
      Choice.create("WithdrawFixing", value$ -> value$.toValue(), value$ ->
        WithdrawFixing.valueDecoder().decode(value$), value$ -> PrimitiveValueDecoders.fromUnit
        .decode(value$));

  public static final Choice<FixingProposal, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, FixingProposal> COMPANION = 
      new ContractCompanion.WithoutKey<>("com.lucilla.settlement.model.governance.FixingProposal",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> FixingProposal.templateValueDecoder().decode(v), FixingProposal::fromJson,
        Contract::new, List.of(CHOICE_Confirm, CHOICE_FinalizeFixing, CHOICE_WithdrawFixing,
        CHOICE_Archive));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String admin;

  public final List<String> members;

  public final Long threshold;

  public final String auditor;

  public final String proposer;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal price;

  public final String rationale;

  public final List<String> approvers;

  public FixingProposal(String admin, List<String> members, Long threshold, String auditor,
      String proposer, String instrumentId, String cashInstrument, String session, BigDecimal price,
      String rationale, List<String> approvers) {
    this.admin = admin;
    this.members = members;
    this.threshold = threshold;
    this.auditor = auditor;
    this.proposer = proposer;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.price = price;
    this.rationale = rationale;
    this.approvers = approvers;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(FixingProposal.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseConfirm} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseConfirm(Confirm arg) {
    return createAnd().exerciseConfirm(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseConfirm} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseConfirm(String member) {
    return createAndExerciseConfirm(new Confirm(member));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseFinalizeFixing} instead
   */
  @Deprecated
  public Update<Exercised<NavFixing.ContractId>> createAndExerciseFinalizeFixing(
      FinalizeFixing arg) {
    return createAnd().exerciseFinalizeFixing(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseFinalizeFixing} instead
   */
  @Deprecated
  public Update<Exercised<NavFixing.ContractId>> createAndExerciseFinalizeFixing(
      List<String> publishTo) {
    return createAndExerciseFinalizeFixing(new FinalizeFixing(publishTo));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseWithdrawFixing} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseWithdrawFixing(WithdrawFixing arg) {
    return createAnd().exerciseWithdrawFixing(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseWithdrawFixing} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseWithdrawFixing() {
    return createAndExerciseWithdrawFixing(new WithdrawFixing());
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
      Long threshold, String auditor, String proposer, String instrumentId, String cashInstrument,
      String session, BigDecimal price, String rationale, List<String> approvers) {
    return new FixingProposal(admin, members, threshold, auditor, proposer, instrumentId,
        cashInstrument, session, price, rationale, approvers).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, FixingProposal> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static FixingProposal fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<FixingProposal> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(11);
    fields.add(new DamlRecord.Field("admin", new Party(this.admin)));
    fields.add(new DamlRecord.Field("members", this.members.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("threshold", new Int64(this.threshold)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("proposer", new Party(this.proposer)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("price", new Numeric(this.price)));
    fields.add(new DamlRecord.Field("rationale", new Text(this.rationale)));
    fields.add(new DamlRecord.Field("approvers", this.approvers.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<FixingProposal> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(11,0, recordValue$);
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      List<String> members = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(1).getValue());
      Long threshold = PrimitiveValueDecoders.fromInt64.decode(fields$.get(2).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(3).getValue());
      String proposer = PrimitiveValueDecoders.fromParty.decode(fields$.get(4).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(6).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(7).getValue());
      BigDecimal price = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(8).getValue());
      String rationale = PrimitiveValueDecoders.fromText.decode(fields$.get(9).getValue());
      List<String> approvers = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(10).getValue());
      return new FixingProposal(admin, members, threshold, auditor, proposer, instrumentId,
          cashInstrument, session, price, rationale, approvers);
    } ;
  }

  public static JsonLfDecoder<FixingProposal> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("admin", "members", "threshold", "auditor", "proposer", "instrumentId", "cashInstrument", "session", "price", "rationale", "approvers"), name -> {
          switch (name) {
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "members": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "threshold": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "proposer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "price": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "rationale": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "approvers": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            default: return null;
          }
        }
        , (Object[] args) -> new FixingProposal(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10])));
  }

  public static FixingProposal fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)),
        JsonLfEncoders.Field.of("members", apply(JsonLfEncoders.list(JsonLfEncoders::party), members)),
        JsonLfEncoders.Field.of("threshold", apply(JsonLfEncoders::int64, threshold)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("proposer", apply(JsonLfEncoders::party, proposer)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("price", apply(JsonLfEncoders::numeric, price)),
        JsonLfEncoders.Field.of("rationale", apply(JsonLfEncoders::text, rationale)),
        JsonLfEncoders.Field.of("approvers", apply(JsonLfEncoders.list(JsonLfEncoders::party), approvers)));
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
    if (!(object instanceof FixingProposal)) {
      return false;
    }
    FixingProposal other = (FixingProposal) object;
    return Objects.equals(this.admin, other.admin) && Objects.equals(this.members, other.members) &&
        Objects.equals(this.threshold, other.threshold) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.proposer, other.proposer) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) && Objects.equals(this.price, other.price) &&
        Objects.equals(this.rationale, other.rationale) &&
        Objects.equals(this.approvers, other.approvers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.admin, this.members, this.threshold, this.auditor, this.proposer,
        this.instrumentId, this.cashInstrument, this.session, this.price, this.rationale,
        this.approvers);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.FixingProposal(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.admin, this.members, this.threshold, this.auditor, this.proposer, this.instrumentId,
        this.cashInstrument, this.session, this.price, this.rationale, this.approvers);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<FixingProposal> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, FixingProposal, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<FixingProposal> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, FixingProposal> {
    public Contract(ContractId id, FixingProposal data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, FixingProposal> getCompanion() {
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
    default Update<Exercised<ContractId>> exerciseConfirm(Confirm arg) {
      return makeExerciseCmd(CHOICE_Confirm, arg);
    }

    default Update<Exercised<ContractId>> exerciseConfirm(String member) {
      return exerciseConfirm(new Confirm(member));
    }

    default Update<Exercised<NavFixing.ContractId>> exerciseFinalizeFixing(FinalizeFixing arg) {
      return makeExerciseCmd(CHOICE_FinalizeFixing, arg);
    }

    default Update<Exercised<NavFixing.ContractId>> exerciseFinalizeFixing(List<String> publishTo) {
      return exerciseFinalizeFixing(new FinalizeFixing(publishTo));
    }

    default Update<Exercised<Unit>> exerciseWithdrawFixing(WithdrawFixing arg) {
      return makeExerciseCmd(CHOICE_WithdrawFixing, arg);
    }

    default Update<Exercised<Unit>> exerciseWithdrawFixing() {
      return exerciseWithdrawFixing(new WithdrawFixing());
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, FixingProposal, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<FixingProposal> get() {
      return jsonDecoder();
    }
  }
}
