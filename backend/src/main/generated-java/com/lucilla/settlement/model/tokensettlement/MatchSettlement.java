package com.lucilla.settlement.model.tokensettlement;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlCollectors;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
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
import com.lucilla.settlement.model.da.internal.template.Archive;
import com.lucilla.settlement.model.settlement.FillRecord;
import com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation;
import com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MatchSettlement extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "TokenSettlement", "MatchSettlement");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1", "TokenSettlement", "MatchSettlement");

  public static final String PACKAGE_ID = "abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<MatchSettlement, MatchSettlement_Settle, FillRecord> CHOICE_MatchSettlement_Settle = 
      Choice.create("MatchSettlement_Settle", value$ -> value$.toValue(), value$ ->
        MatchSettlement_Settle.valueDecoder().decode(value$), value$ -> FillRecord.valueDecoder()
        .decode(value$), new MatchSettlement_Settle.JsonDecoder$().get(),
        new FillRecord.JsonDecoder$().get(), MatchSettlement_Settle::jsonEncoder,
        FillRecord::jsonEncoder);

  public static final Choice<MatchSettlement, MatchSettlement_Cancel, Unit> CHOICE_MatchSettlement_Cancel = 
      Choice.create("MatchSettlement_Cancel", value$ -> value$.toValue(), value$ ->
        MatchSettlement_Cancel.valueDecoder().decode(value$), value$ ->
        PrimitiveValueDecoders.fromUnit.decode(value$),
        new MatchSettlement_Cancel.JsonDecoder$().get(), JsonLfDecoders.unit,
        MatchSettlement_Cancel::jsonEncoder, JsonLfEncoders::unit);

  public static final Choice<MatchSettlement, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, MatchSettlement> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(MatchSettlement.PACKAGE_ID, MatchSettlement.PACKAGE_NAME, MatchSettlement.PACKAGE_VERSION),
        "com.lucilla.settlement.model.tokensettlement.MatchSettlement", TEMPLATE_ID,
        ContractId::new, v -> MatchSettlement.templateValueDecoder().decode(v),
        MatchSettlement::fromJson, Contract::new, List.of(CHOICE_MatchSettlement_Settle,
        CHOICE_MatchSettlement_Cancel, CHOICE_Archive));

  public final String operator;

  public final String auditor;

  public final List<String> signers;

  public final String settlementId;

  public final InstrumentId assetInstrument;

  public final InstrumentId cashInstrument;

  public final BigDecimal closingPrice;

  public final AuctionMatch match;

  public final Instant createdAt;

  public final Instant allocateBefore;

  public final Instant settleBefore;

  public MatchSettlement(String operator, String auditor, List<String> signers, String settlementId,
      InstrumentId assetInstrument, InstrumentId cashInstrument, BigDecimal closingPrice,
      AuctionMatch match, Instant createdAt, Instant allocateBefore, Instant settleBefore) {
    this.operator = operator;
    this.auditor = auditor;
    this.signers = signers;
    this.settlementId = settlementId;
    this.assetInstrument = assetInstrument;
    this.cashInstrument = cashInstrument;
    this.closingPrice = closingPrice;
    this.match = match;
    this.createdAt = createdAt;
    this.allocateBefore = allocateBefore;
    this.settleBefore = settleBefore;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(MatchSettlement.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseMatchSettlement_Settle} instead
   */
  @Deprecated
  public Update<Exercised<FillRecord>> createAndExerciseMatchSettlement_Settle(
      MatchSettlement_Settle arg) {
    return createAnd().exerciseMatchSettlement_Settle(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseMatchSettlement_Settle} instead
   */
  @Deprecated
  public Update<Exercised<FillRecord>> createAndExerciseMatchSettlement_Settle(
      Allocation.ContractId assetAllocation, Allocation.ContractId cashAllocation,
      ExtraArgs assetArgs, ExtraArgs cashArgs) {
    return createAndExerciseMatchSettlement_Settle(new MatchSettlement_Settle(assetAllocation,
        cashAllocation, assetArgs, cashArgs));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseMatchSettlement_Cancel} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseMatchSettlement_Cancel(
      MatchSettlement_Cancel arg) {
    return createAnd().exerciseMatchSettlement_Cancel(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseMatchSettlement_Cancel} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseMatchSettlement_Cancel(
      List<Allocation.ContractId> allocations, ExtraArgs extraArgs) {
    return createAndExerciseMatchSettlement_Cancel(new MatchSettlement_Cancel(allocations,
        extraArgs));
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

  public static Update<Created<ContractId>> create(String operator, String auditor,
      List<String> signers, String settlementId, InstrumentId assetInstrument,
      InstrumentId cashInstrument, BigDecimal closingPrice, AuctionMatch match, Instant createdAt,
      Instant allocateBefore, Instant settleBefore) {
    return new MatchSettlement(operator, auditor, signers, settlementId, assetInstrument,
        cashInstrument, closingPrice, match, createdAt, allocateBefore, settleBefore).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, MatchSettlement> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<MatchSettlement> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(11);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("signers", this.signers.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("settlementId", new Text(this.settlementId)));
    fields.add(new DamlRecord.Field("assetInstrument", this.assetInstrument.toValue()));
    fields.add(new DamlRecord.Field("cashInstrument", this.cashInstrument.toValue()));
    fields.add(new DamlRecord.Field("closingPrice", new Numeric(this.closingPrice)));
    fields.add(new DamlRecord.Field("match", this.match.toValue()));
    fields.add(new DamlRecord.Field("createdAt", Timestamp.fromInstant(this.createdAt)));
    fields.add(new DamlRecord.Field("allocateBefore", Timestamp.fromInstant(this.allocateBefore)));
    fields.add(new DamlRecord.Field("settleBefore", Timestamp.fromInstant(this.settleBefore)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<MatchSettlement> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(11,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      List<String> signers = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(2).getValue());
      String settlementId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      InstrumentId assetInstrument = InstrumentId.valueDecoder().decode(fields$.get(4).getValue());
      InstrumentId cashInstrument = InstrumentId.valueDecoder().decode(fields$.get(5).getValue());
      BigDecimal closingPrice = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(6).getValue());
      AuctionMatch match = AuctionMatch.valueDecoder().decode(fields$.get(7).getValue());
      Instant createdAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(8).getValue());
      Instant allocateBefore = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(9).getValue());
      Instant settleBefore = PrimitiveValueDecoders.fromTimestamp
          .decode(fields$.get(10).getValue());
      return new MatchSettlement(operator, auditor, signers, settlementId, assetInstrument,
          cashInstrument, closingPrice, match, createdAt, allocateBefore, settleBefore);
    } ;
  }

  public static JsonLfDecoder<MatchSettlement> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "signers", "settlementId", "assetInstrument", "cashInstrument", "closingPrice", "match", "createdAt", "allocateBefore", "settleBefore"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "signers": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "settlementId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "assetInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, new com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId.JsonDecoder$().get());
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, new com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId.JsonDecoder$().get());
            case "closingPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "match": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, new com.lucilla.settlement.model.tokensettlement.AuctionMatch.JsonDecoder$().get());
            case "createdAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "allocateBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "settleBefore": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new MatchSettlement(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10])));
  }

  public static MatchSettlement fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("signers", apply(JsonLfEncoders.list(JsonLfEncoders::party), signers)),
        JsonLfEncoders.Field.of("settlementId", apply(JsonLfEncoders::text, settlementId)),
        JsonLfEncoders.Field.of("assetInstrument", apply(InstrumentId::jsonEncoder, assetInstrument)),
        JsonLfEncoders.Field.of("cashInstrument", apply(InstrumentId::jsonEncoder, cashInstrument)),
        JsonLfEncoders.Field.of("closingPrice", apply(JsonLfEncoders::numeric, closingPrice)),
        JsonLfEncoders.Field.of("match", apply(AuctionMatch::jsonEncoder, match)),
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
    if (!(object instanceof MatchSettlement)) {
      return false;
    }
    MatchSettlement other = (MatchSettlement) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.signers, other.signers) &&
        Objects.equals(this.settlementId, other.settlementId) &&
        Objects.equals(this.assetInstrument, other.assetInstrument) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.closingPrice, other.closingPrice) &&
        Objects.equals(this.match, other.match) &&
        Objects.equals(this.createdAt, other.createdAt) &&
        Objects.equals(this.allocateBefore, other.allocateBefore) &&
        Objects.equals(this.settleBefore, other.settleBefore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.signers, this.settlementId,
        this.assetInstrument, this.cashInstrument, this.closingPrice, this.match, this.createdAt,
        this.allocateBefore, this.settleBefore);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.tokensettlement.MatchSettlement(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.signers, this.settlementId, this.assetInstrument,
        this.cashInstrument, this.closingPrice, this.match, this.createdAt, this.allocateBefore,
        this.settleBefore);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<MatchSettlement> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, MatchSettlement, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<MatchSettlement> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, MatchSettlement> {
    public Contract(ContractId id, MatchSettlement data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, MatchSettlement> getCompanion() {
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
    default Update<Exercised<FillRecord>> exerciseMatchSettlement_Settle(
        MatchSettlement_Settle arg) {
      return makeExerciseCmd(CHOICE_MatchSettlement_Settle, arg);
    }

    default Update<Exercised<FillRecord>> exerciseMatchSettlement_Settle(
        Allocation.ContractId assetAllocation, Allocation.ContractId cashAllocation,
        ExtraArgs assetArgs, ExtraArgs cashArgs) {
      return exerciseMatchSettlement_Settle(new MatchSettlement_Settle(assetAllocation,
          cashAllocation, assetArgs, cashArgs));
    }

    default Update<Exercised<Unit>> exerciseMatchSettlement_Cancel(MatchSettlement_Cancel arg) {
      return makeExerciseCmd(CHOICE_MatchSettlement_Cancel, arg);
    }

    default Update<Exercised<Unit>> exerciseMatchSettlement_Cancel(
        List<Allocation.ContractId> allocations, ExtraArgs extraArgs) {
      return exerciseMatchSettlement_Cancel(new MatchSettlement_Cancel(allocations, extraArgs));
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, MatchSettlement, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<MatchSettlement> get() {
      return jsonDecoder();
    }
  }
}
