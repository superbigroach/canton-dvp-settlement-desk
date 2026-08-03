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
import com.lucilla.settlement.model.splice.api.token.allocationv1.AllocationSpecification;
import com.lucilla.settlement.model.splice.api.token.holdingv1.Holding;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import java.lang.Override;
import java.lang.String;
import java.time.Instant;
import java.util.List;

public final class AllocationFactory {
  public static final Identifier TEMPLATE_ID = new Identifier("#splice-api-token-allocation-instruction-v1", "Splice.Api.Token.AllocationInstructionV1", "AllocationFactory");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520", "Splice.Api.Token.AllocationInstructionV1", "AllocationFactory");

  public static final Identifier INTERFACE_ID = new Identifier("#splice-api-token-allocation-instruction-v1", "Splice.Api.Token.AllocationInstructionV1", "AllocationFactory");

  public static final Identifier INTERFACE_ID_WITH_PACKAGE_ID = new Identifier("275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520", "Splice.Api.Token.AllocationInstructionV1", "AllocationFactory");

  public static final String PACKAGE_ID = "275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520";

  public static final String PACKAGE_NAME = "splice-api-token-allocation-instruction-v1";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<AllocationFactory, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<AllocationFactory, AllocationFactory_Allocate, AllocationInstructionResult> CHOICE_AllocationFactory_Allocate = 
      Choice.create("AllocationFactory_Allocate", value$ -> value$.toValue(), value$ ->
        AllocationFactory_Allocate.valueDecoder().decode(value$), value$ ->
        AllocationInstructionResult.valueDecoder().decode(value$),
        new AllocationFactory_Allocate.JsonDecoder$().get(),
        new AllocationInstructionResult.JsonDecoder$().get(),
        AllocationFactory_Allocate::jsonEncoder, AllocationInstructionResult::jsonEncoder);

  public static final Choice<AllocationFactory, AllocationFactory_PublicFetch, AllocationFactoryView> CHOICE_AllocationFactory_PublicFetch = 
      Choice.create("AllocationFactory_PublicFetch", value$ -> value$.toValue(), value$ ->
        AllocationFactory_PublicFetch.valueDecoder().decode(value$), value$ ->
        AllocationFactoryView.valueDecoder().decode(value$),
        new AllocationFactory_PublicFetch.JsonDecoder$().get(),
        new AllocationFactoryView.JsonDecoder$().get(), AllocationFactory_PublicFetch::jsonEncoder,
        AllocationFactoryView::jsonEncoder);

  public static final INTERFACE_ INTERFACE = new INTERFACE_();

  private AllocationFactory() {
  }

  public static ContractFilter<Contract<ContractId, AllocationFactoryView>> contractFilter() {
    return ContractFilter.of(INTERFACE);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<AllocationFactory> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AllocationFactory, ?> getCompanion(
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

    default Update<Exercised<AllocationInstructionResult>> exerciseAllocationFactory_Allocate(
        AllocationFactory_Allocate arg) {
      return makeExerciseCmd(CHOICE_AllocationFactory_Allocate, arg);
    }

    default Update<Exercised<AllocationInstructionResult>> exerciseAllocationFactory_Allocate(
        String expectedAdmin, AllocationSpecification allocation, Instant requestedAt,
        List<Holding.ContractId> inputHoldingCids, ExtraArgs extraArgs) {
      return exerciseAllocationFactory_Allocate(new AllocationFactory_Allocate(expectedAdmin,
          allocation, requestedAt, inputHoldingCids, extraArgs));
    }

    default Update<Exercised<AllocationFactoryView>> exerciseAllocationFactory_PublicFetch(
        AllocationFactory_PublicFetch arg) {
      return makeExerciseCmd(CHOICE_AllocationFactory_PublicFetch, arg);
    }

    default Update<Exercised<AllocationFactoryView>> exerciseAllocationFactory_PublicFetch(
        String expectedAdmin, String actor) {
      return exerciseAllocationFactory_PublicFetch(new AllocationFactory_PublicFetch(expectedAdmin,
          actor));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd.ToInterface implements Exercises<CreateAndExerciseCommand> {
    public CreateAnd(ContractCompanion<?, ?, ?> companion, Template createArguments) {
      super(companion, createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AllocationFactory, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class ByKey extends com.daml.ledger.javaapi.data.codegen.ByKey.ToInterface implements Exercises<ExerciseByKeyCommand> {
    public ByKey(ContractCompanion<?, ?, ?> companion, Value key) {
      super(companion, key);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AllocationFactory, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class INTERFACE_ extends InterfaceCompanion<AllocationFactory, ContractId, AllocationFactoryView> {
    INTERFACE_() {
      super(new ContractTypeCompanion.Package(AllocationFactory.PACKAGE_ID, AllocationFactory.PACKAGE_NAME, AllocationFactory.PACKAGE_VERSION),
            "com.lucilla.settlement.model.splice.api.token.allocationinstructionv1.AllocationFactory", AllocationFactory.TEMPLATE_ID, ContractId::new, AllocationFactoryView.valueDecoder(),
            AllocationFactoryView::fromJson,List.of(CHOICE_Archive,
            CHOICE_AllocationFactory_Allocate, CHOICE_AllocationFactory_PublicFetch));
    }
  }
}
