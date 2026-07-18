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
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class NavFixing extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Governance", "NavFixing");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "Governance", "NavFixing");

  public static final String PACKAGE_ID = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public static final Choice<NavFixing, PublishTo, ContractId> CHOICE_PublishTo = 
      Choice.create("PublishTo", value$ -> value$.toValue(), value$ -> PublishTo.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<NavFixing, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, NavFixing> COMPANION = 
      new ContractCompanion.WithoutKey<>("com.lucilla.settlement.model.governance.NavFixing",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> NavFixing.templateValueDecoder().decode(v), NavFixing::fromJson, Contract::new,
        List.of(CHOICE_PublishTo, CHOICE_Archive));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final List<String> attestors;

  public final String admin;

  public final String auditor;

  public final Long threshold;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal price;

  public final String rationale;

  public final List<String> publishedTo;

  public final Instant finalizedAt;

  public NavFixing(List<String> attestors, String admin, String auditor, Long threshold,
      String instrumentId, String cashInstrument, String session, BigDecimal price,
      String rationale, List<String> publishedTo, Instant finalizedAt) {
    this.attestors = attestors;
    this.admin = admin;
    this.auditor = auditor;
    this.threshold = threshold;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.price = price;
    this.rationale = rationale;
    this.publishedTo = publishedTo;
    this.finalizedAt = finalizedAt;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(NavFixing.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePublishTo} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExercisePublishTo(PublishTo arg) {
    return createAnd().exercisePublishTo(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePublishTo} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExercisePublishTo(String attestor, String venue) {
    return createAndExercisePublishTo(new PublishTo(attestor, venue));
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

  public static Update<Created<ContractId>> create(List<String> attestors, String admin,
      String auditor, Long threshold, String instrumentId, String cashInstrument, String session,
      BigDecimal price, String rationale, List<String> publishedTo, Instant finalizedAt) {
    return new NavFixing(attestors, admin, auditor, threshold, instrumentId, cashInstrument,
        session, price, rationale, publishedTo, finalizedAt).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, NavFixing> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static NavFixing fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<NavFixing> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(11);
    fields.add(new DamlRecord.Field("attestors", this.attestors.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("admin", new Party(this.admin)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("threshold", new Int64(this.threshold)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("price", new Numeric(this.price)));
    fields.add(new DamlRecord.Field("rationale", new Text(this.rationale)));
    fields.add(new DamlRecord.Field("publishedTo", this.publishedTo.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("finalizedAt", Timestamp.fromInstant(this.finalizedAt)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<NavFixing> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(11,0, recordValue$);
      List<String> attestors = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(0).getValue());
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      Long threshold = PrimitiveValueDecoders.fromInt64.decode(fields$.get(3).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(6).getValue());
      BigDecimal price = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(7).getValue());
      String rationale = PrimitiveValueDecoders.fromText.decode(fields$.get(8).getValue());
      List<String> publishedTo = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(9).getValue());
      Instant finalizedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(10).getValue());
      return new NavFixing(attestors, admin, auditor, threshold, instrumentId, cashInstrument,
          session, price, rationale, publishedTo, finalizedAt);
    } ;
  }

  public static JsonLfDecoder<NavFixing> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("attestors", "admin", "auditor", "threshold", "instrumentId", "cashInstrument", "session", "price", "rationale", "publishedTo", "finalizedAt"), name -> {
          switch (name) {
            case "attestors": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "threshold": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "price": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "rationale": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "publishedTo": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "finalizedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new NavFixing(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10])));
  }

  public static NavFixing fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("attestors", apply(JsonLfEncoders.list(JsonLfEncoders::party), attestors)),
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("threshold", apply(JsonLfEncoders::int64, threshold)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("price", apply(JsonLfEncoders::numeric, price)),
        JsonLfEncoders.Field.of("rationale", apply(JsonLfEncoders::text, rationale)),
        JsonLfEncoders.Field.of("publishedTo", apply(JsonLfEncoders.list(JsonLfEncoders::party), publishedTo)),
        JsonLfEncoders.Field.of("finalizedAt", apply(JsonLfEncoders::timestamp, finalizedAt)));
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
    if (!(object instanceof NavFixing)) {
      return false;
    }
    NavFixing other = (NavFixing) object;
    return Objects.equals(this.attestors, other.attestors) &&
        Objects.equals(this.admin, other.admin) && Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.threshold, other.threshold) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) && Objects.equals(this.price, other.price) &&
        Objects.equals(this.rationale, other.rationale) &&
        Objects.equals(this.publishedTo, other.publishedTo) &&
        Objects.equals(this.finalizedAt, other.finalizedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.attestors, this.admin, this.auditor, this.threshold, this.instrumentId,
        this.cashInstrument, this.session, this.price, this.rationale, this.publishedTo,
        this.finalizedAt);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.NavFixing(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.attestors, this.admin, this.auditor, this.threshold, this.instrumentId,
        this.cashInstrument, this.session, this.price, this.rationale, this.publishedTo,
        this.finalizedAt);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<NavFixing> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, NavFixing, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<NavFixing> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, NavFixing> {
    public Contract(ContractId id, NavFixing data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, NavFixing> getCompanion() {
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
    default Update<Exercised<ContractId>> exercisePublishTo(PublishTo arg) {
      return makeExerciseCmd(CHOICE_PublishTo, arg);
    }

    default Update<Exercised<ContractId>> exercisePublishTo(String attestor, String venue) {
      return exercisePublishTo(new PublishTo(attestor, venue));
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, NavFixing, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<NavFixing> get() {
      return jsonDecoder();
    }
  }
}
