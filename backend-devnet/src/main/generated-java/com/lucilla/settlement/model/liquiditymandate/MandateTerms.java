package com.lucilla.settlement.model.liquiditymandate;

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
import com.lucilla.settlement.model.da.internal.template.Archive;
import com.lucilla.settlement.model.da.types.Tuple2;
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
import java.util.Set;

public final class MandateTerms extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "LiquidityMandate", "MandateTerms");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1", "LiquidityMandate", "MandateTerms");

  public static final String PACKAGE_ID = "abbcb556af749c83f1afa7694d9aef2854b73e4e26080ad1d301b6b1789b47d1";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<MandateTerms, AdmitParticipant, ContractId> CHOICE_AdmitParticipant = 
      Choice.create("AdmitParticipant", value$ -> value$.toValue(), value$ ->
        AdmitParticipant.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new AdmitParticipant.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        AdmitParticipant::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<MandateTerms, WithdrawTerms, Unit> CHOICE_WithdrawTerms = 
      Choice.create("WithdrawTerms", value$ -> value$.toValue(), value$ ->
        WithdrawTerms.valueDecoder().decode(value$), value$ -> PrimitiveValueDecoders.fromUnit
        .decode(value$), new WithdrawTerms.JsonDecoder$().get(), JsonLfDecoders.unit,
        WithdrawTerms::jsonEncoder, JsonLfEncoders::unit);

  public static final Choice<MandateTerms, AcceptTerms, Tuple2<ContractId, LiquidityMandate.ContractId>> CHOICE_AcceptTerms = 
      Choice.create("AcceptTerms", value$ -> value$.toValue(), value$ -> AcceptTerms.valueDecoder()
        .decode(value$), value$ ->
        Tuple2.<com.lucilla.settlement.model.liquiditymandate.MandateTerms.ContractId,
        com.lucilla.settlement.model.liquiditymandate.LiquidityMandate.ContractId>valueDecoder(v$0 ->
          new ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        v$1 ->
          new LiquidityMandate.ContractId(v$1.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
        .decode(value$), new AcceptTerms.JsonDecoder$().get(),
        new Tuple2.JsonDecoder$().get(JsonLfDecoders.contractId(ContractId::new), JsonLfDecoders.contractId(LiquidityMandate.ContractId::new)),
        AcceptTerms::jsonEncoder,
        _x0 -> _x0.jsonEncoder(JsonLfEncoders::contractId, JsonLfEncoders::contractId));

  public static final Choice<MandateTerms, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<MandateTerms, BarProvider, ContractId> CHOICE_BarProvider = 
      Choice.create("BarProvider", value$ -> value$.toValue(), value$ -> BarProvider.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new BarProvider.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        BarProvider::jsonEncoder, JsonLfEncoders::contractId);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, MandateTerms> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(MandateTerms.PACKAGE_ID, MandateTerms.PACKAGE_NAME, MandateTerms.PACKAGE_VERSION),
        "com.lucilla.settlement.model.liquiditymandate.MandateTerms", TEMPLATE_ID, ContractId::new,
        v -> MandateTerms.templateValueDecoder().decode(v), MandateTerms::fromJson, Contract::new,
        List.of(CHOICE_WithdrawTerms, CHOICE_BarProvider, CHOICE_AdmitParticipant, CHOICE_Archive,
        CHOICE_AcceptTerms));

  public final String operator;

  public final String auditor;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal anchorPrice;

  public final BigDecimal commitmentSize;

  public final Long maxBandBps;

  public final Instant expiresAt;

  public final List<String> eligible;

  public final List<String> accepted;

  public final List<String> barred;

  public MandateTerms(String operator, String auditor, String instrumentId, String cashInstrument,
      String session, BigDecimal anchorPrice, BigDecimal commitmentSize, Long maxBandBps,
      Instant expiresAt, List<String> eligible, List<String> accepted, List<String> barred) {
    this.operator = operator;
    this.auditor = auditor;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.anchorPrice = anchorPrice;
    this.commitmentSize = commitmentSize;
    this.maxBandBps = maxBandBps;
    this.expiresAt = expiresAt;
    this.eligible = eligible;
    this.accepted = accepted;
    this.barred = barred;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(MandateTerms.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAdmitParticipant} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseAdmitParticipant(AdmitParticipant arg) {
    return createAnd().exerciseAdmitParticipant(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAdmitParticipant} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseAdmitParticipant(String participant) {
    return createAndExerciseAdmitParticipant(new AdmitParticipant(participant));
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseWithdrawTerms} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseWithdrawTerms(WithdrawTerms arg) {
    return createAnd().exerciseWithdrawTerms(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseWithdrawTerms} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseWithdrawTerms() {
    return createAndExerciseWithdrawTerms(new WithdrawTerms());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAcceptTerms} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, LiquidityMandate.ContractId>>> createAndExerciseAcceptTerms(
      AcceptTerms arg) {
    return createAnd().exerciseAcceptTerms(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAcceptTerms} instead
   */
  @Deprecated
  public Update<Exercised<Tuple2<ContractId, LiquidityMandate.ContractId>>> createAndExerciseAcceptTerms(
      String provider) {
    return createAndExerciseAcceptTerms(new AcceptTerms(provider));
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseBarProvider} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseBarProvider(BarProvider arg) {
    return createAnd().exerciseBarProvider(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseBarProvider} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseBarProvider(String provider,
      MandatePerformance.ContractId performanceCid) {
    return createAndExerciseBarProvider(new BarProvider(provider, performanceCid));
  }

  public static Update<Created<ContractId>> create(String operator, String auditor,
      String instrumentId, String cashInstrument, String session, BigDecimal anchorPrice,
      BigDecimal commitmentSize, Long maxBandBps, Instant expiresAt, List<String> eligible,
      List<String> accepted, List<String> barred) {
    return new MandateTerms(operator, auditor, instrumentId, cashInstrument, session, anchorPrice,
        commitmentSize, maxBandBps, expiresAt, eligible, accepted, barred).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, MandateTerms> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<MandateTerms> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(12);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("anchorPrice", new Numeric(this.anchorPrice)));
    fields.add(new DamlRecord.Field("commitmentSize", new Numeric(this.commitmentSize)));
    fields.add(new DamlRecord.Field("maxBandBps", new Int64(this.maxBandBps)));
    fields.add(new DamlRecord.Field("expiresAt", Timestamp.fromInstant(this.expiresAt)));
    fields.add(new DamlRecord.Field("eligible", this.eligible.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("accepted", this.accepted.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("barred", this.barred.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<MandateTerms> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(12,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      BigDecimal anchorPrice = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(5).getValue());
      BigDecimal commitmentSize = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(6).getValue());
      Long maxBandBps = PrimitiveValueDecoders.fromInt64.decode(fields$.get(7).getValue());
      Instant expiresAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(8).getValue());
      List<String> eligible = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(9).getValue());
      List<String> accepted = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(10).getValue());
      List<String> barred = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(11).getValue());
      return new MandateTerms(operator, auditor, instrumentId, cashInstrument, session, anchorPrice,
          commitmentSize, maxBandBps, expiresAt, eligible, accepted, barred);
    } ;
  }

  public static JsonLfDecoder<MandateTerms> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "instrumentId", "cashInstrument", "session", "anchorPrice", "commitmentSize", "maxBandBps", "expiresAt", "eligible", "accepted", "barred"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "anchorPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "commitmentSize": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "maxBandBps": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "expiresAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "eligible": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "accepted": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "barred": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(11, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            default: return null;
          }
        }
        , (Object[] args) -> new MandateTerms(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10]), JsonLfDecoders.cast(args[11])));
  }

  public static MandateTerms fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("anchorPrice", apply(JsonLfEncoders::numeric, anchorPrice)),
        JsonLfEncoders.Field.of("commitmentSize", apply(JsonLfEncoders::numeric, commitmentSize)),
        JsonLfEncoders.Field.of("maxBandBps", apply(JsonLfEncoders::int64, maxBandBps)),
        JsonLfEncoders.Field.of("expiresAt", apply(JsonLfEncoders::timestamp, expiresAt)),
        JsonLfEncoders.Field.of("eligible", apply(JsonLfEncoders.list(JsonLfEncoders::party), eligible)),
        JsonLfEncoders.Field.of("accepted", apply(JsonLfEncoders.list(JsonLfEncoders::party), accepted)),
        JsonLfEncoders.Field.of("barred", apply(JsonLfEncoders.list(JsonLfEncoders::party), barred)));
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
    if (!(object instanceof MandateTerms)) {
      return false;
    }
    MandateTerms other = (MandateTerms) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) &&
        Objects.equals(this.anchorPrice, other.anchorPrice) &&
        Objects.equals(this.commitmentSize, other.commitmentSize) &&
        Objects.equals(this.maxBandBps, other.maxBandBps) &&
        Objects.equals(this.expiresAt, other.expiresAt) &&
        Objects.equals(this.eligible, other.eligible) &&
        Objects.equals(this.accepted, other.accepted) && Objects.equals(this.barred, other.barred);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.instrumentId, this.cashInstrument,
        this.session, this.anchorPrice, this.commitmentSize, this.maxBandBps, this.expiresAt,
        this.eligible, this.accepted, this.barred);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.liquiditymandate.MandateTerms(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.instrumentId, this.cashInstrument, this.session,
        this.anchorPrice, this.commitmentSize, this.maxBandBps, this.expiresAt, this.eligible,
        this.accepted, this.barred);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<MandateTerms> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, MandateTerms, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<MandateTerms> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, MandateTerms> {
    public Contract(ContractId id, MandateTerms data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, MandateTerms> getCompanion() {
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
    default Update<Exercised<ContractId>> exerciseAdmitParticipant(AdmitParticipant arg) {
      return makeExerciseCmd(CHOICE_AdmitParticipant, arg);
    }

    default Update<Exercised<ContractId>> exerciseAdmitParticipant(String participant) {
      return exerciseAdmitParticipant(new AdmitParticipant(participant));
    }

    default Update<Exercised<Unit>> exerciseWithdrawTerms(WithdrawTerms arg) {
      return makeExerciseCmd(CHOICE_WithdrawTerms, arg);
    }

    default Update<Exercised<Unit>> exerciseWithdrawTerms() {
      return exerciseWithdrawTerms(new WithdrawTerms());
    }

    default Update<Exercised<Tuple2<ContractId, LiquidityMandate.ContractId>>> exerciseAcceptTerms(
        AcceptTerms arg) {
      return makeExerciseCmd(CHOICE_AcceptTerms, arg);
    }

    default Update<Exercised<Tuple2<ContractId, LiquidityMandate.ContractId>>> exerciseAcceptTerms(
        String provider) {
      return exerciseAcceptTerms(new AcceptTerms(provider));
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<ContractId>> exerciseBarProvider(BarProvider arg) {
      return makeExerciseCmd(CHOICE_BarProvider, arg);
    }

    default Update<Exercised<ContractId>> exerciseBarProvider(String provider,
        MandatePerformance.ContractId performanceCid) {
      return exerciseBarProvider(new BarProvider(provider, performanceCid));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, MandateTerms, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<MandateTerms> get() {
      return jsonDecoder();
    }
  }
}
