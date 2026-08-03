package com.lucilla.settlement.model.splice.api.token.transferinstructionv1;

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

public final class TransferInstruction {
  public static final Identifier TEMPLATE_ID = new Identifier("#splice-api-token-transfer-instruction-v1", "Splice.Api.Token.TransferInstructionV1", "TransferInstruction");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281", "Splice.Api.Token.TransferInstructionV1", "TransferInstruction");

  public static final Identifier INTERFACE_ID = new Identifier("#splice-api-token-transfer-instruction-v1", "Splice.Api.Token.TransferInstructionV1", "TransferInstruction");

  public static final Identifier INTERFACE_ID_WITH_PACKAGE_ID = new Identifier("55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281", "Splice.Api.Token.TransferInstructionV1", "TransferInstruction");

  public static final String PACKAGE_ID = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public static final String PACKAGE_NAME = "splice-api-token-transfer-instruction-v1";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<TransferInstruction, TransferInstruction_Reject, TransferInstructionResult> CHOICE_TransferInstruction_Reject = 
      Choice.create("TransferInstruction_Reject", value$ -> value$.toValue(), value$ ->
        TransferInstruction_Reject.valueDecoder().decode(value$), value$ ->
        TransferInstructionResult.valueDecoder().decode(value$),
        new TransferInstruction_Reject.JsonDecoder$().get(),
        new TransferInstructionResult.JsonDecoder$().get(), TransferInstruction_Reject::jsonEncoder,
        TransferInstructionResult::jsonEncoder);

  public static final Choice<TransferInstruction, TransferInstruction_Update, TransferInstructionResult> CHOICE_TransferInstruction_Update = 
      Choice.create("TransferInstruction_Update", value$ -> value$.toValue(), value$ ->
        TransferInstruction_Update.valueDecoder().decode(value$), value$ ->
        TransferInstructionResult.valueDecoder().decode(value$),
        new TransferInstruction_Update.JsonDecoder$().get(),
        new TransferInstructionResult.JsonDecoder$().get(), TransferInstruction_Update::jsonEncoder,
        TransferInstructionResult::jsonEncoder);

  public static final Choice<TransferInstruction, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<TransferInstruction, TransferInstruction_Accept, TransferInstructionResult> CHOICE_TransferInstruction_Accept = 
      Choice.create("TransferInstruction_Accept", value$ -> value$.toValue(), value$ ->
        TransferInstruction_Accept.valueDecoder().decode(value$), value$ ->
        TransferInstructionResult.valueDecoder().decode(value$),
        new TransferInstruction_Accept.JsonDecoder$().get(),
        new TransferInstructionResult.JsonDecoder$().get(), TransferInstruction_Accept::jsonEncoder,
        TransferInstructionResult::jsonEncoder);

  public static final Choice<TransferInstruction, TransferInstruction_Withdraw, TransferInstructionResult> CHOICE_TransferInstruction_Withdraw = 
      Choice.create("TransferInstruction_Withdraw", value$ -> value$.toValue(), value$ ->
        TransferInstruction_Withdraw.valueDecoder().decode(value$), value$ ->
        TransferInstructionResult.valueDecoder().decode(value$),
        new TransferInstruction_Withdraw.JsonDecoder$().get(),
        new TransferInstructionResult.JsonDecoder$().get(),
        TransferInstruction_Withdraw::jsonEncoder, TransferInstructionResult::jsonEncoder);

  public static final INTERFACE_ INTERFACE = new INTERFACE_();

  private TransferInstruction() {
  }

  public static ContractFilter<Contract<ContractId, TransferInstructionView>> contractFilter() {
    return ContractFilter.of(INTERFACE);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TransferInstruction> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, TransferInstruction, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public interface Exercises<Cmd> extends com.daml.ledger.javaapi.data.codegen.Exercises.Archivable<Cmd> {
    default Update<Exercised<TransferInstructionResult>> exerciseTransferInstruction_Reject(
        TransferInstruction_Reject arg) {
      return makeExerciseCmd(CHOICE_TransferInstruction_Reject, arg);
    }

    default Update<Exercised<TransferInstructionResult>> exerciseTransferInstruction_Reject(
        ExtraArgs extraArgs) {
      return exerciseTransferInstruction_Reject(new TransferInstruction_Reject(extraArgs));
    }

    default Update<Exercised<TransferInstructionResult>> exerciseTransferInstruction_Update(
        TransferInstruction_Update arg) {
      return makeExerciseCmd(CHOICE_TransferInstruction_Update, arg);
    }

    default Update<Exercised<TransferInstructionResult>> exerciseTransferInstruction_Update(
        List<String> extraActors, ExtraArgs extraArgs) {
      return exerciseTransferInstruction_Update(new TransferInstruction_Update(extraActors,
          extraArgs));
    }

    default Update<Exercised<Unit>> exerciseArchive(Archive arg) {
      return makeExerciseCmd(CHOICE_Archive, arg);
    }

    default Update<Exercised<Unit>> exerciseArchive() {
      return exerciseArchive(new Archive());
    }

    default Update<Exercised<TransferInstructionResult>> exerciseTransferInstruction_Accept(
        TransferInstruction_Accept arg) {
      return makeExerciseCmd(CHOICE_TransferInstruction_Accept, arg);
    }

    default Update<Exercised<TransferInstructionResult>> exerciseTransferInstruction_Accept(
        ExtraArgs extraArgs) {
      return exerciseTransferInstruction_Accept(new TransferInstruction_Accept(extraArgs));
    }

    default Update<Exercised<TransferInstructionResult>> exerciseTransferInstruction_Withdraw(
        TransferInstruction_Withdraw arg) {
      return makeExerciseCmd(CHOICE_TransferInstruction_Withdraw, arg);
    }

    default Update<Exercised<TransferInstructionResult>> exerciseTransferInstruction_Withdraw(
        ExtraArgs extraArgs) {
      return exerciseTransferInstruction_Withdraw(new TransferInstruction_Withdraw(extraArgs));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd.ToInterface implements Exercises<CreateAndExerciseCommand> {
    public CreateAnd(ContractCompanion<?, ?, ?> companion, Template createArguments) {
      super(companion, createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, TransferInstruction, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class ByKey extends com.daml.ledger.javaapi.data.codegen.ByKey.ToInterface implements Exercises<ExerciseByKeyCommand> {
    public ByKey(ContractCompanion<?, ?, ?> companion, Value key) {
      super(companion, key);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, TransferInstruction, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class INTERFACE_ extends InterfaceCompanion<TransferInstruction, ContractId, TransferInstructionView> {
    INTERFACE_() {
      super(new ContractTypeCompanion.Package(TransferInstruction.PACKAGE_ID, TransferInstruction.PACKAGE_NAME, TransferInstruction.PACKAGE_VERSION),
            "com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstruction", TransferInstruction.TEMPLATE_ID, ContractId::new, TransferInstructionView.valueDecoder(),
            TransferInstructionView::fromJson,List.of(CHOICE_TransferInstruction_Update,
            CHOICE_Archive, CHOICE_TransferInstruction_Accept, CHOICE_TransferInstruction_Reject,
            CHOICE_TransferInstruction_Withdraw));
    }
  }
}
