package com.lucilla.settlement.model.splice.api.token.allocationrequestv1;

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
import com.lucilla.settlement.model.splice.api.token.metadatav1.ChoiceExecutionMetadata;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public final class AllocationRequest {
  public static final Identifier TEMPLATE_ID = new Identifier("#splice-api-token-allocation-request-v1", "Splice.Api.Token.AllocationRequestV1", "AllocationRequest");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("6fe848530b2404017c4a12874c956ad7d5c8a419ee9b040f96b5c13172d2e193", "Splice.Api.Token.AllocationRequestV1", "AllocationRequest");

  public static final Identifier INTERFACE_ID = new Identifier("#splice-api-token-allocation-request-v1", "Splice.Api.Token.AllocationRequestV1", "AllocationRequest");

  public static final Identifier INTERFACE_ID_WITH_PACKAGE_ID = new Identifier("6fe848530b2404017c4a12874c956ad7d5c8a419ee9b040f96b5c13172d2e193", "Splice.Api.Token.AllocationRequestV1", "AllocationRequest");

  public static final String PACKAGE_ID = "6fe848530b2404017c4a12874c956ad7d5c8a419ee9b040f96b5c13172d2e193";

  public static final String PACKAGE_NAME = "splice-api-token-allocation-request-v1";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<AllocationRequest, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final Choice<AllocationRequest, AllocationRequest_Reject, ChoiceExecutionMetadata> CHOICE_AllocationRequest_Reject = 
      Choice.create("AllocationRequest_Reject", value$ -> value$.toValue(), value$ ->
        AllocationRequest_Reject.valueDecoder().decode(value$), value$ ->
        ChoiceExecutionMetadata.valueDecoder().decode(value$),
        new AllocationRequest_Reject.JsonDecoder$().get(),
        new ChoiceExecutionMetadata.JsonDecoder$().get(), AllocationRequest_Reject::jsonEncoder,
        ChoiceExecutionMetadata::jsonEncoder);

  public static final Choice<AllocationRequest, AllocationRequest_Withdraw, ChoiceExecutionMetadata> CHOICE_AllocationRequest_Withdraw = 
      Choice.create("AllocationRequest_Withdraw", value$ -> value$.toValue(), value$ ->
        AllocationRequest_Withdraw.valueDecoder().decode(value$), value$ ->
        ChoiceExecutionMetadata.valueDecoder().decode(value$),
        new AllocationRequest_Withdraw.JsonDecoder$().get(),
        new ChoiceExecutionMetadata.JsonDecoder$().get(), AllocationRequest_Withdraw::jsonEncoder,
        ChoiceExecutionMetadata::jsonEncoder);

  public static final INTERFACE_ INTERFACE = new INTERFACE_();

  private AllocationRequest() {
  }

  public static ContractFilter<Contract<ContractId, AllocationRequestView>> contractFilter() {
    return ContractFilter.of(INTERFACE);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<AllocationRequest> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AllocationRequest, ?> getCompanion(
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

    default Update<Exercised<ChoiceExecutionMetadata>> exerciseAllocationRequest_Reject(
        AllocationRequest_Reject arg) {
      return makeExerciseCmd(CHOICE_AllocationRequest_Reject, arg);
    }

    default Update<Exercised<ChoiceExecutionMetadata>> exerciseAllocationRequest_Reject(
        String actor, ExtraArgs extraArgs) {
      return exerciseAllocationRequest_Reject(new AllocationRequest_Reject(actor, extraArgs));
    }

    default Update<Exercised<ChoiceExecutionMetadata>> exerciseAllocationRequest_Withdraw(
        AllocationRequest_Withdraw arg) {
      return makeExerciseCmd(CHOICE_AllocationRequest_Withdraw, arg);
    }

    default Update<Exercised<ChoiceExecutionMetadata>> exerciseAllocationRequest_Withdraw(
        ExtraArgs extraArgs) {
      return exerciseAllocationRequest_Withdraw(new AllocationRequest_Withdraw(extraArgs));
    }
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd.ToInterface implements Exercises<CreateAndExerciseCommand> {
    public CreateAnd(ContractCompanion<?, ?, ?> companion, Template createArguments) {
      super(companion, createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AllocationRequest, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class ByKey extends com.daml.ledger.javaapi.data.codegen.ByKey.ToInterface implements Exercises<ExerciseByKeyCommand> {
    public ByKey(ContractCompanion<?, ?, ?> companion, Value key) {
      super(companion, key);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AllocationRequest, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class INTERFACE_ extends InterfaceCompanion<AllocationRequest, ContractId, AllocationRequestView> {
    INTERFACE_() {
      super(new ContractTypeCompanion.Package(AllocationRequest.PACKAGE_ID, AllocationRequest.PACKAGE_NAME, AllocationRequest.PACKAGE_VERSION),
            "com.lucilla.settlement.model.splice.api.token.allocationrequestv1.AllocationRequest", AllocationRequest.TEMPLATE_ID, ContractId::new, AllocationRequestView.valueDecoder(),
            AllocationRequestView::fromJson,List.of(CHOICE_Archive, CHOICE_AllocationRequest_Reject,
            CHOICE_AllocationRequest_Withdraw));
    }
  }
}
