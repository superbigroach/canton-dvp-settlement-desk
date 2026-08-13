package com.lucilla.settlement.model.continuousbook;

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

public final class TradeConfirm extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("#crossdesk", "ContinuousBook", "TradeConfirm");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("504d21e4573fdcb737242ee9149b3e88f1ec7d6bd5a76b5701f4762c36fd8ae4", "ContinuousBook", "TradeConfirm");

  public static final String PACKAGE_ID = "504d21e4573fdcb737242ee9149b3e88f1ec7d6bd5a76b5701f4762c36fd8ae4";

  public static final String PACKAGE_NAME = "crossdesk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {2, 1, 0});

  public static final Choice<TradeConfirm, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final ContractCompanion.WithoutKey<Contract, ContractId, TradeConfirm> COMPANION = 
      new ContractCompanion.WithoutKey<>(new ContractTypeCompanion.Package(TradeConfirm.PACKAGE_ID, TradeConfirm.PACKAGE_NAME, TradeConfirm.PACKAGE_VERSION),
        "com.lucilla.settlement.model.continuousbook.TradeConfirm", TEMPLATE_ID, ContractId::new,
        v -> TradeConfirm.templateValueDecoder().decode(v), TradeConfirm::fromJson, Contract::new,
        List.of(CHOICE_Archive));

  public final String operator;

  public final String auditor;

  public final String trader;

  public final String instrumentId;

  public final String cashInstrument;

  public final BookSide side;

  public final BigDecimal quantity;

  public final BigDecimal price;

  public final BigDecimal cashAmount;

  public final String liquidity;

  public final Instant tradedAt;

  public TradeConfirm(String operator, String auditor, String trader, String instrumentId,
      String cashInstrument, BookSide side, BigDecimal quantity, BigDecimal price,
      BigDecimal cashAmount, String liquidity, Instant tradedAt) {
    this.operator = operator;
    this.auditor = auditor;
    this.trader = trader;
    this.instrumentId = instrumentId;
    this.cashInstrument = cashInstrument;
    this.side = side;
    this.quantity = quantity;
    this.price = price;
    this.cashAmount = cashAmount;
    this.liquidity = liquidity;
    this.tradedAt = tradedAt;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(TradeConfirm.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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

  public static Update<Created<ContractId>> create(String operator, String auditor, String trader,
      String instrumentId, String cashInstrument, BookSide side, BigDecimal quantity,
      BigDecimal price, BigDecimal cashAmount, String liquidity, Instant tradedAt) {
    return new TradeConfirm(operator, auditor, trader, instrumentId, cashInstrument, side, quantity,
        price, cashAmount, liquidity, tradedAt).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, TradeConfirm> getCompanion() {
    return COMPANION;
  }

  public static ValueDecoder<TradeConfirm> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(11);
    fields.add(new DamlRecord.Field("operator", new Party(this.operator)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("trader", new Party(this.trader)));
    fields.add(new DamlRecord.Field("instrumentId", new Text(this.instrumentId)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("side", this.side.toValue()));
    fields.add(new DamlRecord.Field("quantity", new Numeric(this.quantity)));
    fields.add(new DamlRecord.Field("price", new Numeric(this.price)));
    fields.add(new DamlRecord.Field("cashAmount", new Numeric(this.cashAmount)));
    fields.add(new DamlRecord.Field("liquidity", new Text(this.liquidity)));
    fields.add(new DamlRecord.Field("tradedAt", Timestamp.fromInstant(this.tradedAt)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<TradeConfirm> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(11,0, recordValue$);
      String operator = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String trader = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      String instrumentId = PrimitiveValueDecoders.fromText.decode(fields$.get(3).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      BookSide side = BookSide.valueDecoder().decode(fields$.get(5).getValue());
      BigDecimal quantity = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      BigDecimal price = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(7).getValue());
      BigDecimal cashAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(8).getValue());
      String liquidity = PrimitiveValueDecoders.fromText.decode(fields$.get(9).getValue());
      Instant tradedAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(10).getValue());
      return new TradeConfirm(operator, auditor, trader, instrumentId, cashInstrument, side,
          quantity, price, cashAmount, liquidity, tradedAt);
    } ;
  }

  public static JsonLfDecoder<TradeConfirm> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("operator", "auditor", "trader", "instrumentId", "cashInstrument", "side", "quantity", "price", "cashAmount", "liquidity", "tradedAt"), name -> {
          switch (name) {
            case "operator": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "trader": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrumentId": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "side": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, new com.lucilla.settlement.model.continuousbook.BookSide.JsonDecoder$().get());
            case "quantity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "price": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "cashAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "liquidity": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "tradedAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(10, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new TradeConfirm(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9]), JsonLfDecoders.cast(args[10])));
  }

  public static TradeConfirm fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("operator", apply(JsonLfEncoders::party, operator)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("trader", apply(JsonLfEncoders::party, trader)),
        JsonLfEncoders.Field.of("instrumentId", apply(JsonLfEncoders::text, instrumentId)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("side", apply(BookSide::jsonEncoder, side)),
        JsonLfEncoders.Field.of("quantity", apply(JsonLfEncoders::numeric, quantity)),
        JsonLfEncoders.Field.of("price", apply(JsonLfEncoders::numeric, price)),
        JsonLfEncoders.Field.of("cashAmount", apply(JsonLfEncoders::numeric, cashAmount)),
        JsonLfEncoders.Field.of("liquidity", apply(JsonLfEncoders::text, liquidity)),
        JsonLfEncoders.Field.of("tradedAt", apply(JsonLfEncoders::timestamp, tradedAt)));
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
    if (!(object instanceof TradeConfirm)) {
      return false;
    }
    TradeConfirm other = (TradeConfirm) object;
    return Objects.equals(this.operator, other.operator) &&
        Objects.equals(this.auditor, other.auditor) && Objects.equals(this.trader, other.trader) &&
        Objects.equals(this.instrumentId, other.instrumentId) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.side, other.side) && Objects.equals(this.quantity, other.quantity) &&
        Objects.equals(this.price, other.price) &&
        Objects.equals(this.cashAmount, other.cashAmount) &&
        Objects.equals(this.liquidity, other.liquidity) &&
        Objects.equals(this.tradedAt, other.tradedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.operator, this.auditor, this.trader, this.instrumentId,
        this.cashInstrument, this.side, this.quantity, this.price, this.cashAmount, this.liquidity,
        this.tradedAt);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.continuousbook.TradeConfirm(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.operator, this.auditor, this.trader, this.instrumentId, this.cashInstrument, this.side,
        this.quantity, this.price, this.cashAmount, this.liquidity, this.tradedAt);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TradeConfirm> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TradeConfirm, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<TradeConfirm> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, TradeConfirm> {
    public Contract(ContractId id, TradeConfirm data, Set<String> signatories,
        Set<String> observers) {
      super(id, data, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, TradeConfirm> getCompanion() {
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
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TradeConfirm, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TradeConfirm> get() {
      return jsonDecoder();
    }
  }
}
