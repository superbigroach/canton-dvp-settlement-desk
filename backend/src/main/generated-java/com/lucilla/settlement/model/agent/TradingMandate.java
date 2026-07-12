package com.lucilla.settlement.model.agent;

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
import com.lucilla.settlement.model.settlement.DvPProposal;
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

public final class TradingMandate extends Template {
  public static final Identifier TEMPLATE_ID = new Identifier("686f466cc26deeb74b73fb7f5ee448959dec397b966196641a6e0925ac652c79", "Agent", "TradingMandate");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("686f466cc26deeb74b73fb7f5ee448959dec397b966196641a6e0925ac652c79", "Agent", "TradingMandate");

  public static final String PACKAGE_ID = "686f466cc26deeb74b73fb7f5ee448959dec397b966196641a6e0925ac652c79";

  public static final Choice<TradingMandate, InitiateDvP, DvPProposal.ContractId> CHOICE_InitiateDvP = 
      Choice.create("InitiateDvP", value$ -> value$.toValue(), value$ -> InitiateDvP.valueDecoder()
        .decode(value$), value$ ->
        new DvPProposal.ContractId(value$.asContractId().orElseThrow(() -> new IllegalArgumentException("Expected value$ to be of type com.daml.ledger.javaapi.data.ContractId")).getValue()));

