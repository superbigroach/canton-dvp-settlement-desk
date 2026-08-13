package com.lucilla.settlement.model.tokenstandarddvp;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
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
import com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationFactory;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferFactory;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TokenStandardRegistry extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "TokenStandardDvp", "TokenStandardRegistry");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("87c24b9a3ade1253eebbb4ea1feef8f4b9963f33c7cc6272efb5f79afdef1bb0", "TokenStandardDvp", "TokenStandardRegistry");

  public static final String PACKAGE_ID = "87c24b9a3ade1253eebbb4ea1feef8f4b9963f33c7cc6272efb5f79afdef1bb0";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<TokenStandardRegistry, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardRegistry> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(TokenStandardRegistry.PACKAGE_ID, TokenStandardRegistry.PACKAGE_NAME, TokenStandardRegistry.PACKAGE_VERSION),
        "com.lucilla.settlement.model.tokenstandarddvp.TokenStandardRegistry", TEMPLATE_ID,
        ContractId::new, v -> TokenStandardRegistry.templateValueDecoder().decode(v),
        TokenStandardRegistry::fromJson, Contract::new, List.of(CHOICE_Archive));

  public final String admin;

  public TokenStandardRegistry(String admin) {
    this.admin = admin;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(TokenStandardRegistry.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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

  public static Update<Created<ContractId>> create(String admin) {
    return new TokenStandardRegistry(admin).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardRegistry> getCompanion(
      ) {
    return COMPANION;
  }

  public static ValueDecoder<TokenStandardRegistry> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(1);
    fields.add(new DamlRecord.Field("admin", new Party(this.admin)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<TokenStandardRegistry> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(1,0, recordValue$);
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      return new TokenStandardRegistry(admin);
    } ;
  }

  public static JsonLfDecoder<TokenStandardRegistry> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("admin"), name -> {
          switch (name) {
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            default: return null;
          }
        }
        , (Object[] args) -> new TokenStandardRegistry(JsonLfDecoders.cast(args[0])));
  }

  public static TokenStandardRegistry fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)));
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
    if (!(object instanceof TokenStandardRegistry)) {
      return false;
    }
    TokenStandardRegistry other = (TokenStandardRegistry) object;
    return Objects.equals(this.admin, other.admin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.admin);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokenstandarddvp.TokenStandardRegistry(%s)",
        this.admin);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardRegistry> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardRegistry, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public AllocationFactory.ContractId toInterface(
        AllocationFactory.INTERFACE_ interfaceCompanion) {
      return new AllocationFactory.ContractId(this.contractId);
    }

    public TransferFactory.ContractId toInterface(TransferFactory.INTERFACE_ interfaceCompanion) {
      return new TransferFactory.ContractId(this.contractId);
    }

    public static ContractId unsafeFromInterface(AllocationFactory.ContractId interfaceContractId) {
      return new ContractId(interfaceContractId.contractId);
    }

    public static ContractId unsafeFromInterface(TransferFactory.ContractId interfaceContractId) {
      return new ContractId(interfaceContractId.contractId);
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardRegistry> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, TokenStandardRegistry> {
    public Contract(ContractId id, TokenStandardRegistry data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, TokenStandardRegistry> getCompanion() {
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardRegistry, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public AllocationFactory.CreateAnd toInterface(
        AllocationFactory.INTERFACE_ interfaceCompanion) {
      return new AllocationFactory.CreateAnd(COMPANION, this.createArguments);
    }

    public TransferFactory.CreateAnd toInterface(TransferFactory.INTERFACE_ interfaceCompanion) {
      return new TransferFactory.CreateAnd(COMPANION, this.createArguments);
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TokenStandardRegistry> get() {
      return jsonDecoder();
    }
  }
}
