package com.lucilla.settlement.model.tokenstandarddvp;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
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
import com.lucilla.settlement.model.splice.api.token.allocationv1.TransferLeg;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TokenStandardDvpProposal extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "TokenStandardDvp", "TokenStandardDvpProposal");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b", "TokenStandardDvp", "TokenStandardDvpProposal");

  public static final String PACKAGE_ID = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<TokenStandardDvpProposal, TokenStandardDvpProposal_InitiateSettlement, TokenStandardDvp.ContractId> CHOICE_TokenStandardDvpProposal_InitiateSettlement = 
      Choice.create("TokenStandardDvpProposal_InitiateSettlement", value$ -> value$.toValue(),
        value$ -> TokenStandardDvpProposal_InitiateSettlement.valueDecoder().decode(value$),
        value$ ->
        new TokenStandardDvp.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new TokenStandardDvpProposal_InitiateSettlement.JsonDecoder$().get(),
        JsonLfDecoders.contractId(TokenStandardDvp.ContractId::new),
        TokenStandardDvpProposal_InitiateSettlement::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<TokenStandardDvpProposal, TokenStandardDvpProposal_Accept, ContractId> CHOICE_TokenStandardDvpProposal_Accept = 
      Choice.create("TokenStandardDvpProposal_Accept", value$ -> value$.toValue(), value$ ->
        TokenStandardDvpProposal_Accept.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new TokenStandardDvpProposal_Accept.JsonDecoder$().get(),
        JsonLfDecoders.contractId(ContractId::new), TokenStandardDvpProposal_Accept::jsonEncoder,
        JsonLfEncoders::contractId);

  public static final Choice<TokenStandardDvpProposal, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardDvpProposal> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(TokenStandardDvpProposal.PACKAGE_ID, TokenStandardDvpProposal.PACKAGE_NAME, TokenStandardDvpProposal.PACKAGE_VERSION),
        "com.lucilla.settlement.model.tokenstandarddvp.TokenStandardDvpProposal", TEMPLATE_ID,
        ContractId::new, v -> TokenStandardDvpProposal.templateValueDecoder().decode(v),
        TokenStandardDvpProposal::fromJson, Contract::new,
        List.of(CHOICE_TokenStandardDvpProposal_InitiateSettlement,
        CHOICE_TokenStandardDvpProposal_Accept, CHOICE_Archive));

  public final String venue;

  public final String dvpId;

  public final Map<String, TransferLeg> legs;

  public final List<String> approvers;

  public TokenStandardDvpProposal(String venue, String dvpId, Map<String, TransferLeg> legs,
      List<String> approvers) {
    this.venue = venue;
    this.dvpId = dvpId;
    this.legs = legs;
    this.approvers = approvers;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(TokenStandardDvpProposal.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTokenStandardDvpProposal_InitiateSettlement} instead
   */
  @Deprecated
  public Update<Exercised<TokenStandardDvp.ContractId>> createAndExerciseTokenStandardDvpProposal_InitiateSettlement(
      TokenStandardDvpProposal_InitiateSettlement arg) {
    return createAnd().exerciseTokenStandardDvpProposal_InitiateSettlement(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTokenStandardDvpProposal_InitiateSettlement} instead
   */
  @Deprecated
  public Update<Exercised<TokenStandardDvp.ContractId>> createAndExerciseTokenStandardDvpProposal_InitiateSettlement(
      Instant allocateBefore, Instant settleBefore) {
    return createAndExerciseTokenStandardDvpProposal_InitiateSettlement(new TokenStandardDvpProposal_InitiateSettlement(allocateBefore,
        settleBefore));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTokenStandardDvpProposal_Accept} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseTokenStandardDvpProposal_Accept(
      TokenStandardDvpProposal_Accept arg) {
    return createAnd().exerciseTokenStandardDvpProposal_Accept(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTokenStandardDvpProposal_Accept} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseTokenStandardDvpProposal_Accept(
      String approver) {
    return createAndExerciseTokenStandardDvpProposal_Accept(new TokenStandardDvpProposal_Accept(approver));
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

  public static Update<Created<ContractId>> create(String venue, String dvpId,
      Map<String, TransferLeg> legs, List<String> approvers) {
    return new TokenStandardDvpProposal(venue, dvpId, legs, approvers).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardDvpProposal> getCompanion(
      ) {
    return COMPANION;
  }

  public static ValueDecoder<TokenStandardDvpProposal> valueDecoder() throws
      IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(4);
    fields.add(new DamlRecord.Field("venue", new Party(this.venue)));
    fields.add(new DamlRecord.Field("dvpId", new Text(this.dvpId)));
    fields.add(new DamlRecord.Field("legs", this.legs.entrySet().stream()
        .collect(DamlCollectors.toDamlTextMap(Map.Entry::getKey, v$0 -> v$0.getValue().toValue()))
        ));
    fields.add(new DamlRecord.Field("approvers", this.approvers.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<TokenStandardDvpProposal> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(4,0, recordValue$);
      String venue = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String dvpId = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      Map<String, TransferLeg> legs = PrimitiveValueDecoders.fromTextMap(TransferLeg.valueDecoder())
          .decode(fields$.get(2).getValue());
      List<String> approvers = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(3).getValue());
      return new TokenStandardDvpProposal(venue, dvpId, legs, approvers);
    } ;
  }

  public static JsonLfDecoder<TokenStandardDvpProposal> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("venue", "dvpId", "legs", "approvers"), name -> {
          switch (name) {
            case "venue": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "dvpId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "legs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.textMap(new com.lucilla.settlement.model.splice.api.token.allocationv1.TransferLeg.JsonDecoder$().get()));
            case "approvers": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            default: return null;
          }
        }
        , (Object[] args) -> new TokenStandardDvpProposal(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3])));
  }

  public static TokenStandardDvpProposal fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("venue", apply(JsonLfEncoders::party, venue)),
        JsonLfEncoders.Field.of("dvpId", apply(JsonLfEncoders::text, dvpId)),
        JsonLfEncoders.Field.of("legs", apply(JsonLfEncoders.textMap(TransferLeg::jsonEncoder), legs)),
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
    if (!(object instanceof TokenStandardDvpProposal)) {
      return false;
    }
    TokenStandardDvpProposal other = (TokenStandardDvpProposal) object;
    return Objects.equals(this.venue, other.venue) && Objects.equals(this.dvpId, other.dvpId) &&
        Objects.equals(this.legs, other.legs) && Objects.equals(this.approvers, other.approvers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.venue, this.dvpId, this.legs, this.approvers);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokenstandarddvp.TokenStandardDvpProposal(%s, %s, %s, %s)",
        this.venue, this.dvpId, this.legs, this.approvers);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardDvpProposal> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardDvpProposal, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardDvpProposal> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, TokenStandardDvpProposal> {
    public Contract(ContractId id, TokenStandardDvpProposal data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, TokenStandardDvpProposal> getCompanion() {
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
    default Update<Exercised<TokenStandardDvp.ContractId>> exerciseTokenStandardDvpProposal_InitiateSettlement(
        TokenStandardDvpProposal_InitiateSettlement arg) {
      return makeExerciseCmd(CHOICE_TokenStandardDvpProposal_InitiateSettlement, arg);
    }

    default Update<Exercised<TokenStandardDvp.ContractId>> exerciseTokenStandardDvpProposal_InitiateSettlement(
        Instant allocateBefore, Instant settleBefore) {
      return exerciseTokenStandardDvpProposal_InitiateSettlement(new TokenStandardDvpProposal_InitiateSettlement(allocateBefore,
          settleBefore));
    }

    default Update<Exercised<ContractId>> exerciseTokenStandardDvpProposal_Accept(
        TokenStandardDvpProposal_Accept arg) {
      return makeExerciseCmd(CHOICE_TokenStandardDvpProposal_Accept, arg);
    }

    default Update<Exercised<ContractId>> exerciseTokenStandardDvpProposal_Accept(String approver) {
      return exerciseTokenStandardDvpProposal_Accept(new TokenStandardDvpProposal_Accept(approver));
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardDvpProposal, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TokenStandardDvpProposal> get() {
      return jsonDecoder();
    }
  }
}
