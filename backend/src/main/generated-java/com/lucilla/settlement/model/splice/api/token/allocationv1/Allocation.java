package com.lucilla.settlement.model.splice.api.token.allocationv1;

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

public final class Allocation {
  public static final Identifier TEMPLATE_ID = new Identifier("#splice-api-token-allocation-v1", "Splice.Api.Token.AllocationV1", "Allocation");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d", "Splice.Api.Token.AllocationV1", "Allocation");

  public static final Identifier INTERFACE_ID = new Identifier("#splice-api-token-allocation-v1", "Splice.Api.Token.AllocationV1", "Allocation");

  public static final Identifier INTERFACE_ID_WITH_PACKAGE_ID = new Identifier("93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d", "Splice.Api.Token.AllocationV1", "Allocation");

  public static final String PACKAGE_ID = "93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d";

  public static final String PACKAGE_NAME = "splice-api-token-allocation-v1";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<Allocation, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<Allocation, Allocation_Withdraw, Allocation_WithdrawResult> CHOICE_Allocation_Withdraw = 
      Choice.create("Allocation_Withdraw", value$ -> value$.toValue(), value$ ->
        Allocation_Withdraw.valueDecoder().decode(value$), value$ ->
        Allocation_WithdrawResult.valueDecoder().decode(value$),
        new Allocation_Withdraw.JsonDecoder$().get(),
        new Allocation_WithdrawResult.JsonDecoder$().get(), Allocation_Withdraw::jsonEncoder,
        Allocation_WithdrawResult::jsonEncoder);

  public static final Choice<Allocation, Allocation_Cancel, Allocation_CancelResult> CHOICE_Allocation_Cancel = 
      Choice.create("Allocation_Cancel", value$ -> value$.toValue(), value$ ->
        Allocation_Cancel.valueDecoder().decode(value$), value$ ->
        Allocation_CancelResult.valueDecoder().decode(value$),
        new Allocation_Cancel.JsonDecoder$().get(),
        new Allocation_CancelResult.JsonDecoder$().get(), Allocation_Cancel::jsonEncoder,
        Allocation_CancelResult::jsonEncoder);

  public static final Choice<Allocation, Allocation_ExecuteTransfer, Allocation_ExecuteTransferResult> CHOICE_Allocation_ExecuteTransfer = 
      Choice.create("Allocation_ExecuteTransfer", value$ -> value$.toValue(), value$ ->
        Allocation_ExecuteTransfer.valueDecoder().decode(value$), value$ ->
        Allocation_ExecuteTransferResult.valueDecoder().decode(value$),
        new Allocation_ExecuteTransfer.JsonDecoder$().get(),
        new Allocation_ExecuteTransferResult.JsonDecoder$().get(),
        Allocation_ExecuteTransfer::jsonEncoder, Allocation_ExecuteTransferResult::jsonEncoder);

  public static final INTERFACE_ INTERFACE = new INTERFACE_();

  private Allocation() {
  }

  public static ContractFilter<Contract<ContractId, AllocationView>> contractFilter() {
    return ContractFilter.of(INTERFACE);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<Allocation> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, Allocation, ?> getCompanion(
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

    default Update<Exercised<Allocation_WithdrawResult>> exerciseAllocation_Withdraw(
        Allocation_Withdraw arg) {
      return makeExerciseCmd(CHOICE_Allocation_Withdraw, arg);
    }

    default Update<Exercised<Allocation_WithdrawResult>> exerciseAllocation_Withdraw(
        ExtraArgs extraArgs) {
      return exerciseAllocation_Withdraw(new Allocation_Withdraw(extraArgs));
    }

    default Update<Exercised<Allocation_CancelResult>> exerciseAllocation_Cancel(
        Allocation_Cancel arg) {
      return makeExerciseCmd(CHOICE_Allocation_Cancel, arg);
    }

    default Update<Exercised<Allocation_CancelResult>> exerciseAllocation_Cancel(
        ExtraArgs extraArgs) {
      return exerciseAllocation_Cancel(new Allocation_Cancel(extraArgs));
    }

    default Update<Exercised<Allocation_ExecuteTransferResult>> exerciseAllocation_ExecuteTransfer(
        Allocation_ExecuteTransfer arg) {
      return makeExerciseCmd(CHOICE_Allocation_ExecuteTransfer, arg);
    }

    default Update<Exercised<Allocation_ExecuteTransferResult>> exerciseAllocation_ExecuteTransfer(
        ExtraArgs extraArgs) {
      return exerciseAllocation_ExecuteTransfer(new Allocation_ExecuteTransfer(extraArgs));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd.ToInterface implements Exercises<CreateAndExerciseCommand> {
    public CreateAnd(ContractCompanion<?, ?, ?> companion, Template createArguments) {
      super(companion, createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, Allocation, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class ByKey extends com.daml.ledger.javaapi.data.codegen.ByKey.ToInterface implements Exercises<ExerciseByKeyCommand> {
    public ByKey(ContractCompanion<?, ?, ?> companion, Value key) {
      super(companion, key);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, Allocation, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class INTERFACE_ extends InterfaceCompanion<Allocation, ContractId, AllocationView> {
    INTERFACE_() {
      super(new ContractTypeCompanion.Package(Allocation.PACKAGE_ID, Allocation.PACKAGE_NAME, Allocation.PACKAGE_VERSION),
            "com.lucilla.settlement.model.splice.api.token.allocationv1.Allocation", Allocation.TEMPLATE_ID, ContractId::new, AllocationView.valueDecoder(),
            AllocationView::fromJson,List.of(CHOICE_Archive, CHOICE_Allocation_Withdraw,
            CHOICE_Allocation_Cancel, CHOICE_Allocation_ExecuteTransfer));
    }
  }
}
