package com.lucilla.settlement.model.governance;

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
import com.daml.ledger.javaapi.data.PackageVersion;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Template;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Timestamp;
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
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CessationNotice extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "Governance", "CessationNotice");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b", "Governance", "CessationNotice");

  public static final String PACKAGE_ID = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<CessationNotice, ExtendCessation, ContractId> CHOICE_ExtendCessation = 
      Choice.create("ExtendCessation", value$ -> value$.toValue(), value$ ->
        ExtendCessation.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new ExtendCessation.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        ExtendCessation::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<CessationNotice, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<CessationNotice, WithdrawCessation, Unit> CHOICE_WithdrawCessation = 
      Choice.create("WithdrawCessation", value$ -> value$.toValue(), value$ ->
        WithdrawCessation.valueDecoder().decode(value$), value$ -> PrimitiveValueDecoders.fromUnit
        .decode(value$), new WithdrawCessation.JsonDecoder$().get(), JsonLfDecoders.unit,
        WithdrawCessation::jsonEncoder, JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, CessationNotice> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(CessationNotice.PACKAGE_ID, CessationNotice.PACKAGE_NAME, CessationNotice.PACKAGE_VERSION),
        "com.lucilla.settlement.model.governance.CessationNotice", TEMPLATE_ID, ContractId::new,
        v -> CessationNotice.templateValueDecoder().decode(v), CessationNotice::fromJson,
        Contract::new, List.of(CHOICE_ExtendCessation, CHOICE_Archive, CHOICE_WithdrawCessation));

  public final String admin;

  public final String auditor;

  public final String instrumentId;

  public final String session;

  public final Instant publishedAt;

  public final Instant finalStrike;

  public final Optional<String> successor;

  public final String reason;

  public final List<String> notifyTo;

  public CessationNotice(String admin, String auditor, String instrumentId, String session,
      Instant publishedAt, Instant finalStrike, Optional<String> successor, String reason,
      List<String> notifyTo) {
    this.admin = admin;
    this.auditor = auditor;
    this.instrumentId = instrumentId;
    this.session = session;
    this.publishedAt = publishedAt;
    this.finalStrike = finalStrike;
    this.successor = successor;
    this.reason = reason;
    this.notifyTo = notifyTo;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(CessationNotice.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseExtendCessation} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseExtendCessation(ExtendCessation arg) {
    return createAnd().exerciseExtendCessation(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseExtendCessation} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseExtendCessation(Instant newFinalStrike,
      String note) {
    return createAndExerciseExtendCessation(new ExtendCessation(newFinalStrike, note));
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseWithdrawCessation} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseWithdrawCessation(WithdrawCessation arg) {
    return createAnd().exerciseWithdrawCessation(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseWithdrawCessation} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseWithdrawCessation(String note) {
    return createAndExerciseWithdrawCessation(new WithdrawCessation(note));
  }

  public static Update<Created<ContractId>> create(String admin, String auditor,
      String instrumentId, String session, Instant publishedAt, Instant finalStrike,
      Optional<String> successor, String reason, List<String> notifyTo) {
    return new CessationNotice(admin, auditor, instrumentId, session, publishedAt, finalStrike,
        successor, reason, notifyTo).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, CessationNotice> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<CessationNotice> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(9);
    fields.add(new DamlRecord.Field("admin", new Party(this.admin)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("publishedAt", Timestamp.fromInstant(this.publishedAt)));
    fields.add(new DamlRecord.Field("finalStrike", Timestamp.fromInstant(this.finalStrike)));
    fields.add(new DamlRecord.Field("successor", DamlOptional.of(this.successor.map(v$0 -> new Text(v$0)))));
    fields.add(new DamlRecord.Field("reason", new Text(this.reason)));
    fields.add(new DamlRecord.Field("notifyTo", this.notifyTo.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<CessationNotice> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(9,0, recordValue$);
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      Instant publishedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(4).getValue());
      Instant finalStrike = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(5).getValue());
      Optional<String> successor = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromText).decode(fields$.get(6).getValue());
      String reason = PrimitiveValueDecoders.fromText.decode(fields$.get(7).getValue());
      List<String> notifyTo = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(8).getValue());
      return new CessationNotice(admin, auditor, instrumentId, session, publishedAt, finalStrike,
          successor, reason, notifyTo);
    } ;
  }

  public static JsonLfDecoder<CessationNotice> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("admin", "auditor", "instrumentId", "session", "publishedAt", "finalStrike", "successor", "reason", "notifyTo"), name -> {
          switch (name) {
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "publishedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "finalStrike": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "successor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text), java.util.Optional.empty());
            case "reason": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "notifyTo": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            default: return null;
          }
        }
        , (Object[] args) -> new CessationNotice(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8])));
  }

  public static CessationNotice fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("publishedAt", apply(JsonLfEncoders::timestamp, publishedAt)),
        JsonLfEncoders.Field.of("finalStrike", apply(JsonLfEncoders::timestamp, finalStrike)),
        JsonLfEncoders.Field.of("successor", apply(JsonLfEncoders.optional(JsonLfEncoders::text), successor)),
        JsonLfEncoders.Field.of("reason", apply(JsonLfEncoders::text, reason)),
        JsonLfEncoders.Field.of("notifyTo", apply(JsonLfEncoders.list(JsonLfEncoders::party), notifyTo)));
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
    if (!(object instanceof CessationNotice)) {
      return false;
    }
    CessationNotice other = (CessationNotice) object;
    return Objects.equals(this.admin, other.admin) && Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.session, other.session) &&
        Objects.equals(this.publishedAt, other.publishedAt) &&
        Objects.equals(this.finalStrike, other.finalStrike) &&
        Objects.equals(this.successor, other.successor) &&
        Objects.equals(this.reason, other.reason) && Objects.equals(this.notifyTo, other.notifyTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.admin, this.auditor, this.instrumentId, this.session, this.publishedAt,
        this.finalStrike, this.successor, this.reason, this.notifyTo);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.CessationNotice(%s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.admin, this.auditor, this.instrumentId, this.session, this.publishedAt,
        this.finalStrike, this.successor, this.reason, this.notifyTo);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<CessationNotice> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, CessationNotice, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<CessationNotice> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, CessationNotice> {
    public Contract(ContractId id, CessationNotice data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, CessationNotice> getCompanion() {
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
    default Update<Exercised<ContractId>> exerciseExtendCessation(ExtendCessation arg) {
      return makeExerciseCmd(CHOICE_ExtendCessation, arg);
    }

    default Update<Exercised<ContractId>> exerciseExtendCessation(Instant newFinalStrike,
        String note) {
      return exerciseExtendCessation(new ExtendCessation(newFinalStrike, note));
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<Unit>> exerciseWithdrawCessation(WithdrawCessation arg) {
      return makeExerciseCmd(CHOICE_WithdrawCessation, arg);
    }

    default Update<Exercised<Unit>> exerciseWithdrawCessation(String note) {
      return exerciseWithdrawCessation(new WithdrawCessation(note));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, CessationNotice, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<CessationNotice> get() {
      return jsonDecoder();
    }
  }
}
