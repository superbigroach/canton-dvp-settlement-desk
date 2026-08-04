package com.lucilla.settlement.model.liquiditymandate;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.Bool;
import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
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
import java.lang.Boolean;
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

public final class MandatePerformance extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#canton-dvp-settlement-desk", "LiquidityMandate", "MandatePerformance");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d", "LiquidityMandate", "MandatePerformance");

  public static final String PACKAGE_ID = "147ddae1818ea7e3662c51714525ac4d6de9c853914d723962bb7ed563ad363d";

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<MandatePerformance, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, MandatePerformance> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(MandatePerformance.PACKAGE_ID, MandatePerformance.PACKAGE_NAME, MandatePerformance.PACKAGE_VERSION),
        "com.lucilla.settlement.model.liquiditymandate.MandatePerformance", TEMPLATE_ID,
        ContractId::new, v -> MandatePerformance.templateValueDecoder().decode(v),
        MandatePerformance::fromJson, Contract::new, List.of(CHOICE_Archive));

  public final String operator;

  public final String auditor;

  public final String provider;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final BigDecimal anchorPrice;

  public final BigDecimal printedPrice;

  public final Long maxBandBps;

  public final Boolean withinBand;

  public final String shownSide;

  public final BigDecimal shownQty;

  public final Long disclosuresSeen;

  public final BigDecimal committed;

  public final BigDecimal owed;

  public final BigDecimal delivered;

  public final Boolean met;

  public final SettlementBatch.ContractId batchRef;

  public final Instant recordedAt;

  public MandatePerformance(String operator, String auditor, String provider, String instrumentId,
      String cashInstrument, String session, BigDecimal anchorPrice, BigDecimal printedPrice,
      Long maxBandBps, Boolean withinBand, String shownSide, BigDecimal shownQty,
      Long disclosuresSeen, BigDecimal committed, BigDecimal owed, BigDecimal delivered,
      Boolean met, SettlementBatch.ContractId batchRef, Instant recordedAt) {
    this.operator = operator;
    this.auditor = auditor;
    this.provider = provider;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.anchorPrice = anchorPrice;
    this.printedPrice = printedPrice;
    this.maxBandBps = maxBandBps;
    this.withinBand = withinBand;
    this.shownSide = shownSide;
    this.shownQty = shownQty;
    this.disclosuresSeen = disclosuresSeen;
    this.committed = committed;
    this.owed = owed;
    this.delivered = delivered;
    this.met = met;
    this.batchRef = batchRef;
    this.recordedAt = recordedAt;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(MandatePerformance.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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

  public static Update<Created<ContractId>> create(String operator, String auditor, String provider,
      String instrumentId, String cashInstrument, String session, BigDecimal anchorPrice,
      BigDecimal printedPrice, Long maxBandBps, Boolean withinBand, String shownSide,
      BigDecimal shownQty, Long disclosuresSeen, BigDecimal committed, BigDecimal owed,
      BigDecimal delivered, Boolean met, SettlementBatch.ContractId batchRef, Instant recordedAt) {
    return new MandatePerformance(operator, auditor, provider, instrumentId, cashInstrument,
        session, anchorPrice, printedPrice, maxBandBps, withinBand, shownSide, shownQty,
        disclosuresSeen, committed, owed, delivered, met, batchRef, recordedAt).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, MandatePerformance> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<MandatePerformance> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(19);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("provider", new Party(this.provider)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("anchorPrice", new Numeric(this.anchorPrice)));
    fields.add(new DamlRecord.Field("printedPrice", new Numeric(this.printedPrice)));
    fields.add(new DamlRecord.Field("maxBandBps", new Int64(this.maxBandBps)));
    fields.add(new DamlRecord.Field("withinBand", Bool.of(this.withinBand)));
    fields.add(new DamlRecord.Field("shownSide", new Text(this.shownSide)));
    fields.add(new DamlRecord.Field("shownQty", new Numeric(this.shownQty)));
    fields.add(new DamlRecord.Field("disclosuresSeen", new Int64(this.disclosuresSeen)));
    fields.add(new DamlRecord.Field("committed", new Numeric(this.committed)));
    fields.add(new DamlRecord.Field("owed", new Numeric(this.owed)));
    fields.add(new DamlRecord.Field("delivered", new Numeric(this.delivered)));
    fields.add(new DamlRecord.Field("met", Bool.of(this.met)));
    fields.add(new DamlRecord.Field("batchRef", this.batchRef.toValue()));
    fields.add(new DamlRecord.Field("recordedAt", Timestamp.fromInstant(this.recordedAt)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<MandatePerformance> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(19,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String provider = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      BigDecimal anchorPrice = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      BigDecimal printedPrice = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(7).getValue());
      Long maxBandBps = PrimitiveValueDecoders.fromInt64.decode(fields$.get(8).getValue());
      Boolean withinBand = PrimitiveValueDecoders.fromBool.decode(fields$.get(9).getValue());
      String shownSide = PrimitiveValueDecoders.fromText.decode(fields$.get(10).getValue());
      BigDecimal shownQty = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(11).getValue());
      Long disclosuresSeen = PrimitiveValueDecoders.fromInt64.decode(fields$.get(12).getValue());
      BigDecimal committed = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(13).getValue());
      BigDecimal owed = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(14).getValue());
      BigDecimal delivered = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(15).getValue());
      Boolean met = PrimitiveValueDecoders.fromBool.decode(fields$.get(16).getValue());
      SettlementBatch.ContractId batchRef =
          new SettlementBatch.ContractId(fields$.get(17).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected batchRef to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Instant recordedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(18).getValue());
      return new MandatePerformance(operator, auditor, provider, instrumentId, cashInstrument,
          session, anchorPrice, printedPrice, maxBandBps, withinBand, shownSide, shownQty,
          disclosuresSeen, committed, owed, delivered, met, batchRef, recordedAt);
    } ;
  }

  public static JsonLfDecoder<MandatePerformance> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "provider", "instrumentId", "cashInstrument", "session", "anchorPrice", "printedPrice", "maxBandBps", "withinBand", "shownSide", "shownQty", "disclosuresSeen", "committed", "owed", "delivered", "met", "batchRef", "recordedAt"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "provider": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "anchorPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "printedPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "maxBandBps": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "withinBand": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.bool);
            case "shownSide": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "shownQty": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(11, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "disclosuresSeen": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(12, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.int64);
            case "committed": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(13, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "owed": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(14, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "delivered": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(15, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "met": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(16, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.bool);
            case "batchRef": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(17, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.settlement.SettlementBatch.ContractId::new));
            case "recordedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(18, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new MandatePerformance(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10]), JsonLfDecoders.cast(args[11]), JsonLfDecoders.cast(args[12]), JsonLfDecoders.cast(args[13]), JsonLfDecoders.cast(args[14]), JsonLfDecoders.cast(args[15]), JsonLfDecoders.cast(args[16]), JsonLfDecoders.cast(args[17]), JsonLfDecoders.cast(args[18])));
  }

  public static MandatePerformance fromJson(String json) throws JsonLfDecoder.Error {
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
        JsonLfEncoders.Field.of("printedPrice", apply(JsonLfEncoders::numeric, printedPrice)),
        JsonLfEncoders.Field.of("maxBandBps", apply(JsonLfEncoders::int64, maxBandBps)),
        JsonLfEncoders.Field.of("withinBand", apply(JsonLfEncoders::bool, withinBand)),
        JsonLfEncoders.Field.of("shownSide", apply(JsonLfEncoders::text, shownSide)),
        JsonLfEncoders.Field.of("shownQty", apply(JsonLfEncoders::numeric, shownQty)),
        JsonLfEncoders.Field.of("disclosuresSeen", apply(JsonLfEncoders::int64, disclosuresSeen)),
        JsonLfEncoders.Field.of("committed", apply(JsonLfEncoders::numeric, committed)),
        JsonLfEncoders.Field.of("owed", apply(JsonLfEncoders::numeric, owed)),
        JsonLfEncoders.Field.of("delivered", apply(JsonLfEncoders::numeric, delivered)),
        JsonLfEncoders.Field.of("met", apply(JsonLfEncoders::bool, met)),
        JsonLfEncoders.Field.of("batchRef", apply(JsonLfEncoders::contractId, batchRef)),
        JsonLfEncoders.Field.of("recordedAt", apply(JsonLfEncoders::timestamp, recordedAt)));
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
    if (!(object instanceof MandatePerformance)) {
      return false;
    }
    MandatePerformance other = (MandatePerformance) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.provider, other.provider) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) &&
        Objects.equals(this.anchorPrice, other.anchorPrice) &&
        Objects.equals(this.printedPrice, other.printedPrice) &&
        Objects.equals(this.maxBandBps, other.maxBandBps) &&
        Objects.equals(this.withinBand, other.withinBand) &&
        Objects.equals(this.shownSide, other.shownSide) &&
        Objects.equals(this.shownQty, other.shownQty) &&
        Objects.equals(this.disclosuresSeen, other.disclosuresSeen) &&
        Objects.equals(this.committed, other.committed) && Objects.equals(this.owed, other.owed) &&
        Objects.equals(this.delivered, other.delivered) && Objects.equals(this.met, other.met) &&
        Objects.equals(this.batchRef, other.batchRef) &&
        Objects.equals(this.recordedAt, other.recordedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.provider, this.instrumentId,
        this.cashInstrument, this.session, this.anchorPrice, this.printedPrice, this.maxBandBps,
        this.withinBand, this.shownSide, this.shownQty, this.disclosuresSeen, this.committed,
        this.owed, this.delivered, this.met, this.batchRef, this.recordedAt);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.liquiditymandate.MandatePerformance(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.provider, this.instrumentId, this.cashInstrument,
        this.session, this.anchorPrice, this.printedPrice, this.maxBandBps, this.withinBand,
        this.shownSide, this.shownQty, this.disclosuresSeen, this.committed, this.owed,
        this.delivered, this.met, this.batchRef, this.recordedAt);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<MandatePerformance> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, MandatePerformance, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<MandatePerformance> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, MandatePerformance> {
    public Contract(ContractId id, MandatePerformance data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, MandatePerformance> getCompanion() {
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, MandatePerformance, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<MandatePerformance> get() {
      return jsonDecoder();
    }
  }
}
