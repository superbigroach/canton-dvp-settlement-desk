package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlOptional;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.Date;
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
import com.lucilla.settlement.model.da.internal.template.Archive;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class FixingSeries extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "Governance", "FixingSeries");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("9f697598fdc5fee1bf367e5acd6ca4eb84c7368c987ce1093f58227384f3d0f8", "Governance", "FixingSeries");

  public static final String PACKAGE_ID = "9f697598fdc5fee1bf367e5acd6ca4eb84c7368c987ce1093f58227384f3d0f8";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {3, 0, 0});

  public static final Choice<FixingSeries, Advance, ContractId> CHOICE_Advance = 
      Choice.create("Advance", value$ -> value$.toValue(), value$ -> Advance.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new Advance.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        Advance::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<FixingSeries, DiscloseSeries, ContractId> CHOICE_DiscloseSeries = 
      Choice.create("DiscloseSeries", value$ -> value$.toValue(), value$ ->
        DiscloseSeries.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new DiscloseSeries.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        DiscloseSeries::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<FixingSeries, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, FixingSeries> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(FixingSeries.PACKAGE_ID, FixingSeries.PACKAGE_NAME, FixingSeries.PACKAGE_VERSION),
        "com.lucilla.settlement.model.governance.FixingSeries", TEMPLATE_ID, ContractId::new,
        v -> FixingSeries.templateValueDecoder().decode(v), FixingSeries::fromJson, Contract::new,
        List.of(CHOICE_Advance, CHOICE_DiscloseSeries, CHOICE_Archive));

  public final String admin;

  public final String auditor;

  public final String instrumentId;

  public final String session;

  public final List<String> observers;

  public final Optional<LocalDate> lastAsOf;

  public final Long struck;

  public FixingSeries(String admin, String auditor, String instrumentId, String session,
      List<String> observers, Optional<LocalDate> lastAsOf, Long struck) {
    this.admin = admin;
    this.auditor = auditor;
    this.instrumentId = instrumentId;
    this.session = session;
    this.observers = observers;
    this.lastAsOf = lastAsOf;
    this.struck = struck;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(FixingSeries.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAdvance} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseAdvance(Advance arg) {
    return createAnd().exerciseAdvance(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAdvance} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseAdvance(LocalDate asOfDate) {
    return createAndExerciseAdvance(new Advance(asOfDate));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseDiscloseSeries} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseDiscloseSeries(DiscloseSeries arg) {
    return createAnd().exerciseDiscloseSeries(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseDiscloseSeries} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseDiscloseSeries(String party) {
    return createAndExerciseDiscloseSeries(new DiscloseSeries(party));
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

  public static Update<Created<ContractId>> create(String admin, String auditor,
      String instrumentId, String session, List<String> observers, Optional<LocalDate> lastAsOf,
      Long struck) {
    return new FixingSeries(admin, auditor, instrumentId, session, observers, lastAsOf,
        struck).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, FixingSeries> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<FixingSeries> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(7);
    fields.add(new DamlRecord.Field("admin", new Party(this.admin)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("observers", this.observers.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("lastAsOf", DamlOptional.of(this.lastAsOf.map(v$0 -> new Date((int) v$0.toEpochDay())))));
    fields.add(new DamlRecord.Field("struck", new Int64(this.struck)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<FixingSeries> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(7,0, recordValue$);
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      List<String> observers = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(4).getValue());
      Optional<LocalDate> lastAsOf = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromDate).decode(fields$.get(5).getValue());
      Long struck = PrimitiveValueDecoders.fromInt64.decode(fields$.get(6).getValue());
      return new FixingSeries(admin, auditor, instrumentId, session, observers, lastAsOf, struck);
    } ;
  }

  public static JsonLfDecoder<FixingSeries> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("admin", "auditor", "instrumentId", "session", "observers", "lastAsOf", "struck"), name -> {
          switch (name) {
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "observers": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "lastAsOf": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.date), java.util.Optional.empty());
            case "struck": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            default: return null;
          }
        }
        , (Object[] args) -> new FixingSeries(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6])));
  }

  public static FixingSeries fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("observers", apply(JsonLfEncoders.list(JsonLfEncoders::party), observers)),
        JsonLfEncoders.Field.of("lastAsOf", apply(JsonLfEncoders.optional(JsonLfEncoders::date), lastAsOf)),
        JsonLfEncoders.Field.of("struck", apply(JsonLfEncoders::int64, struck)));
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
    if (!(object instanceof FixingSeries)) {
      return false;
    }
    FixingSeries other = (FixingSeries) object;
    return Objects.equals(this.admin, other.admin) && Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.session, other.session) &&
        Objects.equals(this.observers, other.observers) &&
        Objects.equals(this.lastAsOf, other.lastAsOf) && Objects.equals(this.struck, other.struck);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.admin, this.auditor, this.instrumentId, this.session, this.observers,
        this.lastAsOf, this.struck);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.FixingSeries(%s, %s, %s, %s, %s, %s, %s)",
        this.admin, this.auditor, this.instrumentId, this.session, this.observers, this.lastAsOf,
        this.struck);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<FixingSeries> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, FixingSeries, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<FixingSeries> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, FixingSeries> {
    public Contract(ContractId id, FixingSeries data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, FixingSeries> getCompanion() {
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
    default Update<Exercised<ContractId>> exerciseAdvance(Advance arg) {
      return makeExerciseCmd(CHOICE_Advance, arg);
    }

    default Update<Exercised<ContractId>> exerciseAdvance(LocalDate asOfDate) {
      return exerciseAdvance(new Advance(asOfDate));
    }

    default Update<Exercised<ContractId>> exerciseDiscloseSeries(DiscloseSeries arg) {
      return makeExerciseCmd(CHOICE_DiscloseSeries, arg);
    }

    default Update<Exercised<ContractId>> exerciseDiscloseSeries(String party) {
      return exerciseDiscloseSeries(new DiscloseSeries(party));
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, FixingSeries, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<FixingSeries> get() {
      return jsonDecoder();
    }
  }
}
