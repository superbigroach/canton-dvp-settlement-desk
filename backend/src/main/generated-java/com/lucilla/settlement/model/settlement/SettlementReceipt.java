package com.lucilla.settlement.model.settlement;

import static com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders.apply;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.DamlOptional;
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
import java.util.Optional;
import java.util.Set;

public final class SettlementReceipt extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("85b662c2e7d0a4d42bea5a2232989bd04641057da99c8bda47d7f7b912ef699c", "Settlement", "SettlementReceipt");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("85b662c2e7d0a4d42bea5a2232989bd04641057da99c8bda47d7f7b912ef699c", "Settlement", "SettlementReceipt");

  public static final String PACKAGE_ID = "85b662c2e7d0a4d42bea5a2232989bd04641057da99c8bda47d7f7b912ef699c";

  public static final Choice<SettlementReceipt, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, SettlementReceipt> COMPANION = 
      new ContractCompanion.WithoutKey<>(
        "com.lucilla.settlement.model.settlement.SettlementReceipt", TEMPLATE_ID,
        TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> SettlementReceipt.templateValueDecoder().decode(v), SettlementReceipt::fromJson,
        Contract::new, List.of(CHOICE_Archive));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String seller;

  public final String buyer;

  public final Optional<String> venue;

  public final String auditor;

  public final String assetInstrument;

  public final BigDecimal assetAmount;

  public final String cashInstrument;

  public final BigDecimal cashAmount;

  public final BigDecimal unitPrice;

  public final Instant settledAt;

  public SettlementReceipt(String seller, String buyer, Optional<String> venue, String auditor,
      String assetInstrument, BigDecimal assetAmount, String cashInstrument, BigDecimal cashAmount,
      BigDecimal unitPrice, Instant settledAt) {
    this.seller = seller;
    this.buyer = buyer;
    this.venue = venue;
    this.auditor = auditor;
    this.assetInstrument = assetInstrument;
    this.assetAmount = assetAmount;
    this.cashInstrument = cashInstrument;
    this.cashAmount = cashAmount;
    this.unitPrice = unitPrice;
    this.settledAt = settledAt;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(SettlementReceipt.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseArchive} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseArchive(
      com.lucilla.settlement.model.da.internal.template.Archive arg) {
    return createAnd().exerciseArchive(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseArchive} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseArchive() {
    return createAndExerciseArchive(new com.lucilla.settlement.model.da.internal.template.Archive());
  }

  public static Update<Created<ContractId>> create(String seller, String buyer,
      Optional<String> venue, String auditor, String assetInstrument, BigDecimal assetAmount,
      String cashInstrument, BigDecimal cashAmount, BigDecimal unitPrice, Instant settledAt) {
    return new SettlementReceipt(seller, buyer, venue, auditor, assetInstrument, assetAmount,
        cashInstrument, cashAmount, unitPrice, settledAt).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, SettlementReceipt> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static SettlementReceipt fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<SettlementReceipt> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(10);
    fields.add(new DamlRecord.Field("seller", new Party(this.seller)));
    fields.add(new DamlRecord.Field("buyer", new Party(this.buyer)));
    fields.add(new DamlRecord.Field("venue", DamlOptional.of(this.venue.map(v$0 -> new Party(v$0)))));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("assetInstrument", new Text(this.assetInstrument)));
    fields.add(new DamlRecord.Field("assetAmount", new Numeric(this.assetAmount)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("cashAmount", new Numeric(this.cashAmount)));
    fields.add(new DamlRecord.Field("unitPrice", new Numeric(this.unitPrice)));
    fields.add(new DamlRecord.Field("settledAt", Timestamp.fromInstant(this.settledAt)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<SettlementReceipt> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(10,0, recordValue$);
      String seller = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String buyer = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      Optional<String> venue = PrimitiveValueDecoders.fromOptional(PrimitiveValueDecoders.fromParty)
          .decode(fields$.get(2).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(3).getValue());
      String assetInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(4).getValue());
      BigDecimal assetAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(5).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(6).getValue());
      BigDecimal cashAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(7).getValue());
      BigDecimal unitPrice = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(8).getValue());
      Instant settledAt = PrimitiveValueDecoders.fromTimestamp.decode(fields$.get(9).getValue());
      return new SettlementReceipt(seller, buyer, venue, auditor, assetInstrument, assetAmount,
          cashInstrument, cashAmount, unitPrice, settledAt);
    } ;
  }

  public static JsonLfDecoder<SettlementReceipt> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("seller", "buyer", "venue", "auditor", "assetInstrument", "assetAmount", "cashInstrument", "cashAmount", "unitPrice", "settledAt"), name -> {
          switch (name) {
            case "seller": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "buyer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "venue": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.optional(com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party));
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "assetInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "assetAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "unitPrice": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "settledAt": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(9, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.timestamp);
            default: return null;
          }
        }
        , (Object[] args) -> new SettlementReceipt(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8]), JsonLfDecoders.cast(args[9])));
  }

  public static SettlementReceipt fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("seller", apply(JsonLfEncoders::party, seller)),
        JsonLfEncoders.Field.of("buyer", apply(JsonLfEncoders::party, buyer)),
        JsonLfEncoders.Field.of("venue", apply(JsonLfEncoders.optional(JsonLfEncoders::party), venue)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("assetInstrument", apply(JsonLfEncoders::text, assetInstrument)),
        JsonLfEncoders.Field.of("assetAmount", apply(JsonLfEncoders::numeric, assetAmount)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("cashAmount", apply(JsonLfEncoders::numeric, cashAmount)),
        JsonLfEncoders.Field.of("unitPrice", apply(JsonLfEncoders::numeric, unitPrice)),
        JsonLfEncoders.Field.of("settledAt", apply(JsonLfEncoders::timestamp, settledAt)));
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
    if (!(object instanceof SettlementReceipt)) {
      return false;
    }
    SettlementReceipt other = (SettlementReceipt) object;
    return Objects.equals(this.seller, other.seller) && Objects.equals(this.buyer, other.buyer) &&
        Objects.equals(this.venue, other.venue) && Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.assetInstrument, other.assetInstrument) &&
        Objects.equals(this.assetAmount, other.assetAmount) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.cashAmount, other.cashAmount) &&
        Objects.equals(this.unitPrice, other.unitPrice) &&
        Objects.equals(this.settledAt, other.settledAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.seller, this.buyer, this.venue, this.auditor, this.assetInstrument,
        this.assetAmount, this.cashInstrument, this.cashAmount, this.unitPrice, this.settledAt);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.settlement.SettlementReceipt(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.seller, this.buyer, this.venue, this.auditor, this.assetInstrument, this.assetAmount,
        this.cashInstrument, this.cashAmount, this.unitPrice, this.settledAt);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<SettlementReceipt> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, SettlementReceipt, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<SettlementReceipt> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, SettlementReceipt> {
    public Contract(ContractId id, SettlementReceipt data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, SettlementReceipt> getCompanion() {
      return COMPANION;
    }

    public static Contract fromIdAndRecord(String contractId, DamlRecord record$,
        Optional<String> agreementText, Set<String> signatories, Set<String> observers) {
      return COMPANION.fromIdAndRecord(contractId, record$, agreementText, signatories, observers);
    }

    public static Contract fromCreatedEvent(CreatedEvent event) {
      return COMPANION.fromCreatedEvent(event);
    }
  }

  public interface Exercises<Cmd> extends com.daml.ledger.javaapi.data.codegen.Exercises.Archive<Cmd> {
    default Update<Exercised<Unit>> exerciseArchive(
        com.lucilla.settlement.model.da.internal.template.Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new com.lucilla.settlement.model.da.internal.template.Archive());
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, SettlementReceipt, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<SettlementReceipt> get() {
      return jsonDecoder();
    }
  }
}
