package org.dogoodthings.ectr.clone;

import com.dscsag.plm.spi.interfaces.ECTRService;
import com.dscsag.plm.spi.interfaces.process.PluginProcessContainer;
import com.dscsag.plm.spi.interfaces.process.extensions.CloneDocumentsExtensionService;
import com.dscsag.plm.spi.interfaces.services.document.key.KeyConverterService;

import java.util.Collections;
import java.util.function.Consumer;


public class CloneDependentDocumentService implements CloneDocumentsExtensionService {
  private final ECTRService ectrService;

  public CloneDependentDocumentService(ECTRService ectrService, KeyConverterService keyConverter) {
    this.ectrService = ectrService;
    //this.keyConverter = keyConverter;
  }

  /**
   * take only the first drawing and drop all others
   */
  @Override
  public Consumer<PluginProcessContainer> onDetermineDependentDocuments() {
    return container -> {
      var cloneEntry = container.getParameter(DATAKEY_CLONE_ENTRY);
      ectrService.getPlmLogger().debug("CloneDependentDocumentService::onDetermineDependentDocuments(): called for " + cloneEntry.masterKey());
      var currentDependentDocuments = container.getParameter(DATAKEY_DEPENDENT_DOCUMENT_KEYS);
      if (currentDependentDocuments.size() > 1) {
        var highlander = currentDependentDocuments.iterator().next();
        container.setParameter(DATAKEY_OUT_DEPENDENT_DOCUMENT_KEYS, Collections.singleton(highlander));
        ectrService.getPlmLogger().debug("CloneDependentDocumentService::onDetermineDependentDocuments(): reduced from " + currentDependentDocuments.size() + " to only one:" + highlander);
      }
    };
  }
}
