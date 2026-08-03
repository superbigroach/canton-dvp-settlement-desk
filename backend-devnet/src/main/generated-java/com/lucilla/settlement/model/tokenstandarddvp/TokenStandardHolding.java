package com.lucilla.settlement.model.tokenstandarddvp;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlOptional;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Numeric;
import com.daml.ledger.javaapi.data.PackageVersion;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Template;
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
import com.lucilla.settlement.model.splice.api.token.holdingv1.Holding;
import com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId;
import com.lucilla.settlement.model.splice.api.token.holdingv1.Lock;
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

public final class TokenStandardHolding extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#canton-dvp-settlement-desk", "TokenStandardDvp", "TokenStandardHolding");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("cd6202b647482a998c93612fd615750e35250bcfb57272e00d9198ebe014161a", "TokenStandardDvp", "TokenStandardHolding");

  public static final String PACKAGE_ID = "cd6202b647482a998c93612fd615750e35250bcfb57272e00d9198ebe014161a";

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<TokenStandardHolding, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardHolding> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(TokenStandardHolding.PACKAGE_ID, TokenStandardHolding.PACKAGE_NAME, TokenStandardHolding.PACKAGE_VERSION),
        "com.lucilla.settlement.model.tokenstandarddvp.TokenStandardHolding", TEMPLATE_ID,
        ContractId::new, v -> TokenStandardHolding.templateValueDecoder().decode(v),
        TokenStandardHolding::fromJson, Contract::new, List.of(CHOICE_Archive));

  public final String admin;

  public final String owner;

  public final InstrumentId instrumentId;

  public final BigDecimal amount;

  public final Optional<Lock> lock;

  public TokenStandardHolding(String admin, String owner, InstrumentId instrumentId,
      BigDecimal amount, Optional<Lock> lock) {
    this.admin = admin;
    this.owner = owner;
    this.instrumentId = instrumentId;
    this.amount = amount;
    this.lock = lock;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(TokenStandardHolding.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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

  public static Update<Created<ContractId>> create(String admin, String owner,
      InstrumentId instrumentId, BigDecimal amount, Optional<Lock> lock) {
    return new TokenStandardHolding(admin, owner, instrumentId, amount, lock).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardHolding> getCompanion(
      ) {
    return COMPANION;
  }

  public static ValueDecoder<TokenStandardHolding> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(5);
    fields.add(new DamlRecord.Field("admin", new Party(this.admin)));
    fields.add(new DamlRecord.Field("owner", new Party(this.owner)));
    fields.add(new DamlRecord.Field("instrumentId", this.instrumentId.toValue()));
    fields.add(new DamlRecord.Field("amount", new Numeric(this.amount)));
    fields.add(new DamlRecord.Field("lock", DamlOptional.of(this.lock.map(v$0 -> v$0.toValue()))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<TokenStandardHolding> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(5,1, recordValue$);
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String owner = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      InstrumentId instrumentId = InstrumentId.valueDecoder().decode(fields$.get(2).getValue());
      BigDecimal amount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(3).getValue());
      Optional<Lock> lock = PrimitiveValueDecoders.fromOptional(Lock.valueDecoder())
          .decode(fields$.get(4).getValue());
      return new TokenStandardHolding(admin, owner, instrumentId, amount, lock);
    } ;
  }

  public static JsonLfDecoder<TokenStandardHolding> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("admin", "owner", "instrumentId", "amount", "lock"), name -> {
          switch (name) {
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "owner": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, new com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId.JsonDecoder$().get());
            case "amount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "lock": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(new com.lucilla.settlement.model.splice.api.token.holdingv1.Lock.JsonDecoder$().get()), java.util.Optional.empty());
            default: return null;
          }
        }
        , (Object[] args) -> new TokenStandardHolding(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4])));
  }

  public static TokenStandardHolding fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)),
        JsonLfEncoders.Field.of("owner", apply(JsonLfEncoders::party, owner)),
        JsonLfEncoders.Field.of("instrumentId", apply(InstrumentId::jsonEncoder, instrumentId)),
        JsonLfEncoders.Field.of("amount", apply(JsonLfEncoders::numeric, amount)),
        JsonLfEncoders.Field.of("lock", apply(JsonLfEncoders.optional(Lock::jsonEncoder), lock)));
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
    if (!(object instanceof TokenStandardHolding)) {
      return false;
    }
    TokenStandardHolding other = (TokenStandardHolding) object;
    return Objects.equals(this.admin, other.admin) && Objects.equals(this.owner, other.owner) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.amount, other.amount) && Objects.equals(this.lock, other.lock);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.admin, this.owner, this.instrumentId, this.amount, this.lock);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokenstandarddvp.TokenStandardHolding(%s, %s, %s, %s, %s)",
        this.admin, this.owner, this.instrumentId, this.amount, this.lock);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardHolding> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardHolding, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public Holding.ContractId toInterface(Holding.INTERFACE_ interfaceCompanion) {
      return new Holding.ContractId(this.contractId);
    }

    public static ContractId unsafeFromInterface(Holding.ContractId interfaceContractId) {
      return new ContractId(interfaceContractId.contractId);
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardHolding> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, TokenStandardHolding> {
    public Contract(ContractId id, TokenStandardHolding data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, TokenStandardHolding> getCompanion() {
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardHolding, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public Holding.CreateAnd toInterface(Holding.INTERFACE_ interfaceCompanion) {
      return new Holding.CreateAnd(COMPANION, this.createArguments);
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TokenStandardHolding> get() {
      return jsonDecoder();
    }
  }
}
