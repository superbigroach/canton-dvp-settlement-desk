package com.lucilla.settlement.model.marketonclose;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
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

public final class ImbalanceDisclosure extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#canton-dvp-settlement-desk", "MarketOnClose", "ImbalanceDisclosure");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12", "MarketOnClose", "ImbalanceDisclosure");

  public static final String PACKAGE_ID = "f10d37a10d40ff7923e1d7476f49347809a28a7803b3be0c4252b2417f921d12";

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<ImbalanceDisclosure, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, ImbalanceDisclosure> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(ImbalanceDisclosure.PACKAGE_ID, ImbalanceDisclosure.PACKAGE_NAME, ImbalanceDisclosure.PACKAGE_VERSION),
        "com.lucilla.settlement.model.marketonclose.ImbalanceDisclosure", TEMPLATE_ID,
        ContractId::new, v -> ImbalanceDisclosure.templateValueDecoder().decode(v),
        ImbalanceDisclosure::fromJson, Contract::new, List.of(CHOICE_Archive));

  public final String operator;

  public final String liquidityProvider;

  public final String instrumentId;

  public final String cashInstrument;

  public final String session;

  public final String netSide;

  public final BigDecimal netQuantity;

  public final BigDecimal referencePrice;

  public final Instant computedAt;

  public ImbalanceDisclosure(String operator, String liquidityProvider, String instrumentId,
      String cashInstrument, String session, String netSide, BigDecimal netQuantity,
      BigDecimal referencePrice, Instant computedAt) {
    this.operator = operator;
    this.liquidityProvider = liquidityProvider;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.session = session;
    this.netSide = netSide;
    this.netQuantity = netQuantity;
    this.referencePrice = referencePrice;
    this.computedAt = computedAt;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(ImbalanceDisclosure.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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

  public static Update<Created<ContractId>> create(String operator, String liquidityProvider,
      String instrumentId, String cashInstrument, String session, String netSide,
      BigDecimal netQuantity, BigDecimal referencePrice, Instant computedAt) {
    return new ImbalanceDisclosure(operator, liquidityProvider, instrumentId, cashInstrument,
        session, netSide, netQuantity, referencePrice, computedAt).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, ImbalanceDisclosure> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<ImbalanceDisclosure> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(9);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("liquidityProvider", new Party(this.liquidityProvider)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("session", new Text(this.session)));
    fields.add(new DamlRecord.Field("netSide", new Text(this.netSide)));
    fields.add(new DamlRecord.Field("netQuantity", new Numeric(this.netQuantity)));
    fields.add(new DamlRecord.Field("referencePrice", new Numeric(this.referencePrice)));
    fields.add(new DamlRecord.Field("computedAt", Timestamp.fromInstant(this.computedAt)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<ImbalanceDisclosure> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(9,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String liquidityProvider = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String session = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      String netSide = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      BigDecimal netQuantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      BigDecimal referencePrice = PrimitiveValueDecoders.fromNumeric
          .decode(fields$.get(7).getValue());
      Instant computedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(8).getValue());
      return new ImbalanceDisclosure(operator, liquidityProvider, instrumentId, cashInstrument,
          session, netSide, netQuantity, referencePrice, computedAt);
    } ;
  }

  public static JsonLfDecoder<ImbalanceDisclosure> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "liquidityProvider", "instrumentId", "cashInstrument", "session", "netSide", "netQuantity", "referencePrice", "computedAt"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "liquidityProvider": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "session": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "netSide": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "netQuantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "referencePrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "computedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new ImbalanceDisclosure(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8])));
  }

  public static ImbalanceDisclosure fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("liquidityProvider", apply(JsonLfEncoders::party, liquidityProvider)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("session", apply(JsonLfEncoders::text, session)),
        JsonLfEncoders.Field.of("netSide", apply(JsonLfEncoders::text, netSide)),
        JsonLfEncoders.Field.of("netQuantity", apply(JsonLfEncoders::numeric, netQuantity)),
        JsonLfEncoders.Field.of("referencePrice", apply(JsonLfEncoders::numeric, referencePrice)),
        JsonLfEncoders.Field.of("computedAt", apply(JsonLfEncoders::timestamp, computedAt)));
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
    if (!(object instanceof ImbalanceDisclosure)) {
      return false;
    }
    ImbalanceDisclosure other = (ImbalanceDisclosure) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.liquidityProvider, other.liquidityProvider) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.session, other.session) &&
        Objects.equals(this.netSide, other.netSide) &&
        Objects.equals(this.netQuantity, other.netQuantity) &&
        Objects.equals(this.referencePrice, other.referencePrice) &&
        Objects.equals(this.computedAt, other.computedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.liquidityProvider, this.instrumentId,
        this.cashInstrument, this.session, this.netSide, this.netQuantity, this.referencePrice,
        this.computedAt);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.marketonclose.ImbalanceDisclosure(%s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.liquidityProvider, this.instrumentId, this.cashInstrument, this.session,
        this.netSide, this.netQuantity, this.referencePrice, this.computedAt);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<ImbalanceDisclosure> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, ImbalanceDisclosure, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<ImbalanceDisclosure> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ImbalanceDisclosure> {
    public Contract(ContractId id, ImbalanceDisclosure data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, ImbalanceDisclosure> getCompanion() {
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, ImbalanceDisclosure, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<ImbalanceDisclosure> get() {
      return jsonDecoder();
    }
  }
}
