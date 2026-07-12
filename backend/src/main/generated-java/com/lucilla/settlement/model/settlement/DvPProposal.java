package com.lucilla.settlement.model.settlement;

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
import com.lucilla.settlement.model.holding.Holding;
import java.lang.Deprecated;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DvPProposal extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("686f466cc26deeb74b73fb7f5ee448959dec397b966196641a6e0925ac652c79", "Settlement", "DvPProposal");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("686f466cc26deeb74b73fb7f5ee448959dec397b966196641a6e0925ac652c79", "Settlement", "DvPProposal");

  public static final String PACKAGE_ID = "686f466cc26deeb74b73fb7f5ee448959dec397b966196641a6e0925ac652c79";

  public static final Choice<DvPProposal, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<DvPProposal, Withdraw, Unit> CHOICE_Withdraw = 
      Choice.create("Withdraw", value$ -> value$.toValue(), value$ -> Withdraw.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<DvPProposal, Reject, Unit> CHOICE_Reject = 
      Choice.create("Reject", value$ -> value$.toValue(), value$ -> Reject.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<DvPProposal, Accept, DvPAgreement.ContractId> CHOICE_Accept = 
      Choice.create("Accept", value$ -> value$.toValue(), value$ -> Accept.valueDecoder()
        .decode(value$), value$ ->
        new DvPAgreement.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, DvPProposal> COMPANION = 
      new ContractCompanion.WithoutKey<>("com.lucilla.settlement.model.settlement.DvPProposal",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> DvPProposal.templateValueDecoder().decode(v), DvPProposal::fromJson, Contract::new,
        List.of(CHOICE_Archive, CHOICE_Withdraw, CHOICE_Reject, CHOICE_Accept));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String proposer;

  public final String counterparty;

  public final String auditor;

  public final Holding.ContractId assetHoldingCid;

  public final Holding.ContractId cashHoldingCid;

  public final String assetInstrument;

  public final BigDecimal assetAmount;

  public final String cashInstrument;

  public final BigDecimal cashAmount;

  public DvPProposal(String proposer, String counterparty, String auditor,
      Holding.ContractId assetHoldingCid, Holding.ContractId cashHoldingCid, String assetInstrument,
      BigDecimal assetAmount, String cashInstrument, BigDecimal cashAmount) {
    this.proposer = proposer;
    this.counterparty = counterparty;
    this.auditor = auditor;
    this.assetHoldingCid = assetHoldingCid;
    this.cashHoldingCid = cashHoldingCid;
    this.assetInstrument = assetInstrument;
    this.assetAmount = assetAmount;
    this.cashInstrument = cashInstrument;
    this.cashAmount = cashAmount;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(DvPProposal.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
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

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseWithdraw} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseWithdraw(Withdraw arg) {
    return createAnd().exerciseWithdraw(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseWithdraw} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseWithdraw() {
    return createAndExerciseWithdraw(new Withdraw());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseReject} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseReject(Reject arg) {
    return createAnd().exerciseReject(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseReject} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseReject() {
    return createAndExerciseReject(new Reject());
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAccept} instead
   */
  @Deprecated
  public Update<Exercised<DvPAgreement.ContractId>> createAndExerciseAccept(Accept arg) {
    return createAnd().exerciseAccept(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseAccept} instead
   */
  @Deprecated
  public Update<Exercised<DvPAgreement.ContractId>> createAndExerciseAccept() {
    return createAndExerciseAccept(new Accept());
  }

  public static Update<Created<ContractId>> create(String proposer, String counterparty,
      String auditor, Holding.ContractId assetHoldingCid, Holding.ContractId cashHoldingCid,
      String assetInstrument, BigDecimal assetAmount, String cashInstrument,
      BigDecimal cashAmount) {
    return new DvPProposal(proposer, counterparty, auditor, assetHoldingCid, cashHoldingCid,
        assetInstrument, assetAmount, cashInstrument, cashAmount).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, DvPProposal> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static DvPProposal fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<DvPProposal> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(9);
    fields.add(new DamlRecord.Field("proposer", new Party(this.proposer)));
    fields.add(new DamlRecord.Field("counterparty", new Party(this.counterparty)));
    fields.add(new DamlRecord.Field("auditor", new Party(this.auditor)));
    fields.add(new DamlRecord.Field("assetHoldingCid", this.assetHoldingCid.toValue()));
    fields.add(new DamlRecord.Field("cashHoldingCid", this.cashHoldingCid.toValue()));
    fields.add(new DamlRecord.Field("assetInstrument", new Text(this.assetInstrument)));
    fields.add(new DamlRecord.Field("assetAmount", new Numeric(this.assetAmount)));
    fields.add(new DamlRecord.Field("cashInstrument", new Text(this.cashInstrument)));
    fields.add(new DamlRecord.Field("cashAmount", new Numeric(this.cashAmount)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<DvPProposal> templateValueDecoder() throws IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(9,0, recordValue$);
      String proposer = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String counterparty = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String auditor = PrimitiveValueDecoders.fromParty.decode(fields$.get(2).getValue());
      Holding.ContractId assetHoldingCid =
          new Holding.ContractId(fields$.get(3).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected assetHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      Holding.ContractId cashHoldingCid =
          new Holding.ContractId(fields$.get(4).getValue().asContractId().orElseThrow(() -> new IllegalArgumentException("Expected cashHoldingCid to be of type com.daml.ledger.javaapi.data.ContractId")).getValue());
      String assetInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(5).getValue());
      BigDecimal assetAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(6).getValue());
      String cashInstrument = PrimitiveValueDecoders.fromText.decode(fields$.get(7).getValue());
      BigDecimal cashAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(8).getValue());
      return new DvPProposal(proposer, counterparty, auditor, assetHoldingCid, cashHoldingCid,
          assetInstrument, assetAmount, cashInstrument, cashAmount);
    } ;
  }

  public static JsonLfDecoder<DvPProposal> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("proposer", "counterparty", "auditor", "assetHoldingCid", "cashHoldingCid", "assetInstrument", "assetAmount", "cashInstrument", "cashAmount"), name -> {
          switch (name) {
            case "proposer": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "counterparty": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "auditor": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "assetHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            case "cashHoldingCid": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(4, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.contractId(com.lucilla.settlement.model.holding.Holding.ContractId::new));
            case "assetInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(5, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "assetAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(6, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            case "cashInstrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(7, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "cashAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(8, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new DvPProposal(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3]), JsonLfDecoders.cast(args[4]), JsonLfDecoders.cast(args[5]), JsonLfDecoders.cast(args[6]), JsonLfDecoders.cast(args[7]), JsonLfDecoders.cast(args[8])));
  }

  public static DvPProposal fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("proposer", apply(JsonLfEncoders::party, proposer)),
        JsonLfEncoders.Field.of("counterparty", apply(JsonLfEncoders::party, counterparty)),
        JsonLfEncoders.Field.of("auditor", apply(JsonLfEncoders::party, auditor)),
        JsonLfEncoders.Field.of("assetHoldingCid", apply(JsonLfEncoders::contractId, assetHoldingCid)),
        JsonLfEncoders.Field.of("cashHoldingCid", apply(JsonLfEncoders::contractId, cashHoldingCid)),
        JsonLfEncoders.Field.of("assetInstrument", apply(JsonLfEncoders::text, assetInstrument)),
        JsonLfEncoders.Field.of("assetAmount", apply(JsonLfEncoders::numeric, assetAmount)),
        JsonLfEncoders.Field.of("cashInstrument", apply(JsonLfEncoders::text, cashInstrument)),
        JsonLfEncoders.Field.of("cashAmount", apply(JsonLfEncoders::numeric, cashAmount)));
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
    if (!(object instanceof DvPProposal)) {
      return false;
    }
    DvPProposal other = (DvPProposal) object;
    return Objects.equals(this.proposer, other.proposer) &&
        Objects.equals(this.counterparty, other.counterparty) &&
        Objects.equals(this.auditor, other.auditor) &&
        Objects.equals(this.assetHoldingCid, other.assetHoldingCid) &&
        Objects.equals(this.cashHoldingCid, other.cashHoldingCid) &&
        Objects.equals(this.assetInstrument, other.assetInstrument) &&
        Objects.equals(this.assetAmount, other.assetAmount) &&
        Objects.equals(this.cashInstrument, other.cashInstrument) &&
        Objects.equals(this.cashAmount, other.cashAmount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.proposer, this.counterparty, this.auditor, this.assetHoldingCid,
        this.cashHoldingCid, this.assetInstrument, this.assetAmount, this.cashInstrument,
        this.cashAmount);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.settlement.DvPProposal(%s, %s, %s, %s, %s, %s, %s, %s, %s)",
        this.proposer, this.counterparty, this.auditor, this.assetHoldingCid, this.cashHoldingCid,
        this.assetInstrument, this.assetAmount, this.cashInstrument, this.cashAmount);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<DvPProposal> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, DvPProposal, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<DvPProposal> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, DvPProposal> {
    public Contract(ContractId id, DvPProposal data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, DvPProposal> getCompanion() {
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

    default Update<Exercised<Unit>> exerciseWithdraw(Withdraw arg) {
      return makeExerciseCmd(CHOICE_Withdraw, arg);
    }

    default Update<Exercised<Unit>> exerciseWithdraw() {
      return exerciseWithdraw(new Withdraw());
    }

    default Update<Exercised<Unit>> exerciseReject(Reject arg) {
      return makeExerciseCmd(CHOICE_Reject, arg);
    }

    default Update<Exercised<Unit>> exerciseReject() {
      return exerciseReject(new Reject());
    }

    default Update<Exercised<DvPAgreement.ContractId>> exerciseAccept(Accept arg) {
      return makeExerciseCmd(CHOICE_Accept, arg);
    }

    default Update<Exercised<DvPAgreement.ContractId>> exerciseAccept() {
      return exerciseAccept(new Accept());
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, DvPProposal, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<DvPProposal> get() {
      return jsonDecoder();
    }
  }
}