  public static final Choice<TradingMandate, com.lucilla.settlement.model.da.internal.template.Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ ->
        com.lucilla.settlement.model.da.internal.template.Archive.valueDecoder().decode(value$),
        value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final Choice<TradingMandate, Revoke, Unit> CHOICE_Revoke = 
      Choice.create("Revoke", value$ -> value$.toValue(), value$ -> Revoke.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$));

  public static final ContractCompanion.WithoutKey<Contract, ContractId, TradingMandate> COMPANION = 
      new ContractCompanion.WithoutKey<>("com.lucilla.settlement.model.agent.TradingMandate",
        TEMPLATE_ID, TEMPLATE_ID_WITH_PACKAGE_ID, ContractId::new,
        v -> TradingMandate.templateValueDecoder().decode(v), TradingMandate::fromJson,
        Contract::new, List.of(CHOICE_InitiateDvP, CHOICE_Archive, CHOICE_Revoke));

  public static final String PACKAGE_NAME = "canton-dvp-settlement-desk";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public final String principal;

  public final String agent;

  public final String instrument;

  public final BigDecimal maxAmount;

  public TradingMandate(String principal, String agent, String instrument, BigDecimal maxAmount) {
    this.principal = principal;
    this.agent = agent;
    this.instrument = instrument;
    this.maxAmount = maxAmount;
  }

  @Override
  public Update<Created<ContractId>> create() {
    return new Update.CreateUpdate<ContractId, Created<ContractId>>(new CreateCommand(TradingMandate.TEMPLATE_ID, this.toValue()), x -> x, ContractId::new);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseInitiateDvP} instead
   */
  @Deprecated
  public Update<Exercised<DvPProposal.ContractId>> createAndExerciseInitiateDvP(InitiateDvP arg) {
    return createAnd().exerciseInitiateDvP(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseInitiateDvP} instead
   */
  @Deprecated
  public Update<Exercised<DvPProposal.ContractId>> createAndExerciseInitiateDvP(String counterparty,
      String auditor, Holding.ContractId assetHoldingCid, Holding.ContractId cashHoldingCid,
      BigDecimal assetAmount, String cashInstrument, BigDecimal cashAmount) {
    return createAndExerciseInitiateDvP(new InitiateDvP(counterparty, auditor, assetHoldingCid,
        cashHoldingCid, assetAmount, cashInstrument, cashAmount));
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
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRevoke} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseRevoke(Revoke arg) {
    return createAnd().exerciseRevoke(arg);
  }

  /**
   * @deprecated since Daml 2.3.0; use {@code createAnd().exerciseRevoke} instead
   */
  @Deprecated
  public Update<Exercised<Unit>> createAndExerciseRevoke() {
    return createAndExerciseRevoke(new Revoke());
  }

  public static Update<Created<ContractId>> create(String principal, String agent,
      String instrument, BigDecimal maxAmount) {
    return new TradingMandate(principal, agent, instrument, maxAmount).create();
  }

  @Override
  public CreateAnd createAnd() {
    return new CreateAnd(this);
  }

  @Override
  protected ContractCompanion.WithoutKey<Contract, ContractId, TradingMandate> getCompanion() {
    return COMPANION;
  }

  /**
   * @deprecated since Daml 2.5.0; use {@code valueDecoder} instead
   */
  @Deprecated
  public static TradingMandate fromValue(Value value$) throws IllegalArgumentException {
    return valueDecoder().decode(value$);
  }

  public static ValueDecoder<TradingMandate> valueDecoder() throws IllegalArgumentException {
    return ContractCompanion.valueDecoder(COMPANION);
  }

  public DamlRecord toValue() {
    ArrayList<DamlRecord.Field> fields = new ArrayList<DamlRecord.Field>(4);
    fields.add(new DamlRecord.Field("principal", new Party(this.principal)));
    fields.add(new DamlRecord.Field("agent", new Party(this.agent)));
    fields.add(new DamlRecord.Field("instrument", new Text(this.instrument)));
    fields.add(new DamlRecord.Field("maxAmount", new Numeric(this.maxAmount)));
    return new DamlRecord(fields);
  }

  private static ValueDecoder<TradingMandate> templateValueDecoder() throws
      IllegalArgumentException {
    return value$ -> {
      Value recordValue$ = value$;
      List<DamlRecord.Field> fields$ = PrimitiveValueDecoders.recordCheck(4,0, recordValue$);
      String principal = PrimitiveValueDecoders.fromParty.decode(fields$.get(0).getValue());
      String agent = PrimitiveValueDecoders.fromParty.decode(fields$.get(1).getValue());
      String instrument = PrimitiveValueDecoders.fromText.decode(fields$.get(2).getValue());
      BigDecimal maxAmount = PrimitiveValueDecoders.fromNumeric.decode(fields$.get(3).getValue());
      return new TradingMandate(principal, agent, instrument, maxAmount);
    } ;
  }

  public static JsonLfDecoder<TradingMandate> jsonDecoder() {
    return JsonLfDecoders.record(Arrays.asList("principal", "agent", "instrument", "maxAmount"), name -> {
          switch (name) {
            case "principal": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(0, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "agent": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(1, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.party);
            case "instrument": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(2, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.text);
            case "maxAmount": return com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.JavaArg.at(3, com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders.numeric(10));
            default: return null;
          }
        }
        , (Object[] args) -> new TradingMandate(JsonLfDecoders.cast(args[0]), JsonLfDecoders.cast(args[1]), JsonLfDecoders.cast(args[2]), JsonLfDecoders.cast(args[3])));
  }

  public static TradingMandate fromJson(String json) throws JsonLfDecoder.Error {
    return jsonDecoder().decode(new JsonLfReader(json));
  }

  public JsonLfEncoder jsonEncoder() {
    return JsonLfEncoders.record(
        JsonLfEncoders.Field.of("principal", apply(JsonLfEncoders::party, principal)),
        JsonLfEncoders.Field.of("agent", apply(JsonLfEncoders::party, agent)),
        JsonLfEncoders.Field.of("instrument", apply(JsonLfEncoders::text, instrument)),
        JsonLfEncoders.Field.of("maxAmount", apply(JsonLfEncoders::numeric, maxAmount)));
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
    if (!(object instanceof TradingMandate)) {
      return false;
    }
    TradingMandate other = (TradingMandate) object;
    return Objects.equals(this.principal, other.principal) &&
        Objects.equals(this.agent, other.agent) &&
        Objects.equals(this.instrument, other.instrument) &&
        Objects.equals(this.maxAmount, other.maxAmount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.principal, this.agent, this.instrument, this.maxAmount);
  }

  @Override
  public String toString() {
    return String.format("com.lucilla.settlement.model.agent.TradingMandate(%s, %s, %s, %s)",
        this.principal, this.agent, this.instrument, this.maxAmount);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TradingMandate> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TradingMandate, ?> getCompanion(
        ) {
      return COMPANION;
    }

    public static ContractId fromContractId(
        com.daml.ledger.javaapi.data.codegen.ContractId<TradingMandate> contractId) {
      return COMPANION.toContractId(contractId);
    }
  }

  public static class Contract extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, TradingMandate> {
    public Contract(ContractId id, TradingMandate data, Optional<String> agreementText,
        Set<String> signatories, Set<String> observers) {
      super(id, data, agreementText, signatories, observers);
    }

    @Override
    protected ContractCompanion<Contract, ContractId, TradingMandate> getCompanion() {
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
    default Update<Exercised<DvPProposal.ContractId>> exerciseInitiateDvP(InitiateDvP arg) {
      return makeExerciseCmd(CHOICE_InitiateDvP, arg);
    }

    default Update<Exercised<DvPProposal.ContractId>> exerciseInitiateDvP(String counterparty,
        String auditor, Holding.ContractId assetHoldingCid, Holding.ContractId cashHoldingCid,
        BigDecimal assetAmount, String cashInstrument, BigDecimal cashAmount) {
      return exerciseInitiateDvP(new InitiateDvP(counterparty, auditor, assetHoldingCid,
          cashHoldingCid, assetAmount, cashInstrument, cashAmount));
    }

    default Update<Exercised<Unit>> exerciseArchive(
        com.lucilla.settlement.model.da.internal.template.Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new com.lucilla.settlement.model.da.internal.template.Archive());
    }

    default Update<Exercised<Unit>> exerciseRevoke(Revoke arg) {
      return makeExerciseCmd(CHOICE_Revoke, arg);
    }

    default Update<Exercised<Unit>> exerciseRevoke() {
      return exerciseRevoke(new Revoke());
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd implements Exercises<CreateAndExerciseCommand> {
    CreateAnd(Template createArguments) {
      super(createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends com.daml.ledger.javaapi.data.codegen.Contract<ContractId, ?>, ContractId, TradingMandate, ?> getCompanion(
        ) {
      return COMPANION;
    }
  }

  /**
   * Proxies the jsonDecoder(...) static method, to provide an alternative calling synatx, which avoids some cases in generated code where javac gets confused
   */
  public static class JsonDecoder$ {
    public JsonLfDecoder<TradingMandate> get() {
      return jsonDecoder();
    }
  }
}
