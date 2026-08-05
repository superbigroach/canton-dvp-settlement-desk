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
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.Transfer;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstruction;
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

public final class TokenStandardTransferOffer extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "TokenStandardDvp", "TokenStandardTransferOffer");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be", "TokenStandardDvp", "TokenStandardTransferOffer");

  public static final String PACKAGE_ID = "d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 0, 0});

  public static final Choice<TokenStandardTransferOffer, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardTransferOffer> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(TokenStandardTransferOffer.PACKAGE_ID, TokenStandardTransferOffer.PACKAGE_NAME, TokenStandardTransferOffer.PACKAGE_VERSION),
        "com.lucilla.settlement.model.tokenstandarddvp.TokenStandardTransferOffer", TEMPLATE_ID,
        ContractId::new, v -> TokenStandardTransferOffer.templateValueDecoder().decode(v),
        TokenStandardTransferOffer::fromJson, Contract::new, List.of(CHOICE_Archive));

  public final Transfer transfer;

  public final TokenStandardHolding.ContractId lockedHoldingCid;

  public TokenStandardTransferOffer(Transfer transfer,
      TokenStandardHolding.ContractId lockedHoldingCid) {
    this.transfer = transfer;
    this.lockedHoldingCid = lockedHoldingCid;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(TokenStandardTransferOffer.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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

  public static Update<Created<ContractId>> create(Transfer transfer,
      TokenStandardHolding.ContractId lockedHoldingCid) {
    return new TokenStandardTransferOffer(transfer, lockedHoldingCid).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardTransferOffer> getCompanion(
      ) {
    return COMPANION;
  }

  public static ValueDecoder<TokenStandardTransferOffer> valueDecoder() throws
      IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(2);
    fields.add(new DamlRecord.Field("transfer", this.transfer.toValue()));
    fields.add(new DamlRecord.Field("lockedHoldingCid", this.lockedHoldingCid.toValue()));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<TokenStandardTransferOffer> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(2,0, recordValue$);
      Transfer transfer = Transfer.valueDecoder().decode(fields$.get(0).getValue());
      TokenStandardHolding.ContractId lockedHoldingCid =
          new TokenStandardHolding.ContractId(fields$.get(1).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected lockedHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      return new TokenStandardTransferOffer(transfer, lockedHoldingCid);
    } ;
  }

  public static JsonLfDecoder<TokenStandardTransferOffer> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("transfer", "lockedHoldingCid"), name -> {
          switch (name) {
            case "transfer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, new com.lucilla.settlement.model.splice.api.token.transferinstructionv1.Transfer.JsonDecoder$().get());
            case "lockedHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.tokenstandarddvp.TokenStandardHolding.ContractId::new));
            default: return null;
          }
        }
        , (Object[] args) -> new TokenStandardTransferOffer(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1])));
  }

  public static TokenStandardTransferOffer fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("transfer", apply(Transfer::jsonEncoder, transfer)),
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
    if (!(object instanceof TokenStandardTransferOffer)) {
      return false;
    }
    TokenStandardTransferOffer other = (TokenStandardTransferOffer) object;
    return Objects.equals(this.transfer, other.transfer) &&
        Objects.equals(this.lockedHoldingCid, other.lockedHoldingCid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.transfer, this.lockedHoldingCid);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokenstandarddvp.TokenStandardTransferOffer(%s, %s)",
        this.transfer, this.lockedHoldingCid);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardTransferOffer> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardTransferOffer, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public TransferInstruction.ContractId toInterface(
        TransferInstruction.INTERFACE_ interfaceCompanion) {
      return new TransferInstruction.ContractId(this.contractId);
    }

    public static ContractId unsafeFromInterface(
        TransferInstruction.ContractId interfaceContractId) {
      return new ContractId(interfaceContractId.contractId);
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardTransferOffer> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, TokenStandardTransferOffer> {
    public Contract(ContractId id, TokenStandardTransferOffer data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, TokenStandardTransferOffer> getCompanion() {
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardTransferOffer, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public TransferInstruction.CreateAnd toInterface(
        TransferInstruction.INTERFACE_ interfaceCompanion) {
      return new TransferInstruction.CreateAnd(COMPANION, this.createArguments);
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TokenStandardTransferOffer> get() {
      return jsonDecoder();
    }
  }
}
