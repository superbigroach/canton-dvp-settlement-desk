package com.lucilla.settlement.model.governance;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlCollectors;
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

public final class NavFixing extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "Governance", "NavFixing");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b", "Governance", "NavFixing");

  public static final String PACKAGE_ID = "f442ed0a18dad43b70c730775e6991c2bb8ee6bf01385f7c5325552559cafa9b";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<NavFixing, PublishTo, ContractId> CHOICE_PublishTo = 
      Choice.create("PublishTo", value$ -> value$.toValue(), value$ -> PublishTo.valueDecoder()
        .decode(value$), value$ ->
        new ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()),
        new PublishTo.JsonDecoder$().get(), JsonLfDecoders.contractId(ContractId::new),
        PublishTo::jsonEncoder, JsonLfEncoders::contractId);

  public static final Choice<NavFixing, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, NavFixing> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(NavFixing.PACKAGE_ID, NavFixing.PACKAGE_NAME, NavFixing.PACKAGE_VERSION),
        "com.lucilla.settlement.model.governance.NavFixing", TEMPLATE_ID, ContractId::new,
        v -> NavFixing.templateValueDecoder().decode(v), NavFixing::fromJson, Contract::new,
        List.of(CHOICE_PublishTo, CHOICE_Archive));

  public final List<String> attestors;

  public final String admin;

  public final String auditor;

  public final Long threshold;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal price;

  public final String rationale;

  public final BigDecimal ratePerAnnum;

  public final String dayCount;

  public final Instant accrualFrom;

  public final List<String> publishedTo;

  public final Instant finalizedAt;

  public final Optional<ContractId> supersedes;

  public final Optional<String> restatementReason;

  public final Optional<BigDecimal> referencePrice;

  public final Optional<BigDecimal> wrapperFactor;

  public final Optional<List<SignerCheck>> attestations;

  public final Optional<String> tier;

  public NavFixing(List<String> attestors, String admin, String auditor, Long threshold,
      String instrumentId, String cashInstrument, String session, BigDecimal price,
      String rationale, BigDecimal ratePerAnnum, String dayCount, Instant accrualFrom,
      List<String> publishedTo, Instant finalizedAt, Optional<ContractId> supersedes,
      Optional<String> restatementReason, Optional<BigDecimal> referencePrice,
      Optional<BigDecimal> wrapperFactor, Optional<List<SignerCheck>> attestations,
      Optional<String> tier) {
    this.attestors = attestors;
    this.admin = admin;
    this.auditor = auditor;
    this.threshold = threshold;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.price = price;
    this.rationale = rationale;
    this.ratePerAnnum = ratePerAnnum;
    this.dayCount = dayCount;
    this.accrualFrom = accrualFrom;
    this.publishedTo = publishedTo;
    this.finalizedAt = finalizedAt;
    this.supersedes = supersedes;
    this.restatementReason = restatementReason;
    this.referencePrice = referencePrice;
    this.wrapperFactor = wrapperFactor;
    this.attestations = attestations;
    this.tier = tier;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(NavFixing.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePublishTo} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExercisePublishTo(PublishTo arg) {
    return createAnd().exercisePublishTo(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exercisePublishTo} instead
   */
  @Deprecated
  public Update<Exercised<ContractId>> createAndExercisePublishTo(String attestor, String venue) {
    return createAndExercisePublishTo(new PublishTo(attestor, venue));
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

  public static Update<Created<ContractId>> create(List<String> attestors, String admin,
      String auditor, Long threshold, String instrumentId, String cashInstrument, String session,
      BigDecimal price, String rationale, BigDecimal ratePerAnnum, String dayCount,
      Instant accrualFrom, List<String> publishedTo, Instant finalizedAt,
      Optional<ContractId> supersedes, Optional<String> restatementReason,
      Optional<BigDecimal> referencePrice, Optional<BigDecimal> wrapperFactor,
      Optional<List<SignerCheck>> attestations, Optional<String> tier) {
    return new NavFixing(attestors, admin, auditor, threshold, instrumentId, cashInstrument,
        session, price, rationale, ratePerAnnum, dayCount, accrualFrom, publishedTo, finalizedAt,
        supersedes, restatementReason, referencePrice, wrapperFactor, attestations, tier).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, NavFixing> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<NavFixing> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(20);
    fields.add(new DamlRecord.Field("attestors", this.attestors.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("admin", new Party(this.admin)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("threshold", new Int64(this.threshold)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("price", new Numeric(this.price)));
    fields.add(new DamlRecord.Field("rationale", new Text(this.rationale)));
    fields.add(new DamlRecord.Field("ratePerAnnum", new Numeric(this.ratePerAnnum)));
    fields.add(new DamlRecord.Field("dayCount", new Text(this.dayCount)));
    fields.add(new DamlRecord.Field("accrualFrom", Timestamp.fromInstant(this.accrualFrom)));
    fields.add(new DamlRecord.Field("publishedTo", this.publishedTo.stream().collect(DamlCollectors.toDamlList(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("finalizedAt", Timestamp.fromInstant(this.finalizedAt)));
    fields.add(new DamlRecord.Field("supersedes", DamlOptional.of(this.supersedes.map(v$0 -> v$0.toValue()))));
    fields.add(new DamlRecord.Field("restatementReason", DamlOptional.of(this.restatementReason.map(v$0 -> new Text(v$0)))));
    fields.add(new DamlRecord.Field("referencePrice", DamlOptional.of(this.referencePrice.map(v$0 -> new Numeric(v$0)))));
    fields.add(new DamlRecord.Field("wrapperFactor", DamlOptional.of(this.wrapperFactor.map(v$0 -> new Numeric(v$0)))));
    fields.add(new DamlRecord.Field("attestations", DamlOptional.of(this.attestations.map(v$0 -> v$0.stream().collect(DamlCollectors.toDamlList(v$1 -> v$1.toValue()))))));
    fields.add(new DamlRecord.Field("tier", DamlOptional.of(this.tier.map(v$0 -> new Text(v$0)))));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<NavFixing> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(20,6, recordValue$);
      List<String> attestors = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(0).getValue());
      String admin = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      Long threshold = PrimitiveValueDecoders.fromInt64.decode(fields$.get(3).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(6).getValue());
      BigDecimal price = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(7).getValue());
      String rationale = PrimitiveValueDecoders.fromText.decode(fields$.get(8).getValue());
      BigDecimal ratePerAnnum = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(9).getValue());
      String dayCount = PrimitiveValueDecoders.fromText.decode(fields$.get(10).getValue());
      Instant accrualFrom = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(11).getValue());
      List<String> publishedTo = PrimitiveValueDecoders.fromList(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(12).getValue());
      Instant finalizedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(13).getValue());
      Optional<ContractId> supersedes = PrimitiveValueDecoders.fromOptional(v$0 ->
              new ContractId(v$0.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected supersedes to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()))
          .decode(fields$.get(14).getValue());
      Optional<String> restatementReason = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromText).decode(fields$.get(15).getValue());
      Optional<BigDecimal> referencePrice = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromNumeric).decode(fields$.get(16).getValue());
      Optional<BigDecimal> wrapperFactor = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromNumeric).decode(fields$.get(17).getValue());
      Optional<List<SignerCheck>> attestations = PrimitiveValueDecoders.fromOptional(
            PrimitiveValueDecoders.fromList(SignerCheck.valueDecoder()))
          .decode(fields$.get(18).getValue());
      Optional<String> tier = PrimitiveValueDecoders.fromOptional(PrimitiveValueDecoders.fromText)
          .decode(fields$.get(19).getValue());
      return new NavFixing(attestors, admin, auditor, threshold, instrumentId, cashInstrument,
          session, price, rationale, ratePerAnnum, dayCount, accrualFrom, publishedTo, finalizedAt,
          supersedes, restatementReason, referencePrice, wrapperFactor, attestations, tier);
    } ;
  }

  public static JsonLfDecoder<NavFixing> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("attestors", "admin", "auditor", "threshold", "instrumentId", "cashInstrument", "session", "price", "rationale", "ratePerAnnum", "dayCount", "accrualFrom", "publishedTo", "finalizedAt", "supersedes", "restatementReason", "referencePrice", "wrapperFactor", "attestations", "tier"), name -> {
          switch (name) {
            case "attestors": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "admin": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "threshold": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "price": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "rationale": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "ratePerAnnum": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "dayCount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "accrualFrom": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(11, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "publishedTo": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(12, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "finalizedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(13, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            case "supersedes": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(14, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.governance.NavFixing.ContractId::new)), java.util.Optional.empty());
            case "restatementReason": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(15, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text), java.util.Optional.empty());
            case "referencePrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(16, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10)), java.util.Optional.empty());
            case "wrapperFactor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(17, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10)), java.util.Optional.empty());
            case "attestations": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(18, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.list(new com.lucilla.settlement.model.governance.SignerCheck.JsonDecoder$().get())), java.util.Optional.empty());
            case "tier": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(19, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text), java.util.Optional.empty());
            default: return null;
          }
        }
        , (Object[] args) -> new NavFixing(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10]), JsonLfDecoders.cast(args[11]), JsonLfDecoders.cast(args[12]), JsonLfDecoders.cast(args[13]), JsonLfDecoders.cast(args[14]), JsonLfDecoders.cast(args[15]), JsonLfDecoders.cast(args[16]), JsonLfDecoders.cast(args[17]), JsonLfDecoders.cast(args[18]), JsonLfDecoders.cast(args[19])));
  }

  public static NavFixing fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("attestors", apply(JsonLfEncoders.list(JsonLfEncoders::party), attestors)),
        JsonLfEncoders.Field.of("admin", apply(JsonLfEncoders::party, admin)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("threshold", apply(JsonLfEncoders::int64, threshold)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("price", apply(JsonLfEncoders::numeric, price)),
        JsonLfEncoders.Field.of("rationale", apply(JsonLfEncoders::text, rationale)),
        JsonLfEncoders.Field.of("ratePerAnnum", apply(JsonLfEncoders::numeric, ratePerAnnum)),
        JsonLfEncoders.Field.of("dayCount", apply(JsonLfEncoders::text, dayCount)),
        JsonLfEncoders.Field.of("accrualFrom", apply(JsonLfEncoders::timestamp, accrualFrom)),
        JsonLfEncoders.Field.of("publishedTo", apply(JsonLfEncoders.list(JsonLfEncoders::party), publishedTo)),
        JsonLfEncoders.Field.of("finalizedAt", apply(JsonLfEncoders::timestamp, finalizedAt)),
        JsonLfEncoders.Field.of("supersedes", apply(JsonLfEncoders.optional(JsonLfEncoders::contractId), supersedes)),
        JsonLfEncoders.Field.of("restatementReason", apply(JsonLfEncoders.optional(JsonLfEncoders::text), restatementReason)),
        JsonLfEncoders.Field.of("referencePrice", apply(JsonLfEncoders.optional(JsonLfEncoders::numeric), referencePrice)),
        JsonLfEncoders.Field.of("wrapperFactor", apply(JsonLfEncoders.optional(JsonLfEncoders::numeric), wrapperFactor)),
        JsonLfEncoders.Field.of("attestations", apply(JsonLfEncoders.optional(JsonLfEncoders.list(SignerCheck::jsonEncoder)), attestations)),
        JsonLfEncoders.Field.of("tier", apply(JsonLfEncoders.optional(JsonLfEncoders::text), tier)));
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
    if (!(object instanceof NavFixing)) {
      return false;
    }
    NavFixing other = (NavFixing) object;
    return Objects.equals(this.attestors, other.attestors) &&
        Objects.equals(this.admin, other.admin) && Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.threshold, other.threshold) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) && Objects.equals(this.price, other.price) &&
        Objects.equals(this.rationale, other.rationale) &&
        Objects.equals(this.ratePerAnnum, other.ratePerAnnum) &&
        Objects.equals(this.dayCount, other.dayCount) &&
        Objects.equals(this.accrualFrom, other.accrualFrom) &&
        Objects.equals(this.publishedTo, other.publishedTo) &&
        Objects.equals(this.finalizedAt, other.finalizedAt) &&
        Objects.equals(this.supersedes, other.supersedes) &&
        Objects.equals(this.restatementReason, other.restatementReason) &&
        Objects.equals(this.referencePrice, other.referencePrice) &&
        Objects.equals(this.wrapperFactor, other.wrapperFactor) &&
        Objects.equals(this.attestations, other.attestations) &&
        Objects.equals(this.tier, other.tier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.attestors, this.admin, this.auditor, this.threshold, this.instrumentId,
        this.cashInstrument, this.session, this.price, this.rationale, this.ratePerAnnum,
        this.dayCount, this.accrualFrom, this.publishedTo, this.finalizedAt, this.supersedes,
        this.restatementReason, this.referencePrice, this.wrapperFactor, this.attestations,
        this.tier);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.governance.NavFixing(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.attestors, this.admin, this.auditor, this.threshold, this.instrumentId,
        this.cashInstrument, this.session, this.price, this.rationale, this.ratePerAnnum,
        this.dayCount, this.accrualFrom, this.publishedTo, this.finalizedAt, this.supersedes,
        this.restatementReason, this.referencePrice, this.wrapperFactor, this.attestations,
        this.tier);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<NavFixing> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, NavFixing, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<NavFixing> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, NavFixing> {
    public Contract(ContractId id, NavFixing data, Set<String> signatories, Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, NavFixing> getCompanion() {
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
    default Update<Exercised<ContractId>> exercisePublishTo(PublishTo arg) {
      return makeExerciseCmd(CHOICE_PublishTo, arg);
    }

    default Update<Exercised<ContractId>> exercisePublishTo(String attestor, String venue) {
      return exercisePublishTo(new PublishTo(attestor, venue));
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, NavFixing, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<NavFixing> get() {
      return jsonDecoder();
    }
  }
}
