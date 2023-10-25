package org.dogoodthings.ectr.clone;

import com.dscsag.plm.spi.interfaces.ECTRService;
import com.dscsag.plm.spi.interfaces.process.extensions.CloneDocumentsExtensionService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {
    public void start(BundleContext context) throws Exception {
        // first, get ECTRService which provides some common
        // useful services like logging, dictionary and so on.
        ECTRService ectrService = getService(context, ECTRService.class);

        context.registerService(CloneDocumentsExtensionService.class, new CloneDependentDocumentService(ectrService), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // empty
    }

    private <T> T getService(BundleContext context, Class<T> clazz) throws Exception {
        ServiceReference<T> serviceRef = context.getServiceReference(clazz);
        if (serviceRef != null)
            return context.getService(serviceRef);
        throw new Exception("Unable to find implementation for service " + clazz.getName());
    }
}
