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

public final class TransferFactory {
  public static final Identifier TEMPLATE_ID = new Identifier("#splice-api-token-transfer-instruction-v1", "Splice.Api.Token.TransferInstructionV1", "TransferFactory");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281", "Splice.Api.Token.TransferInstructionV1", "TransferFactory");

  public static final Identifier INTERFACE_ID = new Identifier("#splice-api-token-transfer-instruction-v1", "Splice.Api.Token.TransferInstructionV1", "TransferFactory");

  public static final Identifier INTERFACE_ID_WITH_PACKAGE_ID = new Identifier("55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281", "Splice.Api.Token.TransferInstructionV1", "TransferFactory");

  public static final String PACKAGE_ID = "55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281";

  public static final String PACKAGE_NAME = "splice-api-token-transfer-instruction-v1";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<TransferFactory, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<TransferFactory, TransferFactory_Transfer, TransferInstructionResult> CHOICE_TransferFactory_Transfer = 
      Choice.create("TransferFactory_Transfer", value$ -> value$.toValue(), value$ ->
        TransferFactory_Transfer.valueDecoder().decode(value$), value$ ->
        TransferInstructionResult.valueDecoder().decode(value$),
        new TransferFactory_Transfer.JsonDecoder$().get(),
        new TransferInstructionResult.JsonDecoder$().get(), TransferFactory_Transfer::jsonEncoder,
        TransferInstructionResult::jsonEncoder);

  public static final Choice<TransferFactory, TransferFactory_PublicFetch, TransferFactoryView> CHOICE_TransferFactory_PublicFetch = 
      Choice.create("TransferFactory_PublicFetch", value$ -> value$.toValue(), value$ ->
        TransferFactory_PublicFetch.valueDecoder().decode(value$), value$ ->
        TransferFactoryView.valueDecoder().decode(value$),
        new TransferFactory_PublicFetch.JsonDecoder$().get(),
        new TransferFactoryView.JsonDecoder$().get(), TransferFactory_PublicFetch::jsonEncoder,
        TransferFactoryView::jsonEncoder);

  public static final INTERFACE_ INTERFACE = new INTERFACE_();

  private TransferFactory() {
  }

  public static ContractFilter<Contract<ContractId, TransferFactoryView>> contractFilter() {
    return ContractFilter.of(INTERFACE);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<TransferFactory> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, TransferFactory, ?> getCompanion(
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

    default Update<Exercised<TransferInstructionResult>> exerciseTransferFactory_Transfer(
        TransferFactory_Transfer arg) {
      return makeExerciseCmd(CHOICE_TransferFactory_Transfer, arg);
    }

    default Update<Exercised<TransferInstructionResult>> exerciseTransferFactory_Transfer(
        String expectedAdmin, Transfer transfer, ExtraArgs extraArgs) {
      return exerciseTransferFactory_Transfer(new TransferFactory_Transfer(expectedAdmin, transfer,
          extraArgs));
    }

    default Update<Exercised<TransferFactoryView>> exerciseTransferFactory_PublicFetch(
        TransferFactory_PublicFetch arg) {
      return makeExerciseCmd(CHOICE_TransferFactory_PublicFetch, arg);
    }

    default Update<Exercised<TransferFactoryView>> exerciseTransferFactory_PublicFetch(
        String expectedAdmin, String actor) {
      return exerciseTransferFactory_PublicFetch(new TransferFactory_PublicFetch(expectedAdmin,
          actor));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd.ToInterface implements Exercises<CreateAndExerciseCommand> {
    public CreateAnd(ContractCompanion<?, ?, ?> companion, Template createArguments) {
      super(companion, createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, TransferFactory, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class ByKey extends com.daml.ledger.javaapi.data.codegen.ByKey.ToInterface implements Exercises<ExerciseByKeyCommand> {
    public ByKey(ContractCompanion<?, ?, ?> companion, Value key) {
      super(companion, key);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, TransferFactory, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class INTERFACE_ extends InterfaceCompanion<TransferFactory, ContractId, TransferFactoryView> {
    INTERFACE_() {
      super(new ContractTypeCompanion.Package(TransferFactory.PACKAGE_ID, TransferFactory.PACKAGE_NAME, TransferFactory.PACKAGE_VERSION),
            "com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferFactory", TransferFactory.TEMPLATE_ID, ContractId::new, TransferFactoryView.valueDecoder(),
            TransferFactoryView::fromJson,List.of(CHOICE_Archive, CHOICE_TransferFactory_Transfer,
            CHOICE_TransferFactory_PublicFetch));
    }
  }
}
