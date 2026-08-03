package com.lucilla.settlement.model.splice.api.token.allocationinstructionv1;

import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreateAndExerciseCommand;
import com.daml.ledger.javaapi.data.ExerciseByKeyCommand;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.PackageVersion;
import com.daml.ledger.javaapi.data.Template;
import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.javaapi.data.Value;
import com.daml.ledger.javaapi.data.codegen.Choice;
import com.daml.ledger.javaapi.data.codegen.Contract;
import com.daml.ledger.javaapi.data.codegen.ContractCompanion;
import com.daml.ledger.javaapi.data.codegen.ContractTypeCompanion;
import com.daml.ledger.javaapi.data.codegen.Exercised;
import com.daml.ledger.javaapi.data.codegen.InterfaceCompanion;
import com.daml.ledger.javaapi.data.codegen.PrimitiveValueDecoders;
import com.daml.ledger.javaapi.data.codegen.Update;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfDecoders;
import com.daml.ledger.javaapi.data.codegen.json.JsonLfEncoders;
import com.lucilla.settlement.model.da.internal.template.Archive;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public final class AllocationInstruction {
  public static final Identifier TEMPLATE_ID = new Identifier("#splice-api-token-allocation-instruction-v1", "Splice.Api.Token.AllocationInstructionV1", "AllocationInstruction");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520", "Splice.Api.Token.AllocationInstructionV1", "AllocationInstruction");

  public static final Identifier INTERFACE_ID = new Identifier("#splice-api-token-allocation-instruction-v1", "Splice.Api.Token.AllocationInstructionV1", "AllocationInstruction");

  public static final Identifier INTERFACE_ID_WITH_PACKAGE_ID = new Identifier("275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520", "Splice.Api.Token.AllocationInstructionV1", "AllocationInstruction");

  public static final String PACKAGE_ID = "275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520";

  public static final String PACKAGE_NAME = "splice-api-token-allocation-instruction-v1";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<AllocationInstruction, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<AllocationInstruction, AllocationInstruction_Withdraw, AllocationInstructionResult> CHOICE_AllocationInstruction_Withdraw = 
      Choice.create("AllocationInstruction_Withdraw", value$ -> value$.toValue(), value$ ->
        AllocationInstruction_Withdraw.valueDecoder().decode(value$), value$ ->
        AllocationInstructionResult.valueDecoder().decode(value$),
        new AllocationInstruction_Withdraw.JsonDecoder$().get(),
        new AllocationInstructionResult.JsonDecoder$().get(),
        AllocationInstruction_Withdraw::jsonEncoder, AllocationInstructionResult::jsonEncoder);

  public static final Choice<AllocationInstruction, AllocationInstruction_Update, AllocationInstructionResult> CHOICE_AllocationInstruction_Update = 
      Choice.create("AllocationInstruction_Update", value$ -> value$.toValue(), value$ ->
        AllocationInstruction_Update.valueDecoder().decode(value$), value$ ->
        AllocationInstructionResult.valueDecoder().decode(value$),
        new AllocationInstruction_Update.JsonDecoder$().get(),
        new AllocationInstructionResult.JsonDecoder$().get(),
        AllocationInstruction_Update::jsonEncoder, AllocationInstructionResult::jsonEncoder);

  public static final INTERFACE_ INTERFACE = new INTERFACE_();

  private AllocationInstruction() {
  }

  public static ContractFilter<Contract<ContractId, AllocationInstructionView>> contractFilter() {
    return ContractFilter.of(INTERFACE);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<AllocationInstruction> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AllocationInstruction, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public interface Exercises<Cmd> extends com.daml.ledger.javaapi.data.codegen.Exercises.Archivable<Cmd> {
    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<AllocationInstructionResult>> exerciseAllocationInstruction_Withdraw(
        AllocationInstruction_Withdraw arg) {
      return makeExerciseCmd(CHOICE_AllocationInstruction_Withdraw, arg);
    }

    default Update<Exercised<AllocationInstructionResult>> exerciseAllocationInstruction_Withdraw(
        ExtraArgs extraArgs) {
      return exerciseAllocationInstruction_Withdraw(new AllocationInstruction_Withdraw(extraArgs));
    }

    default Update<Exercised<AllocationInstructionResult>> exerciseAllocationInstruction_Update(
        AllocationInstruction_Update arg) {
      return makeExerciseCmd(CHOICE_AllocationInstruction_Update, arg);
    }

    default Update<Exercised<AllocationInstructionResult>> exerciseAllocationInstruction_Update(
        List<String> extraActors, ExtraArgs extraArgs) {
      return exerciseAllocationInstruction_Update(new AllocationInstruction_Update(extraActors,
          extraArgs));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd.ToInterface implements Exercises<CreateAndExerciseCommand> {
    public CreateAnd(ContractCompanion<?, ?, ?> companion, Template createArguments) {
      super(companion, createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AllocationInstruction, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class ByKey extends com.daml.ledger.javaapi.data.codegen.ByKey.ToInterface implements Exercises<ExerciseByKeyCommand> {
    public ByKey(ContractCompanion<?, ?, ?> companion, Value key) {
      super(companion, key);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AllocationInstruction, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class INTERFACE_ extends InterfaceCompanion<AllocationInstruction, ContractId, AllocationInstructionView> {
    INTERFACE_() {
      super(new ContractTypeCompanion.Package(AllocationInstruction.PACKAGE_ID, AllocationInstruction.PACKAGE_NAME, AllocationInstruction.PACKAGE_VERSION),
            "com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationInstruction", AllocationInstruction.TEMPLATE_ID, ContractId::new, AllocationInstructionView.valueDecoder(),
            AllocationInstructionView::fromJson,List.of(CHOICE_Archive,
            CHOICE_AllocationInstruction_Withdraw, CHOICE_AllocationInstruction_Update));
    }
  }
}
