package org.dogoodthings.ectr.clone;

import com.dscsag.plm.spi.interfaces.ECTRService;
import com.dscsag.plm.spi.interfaces.objects.PlmObjectKey;
import com.dscsag.plm.spi.interfaces.process.PluginProcessContainer;
import com.dscsag.plm.spi.interfaces.process.extensions.CloneDocumentsExtensionService;
import com.dscsag.plm.spi.interfaces.rfc.RfcCall;
import com.dscsag.plm.spi.interfaces.rfc.RfcResult;
import com.dscsag.plm.spi.interfaces.rfc.RfcStructure;
import com.dscsag.plm.spi.rfc.builder2.FmCallBuilder;
import com.google.auto.service.AutoService;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@AutoService(CloneDocumentsExtensionService.class)
public class CloneDependentDocumentService implements CloneDocumentsExtensionService {

  private static final Pattern PATTERN = Pattern.compile("(...)(.{25})(.{2})(.*)");
  private static final Set<String> ERROR_RET_TYPES = Set.of("A", "X", "E");
  private final ECTRService ectrService;

  public CloneDependentDocumentService(ECTRService ectrService) {
    this.ectrService = ectrService;
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
    Matcher m = PATTERN.matcher(cloneDocumentKey.getKey());
    if (m.matches()) {
      String type = m.group(1);
      String number = m.group(2);
      String version = m.group(3);
      String part = m.group(4);
      RfcCall rfcCall = builder.importing()
          .scalar("DOCUMENTTYPE", type)
          .scalar("DOCUMENTNUMBER", number)
          .scalar("DOCUMENTVERSION", version)
          .scalar("DOCUMENTPART", part).build();
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
          if (type.equals(rDocType) && "X".equals(rCadPos) && !"D".equals(rSortString)) {
            depDocToClone = new PlmObjectKey("DRAW", rDocType + rDocNumber + rDocPart + rDocVersion); // the sap key has to be build lie in sap!
          }
        }
      }
    }
    return depDocToClone;
  }


}