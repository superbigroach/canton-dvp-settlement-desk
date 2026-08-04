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
import com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation;
import com.lucilla.settlement.model.splice.api.token.allocationv1.AllocationSpecification;
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

public final class TokenStandardAllocation extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#canton-dvp-settlement-desk", "TokenStandardDvp", "TokenStandardAllocation");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("5ac8f47b9e28322096bc4c9c58f8449ead13792c4a30aad95cd1e80669894a79", "TokenStandardDvp", "TokenStandardAllocation");

  public static final String PACKAGE_ID = "5ac8f47b9e28322096bc4c9c58f8449ead13792c4a30aad95cd1e80669894a79";

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<TokenStandardAllocation, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardAllocation> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(TokenStandardAllocation.PACKAGE_ID, TokenStandardAllocation.PACKAGE_NAME, TokenStandardAllocation.PACKAGE_VERSION),
        "com.lucilla.settlement.model.tokenstandarddvp.TokenStandardAllocation", TEMPLATE_ID,
        ContractId::new, v -> TokenStandardAllocation.templateValueDecoder().decode(v),
        TokenStandardAllocation::fromJson, Contract::new, List.of(CHOICE_Archive));

  public final AllocationSpecification allocation;

  public final TokenStandardHolding.ContractId lockedHoldingCid;

  public TokenStandardAllocation(AllocationSpecification allocation,
      TokenStandardHolding.ContractId lockedHoldingCid) {
    this.allocation = allocation;
    this.lockedHoldingCid = lockedHoldingCid;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(TokenStandardAllocation.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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

  public static Update<Created<ContractId>> create(AllocationSpecification allocation,
      TokenStandardHolding.ContractId lockedHoldingCid) {
    return new TokenStandardAllocation(allocation, lockedHoldingCid).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardAllocation> getCompanion(
      ) {
    return COMPANION;
  }

  public static ValueDecoder<TokenStandardAllocation> valueDecoder() throws
      IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(2);
    fields.add(new DamlRecord.Field("allocation", this.allocation.toValue()));
    fields.add(new DamlRecord.Field("lockedHoldingCid", this.lockedHoldingCid.toValue()));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<TokenStandardAllocation> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0, recordValue$);
      AllocationSpecification allocation = AllocationSpecification.valueDecoder()
          .decode(fields$.get(0).getValue());
      TokenStandardHolding.ContractId lockedHoldingCid =
          new TokenStandardHolding.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected lockedHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new TokenStandardAllocation(allocation, lockedHoldingCid);
    } ;
  }

  public static JsonLfDecoder<TokenStandardAllocation> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("allocation", "lockedHoldingCid"), name -> {
          switch (name) {
            case "allocation": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, new com.lucilla.settlement.model.splice.api.token.allocationv1.AllocationSpecification.JsonDecoder$().get());
            case "lockedHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.tokenstandarddvp.TokenStandardHolding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new TokenStandardAllocation(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static TokenStandardAllocation fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("allocation", apply(AllocationSpecification::jsonEncoder, allocation)),
        JsonLfEncoders.Field.of("lockedHoldingCid", apply(JsonLfEncoders::contractId, lockedHoldingCid)));
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
    if (!(object instanceof TokenStandardAllocation)) {
      return false;
    }
    TokenStandardAllocation other = (TokenStandardAllocation) object;
    return Objects.equals(this.allocation, other.allocation) &&
        Objects.equals(this.lockedHoldingCid, other.lockedHoldingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.allocation, this.lockedHoldingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokenstandarddvp.TokenStandardAllocation(%s, %s)",
        this.allocation, this.lockedHoldingCid);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardAllocation> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardAllocation, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public Allocation.ContractId toInterface(Allocation.INTERFACE_ interfaceCompanion) {
      return new Allocation.ContractId(this.contractId);
    }

    public static ContractId unsafeFromInterface(Allocation.ContractId interfaceContractId) {
      return new ContractId(interfaceContractId.contractId);
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardAllocation> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, TokenStandardAllocation> {
    public Contract(ContractId id, TokenStandardAllocation data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, TokenStandardAllocation> getCompanion() {
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardAllocation, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public Allocation.CreateAnd toInterface(Allocation.INTERFACE_ interfaceCompanion) {
      return new Allocation.CreateAnd(COMPANION, this.createArguments);
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TokenStandardAllocation> get() {
      return jsonDecoder();
    }
  }
}
