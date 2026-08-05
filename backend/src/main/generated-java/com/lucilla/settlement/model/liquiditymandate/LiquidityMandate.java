package com.lucilla.settlement.model.liquiditymandate;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlOptional;
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
import com.lucilla.settlement.model.settlement.SettlementBatch;
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

public final class LiquidityMandate extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#canton-dvp-settlement-desk", "LiquidityMandate", "LiquidityMandate");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("16b1d7198cf7c7ec9373fe2d1bdb48ab1770fe7ffcb7281ad87048ebecd45ab4", "LiquidityMandate", "LiquidityMandate");

  public static final String PACKAGE_ID = "16b1d7198cf7c7ec9373fe2d1bdb48ab1770fe7ffcb7281ad87048ebecd45ab4";

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<LiquidityMandate, RecordPerformance, MandateOutcome> CHOICE_RecordPerformance = 
      Choice.create("RecordPerformance", value$ -> value$.toValue(), value$ ->
        RecordPerformance.valueDecoder().decode(value$), value$ -> MandateOutcome.valueDecoder()
        .decode(value$), new RecordPerformance.JsonDecoder$().get(),
        new MandateOutcome.JsonDecoder$().get(), RecordPerformance::jsonEncoder,
        MandateOutcome::jsonEncoder);

  public static final Choice<LiquidityMandate, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<LiquidityMandate, NoteDisclosure, ContractId> CHOICE_NoteDisclosure = 
      Choice.create("NoteDisclosure", value$ -> value$.toValue(), value$ ->
        NoteDisclosure.valueDecoder().decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new NoteDisclosure.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        NoteDisclosure::jsonEncoder, JsonLfEncoders::contractId);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, LiquidityMandate> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(LiquidityMandate.PACKAGE_ID, LiquidityMandate.PACKAGE_NAME, LiquidityMandate.PACKAGE_VERSION),
        "com.lucilla.settlement.model.liquiditymandate.LiquidityMandate", TEMPLATE_ID,
        ContractId::new, v -> LiquidityMandate.templateValueDecoder().decode(v),
        LiquidityMandate::fromJson, Contract::new, List.of(CHOICE_RecordPerformance, CHOICE_Archive,
        CHOICE_NoteDisclosure));

  public final String operator;

  public final String auditor;

  public final String provider;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal anchorPrice;

  public final BigDecimal commitmentSize;

  public final Long maxBandBps;

  public final Instant expiresAt;

  public final Instant acceptedAt;

  public final String shownSide;

  public final BigDecimal peakShownQty;

  public final Optional<Instant> lastShownAt;

  public final Long disclosuresSeen;

  public LiquidityMandate(String operator, String auditor, String provider, String instrumentId,
      String cashInstrument, String session, BigDecimal anchorPrice, BigDecimal commitmentSize,
      Long maxBandBps, Instant expiresAt, Instant acceptedAt, String shownSide,
      BigDecimal peakShownQty, Optional<Instant> lastShownAt, Long disclosuresSeen) {
    this.operator = operator;
    this.auditor = auditor;
    this.provider = provider;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.anchorPrice = anchorPrice;
    this.commitmentSize = commitmentSize;
    this.maxBandBps = maxBandBps;
    this.expiresAt = expiresAt;
    this.acceptedAt = acceptedAt;
    this.shownSide = shownSide;
    this.peakShownQty = peakShownQty;
    this.lastShownAt = lastShownAt;
    this.disclosuresSeen = disclosuresSeen;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(LiquidityMandate.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRecordPerformance} instead
   */
  @Deprecated
  public Update<Exercised<MandateOutcome>> createAndExerciseRecordPerformance(
      RecordPerformance arg) {
    return createAnd().exerciseRecordPerformance(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRecordPerformance} instead
   */
  @Deprecated
  public Update<Exercised<MandateOutcome>> createAndExerciseRecordPerformance(
      SettlementBatch.ContractId batchCid, MandateTerms.ContractId termsCid) {
    return createAndExerciseRecordPerformance(new RecordPerformance(batchCid, termsCid));
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseNoteDisclosure} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseNoteDisclosure(NoteDisclosure arg) {
    return createAnd().exerciseNoteDisclosure(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseNoteDisclosure} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExerciseNoteDisclosure(String netSide,
      BigDecimal netQuantity) {
    return createAndExerciseNoteDisclosure(new NoteDisclosure(netSide, netQuantity));
  }

  public static Update<Created<ContractId>> create(String operator, String auditor, String provider,
      String instrumentId, String cashInstrument, String session, BigDecimal anchorPrice,
      BigDecimal commitmentSize, Long maxBandBps, Instant expiresAt, Instant acceptedAt,
      String shownSide, BigDecimal peakShownQty, Optional<Instant> lastShownAt,
      Long disclosuresSeen) {
    return new LiquidityMandate(operator, auditor, provider, instrumentId, cashInstrument, session,
        anchorPrice, commitmentSize, maxBandBps, expiresAt, acceptedAt, shownSide, peakShownQty,
        lastShownAt, disclosuresSeen).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, LiquidityMandate> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<LiquidityMandate> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(15);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("provider", new Party(this.provider)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("anchorPrice", new Numeric(this.anchorPrice)));
    fields.add(new DamlRecord.Field("commitmentSize", new Numeric(this.commitmentSize)));
    fields.add(new DamlRecord.Field("maxBandBps", new Int64(this.maxBandBps)));
    fields.add(new DamlRecord.Field("expiresAt", Timestamp.fromInstant(this.expiresAt)));
    fields.add(new DamlRecord.Field("acceptedAt", Timestamp.fromInstant(this.acceptedAt)));
    fields.add(new DamlRecord.Field("shownSide", new Text(this.shownSide)));
    fields.add(new DamlRecord.Field("peakShownQty", new Numeric(this.peakShownQty)));
    fields.add(new DamlRecord.Field("lastShownAt", DamlOptional.of(this.lastShownAt.map(v$0 -> Timestamp.fromInstant(v$0)))));
    fields.add(new DamlRecord.Field("disclosuresSeen", new Int64(this.disclosuresSeen)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<LiquidityMandate> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(15,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String provider = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      BigDecimal anchorPrice = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      BigDecimal commitmentSize = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(7).getValue());
      Long maxBandBps = PrimitiveValueDecoders.fromInt64.decode(fields$.get(8).getValue());
      Instant expiresAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(9).getValue());
      Instant acceptedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(10).getValue());
      String shownSide = PrimitiveValueDecoders.fromText.decode(fields$.get(11).getValue());
      BigDecimal peakShownQty = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(12).getValue());
      Optional<Instant> lastShownAt = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromTimestamp).decode(fields$.get(13).getValue());
      Long disclosuresSeen = PrimitiveValueDecoders.fromInt64.decode(fields$.get(14).getValue());
      return new LiquidityMandate(operator, auditor, provider, instrumentId, cashInstrument,
          session, anchorPrice, commitmentSize, maxBandBps, expiresAt, acceptedAt, shownSide,
          peakShownQty, lastShownAt, disclosuresSeen);
    } ;
  }

  public static JsonLfDecoder<LiquidityMandate> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "provider", "instrumentId", "cashInstrument", "session", "anchorPrice", "commitmentSize", "maxBandBps", "expiresAt", "acceptedAt", "shownSide", "peakShownQty", "lastShownAt", "disclosuresSeen"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "provider": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "anchorPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "commitmentSize": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "maxBandBps": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "expiresAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "acceptedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "shownSide": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(11, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "peakShownQty": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(12, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "lastShownAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(13, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp), java.util.Optional.empty());
            case "disclosuresSeen": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(14, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            default: return null;
          }
        }
        , (Object[] args) -> new LiquidityMandate(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10]), JsonLfDecoders.cast(args[11]), JsonLfDecoders.cast(args[12]), JsonLfDecoders.cast(args[13]), JsonLfDecoders.cast(args[14])));
  }

  public static LiquidityMandate fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("provider", apply(JsonLfEncoders::party, provider)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("anchorPrice", apply(JsonLfEncoders::numeric, anchorPrice)),
        JsonLfEncoders.Field.of("commitmentSize", apply(JsonLfEncoders::numeric, commitmentSize)),
        JsonLfEncoders.Field.of("maxBandBps", apply(JsonLfEncoders::int64, maxBandBps)),
        JsonLfEncoders.Field.of("expiresAt", apply(JsonLfEncoders::timestamp, expiresAt)),
        JsonLfEncoders.Field.of("acceptedAt", apply(JsonLfEncoders::timestamp, acceptedAt)),
        JsonLfEncoders.Field.of("shownSide", apply(JsonLfEncoders::text, shownSide)),
        JsonLfEncoders.Field.of("peakShownQty", apply(JsonLfEncoders::numeric, peakShownQty)),
        JsonLfEncoders.Field.of("lastShownAt", apply(JsonLfEncoders.optional(JsonLfEncoders::timestamp), lastShownAt)),
        JsonLfEncoders.Field.of("disclosuresSeen", apply(JsonLfEncoders::int64, disclosuresSeen)));
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
    if (!(object instanceof LiquidityMandate)) {
      return false;
    }
    LiquidityMandate other = (LiquidityMandate) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.provider, other.provider) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) &&
        Objects.equals(this.anchorPrice, other.anchorPrice) &&
        Objects.equals(this.commitmentSize, other.commitmentSize) &&
        Objects.equals(this.maxBandBps, other.maxBandBps) &&
        Objects.equals(this.expiresAt, other.expiresAt) &&
        Objects.equals(this.acceptedAt, other.acceptedAt) &&
        Objects.equals(this.shownSide, other.shownSide) &&
        Objects.equals(this.peakShownQty, other.peakShownQty) &&
        Objects.equals(this.lastShownAt, other.lastShownAt) &&
        Objects.equals(this.disclosuresSeen, other.disclosuresSeen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.provider, this.instrumentId,
        this.cashInstrument, this.session, this.anchorPrice, this.commitmentSize, this.maxBandBps,
        this.expiresAt, this.acceptedAt, this.shownSide, this.peakShownQty, this.lastShownAt,
        this.disclosuresSeen);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.liquiditymandate.LiquidityMandate(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.provider, this.instrumentId, this.cashInstrument,
        this.session, this.anchorPrice, this.commitmentSize, this.maxBandBps, this.expiresAt,
        this.acceptedAt, this.shownSide, this.peakShownQty, this.lastShownAt, this.disclosuresSeen);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<LiquidityMandate> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, LiquidityMandate, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<LiquidityMandate> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, LiquidityMandate> {
    public Contract(ContractId id, LiquidityMandate data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, LiquidityMandate> getCompanion() {
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
    default Update<Exercised<MandateOutcome>> exerciseRecordPerformance(RecordPerformance arg) {
      return makeExerciseCmd(CHOICE_RecordPerformance, arg);
    }

    default Update<Exercised<MandateOutcome>> exerciseRecordPerformance(
        SettlementBatch.ContractId batchCid, MandateTerms.ContractId termsCid) {
      return exerciseRecordPerformance(new RecordPerformance(batchCid, termsCid));
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<ContractId>> exerciseNoteDisclosure(NoteDisclosure arg) {
      return makeExerciseCmd(CHOICE_NoteDisclosure, arg);
    }

    default Update<Exercised<ContractId>> exerciseNoteDisclosure(String netSide,
        BigDecimal netQuantity) {
      return exerciseNoteDisclosure(new NoteDisclosure(netSide, netQuantity));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, LiquidityMandate, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<LiquidityMandate> get() {
      return jsonDecoder();
    }
  }
}
