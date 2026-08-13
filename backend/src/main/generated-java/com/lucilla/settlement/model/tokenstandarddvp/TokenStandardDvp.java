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
import com.lucilla.settlement.model.splice.api.token.allocationrequestv1.AllocationRequest;
import com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation;
import com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation_ExecuteTransferResult;
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

public final class TokenStandardDvp extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "TokenStandardDvp", "TokenStandardDvp");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("87c24b9a3ade1253eebbb4ea1feef8f4b9963f33c7cc6272efb5f79afdef1bb0", "TokenStandardDvp", "TokenStandardDvp");

  public static final String PACKAGE_ID = "87c24b9a3ade1253eebbb4ea1feef8f4b9963f33c7cc6272efb5f79afdef1bb0";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<TokenStandardDvp, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<TokenStandardDvp, TokenStandardDvp_Settle, Map<String, Allocation_ExecuteTransferResult>> CHOICE_TokenStandardDvp_Settle = 
      Choice.create("TokenStandardDvp_Settle", value$ -> value$.toValue(), value$ ->
        TokenStandardDvp_Settle.valueDecoder().decode(value$), value$ ->
        PrimitiveValueDecoders.fromTextMap(Allocation_ExecuteTransferResult.valueDecoder())
        .decode(value$), new TokenStandardDvp_Settle.JsonDecoder$().get(),
        JsonLfDecoders.textMap(new Allocation_ExecuteTransferResult.JsonDecoder$().get()),
        TokenStandardDvp_Settle::jsonEncoder,
        JsonLfEncoders.textMap(Allocation_ExecuteTransferResult::jsonEncoder));

  public static final Choice<TokenStandardDvp, TokenStandardDvp_Abort, Unit> CHOICE_TokenStandardDvp_Abort = 
      Choice.create("TokenStandardDvp_Abort", value$ -> value$.toValue(), value$ ->
        TokenStandardDvp_Abort.valueDecoder().decode(value$), value$ ->
        PrimitiveValueDecoders.fromUnit.decode(value$),
        new TokenStandardDvp_Abort.JsonDecoder$().get(), JsonLfDecoders.unit,
        TokenStandardDvp_Abort::jsonEncoder, JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardDvp> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(TokenStandardDvp.PACKAGE_ID, TokenStandardDvp.PACKAGE_NAME, TokenStandardDvp.PACKAGE_VERSION),
        "com.lucilla.settlement.model.tokenstandarddvp.TokenStandardDvp", TEMPLATE_ID,
        ContractId::new, v -> TokenStandardDvp.templateValueDecoder().decode(v),
        TokenStandardDvp::fromJson, Contract::new, List.of(CHOICE_Archive,
        CHOICE_TokenStandardDvp_Settle, CHOICE_TokenStandardDvp_Abort));

  public final String venue;

  public final String dvpId;

  public final Map<String, TransferLeg> legs;

  public final Instant createdAt;

  public final Instant allocateBefore;

  public final Instant settleBefore;

  public TokenStandardDvp(String venue, String dvpId, Map<String, TransferLeg> legs,
      Instant createdAt, Instant allocateBefore, Instant settleBefore) {
    this.venue = venue;
    this.dvpId = dvpId;
    this.legs = legs;
    this.createdAt = createdAt;
    this.allocateBefore = allocateBefore;
    this.settleBefore = settleBefore;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(TokenStandardDvp.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTokenStandardDvp_Settle} instead
   */
  @Deprecated
  public Update<Exercised<Map<String, Allocation_ExecuteTransferResult>>> createAndExerciseTokenStandardDvp_Settle(
      TokenStandardDvp_Settle arg) {
    return createAnd().exerciseTokenStandardDvp_Settle(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTokenStandardDvp_Settle} instead
   */
  @Deprecated
  public Update<Exercised<Map<String, Allocation_ExecuteTransferResult>>> createAndExerciseTokenStandardDvp_Settle(
      Map<String, Allocation.ContractId> allocations) {
    return createAndExerciseTokenStandardDvp_Settle(new TokenStandardDvp_Settle(allocations));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTokenStandardDvp_Abort} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseTokenStandardDvp_Abort(
      TokenStandardDvp_Abort arg) {
    return createAnd().exerciseTokenStandardDvp_Abort(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseTokenStandardDvp_Abort} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseTokenStandardDvp_Abort(
      List<Allocation.ContractId> allocations) {
    return createAndExerciseTokenStandardDvp_Abort(new TokenStandardDvp_Abort(allocations));
  }

  public static Update<Created<ContractId>> create(String venue, String dvpId,
      Map<String, TransferLeg> legs, Instant createdAt, Instant allocateBefore,
      Instant settleBefore) {
    return new TokenStandardDvp(venue, dvpId, legs, createdAt, allocateBefore,
        settleBefore).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, TokenStandardDvp> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<TokenStandardDvp> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(6);
    fields.add(new DamlRecord.Field("venue", new Party(this.venue)));
    fields.add(new DamlRecord.Field("dvpId", new Text(this.dvpId)));
    fields.add(new DamlRecord.Field("legs", this.legs.entrySet().stream()
        .collect(DamlCollectors.toDamlTextMap(Map.Entry::getKey, v$0 -> v$0.getValue().toValue()))
        ));
    fields.add(new DamlRecord.Field("createdAt", Timestamp.fromInstant(this.createdAt)));
    fields.add(new DamlRecord.Field("allocateBefore", Timestamp.fromInstant(this.allocateBefore)));
    fields.add(new DamlRecord.Field("settleBefore", Timestamp.fromInstant(this.settleBefore)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<TokenStandardDvp> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(6,0, recordValue$);
      String venue = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String dvpId = PrimitiveValueDecoders.fromText.decode(fields$.get(1).getValue());
      Map<String, TransferLeg> legs = PrimitiveValueDecoders.fromTextMap(TransferLeg.valueDecoder())
          .decode(fields$.get(2).getValue());
      Instant createdAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(3).getValue());
      Instant allocateBefore = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(4).getValue());
      Instant settleBefore = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(5).getValue());
      return new TokenStandardDvp(venue, dvpId, legs, createdAt, allocateBefore, settleBefore);
    } ;
  }

  public static JsonLfDecoder<TokenStandardDvp> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("venue", "dvpId", "legs", "createdAt", "allocateBefore", "settleBefore"), name -> {
          switch (name) {
            case "venue": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "dvpId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "legs": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.textMap(new com.lucilla.settlement.model.splice.api.token.allocationv1.TransferLeg.JsonDecoder$().get()));
            case "createdAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "allocateBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "settleBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new TokenStandardDvp(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5])));
  }

  public static TokenStandardDvp fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("venue", apply(JsonLfEncoders::party, venue)),
        JsonLfEncoders.Field.of("dvpId", apply(JsonLfEncoders::text, dvpId)),
        JsonLfEncoders.Field.of("legs", apply(JsonLfEncoders.textMap(TransferLeg::jsonEncoder), legs)),
        JsonLfEncoders.Field.of("createdAt", apply(JsonLfEncoders::timestamp, createdAt)),
        JsonLfEncoders.Field.of("allocateBefore", apply(JsonLfEncoders::timestamp, allocateBefore)),
        JsonLfEncoders.Field.of("settleBefore", apply(JsonLfEncoders::timestamp, settleBefore)));
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
    if (!(object instanceof TokenStandardDvp)) {
      return false;
    }
    TokenStandardDvp other = (TokenStandardDvp) object;
    return Objects.equals(this.venue, other.venue) && Objects.equals(this.dvpId, other.dvpId) &&
        Objects.equals(this.legs, other.legs) && Objects.equals(this.createdAt, other.createdAt) &&
        Objects.equals(this.allocateBefore, other.allocateBefore) &&
        Objects.equals(this.settleBefore, other.settleBefore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.venue, this.dvpId, this.legs, this.createdAt, this.allocateBefore,
        this.settleBefore);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokenstandarddvp.TokenStandardDvp(%s, %s, %s, %s, %s, %s)",
        this.venue, this.dvpId, this.legs, this.createdAt, this.allocateBefore, this.settleBefore);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardDvp> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardDvp, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public AllocationRequest.ContractId toInterface(
        AllocationRequest.INTERFACE_ interfaceCompanion) {
      return new AllocationRequest.ContractId(this.contractId);
    }

    public static ContractId unsafeFromInterface(AllocationRequest.ContractId interfaceContractId) {
      return new ContractId(interfaceContractId.contractId);
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<TokenStandardDvp> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, TokenStandardDvp> {
    public Contract(ContractId id, TokenStandardDvp data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, TokenStandardDvp> getCompanion() {
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

    default Update<Exercised<Map<String, Allocation_ExecuteTransferResult>>> exerciseTokenStandardDvp_Settle(
        TokenStandardDvp_Settle arg) {
      return makeExerciseCmd(CHOICE_TokenStandardDvp_Settle, arg);
    }

    default Update<Exercised<Map<String, Allocation_ExecuteTransferResult>>> exerciseTokenStandardDvp_Settle(
        Map<String, Allocation.ContractId> allocations) {
      return exerciseTokenStandardDvp_Settle(new TokenStandardDvp_Settle(allocations));
    }

    default Update<Exercised<Unit>> exerciseTokenStandardDvp_Abort(TokenStandardDvp_Abort arg) {
      return makeExerciseCmd(CHOICE_TokenStandardDvp_Abort, arg);
    }

    default Update<Exercised<Unit>> exerciseTokenStandardDvp_Abort(
        List<Allocation.ContractId> allocations) {
      return exerciseTokenStandardDvp_Abort(new TokenStandardDvp_Abort(allocations));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TokenStandardDvp, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public AllocationRequest.CreateAnd toInterface(
        AllocationRequest.INTERFACE_ interfaceCompanion) {
      return new AllocationRequest.CreateAnd(COMPANION, this.createArguments);
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TokenStandardDvp> get() {
      return jsonDecoder();
    }
  }
}
