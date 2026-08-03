package com.lucilla.settlement.model.splice.api.token.metadatav1;

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
import java.lang.Override;
import java.lang.String;
import java.util.List;

public final class AnyContract {
  public static final Identifier TEMPLATE_ID = new Identifier("#splice-api-token-metadata-v1", "Splice.Api.Token.MetadataV1", "AnyContract");

  public static final Identifier TEMPLATE_ID_WITH_PACKAGE_ID = new Identifier("4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f", "Splice.Api.Token.MetadataV1", "AnyContract");

  public static final Identifier INTERFACE_ID = new Identifier("#splice-api-token-metadata-v1", "Splice.Api.Token.MetadataV1", "AnyContract");

  public static final Identifier INTERFACE_ID_WITH_PACKAGE_ID = new Identifier("4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f", "Splice.Api.Token.MetadataV1", "AnyContract");

  public static final String PACKAGE_ID = "4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f";

  public static final String PACKAGE_NAME = "splice-api-token-metadata-v1";

  public static final PackageVersion PACKAGE_VERSION = new PackageVersion(new int[] {1, 0, 0});

  public static final Choice<AnyContract, Archive, Unit> CHOICE_Archive = 
      Choice.create("Archive", value$ -> value$.toValue(), value$ -> Archive.valueDecoder()
        .decode(value$), value$ -> PrimitiveValueDecoders.fromUnit.decode(value$),
        new Archive.JsonDecoder$().get(), JsonLfDecoders.unit, Archive::jsonEncoder,
        JsonLfEncoders::unit);

  public static final INTERFACE_ INTERFACE = new INTERFACE_();

  private AnyContract() {
  }

  public static ContractFilter<Contract<ContractId, AnyContractView>> contractFilter() {
    return ContractFilter.of(INTERFACE);
  }

  public static final class ContractId extends com.daml.ledger.javaapi.data.codegen.ContractId<AnyContract> implements Exercises<ExerciseCommand> {
    public ContractId(String contractId) {
      super(contractId);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AnyContract, ?> getCompanion(
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
  }

  public static final class CreateAnd extends com.daml.ledger.javaapi.data.codegen.CreateAnd.ToInterface implements Exercises<CreateAndExerciseCommand> {
    public CreateAnd(ContractCompanion<?, ?, ?> companion, Template createArguments) {
      super(companion, createArguments);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AnyContract, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class ByKey extends com.daml.ledger.javaapi.data.codegen.ByKey.ToInterface implements Exercises<ExerciseByKeyCommand> {
    public ByKey(ContractCompanion<?, ?, ?> companion, Value key) {
      super(companion, key);
    }

    @Override
    protected ContractTypeCompanion<? extends Contract<ContractId, ?>, ContractId, AnyContract, ?> getCompanion(
        ) {
      return INTERFACE;
    }
  }

  public static final class INTERFACE_ extends InterfaceCompanion<AnyContract, ContractId, AnyContractView> {
    INTERFACE_() {
      super(new ContractTypeCompanion.Package(AnyContract.PACKAGE_ID, AnyContract.PACKAGE_NAME, AnyContract.PACKAGE_VERSION),
            "com.lucilla.settlement.model.splice.api.token.metadatav1.AnyContract", AnyContract.TEMPLATE_ID, ContractId::new, AnyContractView.valueDecoder(),
            AnyContractView::fromJson,List.of(CHOICE_Archive));
    }
  }
}
