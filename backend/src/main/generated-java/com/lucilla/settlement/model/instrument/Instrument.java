package com.lucilla.settlement.model.instrument;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlOptional;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseByKeyCommand;
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
import com.daml.ledger.javaapi.data.codegen.ContractWithKey;
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
import com.daml.ledger.javaapi.data.codegen.json.JsonLfWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
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

public final class Instrument extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("686f466cc26deeb74b73fb7f5ee448959dec397b966196641a6e0925ac652c79", "Instrument", "Instrument");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("686f466cc26deeb74b73fb7f5ee448959dec397b966196641a6e0925ac652c79", "Instrument", "Instrument");

  public static final String PACKAGE_ID = "686f466cc26deeb74b73fb7f5ee448959dec397b966196641a6e0925ac652c79";

  public static final Choice<Instrument, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<Instrument, SetReferencePrice, ContractId> CHOICE_SetReferencePrice = 
      Choice.create("SetReferencePrice", value$ -> value$.toValue(), value$ ->
        SetReferencePrice.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final ContractCompanion.WithKey<Contract, ContractId, Instrument, InstrumentKey> COMPANION = 
      new ContractCompanion.WithKey<>("com.lucilla.settlement.model.instrument.Instrument",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> Instrument.templateValueDecoder().decode(v), Instrument::fromJson, Contract::new,
        List.of(CHOICE_Archive, CHOICE_SetReferencePrice), e -> InstrumentKey.valueDecoder()
        .decode(e));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String issuer;

  public final String depository;

  public final String id;

  public final String version;

  public final String kind;

  public final String description;

  public final Optional<BigDecimal> referencePrice;

  public Instrument(String issuer, String depository, String id, String version, String kind,
      String description, Optional<BigDecimal> referencePrice) {
    this.issuer = issuer;
    this.depository = depository;
    this.id = id;
    this.version = version;
    this.kind = kind;
    this.description = description;
    this.referencePrice = referencePrice;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(Instrument.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code byKey(key).exerciseArchive} instead
   */
  @Deprecated
  public static Update<Exercised<Unit>> exerciseByKeyArchive(InstrumentKey key,
      com.lucilla.settlement.model.da.internal.template.Archive arg) {
    return byKey(key).exerciseArchive(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code byKey(key).exerciseArchive()} instead
   */
  @Deprecated
  public static Update<Exercised<Unit>> exerciseByKeyArchive(InstrumentKey key) {
    return byKey(key).exerciseArchive();
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code byKey(key).exerciseSetReferencePrice} instead
   */
  @Deprecated
  public static Update<Exercised<ContractId>> exerciseByKeySetReferencePrice(InstrumentKey key,
      SetReferencePrice arg) {
    return byKey(key).exerciseSetReferencePrice(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code byKey(key).exerciseSetReferencePrice(newPrice)} instead
   */
  @Deprecated
  public static Update<Exercised<ContractId>> exerciseByKeySetReferencePrice(InstrumentKey key,
      BigDecimal newPrice) {
    return byKey(key).exerciseSetReferencePrice(newPrice);
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSetReferencePrice} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseSetReferencePrice(SetReferencePrice arg) {
    return createAnd().exerciseSetReferencePrice(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseSetReferencePrice} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseSetReferencePrice(BigDecimal newPrice) {
    return createAndExerciseSetReferencePrice(new SetReferencePrice(newPrice));
  }

  public static Update<Created<ContractId>> create(String issuer, String depository, String id,
      String version, String kind, String description, Optional<BigDecimal> referencePrice) {
    return new Instrument(issuer, depository, id, version, kind, description,
        referencePrice).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithKey<Contract, ContractId, Instrument, InstrumentKey> getCompanion(
      ) {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static Instrument fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<Instrument> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(7);
    fields.add(new DamlRecord.Field("issuer", new Party(this.issuer)));
    fields.add(new DamlRecord.Field("depository", new Party(this.depository)));
    fields.add(new DamlRecord.Field("id", new Text(this.id)));
    fields.add(new DamlRecord.Field("version", new Text(this.version)));
    fields.add(new DamlRecord.Field("kind", new Text(this.kind)));
    fields.add(new DamlRecord.Field("description", new Text(this.description)));
    fields.add(new DamlRecord.Field("referencePrice", DamlOptional.of(this.referencePrice.map(v$0 -> new Numeric(v$0)))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<Instrument> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(7,1, recordValue$);
      String issuer = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String depository = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String id = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String version = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String kind = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      String description = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      Optional<BigDecimal> referencePrice = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromNumeric).decode(fields$.get(6).getValue());
      return new Instrument(issuer, depository, id, version, kind, description, referencePrice);
    } ;
  }

  public static JsonLfDecoder<Instrument> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("issuer", "depository", "id", "version", "kind", "description", "referencePrice"), name -> {
          switch (name) {
            case "issuer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "depository": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "id": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "version": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "kind": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "description": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "referencePrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10)));
            default: return null;
          }
        }
        , (Object[] args) -> new Instrument(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6])));
  }

  public static Instrument fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("issuer", apply(JsonLfEncoders::party, issuer)),
        JsonLfEncoders.Field.of("depository", apply(JsonLfEncoders::party, depository)),
        JsonLfEncoders.Field.of("id", apply(JsonLfEncoders::text, id)),
        JsonLfEncoders.Field.of("version", apply(JsonLfEncoders::text, version)),
        JsonLfEncoders.Field.of("kind", apply(JsonLfEncoders::text, kind)),
        JsonLfEncoders.Field.of("description", apply(JsonLfEncoders::text, description)),
        JsonLfEncoders.Field.of("referencePrice", apply(JsonLfEncoders.optional(JsonLfEncoders::numeric), referencePrice)));
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
    if (!(object instanceof Instrument)) {
      return false;
    }
    Instrument other = (Instrument) object;
    return Objects.equals(this.issuer, other.issuer) &&
        Objects.equals(this.depository, other.depository) && Objects.equals(this.id, other.id) &&
        Objects.equals(this.version, other.version) && Objects.equals(this.kind, other.kind) &&
        Objects.equals(this.description, other.description) &&
        Objects.equals(this.referencePrice, other.referencePrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.issuer, this.depository, this.id, this.version, this.kind,
        this.description, this.referencePrice);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.instrument.Instrument(%s, %s, %s, %s, %s, %s, %s)",
        this.issuer, this.depository, this.id, this.version, this.kind, this.description,
        this.referencePrice);
  }

  /**
   * Set up an {@link ExerciseByKeyCommand}; invoke an {@code exercise} method on the result of
      this to finish creating the command, or convert to an interface first with {@code toInterface}
      to invoke an interface {@code exercise} method.
   */
  public static ByKey byKey(InstrumentKey key) {
    return new ByKey(key.toValue());
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<Instrument> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, Instrument, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<Instrument> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends ContractWithKey<ContractId, Instrument, InstrumentKey> {
    public Contract(ContractId id, Instrument data, Optional<String> agreementText,
        Optional<InstrumentKey> key, Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, key, signatories, observers);
    }

    public static JsonLfDecoder<InstrumentKey> keyJsonDecoder() {
      return new InstrumentKey.JsonDecoder$().get();
    }

    public static InstrumentKey keyFromJson(String json) throws JsonLfDecoder.Error {
      return keyJsonDecoder().decode(new JsonLfReader(json));
    }

    public JsonLfEncoder keyJsonEncoder() {
      return this.key.map(InstrumentKey::jsonEncoder).orElse(null);
    }

    public String keyToJson() {
      var enc = keyJsonEncoder();
      if (enc == null) return null;
      var w = new StringWriter();
      try {
        enc.encode(new JsonLfWriter(w));
      } catch (IOException e) {
        // Not expected with StringWriter
        throw new UncheckedIOException(e);
      }
      return w.toString();
    }

    @Override
    protected ContractCompanion<Contract, ContractId, Instrument> getCompanion() {
      return COMPANION;
    }

    public static Contract fromIdAndRecord(String contractId, DamlRecord record$,
        Optional<String> agreementText, Optional<InstrumentKey> key, Set<String> signatories,
        Set<String> observers) {
      return COMPANION.fromIdAndRecord(contractId, record$, agreementText, key, signatories,
          observers);
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

    default Update<Exercised<ContractId>> exerciseSetReferencePrice(SetReferencePrice arg) {
      return makeExerciseCmd(CHOICE_SetReferencePrice, arg);
    }

    default Update<Exercised<ContractId>> exerciseSetReferencePrice(BigDecimal newPrice) {
      return exerciseSetReferencePrice(new SetReferencePrice(newPrice));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, Instrument, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<Instrument> get() {
      return jsonDecoder();
    }
  }

  public static final class ByKey extends com.daml.ledger.javaapi.data.codegen.ByKey implements Exercises<ExerciseByKeyCommand> {
    ByKey(Value key) {
      super(key);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, Instrument, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }
}
