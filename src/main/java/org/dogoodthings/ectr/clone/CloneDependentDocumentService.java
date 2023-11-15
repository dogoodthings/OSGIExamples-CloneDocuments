package org.dogoodthings.ectr.clone;

import com.dscsag.plm.spi.interfaces.DocumentKey;
import com.dscsag.plm.spi.interfaces.ECTRService;
import com.dscsag.plm.spi.interfaces.objects.PlmObjectKey;
import com.dscsag.plm.spi.interfaces.process.PluginProcessContainer;
import com.dscsag.plm.spi.interfaces.process.extensions.CloneDocumentsExtensionService;
import com.dscsag.plm.spi.interfaces.rfc.RfcCall;
import com.dscsag.plm.spi.interfaces.rfc.RfcResult;
import com.dscsag.plm.spi.interfaces.rfc.RfcStructure;
import com.dscsag.plm.spi.interfaces.services.document.key.KeyConverterService;
import com.dscsag.plm.spi.rfc.builder2.FmCallBuilder;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;


public class CloneDependentDocumentService implements CloneDocumentsExtensionService {

  private static final Set<String> ERROR_RET_TYPES = Set.of("A", "X", "E");
  private final ECTRService ectrService;
  private final KeyConverterService keyConverter;

  public CloneDependentDocumentService(ECTRService ectrService, KeyConverterService keyConverter) {
    this.ectrService = ectrService;
    this.keyConverter = keyConverter;
  }

  @Override
  public Consumer<PluginProcessContainer> onDetermineDependentDocuments() {
    return container -> {
      var cloneEntry = container.getParameter(DATAKEY_CLONE_ENTRY);
      ectrService.getPlmLogger().debug("CloneDependentDocumentService::onDetermineDependentDocuments(): called for " + cloneEntry.masterKey());
      var depDocToClone = findTheChosenOneDependentDocument(cloneEntry.masterKey());
      if (depDocToClone != null) {
        container.setParameter(DATAKEY_DEPENDENT_DOCUMENT_KEYS, Collections.singleton(depDocToClone));
      }
    };
  }

  private PlmObjectKey findTheChosenOneDependentDocument(PlmObjectKey cloneDocumentKey) {
    PlmObjectKey depDocToClone = null;
    FmCallBuilder builder = new FmCallBuilder("BAPI_DOCUMENT_WHEREUSED");
    DocumentKey documentKey = keyConverter.fromPlmObjectKey(cloneDocumentKey);

    RfcCall rfcCall = builder.importing()
        .scalar("DOCUMENTTYPE", documentKey.getType())
        .scalar("DOCUMENTNUMBER", documentKey.getNumber())
        .scalar("DOCUMENTVERSION", documentKey.getVersion())
        .scalar("DOCUMENTPART", documentKey.getPart()).build();
    RfcResult result = ectrService.getRfcExecutor().execute(rfcCall);
    RfcStructure returnStructure = result.getExportParameter("RETURN").getStructure();
    String retType = returnStructure.getFieldValue("TYPE");
    if (ERROR_RET_TYPES.contains(retType))
      ectrService.getPlmLogger().error("BAPI_DOCUMENT_WHEREUSED has returned an error, s. apilog, message: " + returnStructure.getFieldValue("MESSAGE"));
    else {
      var docStructureTable = result.getTable("DOCUMENTSTRUCTURE");
      int rowCount = docStructureTable.getRowCount();
      for (int i = 0; i < rowCount; i++) {
        RfcStructure tableRow = docStructureTable.getRow(i);
        String rDocType = tableRow.getFieldValue("DOCUMENTTYPE");
        String rDocNumber = tableRow.getFieldValue("DOCUMENTNUMBER");
        String rDocVersion = tableRow.getFieldValue("DOCUMENTVERSION");
        String rDocPart = tableRow.getFieldValue("DOCUMENTPART");
        String rSortString = tableRow.getFieldValue("SORTSTRING");
        String rCadPos = tableRow.getFieldValue("CAD_POS");
        if (documentKey.getType().equals(rDocType) && "X".equals(rCadPos) && !"D".equals(rSortString)) {
          depDocToClone = keyConverter.plmObjectKeyForDocument(rDocType, rDocNumber, rDocVersion, rDocPart);
        }
      }
    }
    return depDocToClone;
  }
}
